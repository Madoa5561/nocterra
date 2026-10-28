"""Persistent job queue (SQLite).

Features required by the pipeline spec:

* state machine ``pending -> running -> done | failed`` (plus ``cancelled``),
* exponential backoff with jitter and a per-job attempt ceiling,
* per-service rate limiting (minimum interval between calls, stored in SQLite so it
  also holds across processes),
* duplicate suppression through a content hash of the request,
* crash/interrupt resume: ``running`` jobs whose lease expired are put back to
  ``pending`` instead of being lost,
* immediate localisation of expiring result URLs: the successful artifact path and
  the source URL are stored in the job row as soon as they are known.
"""

from __future__ import annotations

import hashlib
import json
import random
import sqlite3
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

__all__ = ["JobQueue", "content_hash", "utc_now"]

_SCHEMA = """
CREATE TABLE IF NOT EXISTS jobs (
    id             TEXT PRIMARY KEY,
    kind           TEXT NOT NULL,
    service        TEXT NOT NULL,
    model          TEXT,
    content_hash   TEXT NOT NULL,
    payload_json   TEXT NOT NULL,
    status         TEXT NOT NULL DEFAULT 'pending'
                   CHECK (status IN ('pending','running','done','failed','cancelled')),
    attempts       INTEGER NOT NULL DEFAULT 0,
    max_attempts   INTEGER NOT NULL DEFAULT 3,
    next_run_at    REAL NOT NULL,
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL,
    started_at     TEXT,
    finished_at    TEXT,
    lease_until    REAL,
    remote_task_id TEXT,
    artifact_path  TEXT,
    source_url     TEXT,
    source_url_saved_at TEXT,
    error          TEXT,
    result_json    TEXT,
    dedupe_hits    INTEGER NOT NULL DEFAULT 0,
    notes          TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_jobs_content_hash ON jobs(content_hash);
CREATE INDEX IF NOT EXISTS ix_jobs_status ON jobs(status, next_run_at);
CREATE INDEX IF NOT EXISTS ix_jobs_service ON jobs(service);

CREATE TABLE IF NOT EXISTS job_events (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id     TEXT NOT NULL,
    ts         TEXT NOT NULL,
    event      TEXT NOT NULL,
    detail     TEXT
);
CREATE INDEX IF NOT EXISTS ix_events_job ON job_events(job_id);

CREATE TABLE IF NOT EXISTS service_rate (
    service         TEXT PRIMARY KEY,
    min_interval_s  REAL NOT NULL DEFAULT 0,
    last_started_at REAL NOT NULL DEFAULT 0
);
"""


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def content_hash(kind: str, service: str, model: str | None, payload: dict[str, Any]) -> str:
    """Stable hash of the *request*, ignoring volatile/local fields.

    Keys whose names start with ``_`` are treated as local metadata (notes, ids) and
    excluded so that re-running the same generation is detected as a duplicate.
    """
    canonical = json.dumps(
        {
            "kind": kind,
            "service": service,
            "model": model,
            "payload": {k: v for k, v in sorted(payload.items()) if not k.startswith("_")},
        },
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def backoff_delay(attempt: int, base_s: float = 5.0, cap_s: float = 900.0, jitter: bool = True) -> float:
    """Exponential backoff: ``base * 2**(attempt-1)`` capped, plus up to 30% jitter."""
    delay = min(base_s * (2 ** max(0, attempt - 1)), cap_s)
    if jitter:
        delay *= 1.0 + random.uniform(0.0, 0.3)
    return round(delay, 3)


class JobQueue:
    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(str(self.path), timeout=30.0)
        self._conn.row_factory = sqlite3.Row
        self._conn.executescript("PRAGMA journal_mode=WAL;")
        self._conn.executescript(_SCHEMA)
        self._conn.commit()

    def close(self) -> None:
        self._conn.close()

    def __enter__(self) -> "JobQueue":
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()

    # ---------------------------------------------------------------- events
    def _event(self, job_id: str, event: str, detail: str = "") -> None:
        self._conn.execute(
            "INSERT INTO job_events (job_id, ts, event, detail) VALUES (?,?,?,?)",
            (job_id, utc_now(), event, detail[:2000]),
        )

    # ------------------------------------------------------------------- add
    def add_job(
        self,
        *,
        kind: str,
        service: str,
        model: str | None,
        payload: dict[str, Any],
        job_id: str | None = None,
        max_attempts: int = 3,
        notes: str | None = None,
        request_id: str | None = None,
    ) -> tuple[str, bool]:
        """Enqueue a job. Returns ``(job_id, created)``; duplicates are reused."""
        digest = content_hash(kind, service, model, payload)
        existing = self._conn.execute(
            "SELECT id FROM jobs WHERE content_hash=?", (digest,)
        ).fetchone()
        if existing:
            self._conn.execute(
                "UPDATE jobs SET dedupe_hits = dedupe_hits + 1, updated_at=? WHERE id=?",
                (utc_now(), existing["id"]),
            )
            self._event(existing["id"], "dedupe", request_id or "duplicate request")
            self._conn.commit()
            return existing["id"], False

        jid = job_id or f"job_{int(time.time())}_{digest[:10]}"
        now = time.time()
        self._conn.execute(
            """
            INSERT INTO jobs
                (id, kind, service, model, content_hash, payload_json, status, attempts,
                 max_attempts, next_run_at, created_at, updated_at, notes)
            VALUES (?,?,?,?,?,?,'pending',0,?,?,?,?,?)
            """,
            (
                jid,
                kind,
                service,
                model,
                digest,
                json.dumps(payload, ensure_ascii=False, sort_keys=True),
                max(1, int(max_attempts)),
                now,
                utc_now(),
                utc_now(),
                notes,
            ),
        )
        self._event(jid, "created", f"{kind}/{service}")
        self._conn.commit()
        return jid, True

    # ---------------------------------------------------------------- query
    def get(self, job_id: str) -> dict[str, Any] | None:
        row = self._conn.execute("SELECT * FROM jobs WHERE id=?", (job_id,)).fetchone()
        return dict(row) if row else None

    def list_jobs(
        self,
        status: str | None = None,
        service: str | None = None,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        sql = "SELECT * FROM jobs WHERE 1=1"
        params: list[Any] = []
        if status:
            sql += " AND status=?"
            params.append(status)
        if service:
            sql += " AND service=?"
            params.append(service)
        sql += " ORDER BY created_at DESC, id DESC LIMIT ?"
        params.append(int(limit))
        return [dict(row) for row in self._conn.execute(sql, params).fetchall()]

    def events(self, job_id: str) -> list[dict[str, Any]]:
        return [
            dict(row)
            for row in self._conn.execute(
                "SELECT * FROM job_events WHERE job_id=? ORDER BY id", (job_id,)
            ).fetchall()
        ]

    def stats(self) -> dict[str, Any]:
        by_status = {
            row["status"]: row["n"]
            for row in self._conn.execute(
                "SELECT status, COUNT(*) AS n FROM jobs GROUP BY status"
            ).fetchall()
        }
        by_service = {
            row["service"]: row["n"]
            for row in self._conn.execute(
                "SELECT service, COUNT(*) AS n FROM jobs GROUP BY service"
            ).fetchall()
        }
        duplicates = self._conn.execute(
            "SELECT COALESCE(SUM(dedupe_hits),0) AS s FROM jobs"
        ).fetchone()["s"]
        return {
            "database": str(self.path),
            "total": sum(by_status.values()),
            "by_status": {k: by_status.get(k, 0) for k in ("pending", "running", "done", "failed", "cancelled")},
            "by_service": by_service,
            "dedupe_hits": duplicates,
        }

    # ------------------------------------------------------------- lifecycle
    def claim_next(self, service: str | None = None, now: float | None = None) -> dict[str, Any] | None:
        """Atomically promote the next due ``pending`` job to ``running``."""
        now = time.time() if now is None else now
        conn = self._conn
        try:
            conn.execute("BEGIN IMMEDIATE")
            sql = (
                "SELECT id FROM jobs WHERE status='pending' AND next_run_at<=?"
            )
            params: list[Any] = [now]
            if service:
                sql += " AND service=?"
                params.append(service)
            sql += " ORDER BY next_run_at ASC, created_at ASC LIMIT 1"
            row = conn.execute(sql, params).fetchone()
            if not row:
                conn.commit()
                return None
            job_id = row["id"]
            conn.execute(
                """
                UPDATE jobs
                   SET status='running', attempts=attempts+1,
                       started_at=?, updated_at=?, lease_until=?
                 WHERE id=? AND status='pending'
                """,
                (utc_now(), utc_now(), now + 15 * 60, job_id),
            )
            self._event(job_id, "claimed")
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        return self.get(job_id)

    def mark_running(self, job_id: str) -> None:
        with self._conn:
            self._conn.execute(
                "UPDATE jobs SET status='running', updated_at=?, started_at=COALESCE(started_at, ?) "
                "WHERE id=?",
                (utc_now(), utc_now(), job_id),
            )

    def claim_specific(self, job_id: str, *, now: float | None = None) -> dict[str, Any] | None:
        """Promote one known ``pending`` job to ``running`` (used by ``run <job>``)."""
        now = time.time() if now is None else now
        conn = self._conn
        try:
            conn.execute("BEGIN IMMEDIATE")
            row = conn.execute("SELECT status FROM jobs WHERE id=?", (job_id,)).fetchone()
            if row is None:
                conn.commit()
                return None
            if row["status"] in ("done", "running"):
                conn.commit()
                return self.get(job_id)
            if row["status"] != "pending":
                conn.commit()
                return self.get(job_id)
            conn.execute(
                """
                UPDATE jobs
                   SET status='running', attempts=attempts+1, updated_at=?,
                       started_at=COALESCE(started_at, ?), lease_until=?
                 WHERE id=? AND status='pending'
                """,
                (utc_now(), utc_now(), now + 15 * 60, job_id),
            )
            self._event(job_id, "claimed", "explicit run")
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        return self.get(job_id)

    def set_remote_task_id(self, job_id: str, remote_task_id: str) -> None:
        """Persist the provider task id *before* polling so a crash can resume."""
        with self._conn:
            self._conn.execute(
                "UPDATE jobs SET remote_task_id=?, updated_at=? WHERE id=?",
                (remote_task_id, utc_now(), job_id),
            )
            self._event(job_id, "remote_task_id", remote_task_id)

    def set_source_url(self, job_id: str, source_url: str) -> None:
        """Record an expiring provider URL at the moment it is known."""
        with self._conn:
            self._conn.execute(
                "UPDATE jobs SET source_url=?, source_url_saved_at=?, updated_at=? WHERE id=?",
                (source_url, utc_now(), utc_now(), job_id),
            )

    def mark_done(
        self,
        job_id: str,
        *,
        result: dict[str, Any] | None = None,
        artifact_path: str | None = None,
        remote_task_id: str | None = None,
        source_url: str | None = None,
    ) -> None:
        with self._conn:
            self._conn.execute(
                """
                UPDATE jobs
                   SET status='done', updated_at=?, finished_at=?, lease_until=NULL,
                       result_json=?, artifact_path=COALESCE(?, artifact_path),
                       remote_task_id=COALESCE(?, remote_task_id),
                       source_url=COALESCE(?, source_url),
                       source_url_saved_at=CASE WHEN ? IS NULL THEN source_url_saved_at ELSE ? END,
                       error=NULL
                 WHERE id=?
                """,
                (
                    utc_now(),
                    utc_now(),
                    json.dumps(result, ensure_ascii=False) if result is not None else None,
                    artifact_path,
                    remote_task_id,
                    source_url,
                    source_url,
                    utc_now(),
                    job_id,
                ),
            )
            self._event(job_id, "done", artifact_path or "")

    def mark_failed(
        self,
        job_id: str,
        *,
        error: str,
        retryable: bool = True,
        base_s: float = 5.0,
        cap_s: float = 900.0,
    ) -> dict[str, Any]:
        """Record a failure. Retryable failures go back to ``pending`` with backoff."""
        job = self.get(job_id)
        if job is None:
            raise KeyError(job_id)
        attempts = int(job["attempts"])
        will_retry = retryable and attempts < int(job["max_attempts"])
        delay = backoff_delay(attempts, base_s=base_s, cap_s=cap_s) if will_retry else None
        with self._conn:
            if will_retry:
                self._conn.execute(
                    """
                    UPDATE jobs
                       SET status='pending', next_run_at=?, updated_at=?,
                           lease_until=NULL, error=?
                     WHERE id=?
                    """,
                    (time.time() + float(delay or 0.0), utc_now(), error[:4000], job_id),
                )
                self._event(job_id, "retry_scheduled", f"attempt {attempts}, in {delay}s: {error[:300]}")
            else:
                self._conn.execute(
                    """
                    UPDATE jobs
                       SET status='failed', updated_at=?, finished_at=?,
                           lease_until=NULL, error=?
                     WHERE id=?
                    """,
                    (utc_now(), utc_now(), error[:4000], job_id),
                )
                self._event(job_id, "failed", error[:500])
        return {
            "job_id": job_id,
            "retry": will_retry,
            "attempts": attempts,
            "max_attempts": int(job["max_attempts"]),
            "next_run_in_s": delay,
            "error": error,
        }

    def requeue(self, job_id: str, *, reset_attempts: bool = True) -> bool:
        with self._conn:
            cursor = self._conn.execute(
                """
                UPDATE jobs
                   SET status='pending', next_run_at=?, updated_at=?, error=NULL,
                       attempts=CASE WHEN ? THEN 0 ELSE attempts END,
                       finished_at=NULL, lease_until=NULL
                 WHERE id=?
                """,
                (time.time(), utc_now(), 1 if reset_attempts else 0, job_id),
            )
            self._event(job_id, "requeued")
        return cursor.rowcount > 0

    def cancel(self, job_id: str) -> bool:
        with self._conn:
            cursor = self._conn.execute(
                "UPDATE jobs SET status='cancelled', updated_at=?, finished_at=? "
                "WHERE id=? AND status IN ('pending','running')",
                (utc_now(), utc_now(), job_id),
            )
            self._event(job_id, "cancelled")
        return cursor.rowcount > 0

    def recover_stale(self, now: float | None = None) -> list[str]:
        """Return jobs whose worker died mid-flight to ``pending`` (resume support)."""
        now = time.time() if now is None else now
        rows = self._conn.execute(
            "SELECT id, attempts, max_attempts FROM jobs "
            "WHERE status='running' AND (lease_until IS NULL OR lease_until < ?)",
            (now,),
        ).fetchall()
        recovered: list[str] = []
        for row in rows:
            job_id = row["id"]
            if int(row["attempts"]) >= int(row["max_attempts"]):
                with self._conn:
                    self._conn.execute(
                        "UPDATE jobs SET status='failed', updated_at=?, finished_at=?, "
                        "error=COALESCE(error,'lease expired during last attempt') WHERE id=?",
                        (utc_now(), utc_now(), job_id),
                    )
                    self._event(job_id, "failed_lease_expired")
            else:
                with self._conn:
                    self._conn.execute(
                        "UPDATE jobs SET status='pending', next_run_at=?, updated_at=?, "
                        "lease_until=NULL WHERE id=?",
                        (now, utc_now(), job_id),
                    )
                    self._event(job_id, "resumed_after_interrupt")
            recovered.append(job_id)
        return recovered

    # ----------------------------------------------------------- rate limits
    def acquire_rate_slot(self, service: str, min_interval_s: float, *, sleep: bool = True) -> float:
        """Block until the per-service minimum interval has elapsed.

        Returns the number of seconds that had to be waited.
        """
        conn = self._conn
        now = time.time()
        row = conn.execute(
            "SELECT last_started_at FROM service_rate WHERE service=?", (service,)
        ).fetchone()
        last = float(row["last_started_at"]) if row else 0.0
        wait = max(0.0, min_interval_s - (now - last))
        if wait > 0 and sleep:
            time.sleep(wait)
            now = time.time()
        conn.execute(
            """
            INSERT INTO service_rate (service, min_interval_s, last_started_at)
            VALUES (?,?,?)
            ON CONFLICT(service) DO UPDATE SET
                min_interval_s=excluded.min_interval_s,
                last_started_at=excluded.last_started_at
            """,
            (service, float(min_interval_s), now),
        )
        conn.commit()
        return round(wait, 3)

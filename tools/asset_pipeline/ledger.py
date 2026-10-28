"""Cost ledger (SQLite) for the asset pipeline.

Every billable request must follow this order:

1. ``estimate``  -> :class:`asset_pipeline.config.CostEstimate`
2. ``reserve``   -> atomic check against ``ASSET_MAX_COST_USD`` (and the optional
                    per-service budget) *before* the HTTP call. Raises
                    :class:`BudgetExceeded` when the ceiling would be crossed.
3. send request  -> (only when the spend gate allows it)
4. ``commit``    -> record the actual cost, or ``release`` when the request failed.

``reserved`` rows count against the ceiling, so two concurrent jobs cannot both
spend the last dollar.
"""

from __future__ import annotations

import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

from .config import CostEstimate

__all__ = ["BudgetExceeded", "Ledger", "utc_now"]

_SCHEMA = """
CREATE TABLE IF NOT EXISTS cost_entries (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at     TEXT    NOT NULL,
    job_id         TEXT,
    service        TEXT    NOT NULL,
    model          TEXT,
    kind           TEXT,
    status         TEXT    NOT NULL CHECK (status IN ('reserved','committed','released')),
    estimated_usd  REAL,
    actual_usd     REAL,
    credits        REAL,
    units          INTEGER,
    unit           TEXT,
    cost_source    TEXT,
    note           TEXT,
    usage_json     TEXT
);
CREATE INDEX IF NOT EXISTS ix_cost_service ON cost_entries(service, status);
CREATE INDEX IF NOT EXISTS ix_cost_job ON cost_entries(job_id);
"""


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


class BudgetExceeded(RuntimeError):
    """Raised before sending when the request would exceed a configured ceiling."""


class Ledger:
    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(str(self.path), timeout=30.0)
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA journal_mode=WAL")
        self._conn.executescript(_SCHEMA)
        self._conn.commit()

    # ------------------------------------------------------------------ io
    def close(self) -> None:
        self._conn.close()

    def __enter__(self) -> "Ledger":
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()

    # ------------------------------------------------------------- queries
    def committed_total(self, service: str | None = None) -> float:
        sql = "SELECT COALESCE(SUM(actual_usd), 0) AS s FROM cost_entries WHERE status='committed'"
        params: list[Any] = []
        if service:
            sql += " AND service=?"
            params.append(service)
        return round(float(self._conn.execute(sql, params).fetchone()["s"]), 6)

    def outstanding_total(self, service: str | None = None) -> float:
        sql = "SELECT COALESCE(SUM(estimated_usd), 0) AS s FROM cost_entries WHERE status='reserved'"
        params: list[Any] = []
        if service:
            sql += " AND service=?"
            params.append(service)
        return round(float(self._conn.execute(sql, params).fetchone()["s"]), 6)

    def committed_credits(self, service: str | None = None) -> float:
        sql = (
            "SELECT COALESCE(SUM(credits), 0) AS s FROM cost_entries "
            "WHERE status='committed' AND credits IS NOT NULL"
        )
        params: list[Any] = []
        if service:
            sql += " AND service=?"
            params.append(service)
        return round(float(self._conn.execute(sql, params).fetchone()["s"]), 6)

    def totals(self) -> dict[str, Any]:
        """Aggregate view used by the ``cost`` CLI command."""
        rows = self._conn.execute(
            """
            SELECT service,
                   COUNT(*)                                                        AS entries,
                   COALESCE(SUM(CASE WHEN status='committed' THEN actual_usd END),0) AS committed_usd,
                   COALESCE(SUM(CASE WHEN status='reserved'  THEN estimated_usd END),0) AS reserved_usd,
                   COALESCE(SUM(CASE WHEN status='committed' THEN credits END),0)    AS credits,
                   COALESCE(SUM(CASE WHEN status='committed' THEN 1 ELSE 0 END),0)   AS committed_jobs,
                   COALESCE(SUM(CASE WHEN status='released' THEN 1 ELSE 0 END),0)    AS released_jobs
            FROM cost_entries
            GROUP BY service
            """
        ).fetchall()
        by_service = {
            row["service"]: {
                "entries": row["entries"],
                "committed_usd": round(float(row["committed_usd"]), 6),
                "reserved_usd": round(float(row["reserved_usd"]), 6),
                "credits": round(float(row["credits"]), 6),
                "committed_jobs": row["committed_jobs"],
                "released_jobs": row["released_jobs"],
            }
            for row in rows
        }
        by_job = self._conn.execute(
            """
            SELECT job_id,
                   service,
                   COUNT(*) AS entries,
                   COALESCE(SUM(CASE WHEN status='committed' THEN actual_usd END),0) AS committed_usd,
                   COALESCE(SUM(CASE WHEN status='reserved'  THEN estimated_usd END),0) AS reserved_usd
            FROM cost_entries
            WHERE job_id IS NOT NULL
            GROUP BY job_id, service
            ORDER BY MAX(id) DESC
            LIMIT 100
            """
        ).fetchall()
        return {
            "database": str(self.path),
            "committed_usd": self.committed_total(),
            "reserved_usd": self.outstanding_total(),
            "by_service": by_service,
            "by_job": [
                {
                    "job_id": row["job_id"],
                    "service": row["service"],
                    "entries": row["entries"],
                    "committed_usd": round(float(row["committed_usd"]), 6),
                    "reserved_usd": round(float(row["reserved_usd"]), 6),
                }
                for row in by_job
            ],
        }

    def recent(self, limit: int = 20) -> list[dict[str, Any]]:
        rows = self._conn.execute(
            "SELECT * FROM cost_entries ORDER BY id DESC LIMIT ?", (int(limit),)
        ).fetchall()
        return [dict(row) for row in rows]

    # ------------------------------------------------------ reserve/commit
    def check_and_reserve(
        self,
        *,
        job_id: str,
        service: str,
        model: str | None,
        kind: str | None,
        estimate: CostEstimate,
        max_cost_usd: float,
        service_budget_usd: float | None = None,
        units: int | None = None,
        unit: str | None = None,
    ) -> int:
        """Atomically validate the ceiling and hold the estimated cost.

        Returns the reservation id. Raises :class:`BudgetExceeded` without writing
        anything when the request must not be sent.
        """
        if estimate.usd is None:
            raise BudgetExceeded(
                f"refusing to send: price unknown for {service}/{model or kind} "
                f"({estimate.detail or 'no price table entry'}); cannot prove the "
                f"${max_cost_usd:.2f} ceiling would hold"
            )
        if estimate.usd < 0:
            raise BudgetExceeded(f"refusing to send: negative estimate {estimate.usd}")
        amount = round(float(estimate.usd), 6)

        conn = self._conn
        try:
            conn.execute("BEGIN IMMEDIATE")
            committed = float(
                conn.execute(
                    "SELECT COALESCE(SUM(actual_usd),0) AS s FROM cost_entries WHERE status='committed'"
                ).fetchone()["s"]
            )
            outstanding = float(
                conn.execute(
                    "SELECT COALESCE(SUM(estimated_usd),0) AS s FROM cost_entries WHERE status='reserved'"
                ).fetchone()["s"]
            )
            committed_service = float(
                conn.execute(
                    "SELECT COALESCE(SUM(actual_usd),0) AS s FROM cost_entries "
                    "WHERE status='committed' AND service=?",
                    (service,),
                ).fetchone()["s"]
            )
            outstanding_service = float(
                conn.execute(
                    "SELECT COALESCE(SUM(estimated_usd),0) AS s FROM cost_entries "
                    "WHERE status='reserved' AND service=?",
                    (service,),
                ).fetchone()["s"]
            )
            projected = committed + outstanding + amount
            if projected > max_cost_usd + 1e-9:
                raise BudgetExceeded(
                    f"refusing to send: committed ${committed:.4f} + reserved "
                    f"${outstanding:.4f} + estimate ${amount:.4f} = ${projected:.4f} "
                    f"would exceed ASSET_MAX_COST_USD={max_cost_usd:.4f}"
                )
            if service_budget_usd is not None:
                projected_service = committed_service + outstanding_service + amount
                if projected_service > service_budget_usd + 1e-9:
                    raise BudgetExceeded(
                        f"refusing to send: {service} committed ${committed_service:.4f} + "
                        f"reserved ${outstanding_service:.4f} + estimate ${amount:.4f} = "
                        f"${projected_service:.4f} would exceed the {service} budget "
                        f"${service_budget_usd:.4f}"
                    )
            cursor = conn.execute(
                """
                INSERT INTO cost_entries
                    (created_at, job_id, service, model, kind, status, estimated_usd,
                     actual_usd, credits, units, unit, cost_source, note)
                VALUES (?,?,?,?,?,'reserved',?,NULL,?,?,?,?,?)
                """,
                (
                    utc_now(),
                    job_id,
                    service,
                    model,
                    kind,
                    amount,
                    estimate.credits,
                    units,
                    unit,
                    estimate.source,
                    estimate.detail,
                ),
            )
            conn.commit()
            return int(cursor.lastrowid)
        except BudgetExceeded:
            conn.rollback()
            raise
        except Exception:
            conn.rollback()
            raise

    def commit(
        self,
        reservation_id: int,
        *,
        actual_usd: float | None = None,
        credits: float | None = None,
        usage: dict[str, Any] | None = None,
        note: str | None = None,
    ) -> None:
        import json

        with self._conn:
            self._conn.execute(
                """
                UPDATE cost_entries
                   SET status='committed',
                       actual_usd=COALESCE(?, estimated_usd),
                       credits=COALESCE(?, credits),
                       usage_json=COALESCE(?, usage_json),
                       note=COALESCE(?, note)
                 WHERE id=? AND status='reserved'
                """,
                (
                    None if actual_usd is None else round(float(actual_usd), 6),
                    credits,
                    json.dumps(usage, ensure_ascii=False) if usage else None,
                    note,
                    int(reservation_id),
                ),
            )

    def release(self, reservation_id: int, reason: str = "") -> None:
        with self._conn:
            self._conn.execute(
                "UPDATE cost_entries SET status='released', note=? "
                "WHERE id=? AND status='reserved'",
                (reason[:500], int(reservation_id)),
            )

    def record_zero_cost(
        self,
        *,
        job_id: str,
        service: str,
        model: str | None,
        kind: str | None,
        note: str = "non-billable",
    ) -> None:
        with self._conn:
            self._conn.execute(
                """
                INSERT INTO cost_entries
                    (created_at, job_id, service, model, kind, status, estimated_usd,
                     actual_usd, note, cost_source)
                VALUES (?,?,?,?,?,'committed',0,0,?,'non-billable')
                """,
                (utc_now(), job_id, service, model, kind, note),
            )

    def stale_reservations(self, older_than_seconds: float) -> Iterable[dict[str, Any]]:
        rows = self._conn.execute(
            "SELECT * FROM cost_entries WHERE status='reserved'"
        ).fetchall()
        now = datetime.now(timezone.utc)
        for row in rows:
            try:
                created = datetime.fromisoformat(row["created_at"])
            except ValueError:
                continue
            if (now - created).total_seconds() >= older_than_seconds:
                yield dict(row)

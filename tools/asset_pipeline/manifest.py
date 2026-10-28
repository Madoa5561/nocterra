"""Asset manifest: the provenance ledger for every generated/processed asset.

Stored as a single JSON document at ``generated_assets/manifests/assets.json`` plus a
human-readable mirror ``assets.md`` is *not* produced (keep the tree small); the JSON
is the source of truth and is written atomically (tmp + ``os.replace``).

Every entry records:
  id, kind, stage (raw|processed), service, model, endpoint, prompt, parameters,
  created_at, cost (estimated/actual, source of truth for the ledger), credits,
  sha256 + byte size + format details, source_asset_id (for processed derivatives),
  job_id, version/revision, license note, validation summary, warnings.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from . import SCHEMA_VERSION
from .util import json_default

__all__ = ["AssetManifest", "sha256_file", "utc_now"]

MANIFEST_NAME = "assets.json"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def sha256_file(path: str | Path, chunk_size: int = 1024 * 1024) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        while True:
            chunk = handle.read(chunk_size)
            if not chunk:
                break
            digest.update(chunk)
    return digest.hexdigest()


class AssetManifest:
    def __init__(self, directory: str | Path) -> None:
        self.directory = Path(directory)
        self.path = self.directory / MANIFEST_NAME
        self.document: dict[str, Any] = {
            "schema_version": SCHEMA_VERSION,
            "updated_at": None,
            "assets": [],
        }
        if self.path.is_file():
            self.document = json.loads(self.path.read_text(encoding="utf-8-sig"))
            self.document.setdefault("assets", [])
            self.document.setdefault("schema_version", SCHEMA_VERSION)

    # ---------------------------------------------------------------- io
    def _atomic_write(self) -> None:
        self.directory.mkdir(parents=True, exist_ok=True)
        payload = json.dumps(
            self.document, ensure_ascii=False, indent=2, sort_keys=False, default=json_default
        )
        fd, tmp_name = tempfile.mkstemp(prefix=".assets-", suffix=".tmp", dir=self.directory)
        try:
            with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
                handle.write(payload)
                handle.write("\n")
            os.replace(tmp_name, self.path)
        finally:
            if os.path.exists(tmp_name):
                try:
                    os.remove(tmp_name)
                except OSError:
                    pass

    def reload(self) -> dict[str, Any]:
        if self.path.is_file():
            self.document = json.loads(self.path.read_text(encoding="utf-8-sig"))
        return self.document

    # ------------------------------------------------------------- write
    def add(
        self,
        *,
        asset_id: str,
        kind: str,
        path: str | Path,
        service: str,
        model: str | None,
        prompt: str | None = None,
        parameters: dict[str, Any] | None = None,
        stage: str = "raw",
        cost: dict[str, Any] | None = None,
        job_id: str | None = None,
        source_asset_id: str | None = None,
        license_note: str | None = None,
        validation: dict[str, Any] | None = None,
        warnings: list[str] | None = None,
        extra: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        file_path = Path(path)
        digest = sha256_file(file_path) if file_path.is_file() else None
        existing = next(
            (a for a in self.document["assets"] if a.get("asset_id") == asset_id), None
        )
        revision = int(existing.get("revision", 0)) + 1 if existing else 1
        entry: dict[str, Any] = {
            "asset_id": asset_id,
            "revision": revision,
            "kind": kind,
            "stage": stage,
            "path": self._relative(file_path),
            "service": service,
            "model": model,
            "prompt": prompt,
            "parameters": parameters or {},
            "created_at": utc_now(),
            "cost": cost or {"estimated_usd": None, "actual_usd": None, "note": "unpriced"},
            "job_id": job_id,
            "source_asset_id": source_asset_id,
            "sha256": digest,
            "bytes": file_path.stat().st_size if file_path.is_file() else None,
            "license_note": license_note
            or "UNVERIFIED: confirm the provider's terms and this asset's licence before shipping.",
            "validation": validation or {},
            "warnings": warnings or [],
        }
        if extra:
            entry["extra"] = extra
        if existing:
            entry["history"] = (existing.get("history") or []) + [
                {
                    "revision": existing.get("revision"),
                    "created_at": existing.get("created_at"),
                    "sha256": existing.get("sha256"),
                    "path": existing.get("path"),
                }
            ]
            self.document["assets"][self.document["assets"].index(existing)] = entry
        else:
            self.document["assets"].append(entry)
        self.document["updated_at"] = utc_now()
        self._atomic_write()
        return entry

    def add_processed(
        self,
        *,
        asset_id: str,
        path: str | Path,
        source_asset_id: str,
        operation: str,
        parameters: dict[str, Any] | None = None,
        validation: dict[str, Any] | None = None,
        warnings: list[str] | None = None,
    ) -> dict[str, Any]:
        """Register a locally derived file (e.g. a 16x16 texture from a 1024px PNG)."""
        return self.add(
            asset_id=asset_id,
            kind=self._kind_for(source_asset_id),
            path=path,
            service="local",
            model=None,
            prompt=None,
            parameters={"operation": operation, **(parameters or {})},
            stage="processed",
            cost={"estimated_usd": 0.0, "actual_usd": 0.0, "note": "local processing only"},
            source_asset_id=source_asset_id,
            validation=validation,
            warnings=warnings,
            license_note="Derived locally from the source asset; the source licence applies.",
        )

    # -------------------------------------------------------------- read
    def _kind_for(self, asset_id: str) -> str:
        entry = self.find(asset_id)
        return entry.get("kind", "unknown") if entry else "unknown"

    def _relative(self, path: Path) -> str:
        try:
            return path.resolve().relative_to(self.directory.parent.parent.resolve()).as_posix()
        except ValueError:
            return path.resolve().as_posix()

    def find(self, asset_id: str) -> dict[str, Any] | None:
        return next((a for a in self.document["assets"] if a.get("asset_id") == asset_id), None)

    def find_by_sha(self, digest: str) -> dict[str, Any] | None:
        return next((a for a in self.document["assets"] if a.get("sha256") == digest), None)

    def list(self, kind: str | None = None, stage: str | None = None) -> list[dict[str, Any]]:
        assets = self.document["assets"]
        if kind:
            assets = [a for a in assets if a.get("kind") == kind]
        if stage:
            assets = [a for a in assets if a.get("stage") == stage]
        return assets

    def summary(self) -> dict[str, Any]:
        assets = self.document["assets"]
        total_usd = 0.0
        for entry in assets:
            value = (entry.get("cost") or {}).get("actual_usd")
            if isinstance(value, (int, float)):
                total_usd += float(value)
        return {
            "manifest": str(self.path),
            "schema_version": self.document.get("schema_version", SCHEMA_VERSION),
            "asset_count": len(assets),
            "by_kind": _count(assets, "kind"),
            "by_stage": _count(assets, "stage"),
            "by_service": _count(assets, "service"),
            "total_actual_usd": round(total_usd, 6),
            "updated_at": self.document.get("updated_at"),
        }


def _count(assets: list[dict[str, Any]], key: str) -> dict[str, int]:
    out: dict[str, int] = {}
    for entry in assets:
        value = str(entry.get(key))
        out[value] = out.get(value, 0) + 1
    return out

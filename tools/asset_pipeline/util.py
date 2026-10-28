"""Small shared utilities: JSON IO, dry-run preview persistence, formatting."""

from __future__ import annotations

import json
import os
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

__all__ = [
    "JsonlLog",
    "atomic_write_json",
    "human_bytes",
    "human_usd",
    "now_stamp",
    "read_json",
    "save_dry_run_preview",
    "short",
]


def now_stamp() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def read_json(path: str | Path) -> Any:
    return json.loads(Path(path).read_text(encoding="utf-8-sig"))


def atomic_write_json(path: str | Path, payload: Any) -> Path:
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False) + "\n"
    fd, tmp_name = tempfile.mkstemp(prefix=f".{destination.stem}-", suffix=".tmp", dir=destination.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        os.replace(tmp_name, destination)
    finally:
        if os.path.exists(tmp_name):
            try:
                os.remove(tmp_name)
            except OSError:
                pass
    return destination


def save_dry_run_preview(state_dir: str | Path, name: str, payload: dict[str, Any]) -> Path:
    """Persist the exact request that *would* have been sent (DRY RUN evidence)."""
    safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in name)[:80] or "preview"
    path = Path(state_dir) / "dryrun" / f"{now_stamp()}_{safe}.json"
    return atomic_write_json(path, payload)


def human_usd(value: float | None) -> str:
    if value is None:
        return "unknown"
    return f"${value:,.4f}"


def human_bytes(value: int | None) -> str:
    if value is None:
        return "?"
    size = float(value)
    for unit in ("B", "KiB", "MiB", "GiB"):
        if size < 1024 or unit == "GiB":
            return f"{size:.0f} {unit}" if unit == "B" else f"{size:.1f} {unit}"
        size /= 1024
    return f"{value} B"


def short(value: Any, limit: int = 60) -> str:
    text = str(value)
    return text if len(text) <= limit else text[: limit - 1] + "…"


class JsonlLog:
    """Append-only JSONL log (best effort, never raises)."""

    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)

    def append(self, record: dict[str, Any]) -> None:
        try:
            with self.path.open("a", encoding="utf-8") as handle:
                handle.write(json.dumps(record, ensure_ascii=False) + "\n")
        except OSError:
            pass

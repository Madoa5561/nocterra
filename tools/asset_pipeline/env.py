"""`.env` loading and secret handling.

Hard rules enforced here:
  * API keys are read only from ``<repo>/.env`` (or the process environment) and are
    never written to source, logs or JSON output.
  * :func:`mask` is the ONLY sanctioned way to render a secret.
  * The file is parsed with ``utf-8-sig`` because the project ``.env`` starts with a
    UTF-8 BOM (verified: without BOM handling the first key becomes ``\\ufeffTRIPO``).
"""

from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Iterable, Mapping

__all__ = [
    "Env",
    "MissingSecretError",
    "find_repo_root",
    "load_dotenv",
    "mask",
    "mask_mapping",
]

#: Suffixes/names that must never be printed verbatim.
_SECRET_NAME_RE = re.compile(
    r"(?:^|_)(?:API_?KEY|KEY|TOKEN|SECRET|PASSWORD)$|^(?:OPENAI|TRIPO|FISH|DEEPSEEK)$",
    re.IGNORECASE,
)


class MissingSecretError(RuntimeError):
    """Raised when a required API key is not present in ``.env``."""


def find_repo_root(start: str | os.PathLike[str] | None = None) -> Path:
    """Walk up from *start* until a directory holding ``.env`` or ``.git`` is found."""
    here = Path(start or __file__).resolve()
    candidates: Iterable[Path] = (here, *here.parents)
    for path in candidates:
        probe = path if path.is_dir() else path.parent
        if (probe / ".env").is_file() or (probe / ".git").is_dir():
            return probe
    # tools/asset_pipeline/env.py -> repo root is two levels up
    return Path(__file__).resolve().parents[2]


def parse_dotenv_text(text: str) -> dict[str, str]:
    """Parse ``PARAM=value`` dotenv content (BOM tolerant, no shell expansion)."""
    data: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export ") :].lstrip()
        if "=" not in line:
            continue
        name, value = line.split("=", 1)
        name = name.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        if name:
            data[name] = value
    return data


def load_dotenv(path: str | os.PathLike[str]) -> dict[str, str]:
    """Read a dotenv file. Returns ``{}`` if it does not exist."""
    p = Path(path)
    if not p.is_file():
        return {}
    return parse_dotenv_text(p.read_text(encoding="utf-8-sig"))


def mask(secret: str | None, *, keep_prefix: int = 3, keep_suffix: int = 0) -> str:
    """Render a secret safely, e.g. ``tsk*** (len=47)``."""
    if not secret:
        return "<empty>"
    length = len(secret)
    if length <= keep_prefix:
        return f"*** (len={length})"
    prefix = secret[:keep_prefix]
    suffix = secret[length - keep_suffix :] if keep_suffix > 0 else ""
    stars = "*" * max(3, min(length - keep_prefix - keep_suffix, 12))
    return f"{prefix}{stars}{suffix} (len={length})"


def mask_mapping(values: Mapping[str, str]) -> dict[str, str]:
    """Mask every value whose key looks like a secret; leave config values visible."""
    out: dict[str, str] = {}
    for key, value in values.items():
        out[key] = mask(value) if _SECRET_NAME_RE.search(key) else value
    return out


class Env:
    """Merged view of ``<repo>/.env`` and the process environment.

    Precedence: process environment wins for ``ASSET_*`` keys, dotenv wins for
    everything else (i.e. the project file is the source of truth for credentials).
    """

    def __init__(
        self,
        repo_root: str | os.PathLike[str] | None = None,
        dotenv_path: str | os.PathLike[str] | None = None,
        environ: Mapping[str, str] | None = None,
    ) -> None:
        self.repo_root = Path(repo_root) if repo_root else find_repo_root()
        self.dotenv_path = (
            Path(dotenv_path) if dotenv_path else self.repo_root / ".env"
        ).resolve()
        self._environ = dict(os.environ if environ is None else environ)
        file_values = load_dotenv(self.dotenv_path)
        merged = dict(file_values)
        for key, value in self._environ.items():
            if key.startswith("ASSET_"):
                merged[key] = value
        self.values: dict[str, str] = merged
        self.dotenv_found = bool(file_values)

    # -- generic accessors -------------------------------------------------
    def get(self, name: str, default: str | None = None) -> str | None:
        value = self.values.get(name)
        if value is None or value == "":
            return default
        return value

    def has(self, name: str) -> bool:
        return bool(self.get(name))

    def get_bool(self, name: str, default: bool = False) -> bool:
        raw = self.get(name)
        if raw is None:
            return default
        return raw.strip().lower() in {"1", "true", "yes", "y", "on"}

    def get_float(self, name: str, default: float | None = None) -> float | None:
        raw = self.get(name)
        if raw is None:
            return default
        try:
            return float(raw.strip())
        except ValueError:
            return default

    def get_int(self, name: str, default: int | None = None) -> int | None:
        raw = self.get(name)
        if raw is None:
            return default
        try:
            return int(float(raw.strip()))
        except ValueError:
            return default

    def secret(self, name: str) -> str:
        """Return a credential or raise :class:`MissingSecretError` (never echoes it)."""
        value = self.values.get(name)
        if not value:
            raise MissingSecretError(
                f"{name} is missing. Add `{name}=...` to {self.dotenv_path} "
                "(the file is git-ignored)."
            )
        return value

    # -- diagnostics -------------------------------------------------------
    def secret_names(self) -> list[str]:
        return sorted(k for k in self.values if _SECRET_NAME_RE.search(k))

    def describe(self) -> dict[str, object]:
        """Masked, JSON-safe description for ``status`` output."""
        return {
            "repo_root": str(self.repo_root),
            "dotenv_path": str(self.dotenv_path),
            "dotenv_found": self.dotenv_found,
            "secrets": {
                name: mask(self.values.get(name)) for name in self.secret_names()
            },
            "config": {
                key: value
                for key, value in sorted(self.values.items())
                if key.startswith("ASSET_")
            },
        }

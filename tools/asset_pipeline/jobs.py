"""Job specification loading and validation.

A *job* is a small JSON document describing one asset generation request. The
pipeline keeps them in ``tools/asset_pipeline/jobs/*.json`` so they are reviewable in
git, but ``dry-run``/``run`` also accept a direct path or an inline JSON string.

Example (texture job)::

    {
      "id": "stone_wall_tile",
      "kind": "texture",
      "service": "openai",
      "model": "gpt-image-2.5-flare",
      "prompt": "seamless 16x16 pixel-art stone wall texture ...",
      "params": {"size": "1024x1024", "quality": "medium",
                 "background": "transparent", "output_format": "png", "n": 1},
      "texture": {"sizes": [16, 32, 64], "remove_background": true,
                  "bg_method": "flood", "bg_tolerance": 24,
                  "quantize_colors": 16, "alpha_mode": "cutout"},
      "license_note": "AI-generated for NOCTERRA; confirm provider terms before release."
    }
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

__all__ = ["JobSpec", "JobSpecError", "load_job", "validate_job"]

JOBS_DIR = Path(__file__).resolve().parent / "jobs"

KIND_SERVICE = {
    "texture": "openai",
    "model": "tripo",
    "audio": "fish",
}

MODEL_MODES = {"text_to_model", "image_to_model"}

REQUIRED_FIELDS = ("kind", "prompt")

ALLOWED_TOP_LEVEL = {
    "id",
    "kind",
    "service",
    "model",
    "prompt",
    "negative_prompt",
    "params",
    "texture",
    "audio",
    "mode",
    "input_image",
    "license_note",
    "notes",
    "max_attempts",
    # underscore-prefixed keys are ignored by the content hash (local metadata)
}


class JobSpecError(ValueError):
    """The job document is missing or has invalid fields."""


class JobSpec:
    def __init__(self, data: dict[str, Any], source: str = "<inline>") -> None:
        self.data = data
        self.source = source
        validate_job(data)

    # convenience accessors ------------------------------------------------
    @property
    def kind(self) -> str:
        return str(self.data["kind"])

    @property
    def service(self) -> str:
        return str(self.data.get("service") or KIND_SERVICE[self.kind])

    @property
    def model(self) -> str | None:
        return self.data.get("model")

    @property
    def prompt(self) -> str:
        return str(self.data["prompt"])

    @property
    def params(self) -> dict[str, Any]:
        return dict(self.data.get("params") or {})

    @property
    def texture(self) -> dict[str, Any]:
        return dict(self.data.get("texture") or {})

    @property
    def audio(self) -> dict[str, Any]:
        return dict(self.data.get("audio") or {})

    @property
    def job_id(self) -> str:
        return str(self.data.get("id") or "")

    @property
    def license_note(self) -> str | None:
        return self.data.get("license_note")

    def hash_payload(self) -> dict[str, Any]:
        """Everything that changes the billable request (used for duplicate detection)."""
        return {
            "kind": self.kind,
            "service": self.service,
            "model": self.model,
            "prompt": self.prompt,
            "negative_prompt": self.data.get("negative_prompt"),
            "params": self.params,
            "mode": self.data.get("mode"),
            "input_image": self.data.get("input_image"),
        }

    def as_dict(self) -> dict[str, Any]:
        return dict(self.data)


def validate_job(data: dict[str, Any]) -> None:
    if not isinstance(data, dict):
        raise JobSpecError("job must be a JSON object")
    unknown = set(data) - ALLOWED_TOP_LEVEL
    if unknown:
        raise JobSpecError(f"unknown job field(s): {sorted(unknown)}")
    for field in REQUIRED_FIELDS:
        if not data.get(field):
            raise JobSpecError(f"job is missing required field {field!r}")
    kind = data.get("kind")
    if kind not in KIND_SERVICE:
        raise JobSpecError(f"kind must be one of {sorted(KIND_SERVICE)}, got {kind!r}")
    service = data.get("service") or KIND_SERVICE[kind]
    if service != KIND_SERVICE[kind]:
        raise JobSpecError(
            f"kind {kind!r} is served by {KIND_SERVICE[kind]!r}, not {service!r}"
        )
    params = data.get("params") or {}
    if not isinstance(params, dict):
        raise JobSpecError("params must be an object")
    for section in ("texture", "audio"):
        if section in data and not isinstance(data[section], dict):
            raise JobSpecError(f"{section} must be an object")
    if kind == "model":
        mode = data.get("mode") or "text_to_model"
        if mode not in MODEL_MODES:
            raise JobSpecError(f"model job mode must be one of {sorted(MODEL_MODES)}")
        if mode == "image_to_model" and not data.get("input_image"):
            raise JobSpecError("image_to_model jobs need an input_image path or URL")
    if data.get("max_attempts") is not None:
        attempts = int(data["max_attempts"])
        if not 1 <= attempts <= 10:
            raise JobSpecError("max_attempts must be in 1..10")


def load_job(arg: str | Path) -> JobSpec:
    """Load a job from a filesystem path, a name in ``jobs/``, or inline JSON."""
    text = str(arg).strip()
    if text.startswith("{"):
        try:
            return JobSpec(json.loads(text), source="<inline-json>")
        except json.JSONDecodeError as exc:
            raise JobSpecError(f"inline job JSON is invalid: {exc}") from exc
    path = Path(text)
    if not path.is_file():
        candidate = JOBS_DIR / (text if text.endswith(".json") else f"{text}.json")
        if candidate.is_file():
            path = candidate
        else:
            available = sorted(p.stem for p in JOBS_DIR.glob("*.json"))
            raise JobSpecError(
                f"job {text!r} not found (tried {path} and {candidate}). "
                f"available jobs: {available}"
            )
    try:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
    except json.JSONDecodeError as exc:
        raise JobSpecError(f"{path} is not valid JSON: {exc}") from exc
    return JobSpec(data, source=str(path))

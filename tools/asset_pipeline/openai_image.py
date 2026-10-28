"""OpenAI Images API client (``POST /v1/images/generations``).

Request/response schema was verified against the official OpenAI OpenAPI document
(``openai/openai-openapi`` -> ``components.schemas.CreateImageRequest`` /
``ImagesResponse``) and the live route was confirmed non-billably with a GET probe
(HTTP 405 = route exists, POST-only). See ``docs/ASSET_PIPELINE.md``.

Safety: this client never retries a billable POST (a 5xx after the body was sent is
ambiguous), never logs the API key, and in DRY RUN it stops *before* the transport by
raising nothing at all -- it returns the exact request that would have been sent.
"""

from __future__ import annotations

import base64
import hashlib
import re
from pathlib import Path
from typing import Any

from .config import Config, CostEstimate
from .env import mask
from .http_client import HttpResponse, Transport
from .util import save_dry_run_preview

__all__ = ["DEFAULT_MODELS", "OpenAIImageClient", "SpendLocked", "build_request", "validate_request"]

POST_ENDPOINT = "/images/generations"

#: Verified present in ``GET /v1/models`` (2026-10-28) and in the official spec.
DEFAULT_MODELS = {
    "basic": "gpt-image-2.5-flare",
    "precise": "gpt-image-2.5-sunburst",
}

MODEL_ALIASES = {
    "basic": DEFAULT_MODELS["basic"],
    "flare": DEFAULT_MODELS["basic"],
    "precise": DEFAULT_MODELS["precise"],
    "sunburst": DEFAULT_MODELS["precise"],
}

#: Parameters verified in ``CreateImageRequest``. Unknown keys are rejected loudly so a
#: typo cannot silently fall back to a default (and cost more than expected).
ALLOWED_PARAMS = {
    "n",
    "quality",
    "output_format",
    "output_compression",
    "size",
    "moderation",
    "background",
    "stream",
    "partial_images",
    "safety_identifier",
}

QUALITIES = ("auto", "low", "medium", "high", "xhigh", "max")
OUTPUT_FORMATS = ("png", "jpeg", "webp")
BACKGROUNDS = ("auto", "opaque", "transparent")
MODERATION = ("auto", "low")

_STANDARD_SIZES = {"auto", "1024x1024", "1536x1024", "1024x1536"}
_SIZE_RE = re.compile(r"^(\d{2,4})x(\d{2,4})$")


class SpendLocked(RuntimeError):
    """Raised when a billable request is attempted while the spend gate is closed."""


def resolve_model(name: str | None) -> str:
    if not name:
        return DEFAULT_MODELS["basic"]
    return MODEL_ALIASES.get(name, name)


def validate_request(request: dict[str, Any]) -> list[str]:
    """Validate against the documented constraints. Returns warnings."""
    warnings: list[str] = []
    if not request.get("prompt"):
        raise ValueError("prompt is required")
    if len(str(request["prompt"])) > 32_000:
        raise ValueError("prompt exceeds the documented 32000 character limit")
    model = request.get("model")
    if not model:
        raise ValueError("model is required (spec: 'Specify a model explicitly')")
    if model == "gpt-image-1":
        warnings.append(
            "gpt-image-1 was shut down on 2026-10-23 per the official model enum; use "
            "gpt-image-2.5-flare or gpt-image-2.5-sunburst"
        )
    n = request.get("n", 1)
    if not isinstance(n, int) or not 1 <= n <= 10:
        raise ValueError(f"n must be an integer 1..10, got {n!r}")
    quality = request.get("quality", "auto")
    if quality not in QUALITIES:
        raise ValueError(f"quality must be one of {QUALITIES}, got {quality!r}")
    output_format = request.get("output_format", "png")
    if output_format not in OUTPUT_FORMATS:
        raise ValueError(f"output_format must be one of {OUTPUT_FORMATS}, got {output_format!r}")
    background = request.get("background", "auto")
    if background not in BACKGROUNDS:
        raise ValueError(f"background must be one of {BACKGROUNDS}, got {background!r}")
    if background == "transparent" and output_format not in ("png", "webp"):
        raise ValueError("background='transparent' requires output_format png or webp")
    if request.get("moderation", "auto") not in MODERATION:
        raise ValueError(f"moderation must be one of {MODERATION}")
    if output_format not in ("jpeg", "webp") and "output_compression" in request:
        raise ValueError("output_compression is only supported for jpeg/webp output")
    size = str(request.get("size", "auto"))
    if size not in _STANDARD_SIZES:
        match = _SIZE_RE.match(size)
        if not match:
            raise ValueError(f"size must be 'auto', a standard size or WIDTHxHEIGHT, got {size!r}")
        width, height = int(match.group(1)), int(match.group(2))
        if width % 16 or height % 16:
            raise ValueError(
                f"size {size}: width and height must both be divisible by 16 for arbitrary "
                "resolutions (documented constraint)"
            )
        ratio = width / height
        if not (1 / 3 - 1e-9 <= ratio <= 3 + 1e-9):
            raise ValueError(f"size {size}: aspect ratio must be between 1:3 and 3:1")
        if max(width, height) > 3840:
            raise ValueError(f"size {size}: maximum supported resolution is 3840x2160")
        if width * height > 2560 * 1440:
            warnings.append(
                f"size {size} is above 2560x1440: experimental per the official spec"
            )
    usage = request.get("usage")
    if usage is not None:
        raise ValueError("'usage' is response-only and must not be sent")
    for key in request:
        if key not in ALLOWED_PARAMS and key not in ("model", "prompt"):
            warnings.append(f"parameter {key!r} is not in the verified request schema")
    return warnings


def build_request(spec: Any) -> dict[str, Any]:
    """Build the JSON body for one texture job (pure function, no IO)."""
    params = spec.params
    unknown = set(params) - ALLOWED_PARAMS
    if unknown:
        raise ValueError(
            f"unknown image parameter(s) {sorted(unknown)}; allowed: {sorted(ALLOWED_PARAMS)}"
        )
    request: dict[str, Any] = {
        "model": resolve_model(spec.model),
        "prompt": spec.prompt,
        "n": int(params.get("n", 1)),
        "quality": params.get("quality", "medium"),
        "size": params.get("size", "1024x1024"),
        "output_format": params.get("output_format", "png"),
        "background": params.get("background", "auto"),
        "moderation": params.get("moderation", "auto"),
    }
    # Keep the body minimal: only send optional keys the caller asked for.
    if "output_compression" in params:
        request["output_compression"] = int(params["output_compression"])
    if params.get("stream"):
        raise ValueError(
            "stream=true is not supported by this pipeline: it complicates cost "
            "accounting and partial images are not needed for asset generation"
        )
    if "safety_identifier" in params:
        request["safety_identifier"] = str(params["safety_identifier"])
    validate_request(request)
    return request


class OpenAIImageClient:
    def __init__(self, cfg: Config, *, transport: Transport | None = None) -> None:
        self.cfg = cfg
        self.transport = transport or Transport(timeout_s=300.0, max_retries=3)

    # ---------------------------------------------------------------- info
    @property
    def endpoint(self) -> str:
        return f"{self.cfg.openai_base_url}{POST_ENDPOINT}"

    def _auth_headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.cfg.env.secret('OPENAI')}"}

    def redacted_headers(self) -> dict[str, str]:
        return {
            "Content-Type": "application/json",
            "Authorization": "Bearer ***REDACTED***",
        }

    def preview_identity(self) -> dict[str, Any]:
        return {
            "endpoint": self.endpoint,
            "key": mask(self.cfg.env.values.get("OPENAI")),
            "key_source": str(self.cfg.env.dotenv_path),
        }

    # ---------------------------------------------------------------- cost
    def estimate(self, request: dict[str, Any]) -> CostEstimate:
        return self.cfg.openai_image_estimate(
            quality=str(request.get("quality", "auto")),
            size=str(request.get("size", "1024x1024")),
            n=int(request.get("n", 1)),
        )

    # -------------------------------------------------------------- dry run
    def preview(self, spec: Any, *, job_id: str = "", persist: bool = False) -> dict[str, Any]:
        """Describe the request that *would* be sent. Performs no HTTP."""
        request = build_request(spec)
        warnings = validate_request(request)
        estimate = self.estimate(request)
        payload: dict[str, Any] = {
            "service": "openai",
            "kind": spec.kind,
            "job_id": job_id or spec.job_id,
            "dry_run": True,
            "http_sent": False,
            "method": "POST",
            "endpoint": self.endpoint,
            "headers": self.redacted_headers(),
            "request": request,
            "estimate": estimate.as_dict(),
            "gate": {
                "mode": self.cfg.mode,
                "spend_locked": self.cfg.spend_locked,
                "max_cost_usd": self.cfg.max_cost_usd,
            },
            "warnings": warnings,
            "key": self.preview_identity(),
        }
        if persist:
            payload["preview_saved_to"] = str(
                save_dry_run_preview(self.cfg.state_dir, f"openai_{job_id or 'job'}", payload)
            )
        return payload

    # ---------------------------------------------------------------- live
    def generate(self, spec: Any, *, job_id: str, ledger: Any | None = None) -> dict[str, Any]:
        """Send the billable request. Only reachable when the spend gate is OPEN."""
        if self.cfg.spend_locked:
            raise SpendLocked(
                f"refusing to call OpenAI while spend is locked ({self.cfg.mode}); "
                "use dry-run, or set ASSET_DRY_RUN=false and ASSET_AUTO_SPEND=true"
            )
        request = build_request(spec)
        warnings = validate_request(request)
        estimate = self.estimate(request)

        reservation_id: int | None = None
        if ledger is not None:
            reservation_id = ledger.check_and_reserve(
                job_id=job_id,
                service="openai",
                model=request["model"],
                kind="image",
                estimate=estimate,
                max_cost_usd=self.cfg.max_cost_usd,
                service_budget_usd=self.cfg.service_budget_usd.get("openai"),
                units=int(request.get("n", 1)),
                unit="image",
            )
        try:
            response = self.transport.request(
                "POST",
                self.endpoint,
                headers=self._auth_headers(),
                json_body=request,
                retry=False,
                billable=True,
            )
            if not response.ok:
                raise OpenAIImageError(response)
            payload = response.json()
            artifacts = self._store_images(payload, request)
            usage = payload.get("usage") or {}
            result = {
                "service": "openai",
                "kind": spec.kind,
                "job_id": job_id,
                "dry_run": False,
                "http_sent": True,
                "model": request["model"],
                "endpoint": self.endpoint,
                "http": response.summary(),
                "request": request,
                "artifacts": artifacts,
                "response_meta": {
                    key: payload.get(key)
                    for key in ("created", "background", "output_format", "size", "quality")
                    if payload.get(key) is not None
                },
                "usage": usage,
                "estimate": estimate.as_dict(),
                "warnings": warnings,
            }
            if ledger is not None and reservation_id is not None:
                ledger.commit(
                    reservation_id,
                    actual_usd=estimate.usd,
                    usage=usage,
                    note="actual cost not itemised by the API; estimate used (see docs)",
                )
            return result
        except Exception as exc:
            if ledger is not None and reservation_id is not None:
                # Transport-level failure before a usable response: safe to release.
                ambiguous = isinstance(getattr(exc, "response", None), HttpResponse) and getattr(
                    exc.response, "ambiguous", False
                )
                if not ambiguous:
                    ledger.release(reservation_id, f"request failed: {type(exc).__name__}")
            raise

    # ------------------------------------------------------------- storage
    def _store_images(self, payload: dict[str, Any], request: dict[str, Any]) -> list[dict[str, Any]]:
        if not isinstance(payload, dict):
            raise ValueError("response is not a JSON object")
        data = payload.get("data")
        if not isinstance(data, list) or not data:
            raise ValueError(f"response has no 'data' array: keys={sorted(payload)}")
        output_format = str(payload.get("output_format") or request.get("output_format") or "png")
        out_dir = self.cfg.raw_dir / "openai"
        out_dir.mkdir(parents=True, exist_ok=True)
        from .validate import validate_png  # local import avoids a cycle
        from PIL import Image  # noqa: F401 - proves Pillow can decode what we saved
        import io

        artifacts: list[dict[str, Any]] = []
        for index, item in enumerate(data):
            if not isinstance(item, dict):
                raise ValueError(f"data[{index}] is not an object")
            raw: bytes | None = None
            if item.get("b64_json"):
                raw = base64.b64decode(item["b64_json"])
            elif item.get("url"):
                # GPT image models always return b64_json, but never let a URL expire.
                tmp = out_dir / f".download-{index}.tmp"
                self.transport.download(item["url"], tmp)
                raw = tmp.read_bytes()
                tmp.unlink(missing_ok=True)
            if not raw:
                raise ValueError(f"data[{index}] has neither b64_json nor url")
            digest = hashlib.sha256(raw).hexdigest()
            extension = detect_image_extension(raw) or output_format
            path = out_dir / f"{digest}.{extension}"
            if not path.exists():
                path.write_bytes(raw)
            entry: dict[str, Any] = {
                "path": str(path),
                "sha256": digest,
                "bytes": len(raw),
                "format": extension,
            }
            if extension == "png":
                entry["validation"] = validate_png(path, expected_sizes=()).as_dict()
            else:
                entry["validation"] = self._validate_other(path)
            artifacts.append(entry)
        return artifacts

    @staticmethod
    def _validate_other(path: Path) -> dict[str, Any]:
        from PIL import Image

        try:
            with Image.open(path) as img:
                img.load()
                return {"ok": True, "kind": "image", "path": str(path), "format": img.format, "size": list(img.size)}
        except Exception as exc:  # noqa: BLE001
            return {"ok": False, "kind": "image", "path": str(path), "errors": [str(exc)]}

    # ------------------------------------------------------------ metadata
    def list_models(self) -> list[str]:
        """Non-billable ``GET /v1/models``."""
        response = self.transport.request(
            "GET", f"{self.cfg.openai_base_url}/models", headers=self._auth_headers(), retry=True
        )
        if not response.ok:
            raise RuntimeError(f"GET /v1/models failed: HTTP {response.status}")
        return sorted(model["id"] for model in response.json().get("data", []))


def detect_image_extension(raw: bytes) -> str | None:
    if raw[:8] == b"\x89PNG\r\n\x1a\n":
        return "png"
    if raw[:3] == b"\xff\xd8\xff":
        return "jpeg"
    if raw[:4] == b"RIFF" and raw[8:12] == b"WEBP":
        return "webp"
    return None


class OpenAIImageError(RuntimeError):
    def __init__(self, response: HttpResponse) -> None:
        self.response = response
        body = response.text(1500)
        hint = ""
        if response.status == 400:
            hint = " (invalid request parameters; see the response body)"
        elif response.status == 401:
            hint = " (OPENAI key rejected - check .env; the key is never printed)"
        elif response.status == 403:
            hint = " (this project may not have access to the requested model)"
        elif response.status == 410:
            hint = " (the requested endpoint/model has been retired)"
        elif response.status == 429:
            hint = " (rate limited; retry later - the request was not processed)"
        message = f"OpenAI Images API HTTP {response.status}{hint}: {body}"
        super().__init__(message)
        self.ambiguous = response.ambiguous

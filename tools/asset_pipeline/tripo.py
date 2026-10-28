"""Tripo3D API client (text -> 3D model, image -> 3D model).

Endpoint facts (verified 2026-10-28, see docs/ASSET_PIPELINE.md):

* ``GET https://openapi.tripo3d.ai/v3/account/balance`` -> HTTP 200
  ``{"code":0,"status":"success","data":{"balance":600.00,"frozen":0.00}}``
* legacy ``GET https://api.tripo3d.ai/v2/openapi/user/balance`` -> HTTP 200 (still alive)
* ``GET /v3/tasks/<unknown>`` -> HTTP 404 ``{"code":2001,...}`` (error shape)
* route existence for ``POST /v3/generation/text-to-model`` and
  ``POST /v3/generation/image-to-model`` confirmed with a non-billable GET probe
  (HTTP 405 "HTTP method 'GET' is not supported for this endpoint; use POST").
* same probe confirms the legacy ``POST /v2/openapi/task`` route still exists.

Task creation is **never** performed in DRY RUN. The task_id is persisted before
polling, and the result GLB is downloaded immediately because Tripo output URLs expire.
"""

from __future__ import annotations

import hashlib
import json
import time
import uuid
from pathlib import Path
from typing import Any, Callable

from .config import Config, CostEstimate
from .env import mask
from .http_client import HttpResponse, Transport
from .openai_image import SpendLocked
from .util import save_dry_run_preview

__all__ = ["DEFAULT_MODEL_VERSION", "TERMINAL_STATUSES", "TripoClient", "TripoError"]

#: Default generation model version. Taken from the vendor SDK's default
#: (``tripo-python-sdk`` ``text_to_model(model_version="v2.5-20250123")``). The exact
#: version available to *this* account is UNVERIFIED; override per job.
DEFAULT_MODEL_VERSION = "v2.5-20250123"

TERMINAL_STATUSES = {"success", "failed", "banned", "expired", "cancelled"}

#: Extra generation parameters accepted by the documented v3 schema.
ALLOWED_MODEL_PARAMS = {
    "model_version",
    "face_limit",
    "texture",
    "pbr",
    "texture_quality",
    "geometry_quality",
    "quad",
    "smart_low_poly",
    "export_uv",
    "negative_prompt",
    "image_seed",
    "model_seed",
    "texture_seed",
    "auto_size",
    "generate_parts",
    "compress",
    "texture_alignment",
    "orientation",
    "enable_image_autofix",
}

#: Candidate output keys for the downloadable model, best first.
_MODEL_KEYS = (
    "pbr_model_url",
    "model_url",
    "pbr_model",
    "model",
    "base_model_url",
    "base_model",
)
_PREVIEW_KEYS = (
    "rendered_image_url",
    "rendered_image",
    "front_view_url",
    "front_view",
    "image",
)


class TripoError(RuntimeError):
    def __init__(self, message: str, *, response: HttpResponse | None = None) -> None:
        super().__init__(message)
        self.response = response
        self.ambiguous = bool(response and response.ambiguous)


class TripoClient:
    def __init__(self, cfg: Config, *, transport: Transport | None = None) -> None:
        self.cfg = cfg
        self.transport = transport or Transport(timeout_s=120.0, max_retries=3)

    # ---------------------------------------------------------------- paths
    @property
    def create_url_text(self) -> str:
        return f"{self.cfg.tripo_base_url}/v3/generation/text-to-model"

    @property
    def create_url_image(self) -> str:
        return f"{self.cfg.tripo_base_url}/v3/generation/image-to-model"

    @property
    def create_url_text_v2(self) -> str:
        return f"{self.cfg.tripo_v2_base_url}/task"

    def task_url(self, task_id: str, *, api: str = "v3") -> str:
        if api == "v2":
            return f"{self.cfg.tripo_v2_base_url}/task/{task_id}"
        return f"{self.cfg.tripo_base_url}/v3/tasks/{task_id}"

    def _auth_headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.cfg.env.secret('TRIPO')}"}

    def redacted_headers(self) -> dict[str, str]:
        return {"Content-Type": "application/json", "Authorization": "Bearer ***REDACTED***"}

    # -------------------------------------------------------------- request
    def build_task_request(self, spec: Any, *, api: str = "v3") -> dict[str, Any]:
        params = dict(spec.params)
        unknown = set(params) - ALLOWED_MODEL_PARAMS
        if unknown:
            raise ValueError(
                f"unknown model parameter(s) {sorted(unknown)}; allowed: {sorted(ALLOWED_MODEL_PARAMS)}"
            )
        model_version = str(params.pop("model_version", None) or spec.model or DEFAULT_MODEL_VERSION)
        mode = spec.data.get("mode") or "text_to_model"
        body: dict[str, Any] = {"model": model_version}
        if mode == "text_to_model":
            body["prompt"] = spec.prompt
            if spec.data.get("negative_prompt"):
                body["negative_prompt"] = spec.data["negative_prompt"]
            if api == "v2":
                body["type"] = "text_to_model"
                body["model_version"] = body.pop("model")
        elif mode == "image_to_model":
            input_ref = spec.data.get("input_image")
            if not input_ref:
                raise ValueError("image_to_model needs 'input_image'")
            if api == "v2":
                raise ValueError(
                    "legacy v2 image_to_model requires the /v2/openapi/upload flow, which "
                    "is UNVERIFIED; use the v3 API (default) for image_to_model"
                )
            body["input"] = input_ref  # replaced by a file_token after upload
        else:  # pragma: no cover - jobs.py validates the mode
            raise ValueError(f"unsupported mode {mode!r}")
        for key, value in params.items():
            if value is not None:
                body[key] = value
        return body

    def estimate(self, mode: str = "text_to_model") -> CostEstimate:
        return self.cfg.tripo_estimate(mode)

    # -------------------------------------------------------------- dry run
    def preview(self, spec: Any, *, job_id: str = "", persist: bool = False) -> dict[str, Any]:
        mode = spec.data.get("mode") or "text_to_model"
        body = self.build_task_request(spec)
        warnings = [
            f"model version {body.get('model')!r} is the SDK default and is UNVERIFIED for this account",
        ]
        if mode == "image_to_model":
            path = Path(str(spec.data.get("input_image")))
            upload = {
                "endpoint": f"{self.cfg.tripo_base_url}/v3/files",
                "method": "POST",
                "content_type": "multipart/form-data",
                "field": "file",
                "file": str(path),
                "file_sha256": _sha256_or_none(path),
            }
            warnings.append(
                "image_to_model uploads the image to /v3/files first; the multipart flow is "
                "documented by the vendor SDK but was NOT exercised against the API"
            )
        else:
            upload = None
        estimate = self.estimate(mode)
        payload: dict[str, Any] = {
            "service": "tripo",
            "kind": spec.kind,
            "job_id": job_id or spec.job_id,
            "dry_run": True,
            "http_sent": False,
            "create": {
                "method": "POST",
                "endpoint": self.create_url_image if mode == "image_to_model" else self.create_url_text,
                "headers": self.redacted_headers(),
                "request": body,
            },
            "upload": upload,
            "polling": {
                "method": "GET",
                "endpoint_template": self.task_url("{task_id}"),
                "interval_s": 2.0,
                "timeout_s": 300.0,
                "terminal_statuses": sorted(TERMINAL_STATUSES),
            },
            "download": {
                "note": "output URLs expire: download immediately on success, before validation",
                "destination": str(self.cfg.raw_dir / "tripo" / "{task_id}.glb"),
            },
            "estimate": estimate.as_dict(),
            "gate": {
                "mode": self.cfg.mode,
                "spend_locked": self.cfg.spend_locked,
                "max_cost_usd": self.cfg.max_cost_usd,
            },
            "warnings": warnings,
            "key": {"value": mask(self.cfg.env.values.get("TRIPO")), "source": str(self.cfg.env.dotenv_path)},
        }
        if persist:
            payload["preview_saved_to"] = str(
                save_dry_run_preview(self.cfg.state_dir, f"tripo_{job_id or 'job'}", payload)
            )
        return payload

    # ------------------------------------------------------------- metadata
    def balance(self, *, api: str = "v3") -> dict[str, Any]:
        """Non-billable balance check (both API generations are still alive)."""
        url = (
            f"{self.cfg.tripo_base_url}/v3/account/balance"
            if api == "v3"
            else f"{self.cfg.tripo_v2_base_url}/user/balance"
        )
        response = self.transport.request("GET", url, headers=self._auth_headers(), retry=True)
        if not response.ok:
            raise TripoError(f"GET {url} failed: HTTP {response.status}", response=response)
        return response.json()

    def get_task(self, task_id: str, *, api: str = "v3") -> dict[str, Any]:
        response = self.transport.request(
            "GET", self.task_url(task_id, api=api), headers=self._auth_headers(), retry=True
        )
        if not response.ok:
            raise TripoError(
                f"GET task {task_id} failed: HTTP {response.status} {response.text(400)}",
                response=response,
            )
        data = _unwrap(response.json())
        if not isinstance(data, dict):
            raise TripoError(f"unexpected task payload for {task_id}: {data!r}", response=response)
        data.setdefault("task_id", task_id)
        return data

    # --------------------------------------------------------------- upload
    def upload_image(self, path: str | Path) -> str:
        """Upload via documented ``POST /v3/files`` multipart (returns ``file_token``)."""
        file_path = Path(path)
        if not file_path.is_file():
            raise TripoError(f"input image not found: {file_path}")
        content = file_path.read_bytes()
        boundary = "----nocterra" + uuid.uuid4().hex
        mime = _mime_for(file_path)
        body = b"".join(
            [
                f"--{boundary}\r\n".encode(),
                f'Content-Disposition: form-data; name="file"; filename="{file_path.name}"\r\n'.encode(),
                f"Content-Type: {mime}\r\n\r\n".encode(),
                content,
                f"\r\n--{boundary}--\r\n".encode(),
            ]
        )
        headers = {
            **self._auth_headers(),
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        }
        response = self.transport.request(
            "POST", f"{self.cfg.tripo_base_url}/v3/files", headers=headers, data=body, retry=False
        )
        if not response.ok:
            raise TripoError(
                f"POST /v3/files failed: HTTP {response.status} {response.text(400)}", response=response
            )
        payload = _unwrap(response.json())
        token = payload.get("file_token") if isinstance(payload, dict) else None
        if not token:
            raise TripoError(f"upload response has no file_token: {payload!r}", response=response)
        return str(token)

    # --------------------------------------------------------------- create
    def create_task(
        self,
        spec: Any,
        *,
        job_id: str,
        api: str = "v3",
        ledger: Any | None = None,
    ) -> dict[str, Any]:
        """Create the billable task. Only reachable when the spend gate is OPEN.

        Returns ``{"task_id": ..., "reservation_id": ...}``; the caller commits the
        reservation once the model has been produced (or releases it on failure).
        """
        if self.cfg.spend_locked:
            raise SpendLocked(
                f"refusing to create a Tripo task while spend is locked ({self.cfg.mode})"
            )
        mode = spec.data.get("mode") or "text_to_model"
        body = self.build_task_request(spec, api=api)
        if mode == "image_to_model":
            token = self.upload_image(spec.data["input_image"])
            body["input"] = token
        estimate = self.estimate(mode)

        reservation_id: int | None = None
        if ledger is not None:
            reservation_id = ledger.check_and_reserve(
                job_id=job_id,
                service="tripo",
                model=str(body.get("model")),
                kind=mode,
                estimate=estimate,
                max_cost_usd=self.cfg.max_cost_usd,
                service_budget_usd=self.cfg.service_budget_usd.get("tripo"),
                units=1,
                unit="task",
            )
        url = self.create_url_image if mode == "image_to_model" else self.create_url_text
        try:
            response = self.transport.request(
                "POST", url, headers=self._auth_headers(), json_body=body, retry=False, billable=True
            )
            if not response.ok:
                raise TripoError(
                    f"POST {url} failed: HTTP {response.status} {response.text(600)}",
                    response=response,
                )
            payload = _unwrap(response.json())
            task_id = payload.get("task_id") if isinstance(payload, dict) else None
            if not task_id:
                raise TripoError(f"create response has no task_id: {payload!r}", response=response)
            return {"task_id": str(task_id), "reservation_id": reservation_id}
        except Exception as exc:
            if reservation_id is not None and ledger is not None:
                if not getattr(exc, "ambiguous", False):
                    ledger.release(reservation_id, f"task creation failed: {type(exc).__name__}")
            raise

    # ---------------------------------------------------------------- poll
    def wait_for_task(
        self,
        task_id: str,
        *,
        api: str = "v3",
        interval_s: float = 2.0,
        timeout_s: float = 300.0,
        on_progress: Callable[[dict[str, Any]], None] | None = None,
    ) -> dict[str, Any]:
        """Poll until a terminal status. Polling is not billable, so retries are fine."""
        started = time.monotonic()
        delay = max(0.5, float(interval_s))
        while True:
            task = self.get_task(task_id, api=api)
            status = str(task.get("status", "")).lower()
            if on_progress:
                on_progress(task)
            if status in TERMINAL_STATUSES:
                return task
            if time.monotonic() - started > timeout_s:
                raise TripoError(
                    f"timed out after {timeout_s:.0f}s waiting for Tripo task {task_id} "
                    f"(last status {status!r}); the task may still finish - poll again later"
                )
            time.sleep(delay)
            delay = min(delay * 1.6, 10.0)

    # ------------------------------------------------------------ download
    def download_output(self, task: dict[str, Any], *, destination_dir: str | Path | None = None) -> dict[str, Any]:
        """Download the result GLB (and preview image) right away: URLs expire."""
        task_id = str(task.get("task_id") or "")
        output = task.get("output") or {}
        if not isinstance(output, dict):
            raise TripoError(f"task {task_id} output is not an object: {output!r}")
        model_url = next((str(output[k]) for k in _MODEL_KEYS if output.get(k)), None)
        preview_url = next((str(output[k]) for k in _PREVIEW_KEYS if output.get(k)), None)
        if not model_url:
            raise TripoError(
                f"task {task_id} succeeded but no model URL found in output keys {sorted(output)}"
            )
        out_dir = Path(destination_dir) if destination_dir else self.cfg.raw_dir / "tripo"
        out_dir.mkdir(parents=True, exist_ok=True)
        extension = _extension_from_url(model_url) or ".glb"
        model_path = out_dir / f"{task_id or 'task'}{extension}"
        self.transport.download(model_url, model_path)
        artifact: dict[str, Any] = {
            "model_path": str(model_path),
            "model_url": model_url,
            "source_url_saved_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
            "bytes": model_path.stat().st_size,
            "sha256": _sha256_or_none(model_path),
        }
        if preview_url:
            preview_path = out_dir / f"{task_id or 'task'}_preview{_extension_from_url(preview_url) or '.jpg'}"
            try:
                self.transport.download(preview_url, preview_path)
                artifact["preview_path"] = str(preview_path)
            except Exception as exc:  # noqa: BLE001 - preview is optional
                artifact["preview_error"] = str(exc)[:300]
        return artifact


# --------------------------------------------------------------------------
# helpers
# --------------------------------------------------------------------------
def _unwrap(payload: Any) -> Any:
    """Tripo wraps every payload in ``{"code": 0, "data": ...}``.

    Error responses (verified against the live API: HTTP 404 for an unknown task)
    look like ``{"code": 2001, "status": "error", "message": ..., "suggestion": ...}``
    and carry **no** ``data`` key, so the error check must not depend on ``data``.
    """
    if isinstance(payload, dict) and "code" in payload:
        if payload.get("code") not in (0, None):
            raise TripoError(
                f"Tripo API error code={payload.get('code')}: {payload.get('message')} "
                f"(suggestion: {payload.get('suggestion')})"
            )
        if "data" in payload:
            return payload["data"]
    return payload


def _sha256_or_none(path: str | Path) -> str | None:
    p = Path(path)
    if not p.is_file():
        return None
    digest = hashlib.sha256()
    with p.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 256), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _extension_from_url(url: str) -> str | None:
    path = url.split("?", 1)[0].split("#", 1)[0]
    for ext in (".glb", ".gltf", ".zip", ".fbx", ".obj", ".usdz", ".stl", ".jpg", ".jpeg", ".png", ".webp"):
        if path.lower().endswith(ext):
            return ext
    return None


def _mime_for(path: Path) -> str:
    return {
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".webp": "image/webp",
    }.get(path.suffix.lower(), "application/octet-stream")


def pretty_task(task: dict[str, Any]) -> str:
    return json.dumps(
        {
            "task_id": task.get("task_id"),
            "status": task.get("status"),
            "progress": task.get("progress"),
            "output_keys": sorted((task.get("output") or {}).keys())
            if isinstance(task.get("output"), dict)
            else None,
        },
        ensure_ascii=False,
    )

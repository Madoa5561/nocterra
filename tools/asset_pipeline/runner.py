"""Job orchestration: DRY RUN previews and live execution.

The CLI is a thin wrapper around :func:`dry_run_job`, :func:`run_job` and
:func:`process_local_texture`. Keeping the orchestration here means the same code path
is used by the self-test and by the operator, so "verified in DRY RUN" really means
"verified in the exact code path that would spend money".
"""

from __future__ import annotations

import time
import traceback
from pathlib import Path
from typing import Any, Callable

from .config import Config
from .jobs import JobSpec
from .ledger import BudgetExceeded, Ledger
from .manifest import AssetManifest
from .openai_image import OpenAIImageClient, SpendLocked, build_request as build_image_request
from .queue import JobQueue
from .texture import process_texture
from .tripo import TripoClient, TripoError
from .fish import FishClient, FishError
from .util import atomic_write_json, save_dry_run_preview
from .validate import validate_glb, validate_png, validate_audio

__all__ = [
    "PipelineError",
    "clients_for",
    "dry_run_job",
    "process_local_texture",
    "run_job",
]


class PipelineError(RuntimeError):
    """Operational error with a human-readable message."""


# --------------------------------------------------------------------------
# clients
# --------------------------------------------------------------------------
def clients_for(cfg: Config) -> dict[str, Any]:
    return {
        "openai": OpenAIImageClient(cfg),
        "tripo": TripoClient(cfg),
        "fish": FishClient(cfg),
    }


# --------------------------------------------------------------------------
# DRY RUN
# --------------------------------------------------------------------------
def dry_run_job(cfg: Config, spec: JobSpec, *, persist: bool = True) -> dict[str, Any]:
    """Build the exact request(s) that would be sent. Guarantees zero HTTP traffic."""
    client = clients_for(cfg)[spec.service]
    job_id = spec.job_id or f"{spec.kind}_preview"
    preview = client.preview(spec, job_id=job_id, persist=False)
    preview["job"] = {
        "id": job_id,
        "source": spec.source,
        "kind": spec.kind,
        "service": spec.service,
        "model": spec.model,
    }
    preview["safety"] = {
        "mode": cfg.mode,
        "spend_locked": cfg.spend_locked,
        "max_cost_usd": cfg.max_cost_usd,
        "statement": "No HTTP request was sent. Values below are the exact request body "
        "and the pre-flight cost estimate.",
    }
    preview["next_step"] = (
        "Review the request above. Live execution requires ASSET_DRY_RUN=false, "
        "ASSET_AUTO_SPEND=true and `run <job> --confirm-spend`."
    )
    if persist:
        preview["preview_saved_to"] = str(
            save_dry_run_preview(cfg.state_dir, f"{spec.service}_{job_id}", preview)
        )
    return preview


# --------------------------------------------------------------------------
# local texture processing (no API involved)
# --------------------------------------------------------------------------
def process_local_texture(
    cfg: Config,
    source: str | Path,
    *,
    out_dir: str | Path | None = None,
    base_name: str | None = None,
    options: dict[str, Any] | None = None,
    record_manifest: bool = False,
    source_asset_id: str | None = None,
) -> dict[str, Any]:
    """Run the texture chain on an existing image (used by tests and by ``texture`` CLI)."""
    opts = dict(options or {})
    destination = Path(out_dir) if out_dir else (cfg.processed_dir / "texture")
    report = process_texture(
        source,
        destination,
        base_name=base_name,
        sizes=opts.get("sizes", (16, 32, 64)),
        alpha_mode=opts.get("alpha_mode", "cutout"),
        alpha_threshold=int(opts.get("alpha_threshold", 128)),
        remove_bg=bool(opts.get("remove_background", True)),
        bg_method=opts.get("bg_method", "flood"),
        bg_tolerance=int(opts.get("bg_tolerance", 24)),
        key_color=opts.get("key_color"),
        quantize_colors=opts.get("quantize_colors", 16),
        dither=bool(opts.get("dither", False)),
        comparison_scale=int(opts.get("comparison_scale", 8)),
        write_comparison=bool(opts.get("write_comparison", True)),
    )
    for output in report["outputs"]:
        output["validation"] = validate_png(output["path"], expected_sizes=(16, 32, 64)).as_dict()
    if record_manifest:
        manifest = AssetManifest(cfg.manifests_dir)
        base_id = source_asset_id or f"texture_{base_name or Path(source).stem}"
        for output in report["outputs"]:
            manifest.add(
                asset_id=f"{base_id}_{output['size']}",
                kind="texture",
                path=output["path"],
                service="local",
                model=None,
                prompt=None,
                parameters={"operation": "nearest-resize", **opts, "size": output["size"]},
                stage="processed",
                source_asset_id=base_id,
                validation=output["validation"],
                warnings=[] if (
                    not output["tileability"].get("tiling_relevant", True)
                    or output["tileability"]["verdict"] == "seamless"
                ) else [f"tileability {output['tileability']['verdict']}"],
            )
        report["manifest_asset_ids"] = [f"{base_id}_{o['size']}" for o in report["outputs"]]
    return report


# --------------------------------------------------------------------------
# LIVE
# --------------------------------------------------------------------------
def _assert_live_allowed(cfg: Config, confirm_spend: bool) -> None:
    reasons: list[str] = []
    if cfg.dry_run:
        reasons.append("ASSET_DRY_RUN=true")
    if not cfg.auto_spend:
        reasons.append("ASSET_AUTO_SPEND=false")
    if not confirm_spend:
        reasons.append("missing --confirm-spend")
    if reasons:
        raise PipelineError(
            "live execution refused: " + ", ".join(reasons) + ". "
            "Use `dry-run <job>` to inspect the request, then flip the gates only after "
            "the Lead approves real spend."
        )


def run_job(
    cfg: Config,
    spec: JobSpec,
    *,
    confirm_spend: bool,
    ledger: Ledger,
    queue: JobQueue,
    on_progress: Callable[[str], None] | None = None,
) -> dict[str, Any]:
    """Execute a job for real. All spend gates are enforced before any HTTP."""
    _assert_live_allowed(cfg, confirm_spend)
    cfg.ensure_output_dirs()

    job_id, created = queue.add_job(
        kind=spec.kind,
        service=spec.service,
        model=spec.model,
        payload=spec.hash_payload(),
        job_id=spec.job_id or None,
        max_attempts=int(spec.data.get("max_attempts", cfg.max_attempts)),
        notes=spec.data.get("notes"),
    )
    existing = queue.get(job_id) or {}
    if existing.get("status") == "done":
        return {
            "ok": True,
            "deduplicated": True,
            "job_id": job_id,
            "message": "an identical request already completed; nothing was sent (no cost)",
            "artifact_path": existing.get("artifact_path"),
            "result": _safe_json(existing.get("result_json")),
        }
    if existing.get("status") == "running":
        raise PipelineError(
            f"job {job_id} is already running (attempt {existing.get('attempts')}); "
            "wait for it or call `list --status running`"
        )
    if not created and existing.get("status") == "failed":
        queue.requeue(job_id, reset_attempts=False)

    claimed = queue.claim_specific(job_id)
    if not claimed or claimed.get("status") != "running":
        raise PipelineError(f"could not claim job {job_id} (status={claimed.get('status') if claimed else None})")

    waited = queue.acquire_rate_slot(spec.service, cfg.min_interval_s.get(spec.service, 0.0))
    clients = clients_for(cfg)
    result: dict[str, Any] = {"ok": True, "job_id": job_id, "created": created, "rate_wait_s": waited}
    try:
        if spec.kind == "texture":
            payload = _run_texture(cfg, spec, clients, ledger, job_id)
        elif spec.kind == "model":
            payload = _run_model(cfg, spec, clients, ledger, queue, job_id, on_progress)
        elif spec.kind == "audio":
            payload = _run_audio(cfg, spec, clients, ledger, job_id)
        else:  # pragma: no cover
            raise PipelineError(f"unsupported job kind {spec.kind!r}")
        queue.mark_done(
            job_id,
            result=payload,
            artifact_path=payload.get("artifact_path"),
            remote_task_id=payload.get("remote_task_id"),
            source_url=payload.get("source_url"),
        )
        result.update(payload)
        return result
    except BudgetExceeded as exc:
        queue.mark_failed(job_id, error=f"budget: {exc}", retryable=False)
        raise
    except SpendLocked as exc:
        queue.mark_failed(job_id, error=f"spend locked: {exc}", retryable=False)
        raise
    except (TripoError, FishError) as exc:
        retryable = not getattr(exc, "ambiguous", False)
        queue.mark_failed(
            job_id,
            error=f"{type(exc).__name__}: {exc}",
            retryable=retryable,
            base_s=cfg.backoff_base_s,
            cap_s=cfg.backoff_cap_s,
        )
        result.update(ok=False, error=str(exc), retryable=retryable)
        return result
    except Exception as exc:  # noqa: BLE001 - surface everything to the caller
        queue.mark_failed(
            job_id,
            error=f"{type(exc).__name__}: {exc}",
            retryable=False,
            base_s=cfg.backoff_base_s,
            cap_s=cfg.backoff_cap_s,
        )
        result.update(
            ok=False,
            error=f"{type(exc).__name__}: {exc}",
            traceback=traceback.format_exc(limit=4),
        )
        return result


def _run_texture(
    cfg: Config,
    spec: JobSpec,
    clients: dict[str, Any],
    ledger: Ledger,
    job_id: str,
) -> dict[str, Any]:
    client: OpenAIImageClient = clients["openai"]
    generation = client.generate(spec, job_id=job_id, ledger=ledger)
    artifacts = generation.get("artifacts") or []
    if not artifacts:
        raise PipelineError("OpenAI returned no image artifacts")
    raw = artifacts[0]
    raw_path = Path(raw["path"])
    base = spec.job_id or f"texture_{job_id}"
    manifest = AssetManifest(cfg.manifests_dir)
    request = generation.get("request") or {}
    raw_entry = manifest.add(
        asset_id=f"{base}_raw",
        kind="texture",
        path=raw_path,
        service="openai",
        model=str(request.get("model")),
        prompt=spec.prompt,
        parameters=request,
        stage="raw",
        cost={
            "estimated_usd": (generation.get("estimate") or {}).get("usd"),
            "actual_usd": (generation.get("estimate") or {}).get("usd"),
            "note": "Images API does not return an itemised price; the ledger estimate is used",
        },
        job_id=job_id,
        license_note=spec.license_note,
        validation=raw.get("validation"),
        warnings=generation.get("warnings") or [],
    )
    processed = process_local_texture(
        cfg,
        raw_path,
        base_name=base,
        options=spec.texture,
        record_manifest=True,
        source_asset_id=raw_entry["asset_id"],
    )
    for output in processed["outputs"]:
        existing = manifest.find(f"{raw_entry['asset_id']}_{output['size']}")
        if existing:
            existing["prompt"] = spec.prompt
            existing["service"] = "openai"
            existing["model"] = request.get("model")
            existing["job_id"] = job_id
    manifest._atomic_write()  # noqa: SLF001 - intentional: stamp provenance in one pass
    return {
        "artifact_path": str(raw_path),
        "raw": raw,
        "processed": processed,
        "http": generation.get("http"),
        "usage": generation.get("usage"),
        "manifest_asset_ids": [raw_entry["asset_id"]]
        + [f"{raw_entry['asset_id']}_{o['size']}" for o in processed["outputs"]],
    }


def _run_model(
    cfg: Config,
    spec: JobSpec,
    clients: dict[str, Any],
    ledger: Ledger,
    queue: JobQueue,
    job_id: str,
    on_progress: Callable[[str], None] | None,
) -> dict[str, Any]:
    client: TripoClient = clients["tripo"]
    mode = spec.data.get("mode") or "text_to_model"
    estimate = client.estimate(mode)
    job = queue.get(job_id) or {}
    task_id = job.get("remote_task_id")
    created = client.create_task(spec, job_id=job_id, ledger=ledger) if not task_id else None
    reservation_id = None
    if created:
        task_id = created["task_id"]
        reservation_id = created.get("reservation_id")
        queue.set_remote_task_id(job_id, task_id)
    if not task_id:
        raise PipelineError("no Tripo task id available (create failed and none was persisted)")

    def _progress(task: dict[str, Any]) -> None:
        if on_progress:
            on_progress(
                f"tripo task {task_id}: {task.get('status')} {task.get('progress', 0)}%"
            )

    try:
        task = client.wait_for_task(task_id, interval_s=2.0, timeout_s=300.0, on_progress=_progress)
        status = str(task.get("status", "")).lower()
        if status != "success":
            raise PipelineError(f"Tripo task {task_id} ended with status {status!r}: {task.get('error') or ''}")
        artifact = client.download_output(task)
        queue.set_source_url(job_id, artifact.get("model_url") or "")
        validation = validate_glb(artifact["model_path"], max_triangles=cfg.max_tripo_triangles)
        if not validation.ok:
            raise PipelineError(
                "downloaded GLB failed validation: " + "; ".join(validation.errors)
            )
        if reservation_id is not None:
            ledger.commit(
                reservation_id,
                actual_usd=estimate.usd,
                credits=estimate.credits,
                note="Tripo deducts credits from the frozen balance; USD price is unverified",
            )
        base = spec.job_id or f"model_{job_id}"
        manifest = AssetManifest(cfg.manifests_dir)
        entry = manifest.add(
            asset_id=base,
            kind="model",
            path=artifact["model_path"],
            service="tripo",
            model=str(task.get("model") or spec.model),
            prompt=spec.prompt,
            parameters={
                "mode": mode,
                "request": spec.hash_payload(),
                "task_id": task_id,
                "output_keys": sorted((task.get("output") or {}).keys()),
            },
            stage="raw",
            cost={
                "estimated_usd": estimate.usd,
                "actual_usd": estimate.usd,
                "credits": estimate.credits,
                "note": "credit consumption not re-read from /account/usage; USD price unverified",
            },
            job_id=job_id,
            license_note=spec.license_note,
            validation=validation.as_dict(),
            warnings=validation.warnings,
            extra={"source_url": artifact.get("model_url"), "preview_path": artifact.get("preview_path")},
        )
        return {
            "artifact_path": artifact["model_path"],
            "remote_task_id": task_id,
            "source_url": artifact.get("model_url"),
            "validation": validation.as_dict(),
            "manifest_asset_ids": [entry["asset_id"]],
            "triangles": validation.details.get("triangle_estimate"),
            "credits": estimate.credits,
        }
    except Exception:
        if reservation_id is not None:
            ledger.release(reservation_id, "model task did not complete successfully")
        raise


def _run_audio(
    cfg: Config,
    spec: JobSpec,
    clients: dict[str, Any],
    ledger: Ledger,
    job_id: str,
) -> dict[str, Any]:
    client: FishClient = clients["fish"]
    result = client.synthesize(spec, job_id=job_id, ledger=ledger)
    raw = result["raw_artifact"]
    base = spec.job_id or f"audio_{job_id}"
    manifest = AssetManifest(cfg.manifests_dir)
    entry = manifest.add(
        asset_id=base,
        kind="audio",
        path=raw["path"],
        service="fish",
        model=result.get("tts_model"),
        prompt=spec.prompt,
        parameters={
            "audio": spec.audio,
            "reference_id": spec.audio.get("reference_id"),
            "requested_format": result.get("request", {}).get("format"),
        },
        stage="raw",
        cost={"estimated_usd": None, "actual_usd": None, "note": "Fish pricing unverified"},
        job_id=job_id,
        license_note=spec.license_note,
        validation=result.get("raw_validation"),
    )
    game_entry = None
    conversion = result.get("conversion") or {}
    if conversion.get("game_path"):
        game_entry = manifest.add(
            asset_id=f"{base}_ogg",
            kind="audio",
            path=conversion["game_path"],
            service="local",
            model=None,
            prompt=None,
            parameters={"operation": "ffmpeg libvorbis", "source": entry["asset_id"]},
            stage="processed",
            cost={"estimated_usd": 0.0, "actual_usd": 0.0, "note": "local conversion"},
            job_id=job_id,
            source_asset_id=entry["asset_id"],
            validation=conversion.get("validation"),
        )
    return {
        "artifact_path": raw["path"],
        "game_artifact_path": conversion.get("game_path"),
        "conversion": conversion,
        "manifest_asset_ids": [entry["asset_id"]] + ([game_entry["asset_id"]] if game_entry else []),
    }


# --------------------------------------------------------------------------
# helpers
# --------------------------------------------------------------------------
def _safe_json(value: Any) -> Any:
    import json

    if value is None:
        return None
    try:
        return json.loads(value)
    except (TypeError, json.JSONDecodeError):
        return value


def write_run_report(cfg: Config, payload: dict[str, Any], name: str = "last-run") -> Path:
    return atomic_write_json(cfg.state_dir / "reports" / f"{name}.json", payload)

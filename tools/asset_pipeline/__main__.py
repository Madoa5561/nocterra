"""Command line interface: ``python -m asset_pipeline <command>`` (run from ``tools/``).

Commands
--------
status          configuration, safety gates, queue/ledger/manifest summary
cost            cost ledger totals (per service and per job)
dry-run <job>   build and print the exact request; NEVER sends HTTP
run <job>       execute for real (needs the spend gates open + --confirm-spend)
list            list queued jobs (or manifest assets)
validate <...>  format-check glb / png / ogg-wav-mp3 / json
texture <img>   local texture processing (no API)
balance         non-billable Tripo balance probe
voices          non-billable Fish voice-model listing
converts audio  WAV/MP3 -> Ogg Vorbis via ffmpeg
recover         return interrupted (lease-expired) jobs to pending
produce <group> run the approved LIVE production batch (hud | textures | audio | tripo | all)
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Sequence

from . import __version__
from .audio_tools import FFmpegUnavailable, convert_to_ogg_vorbis, ffmpeg_version, find_ffmpeg, ffprobe_duration
from .config import Config, ConfigError
from .env import Env, MissingSecretError
from .jobs import JobSpecError, load_job
from .ledger import BudgetExceeded, Ledger
from .manifest import AssetManifest
from .openai_image import SpendLocked
from .production import run_group
from .queue import JobQueue
from .runner import PipelineError, dry_run_job, process_local_texture, run_job, write_run_report
from .util import human_bytes, human_usd, short
from .validate import validate_path

EXIT_OK = 0
EXIT_ERROR = 1
EXIT_CONFIG = 2
EXIT_INVALID = 3


# --------------------------------------------------------------------------
# helpers
# --------------------------------------------------------------------------
def _print_json(payload: Any) -> None:
    print(json.dumps(payload, ensure_ascii=False, indent=2))


def _config(args: argparse.Namespace) -> Config:
    env = Env(repo_root=getattr(args, "repo", None))
    cfg = Config.load(env)
    # Every outbound request is audited here. In DRY RUN the log stays absent/empty,
    # which is how the self-test proves that no HTTP was sent.
    from .http_client import set_audit_log

    set_audit_log(cfg.state_dir / "http_audit.jsonl")
    return cfg


def _open_state(cfg: Config) -> tuple[Ledger, JobQueue]:
    cfg.ensure_state_dirs()
    return Ledger(cfg.state_dir / "ledger.sqlite"), JobQueue(cfg.state_dir / "queue.sqlite")


def _manifest(cfg: Config) -> AssetManifest:
    return AssetManifest(cfg.manifests_dir)


def _gate_lines(cfg: Config) -> list[str]:
    lines = [
        f"mode            : {cfg.mode}",
        f"ASSET_DRY_RUN   : {cfg.dry_run}",
        f"ASSET_AUTO_SPEND: {cfg.auto_spend}",
        f"max cost        : {human_usd(cfg.max_cost_usd)}",
    ]
    for warning in cfg.warnings:
        lines.append(f"warning         : {warning}")
    return lines


# --------------------------------------------------------------------------
# status
# --------------------------------------------------------------------------
def cmd_status(args: argparse.Namespace) -> int:
    cfg = _config(args)
    ledger, queue = _open_state(cfg)
    try:
        recovered = queue.recover_stale() if args.recover else []
        payload: dict[str, Any] = {
            "version": __version__,
            "config": cfg.describe(),
            "env": cfg.env.describe(),
            "ffmpeg": {
                "available": bool(find_ffmpeg()),
                "path": find_ffmpeg(),
                "version": ffmpeg_version(),
            },
            "queue": queue.stats(),
            "ledger": {
                "committed_usd": ledger.committed_total(),
                "reserved_usd": ledger.outstanding_total(),
                "by_service": ledger.totals()["by_service"],
            },
            "manifest": _manifest(cfg).summary(),
        }
        if recovered:
            payload["recovered_jobs"] = recovered
        if args.probe:
            payload["probes"] = _probe_services(cfg)
        if args.json:
            _print_json(payload)
            return EXIT_OK
        _render_status(payload)
        return EXIT_OK
    finally:
        ledger.close()
        queue.close()


def _probe_services(cfg: Config) -> dict[str, Any]:
    """Non-billable metadata probes only (no generation requests)."""
    from .fish import FishClient
    from .http_client import Transport
    from .tripo import TripoClient

    out: dict[str, Any] = {}
    transport = Transport(timeout_s=40.0, max_retries=1)
    try:
        tripo = TripoClient(cfg, transport=transport)
        out["tripo"] = {"v3_balance": tripo.balance(api="v3"), "v2_balance": tripo.balance(api="v2")}
    except Exception as exc:  # noqa: BLE001
        out["tripo"] = {"error": f"{type(exc).__name__}: {exc}"}
    try:
        fish = FishClient(cfg, transport=transport)
        voices = fish.list_models(page_size=3)
        out["fish"] = {"voice_models_sample": voices, "note": "GET /model is non-billable"}
    except Exception as exc:  # noqa: BLE001
        out["fish"] = {"error": f"{type(exc).__name__}: {exc}"}
    try:
        from .openai_image import OpenAIImageClient

        openai = OpenAIImageClient(cfg, transport=transport)
        ids = openai.list_models()
        image_ids = [i for i in ids if "image" in i]
        out["openai"] = {
            "total_models": len(ids),
            "image_models": image_ids,
            "note": "GET /v1/models is non-billable",
        }
    except Exception as exc:  # noqa: BLE001
        out["openai"] = {"error": f"{type(exc).__name__}: {exc}"}
    return out


def _render_status(payload: dict[str, Any]) -> None:
    cfg = payload["config"]
    print("=" * 72)
    print(f"NOCTERRA asset pipeline v{payload['version']}  [{cfg['mode']}]")
    print("=" * 72)
    print("Safety gates")
    for line in _gate_lines_from(cfg):
        print(f"  {line}")
    print("\nSecrets (masked, read from .env)")
    env = payload["env"]
    print(f"  .env              : {env['dotenv_path']} (found={env['dotenv_found']})")
    for name, value in env["secrets"].items():
        print(f"  {name:<18}: {value}")
    print("\nPaths")
    for key, value in cfg["paths"].items():
        print(f"  {key:<14}: {value}")
    print("\nEndpoints")
    for key, value in cfg["endpoints"].items():
        print(f"  {key:<10}: {value}")
    print("\nTooling")
    print(f"  ffmpeg            : {payload['ffmpeg']['version'] or 'NOT FOUND'}")
    print("\nQueue")
    queue = payload["queue"]
    print(f"  {queue['database']}")
    print(f"  total={queue['total']} by_status={queue['by_status']} dedupe_hits={queue['dedupe_hits']}")
    if payload.get("recovered_jobs"):
        print(f"  recovered (interrupted -> pending): {payload['recovered_jobs']}")
    print("\nCost ledger")
    ledger = payload["ledger"]
    print(f"  committed={human_usd(ledger['committed_usd'])} reserved={human_usd(ledger['reserved_usd'])}")
    for service, values in ledger["by_service"].items():
        print(
            f"  {service:<8} committed={human_usd(values['committed_usd'])} "
            f"reserved={human_usd(values['reserved_usd'])} credits={values['credits']} "
            f"jobs={values['committed_jobs']}"
        )
    print("\nManifest")
    manifest = payload["manifest"]
    print(f"  {manifest['manifest']}")
    print(
        f"  assets={manifest['asset_count']} by_kind={manifest['by_kind']} "
        f"total_actual={human_usd(manifest['total_actual_usd'])}"
    )
    probes = payload.get("probes")
    if probes:
        print("\nNon-billable metadata probes")
        print(json.dumps(probes, ensure_ascii=False, indent=2))
    print("\nNext: `python -m asset_pipeline dry-run <job>` (no HTTP).")


def _gate_lines_from(cfg: dict[str, Any]) -> list[str]:
    lines = [
        f"mode             : {cfg['mode']}",
        f"ASSET_DRY_RUN    : {cfg['dry_run']}",
        f"ASSET_AUTO_SPEND : {cfg['auto_spend']}",
        f"max cost         : {human_usd(cfg['max_cost_usd'])}",
    ]
    lines.extend(f"warning          : {w}" for w in cfg["warnings"])
    return lines


# --------------------------------------------------------------------------
# cost
# --------------------------------------------------------------------------
def cmd_cost(args: argparse.Namespace) -> int:
    cfg = _config(args)
    ledger, queue = _open_state(cfg)
    try:
        totals = ledger.totals()
        totals["recent"] = ledger.recent(limit=args.limit)
        if args.json:
            _print_json(totals)
            return EXIT_OK
        print(f"Ledger: {totals['database']}")
        print(f"  committed : {human_usd(totals['committed_usd'])}")
        print(f"  reserved  : {human_usd(totals['reserved_usd'])}")
        if totals["by_service"]:
            print("\nBy service:")
            for service, values in totals["by_service"].items():
                print(
                    f"  {service:<8} committed={human_usd(values['committed_usd']):>12} "
                    f"reserved={human_usd(values['reserved_usd']):>12} "
                    f"entries={values['entries']}"
                )
        else:
            print("\n(no cost entries yet)")
        if totals["recent"]:
            print("\nRecent entries:")
            for entry in totals["recent"]:
                print(
                    f"  [{entry['id']:>3}] {entry['created_at']} {entry['service']:<7} "
                    f"{entry['status']:<9} est={human_usd(entry['estimated_usd'])} "
                    f"actual={human_usd(entry['actual_usd'])} job={short(entry['job_id'], 24)}"
                )
        return EXIT_OK
    finally:
        ledger.close()
        queue.close()


# --------------------------------------------------------------------------
# dry-run / run
# --------------------------------------------------------------------------
def cmd_dry_run(args: argparse.Namespace) -> int:
    cfg = _config(args)
    try:
        spec = load_job(args.job)
    except JobSpecError as exc:
        print(f"invalid job: {exc}", file=sys.stderr)
        return EXIT_INVALID
    cfg.ensure_state_dirs()
    payload = dry_run_job(cfg, spec, persist=not args.no_persist)
    if args.json:
        _print_json(payload)
        return EXIT_OK
    _render_preview(payload)
    return EXIT_OK


def _render_preview(payload: dict[str, Any]) -> None:
    job = payload.get("job", {})
    print("=" * 72)
    print(f"DRY RUN  {job.get('id')}  [{payload['service']}/{payload.get('kind')}]")
    print("=" * 72)
    print("No HTTP request was sent.")
    target = payload.get("create") or {}
    endpoint = target.get("endpoint") or payload.get("endpoint")
    method = target.get("method") or payload.get("method")
    print(f"\n{method} {endpoint}")
    print("Headers (redacted):")
    print(json.dumps(target.get("headers") or payload.get("headers"), ensure_ascii=False, indent=2))
    print("\nRequest body:")
    print(json.dumps(target.get("request") or payload.get("request"), ensure_ascii=False, indent=2))
    if payload.get("upload"):
        print("\nPre-upload:")
        print(json.dumps(payload["upload"], ensure_ascii=False, indent=2))
    if payload.get("polling"):
        print("\nPolling:")
        print(json.dumps(payload["polling"], ensure_ascii=False, indent=2))
    estimate = payload.get("estimate") or {}
    print("\nCost estimate:")
    print(
        f"  usd={human_usd(estimate.get('usd'))} credits={estimate.get('credits')} "
        f"priced={estimate.get('priced')}"
    )
    print(f"  detail={estimate.get('detail')}  source={estimate.get('source')}")
    gate = payload.get("gate") or {}
    print(f"\nGate: mode={gate.get('mode')} spend_locked={gate.get('spend_locked')} "
          f"max_cost={human_usd(gate.get('max_cost_usd'))}")
    for warning in payload.get("warnings") or []:
        print(f"  warning: {warning}")
    if payload.get("preview_saved_to"):
        print(f"\nPreview JSON saved to: {payload['preview_saved_to']}")


def cmd_run(args: argparse.Namespace) -> int:
    cfg = _config(args)
    try:
        spec = load_job(args.job)
    except JobSpecError as exc:
        print(f"invalid job: {exc}", file=sys.stderr)
        return EXIT_INVALID
    ledger, queue = _open_state(cfg)
    try:
        try:
            result = run_job(
                cfg,
                spec,
                confirm_spend=args.confirm_spend,
                ledger=ledger,
                queue=queue,
                on_progress=(lambda message: print(message, file=sys.stderr)) if not args.quiet else None,
            )
        except (PipelineError, SpendLocked, BudgetExceeded) as exc:
            print(f"run refused/failed: {exc}", file=sys.stderr)
            if args.json:
                _print_json({"ok": False, "error": str(exc), "spent": False})
            return EXIT_CONFIG
        if args.save_report:
            result["report_path"] = str(write_run_report(cfg, result, name=spec.job_id or "last-run"))
        if args.json:
            _print_json(result)
        else:
            _render_run(result)
        return EXIT_OK if result.get("ok", True) else EXIT_ERROR
    finally:
        ledger.close()
        queue.close()


def _render_run(result: dict[str, Any]) -> None:
    print("=" * 72)
    print(f"RUN {result.get('job_id')}  ok={result.get('ok', True)}")
    print("=" * 72)
    if result.get("deduplicated"):
        print(result.get("message"))
        return
    for key in ("artifact_path", "game_artifact_path", "remote_task_id", "source_url"):
        if result.get(key):
            print(f"  {key}: {result[key]}")
    processed = result.get("processed") or {}
    if processed.get("outputs"):
        print("\nProcessed textures:")
        for output in processed["outputs"]:
            tiling = output["tileability"]
            print(
                f"  {output['size']:>2}px {human_bytes(output['bytes']):>9} "
                f"tile={tiling['verdict']:<12} ratio={tiling['worst_ratio']} {output['path']}"
            )
    validation = result.get("validation")
    if validation:
        print(
            f"\nGLB: triangles={validation['details'].get('triangle_estimate')} "
            f"meshes={validation['details'].get('meshes')} uv={validation['details'].get('has_uv')} "
            f"ok={validation['ok']}"
        )
        for warning in validation.get("warnings") or []:
            print(f"  warning: {warning}")
    conversion = result.get("conversion")
    if conversion:
        print(f"\nAudio conversion: {conversion.get('status')} -> {conversion.get('game_path')}")
    if result.get("manifest_asset_ids"):
        print(f"\nManifest entries: {result['manifest_asset_ids']}")
    if result.get("error"):
        print(f"\nERROR: {result['error']}")


# --------------------------------------------------------------------------
# list
# --------------------------------------------------------------------------
def cmd_list(args: argparse.Namespace) -> int:
    cfg = _config(args)
    ledger, queue = _open_state(cfg)
    try:
        if args.manifest:
            manifest = _manifest(cfg)
            assets = manifest.list(kind=args.kind)
            if args.json:
                _print_json({"summary": manifest.summary(), "assets": assets})
                return EXIT_OK
            print(f"Manifest: {manifest.path} (assets={len(assets)})")
            for asset in assets:
                print(
                    f"  {asset['asset_id']:<32} {asset['kind']:<8} {asset['stage']:<9} "
                    f"{asset['service']:<7} {human_bytes(asset.get('bytes')):>10} "
                    f"sha={short(asset.get('sha256'), 12)}"
                )
            return EXIT_OK
        jobs = queue.list_jobs(status=args.status, service=args.service, limit=args.limit)
        if args.json:
            _print_json({"stats": queue.stats(), "jobs": jobs})
            return EXIT_OK
        print(f"Queue: {queue.path} ({queue.stats()['by_status']})")
        if not jobs:
            print("  (no jobs)")
        for job in jobs:
            print(
                f"  {job['id']:<36} {job['status']:<9} {job['kind']:<8} {job['service']:<7} "
                f"attempts={job['attempts']}/{job['max_attempts']} "
                f"next={short(job['next_run_at'], 14)} err={short(job.get('error'), 40)}"
            )
        return EXIT_OK
    finally:
        ledger.close()
        queue.close()


# --------------------------------------------------------------------------
# validate
# --------------------------------------------------------------------------
def cmd_validate(args: argparse.Namespace) -> int:
    cfg = _config(args)
    results: list[dict[str, Any]] = []
    for raw in args.paths:
        result = validate_path(
            raw, max_triangles=cfg.max_tripo_triangles, expected_sizes=(16, 32, 64)
        )
        results.append(result.as_dict())
    if args.json:
        _print_json({"ok": all(r["ok"] for r in results), "results": results})
    else:
        for result in results:
            status = "PASS" if result["ok"] else "FAIL"
            print(f"[{status}] {result['kind']:<6} {result['path']}")
            for error in result["errors"]:
                print(f"   error: {error}")
            for warning in result["warnings"]:
                print(f"   warn : {warning}")
            details = result["details"]
            if result["kind"] == "glb":
                print(
                    f"   glb: v{details.get('version')} chunks={len(details.get('chunks', []))} "
                    f"meshes={details.get('meshes')} tris≈{details.get('triangle_estimate')} "
                    f"uv={details.get('has_uv')} materials={details.get('materials')}"
                )
            elif result["kind"] == "png":
                print(
                    f"   png: {details.get('width')}x{details.get('height')} "
                    f"mode={details.get('mode')} alpha={details.get('has_alpha')}"
                )
            elif result["kind"] == "audio":
                print(
                    f"   audio: container={details.get('container')} codec={details.get('codec')} "
                    f"duration={details.get('duration_s')}"
                )
    return EXIT_OK if all(r["ok"] for r in results) else EXIT_INVALID


# --------------------------------------------------------------------------
# local texture
# --------------------------------------------------------------------------
def cmd_texture(args: argparse.Namespace) -> int:
    cfg = _config(args)
    options = {
        "sizes": tuple(int(s) for s in args.sizes),
        "alpha_mode": args.alpha_mode,
        "remove_background": not args.keep_background,
        "bg_method": args.bg_method,
        "bg_tolerance": args.bg_tolerance,
        "quantize_colors": args.quantize_colors,
        "comparison_scale": args.comparison_scale,
    }
    try:
        report = process_local_texture(
            cfg,
            args.image,
            out_dir=args.out,
            base_name=args.name,
            options=options,
            record_manifest=args.record_manifest,
        )
    except (OSError, ValueError) as exc:
        print(f"texture processing failed: {exc}", file=sys.stderr)
        return EXIT_ERROR
    if args.json:
        _print_json(report)
        return EXIT_OK
    print(f"source: {report['source']} {report['source_size']}")
    background = report.get("background_removal") or {}
    if not background.get("skipped"):
        print(
            f"background: removed {background.get('removed_pixels')} px "
            f"({background.get('removed_ratio')}) key={background.get('key_color_rgb')}"
        )
    for output in report["outputs"]:
        tiling = output["tileability"]
        print(
            f"  {output['size']:>2}px -> {output['path']} "
            f"tile={tiling['verdict']} ratio_x={tiling['ratio_x']} ratio_y={tiling['ratio_y']}"
        )
    print(f"comparison: {report.get('comparison')}")
    for warning in report.get("warnings") or []:
        print(f"  warning: {warning}")
    return EXIT_OK


# --------------------------------------------------------------------------
# metadata helpers
# --------------------------------------------------------------------------
def cmd_balance(args: argparse.Namespace) -> int:
    cfg = _config(args)
    from .tripo import TripoClient

    client = TripoClient(cfg)
    payload: dict[str, Any] = {}
    for api in ("v3", "v2"):
        try:
            payload[api] = client.balance(api=api)
        except Exception as exc:  # noqa: BLE001
            payload[api] = {"error": f"{type(exc).__name__}: {exc}"}
    payload["non_billable"] = True
    if args.json:
        _print_json(payload)
    else:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    return EXIT_OK


def cmd_voices(args: argparse.Namespace) -> int:
    cfg = _config(args)
    from .fish import FishClient

    client = FishClient(cfg)
    try:
        voices = client.list_models(page_size=args.limit)
    except Exception as exc:  # noqa: BLE001
        print(f"voice listing failed: {exc}", file=sys.stderr)
        return EXIT_ERROR
    if args.json:
        _print_json({"non_billable": True, "voices": voices})
        return EXIT_OK
    print(f"Fish voice models (non-billable GET /model), {len(voices)} shown:")
    for voice in voices:
        tags = ",".join((voice.get("tags") or [])[:4])
        print(f"  {voice['id']}  [{voice.get('state')}] {short(voice.get('title'), 46)}  {tags}")
    return EXIT_OK


def cmd_convert_audio(args: argparse.Namespace) -> int:
    destination = Path(args.output) if args.output else Path(args.source).with_suffix(".ogg")
    try:
        path = convert_to_ogg_vorbis(args.source, destination, quality=args.quality, mono=args.mono)
    except FFmpegUnavailable as exc:
        print(f"cannot convert: {exc}", file=sys.stderr)
        return EXIT_CONFIG
    except Exception as exc:  # noqa: BLE001
        print(f"conversion failed: {exc}", file=sys.stderr)
        return EXIT_ERROR
    info = ffprobe_duration(path) or {}
    result = {"output": str(path), "bytes": path.stat().st_size, "probe": info}
    if args.json:
        _print_json(result)
    else:
        print(f"wrote {path} ({human_bytes(result['bytes'])})")
        print(json.dumps(info, ensure_ascii=False))
    return EXIT_OK


def cmd_recover(args: argparse.Namespace) -> int:
    cfg = _config(args)
    ledger, queue = _open_state(cfg)
    try:
        recovered = queue.recover_stale()
        payload = {"recovered": recovered, "queue": queue.stats()}
        if args.json:
            _print_json(payload)
        else:
            print(f"recovered {len(recovered)} interrupted job(s): {recovered}")
        return EXIT_OK
    finally:
        ledger.close()
        queue.close()


# --------------------------------------------------------------------------
# production
# --------------------------------------------------------------------------
def cmd_produce(args: argparse.Namespace) -> int:
    cfg = _config(args)
    if cfg.spend_locked:
        print(
            f"refusing production: spend gate is closed ({cfg.mode}). "
            "The Lead must set ASSET_DRY_RUN=false and ASSET_AUTO_SPEND=true.",
            file=sys.stderr,
        )
        return EXIT_CONFIG
    if not args.confirm_spend:
        print(
            "refusing production: pass --confirm-spend after the Lead's approval "
            "(each request is also checked against ASSET_MAX_COST_USD before sending).",
            file=sys.stderr,
        )
        return EXIT_CONFIG
    ledger, queue = _open_state(cfg)
    try:
        report = run_group(
            cfg,
            args.group,
            ledger,
            queue,
            force=args.force,
            only=[item for item in (args.only or "").split(",") if item.strip()],
            on_event=(lambda message: print(message, flush=True)),
        )
        if args.json:
            _print_json(report)
        else:
            _render_production(report)
        return EXIT_OK if not report.get("errors") else EXIT_ERROR
    except (BudgetExceeded, SpendLocked) as exc:
        print(f"stopped by the budget guard: {exc}", file=sys.stderr)
        return EXIT_CONFIG
    finally:
        ledger.close()
        queue.close()


def _render_production(report: dict[str, Any]) -> None:
    cost = report.get("cost") or {}
    print("\n" + "=" * 78)
    print("PRODUCTION RESULT")
    print("=" * 78)
    print(f"  images  : {len(report.get('images', []))} entries")
    print(f"  audio   : {len(report.get('audio', []))} entries")
    print(f"  models  : {len(report.get('models', []))} entries")
    print(f"  skipped : {len(report.get('skipped', []))}")
    print(f"  errors  : {len(report.get('errors', []))}")
    print(
        f"\n  cost (operator price table): total={human_usd(cost.get('total_usd'))} "
        f"(images={human_usd(cost.get('images_usd'))} audio={human_usd(cost.get('audio_usd'))} "
        f"models={human_usd(cost.get('models_usd'))})"
    )
    print(
        f"  ledger committed={human_usd(cost.get('ledger_committed_usd'))} "
        f"reserved={human_usd(cost.get('ledger_reserved_usd'))} "
        f"remaining={human_usd(cost.get('remaining_usd'))}/{human_usd(cost.get('max_cost_usd'))}"
    )
    if report.get("errors"):
        print("\n  ERRORS:")
        for error in report["errors"]:
            print(f"    - {error.get('asset_id')}: {error.get('error')}")
    print("\n  manifest: generated_assets/manifests/assets.json")
    print("  report  : generated_assets/manifests/production_report.json")


# --------------------------------------------------------------------------
# parser
# --------------------------------------------------------------------------
def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="asset_pipeline",
        description="NOCTERRA AI asset pipeline (DRY-RUN by default; keys stay in .env).",
    )
    parser.add_argument("--repo", help="repository root (defaults to the .env/.git ancestor)")
    parser.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    sub = parser.add_subparsers(dest="command", required=True)

    status = sub.add_parser("status", help="configuration, gates, queue/ledger/manifest summary")
    status.add_argument("--json", action="store_true", help="machine-readable output")
    status.add_argument("--probe", action="store_true", help="add non-billable metadata probes")
    status.add_argument("--recover", action="store_true", help="also requeue interrupted jobs")
    status.set_defaults(func=cmd_status)

    cost = sub.add_parser("cost", help="cost ledger totals")
    cost.add_argument("--json", action="store_true")
    cost.add_argument("--limit", type=int, default=10, help="recent entries to show")
    cost.set_defaults(func=cmd_cost)

    dry = sub.add_parser("dry-run", help="print the request that would be sent (no HTTP)")
    dry.add_argument("job", help="job name in tools/asset_pipeline/jobs, a JSON path, or inline JSON")
    dry.add_argument("--json", action="store_true")
    dry.add_argument("--no-persist", action="store_true", help="do not write the preview JSON")
    dry.set_defaults(func=cmd_dry_run)

    run = sub.add_parser("run", help="execute a job for real (all spend gates required)")
    run.add_argument("job")
    run.add_argument("--confirm-spend", action="store_true", help="required acknowledgement")
    run.add_argument("--json", action="store_true")
    run.add_argument("--quiet", action="store_true")
    run.add_argument("--save-report", action="store_true", help="write the result JSON under state/reports")
    run.set_defaults(func=cmd_run)

    listing = sub.add_parser("list", help="list jobs or manifest assets")
    listing.add_argument("--status", choices=["pending", "running", "done", "failed", "cancelled"])
    listing.add_argument("--service", choices=["openai", "tripo", "fish", "local"])
    listing.add_argument("--limit", type=int, default=50)
    listing.add_argument("--manifest", action="store_true", help="list manifest assets instead of jobs")
    listing.add_argument("--kind", choices=["texture", "model", "audio", "hud"])
    listing.add_argument("--json", action="store_true")
    listing.set_defaults(func=cmd_list)

    validate = sub.add_parser("validate", help="format-check assets")
    validate.add_argument("paths", nargs="+")
    validate.add_argument("--json", action="store_true")
    validate.set_defaults(func=cmd_validate)

    texture = sub.add_parser("texture", help="local texture processing (no API)")
    texture.add_argument("image")
    texture.add_argument("--out", help="output directory (default generated_assets/processed/texture)")
    texture.add_argument("--name", help="base file name")
    texture.add_argument("--sizes", nargs="+", default=["16", "32", "64"])
    texture.add_argument("--alpha-mode", choices=["cutout", "blend", "keep"], default="cutout")
    texture.add_argument("--keep-background", action="store_true")
    texture.add_argument("--bg-method", choices=["flood", "color"], default="flood")
    texture.add_argument("--bg-tolerance", type=int, default=24)
    texture.add_argument("--quantize-colors", type=int, default=16)
    texture.add_argument("--comparison-scale", type=int, default=8)
    texture.add_argument("--record-manifest", action="store_true")
    texture.add_argument("--json", action="store_true")
    texture.set_defaults(func=cmd_texture)

    balance = sub.add_parser("balance", help="non-billable Tripo balance probe (v3 + v2)")
    balance.add_argument("--json", action="store_true")
    balance.set_defaults(func=cmd_balance)

    voices = sub.add_parser("voices", help="non-billable Fish voice-model listing")
    voices.add_argument("--limit", type=int, default=20)
    voices.add_argument("--json", action="store_true")
    voices.set_defaults(func=cmd_voices)

    convert = sub.add_parser("convert-audio", help="convert audio to Ogg Vorbis (ffmpeg)")
    convert.add_argument("source")
    convert.add_argument("--output")
    convert.add_argument("--quality", type=int, default=5)
    convert.add_argument("--mono", action="store_true")
    convert.add_argument("--json", action="store_true")
    convert.set_defaults(func=cmd_convert_audio)

    recover = sub.add_parser("recover", help="requeue interrupted (lease-expired) jobs")
    recover.add_argument("--json", action="store_true")
    recover.set_defaults(func=cmd_recover)

    produce = sub.add_parser(
        "produce", help="run the approved LIVE production batch (spends real money)"
    )
    produce.add_argument("group", choices=["hud", "textures", "audio", "tripo", "all"])
    produce.add_argument("--force", action="store_true", help="regenerate even if the asset exists")
    produce.add_argument(
        "--only", default="", help="comma-separated boss ids / asset names to process (smoke test)"
    )
    produce.add_argument("--confirm-spend", action="store_true", help="required acknowledgement")
    produce.add_argument("--json", action="store_true")
    produce.set_defaults(func=cmd_produce)

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return int(args.func(args))
    except MissingSecretError as exc:
        print(f"missing credential: {exc}", file=sys.stderr)
        return EXIT_CONFIG
    except ConfigError as exc:
        print(f"configuration error: {exc}", file=sys.stderr)
        return EXIT_CONFIG
    except KeyboardInterrupt:
        print("interrupted", file=sys.stderr)
        return 130
    except PipelineError as exc:
        print(f"pipeline error: {exc}", file=sys.stderr)
        return EXIT_ERROR
    except Exception as exc:  # noqa: BLE001 - CLI boundary
        print(f"unexpected error: {type(exc).__name__}: {exc}", file=sys.stderr)
        return EXIT_ERROR


if __name__ == "__main__":
    raise SystemExit(main())

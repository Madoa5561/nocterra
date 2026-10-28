"""Offline self-test for the asset pipeline (no network, no API keys used for spend).

Run from ``tools/``::

    python -m asset_pipeline.selftest

It proves, with real executions rather than claims:

1. DRY RUN sends **zero** HTTP: the transport is monkey-patched to explode, then the
   three reference jobs are previewed successfully.
2. texture.py actually works: nearest-neighbour resize, alpha cutout, background
   removal, palette quantisation, tileability scoring and comparison sheets, on
   generated dummy PNGs.
3. The GLB validator accepts a hand-built valid GLB and rejects corrupted variants.
4. WAV -> Ogg/Vorbis conversion through ffmpeg works and the audio validator sees it.
5. The cost ledger refuses a request that would cross ASSET_MAX_COST_USD, before
   anything is sent.
6. The queue dedupes identical requests, applies backoff and recovers interrupted
   jobs.

Artifacts are written to ``.agent/tmp/asset_pipeline_selftest`` (git-ignored).
"""

from __future__ import annotations

import io
import json
import os
import shutil
import struct
import sys
import tempfile
import wave
from pathlib import Path
from typing import Any, Callable
from unittest import mock

import numpy as np
from PIL import Image

from . import env as envmod
from .audio_tools import FFmpegUnavailable, convert_to_ogg_vorbis, find_ffmpeg, ffmpeg_version
from .config import Config, CostEstimate
from .http_client import Transport, set_audit_log
from .jobs import load_job
from .ledger import BudgetExceeded, Ledger
from .manifest import AssetManifest
from .queue import JobQueue, content_hash
from .runner import dry_run_job, process_local_texture
from .texture import (
    alpha_cutout,
    nearest_resize,
    open_rgba,
    quantize_palette,
    tile_preview,
    tileability,
)
from .validate import validate_audio, validate_glb, validate_json, validate_png

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_WORK = REPO_ROOT / ".agent" / "tmp" / "asset_pipeline_selftest"

CHECKS: list[tuple[str, bool, str]] = []


def check(name: str, condition: bool, detail: str = "") -> bool:
    CHECKS.append((name, bool(condition), detail))
    status = "PASS" if condition else "FAIL"
    line = f"[{status}] {name}"
    if detail:
        line += f"  ({detail})"
    print(line)
    return bool(condition)


def section(title: str) -> None:
    print("\n" + "=" * 78)
    print(title)
    print("=" * 78)


# --------------------------------------------------------------------------
# test image builders
# --------------------------------------------------------------------------
def make_tileable_texture(size: int = 256) -> Image.Image:
    """Smooth periodic pattern: wraps perfectly on both axes."""
    y, x = np.mgrid[0:size, 0:size].astype(np.float64)
    value = (
        0.50
        + 0.22 * np.sin(2 * np.pi * x / size)
        + 0.18 * np.sin(2 * np.pi * y / size)
        + 0.10 * np.sin(2 * np.pi * (x + y) / size)
    )
    value = (value - value.min()) / (value.max() - value.min())
    rgb = np.stack(
        [
            (60 + 150 * value).astype(np.uint8),
            (70 + 120 * value).astype(np.uint8),
            (90 + 110 * value).astype(np.uint8),
            np.full((size, size), 255, dtype=np.uint8),
        ],
        axis=-1,
    )
    return Image.fromarray(rgb, mode="RGBA")


def make_seamed_texture(size: int = 256) -> Image.Image:
    """Strong left-right gradient: the wrap seam is deliberately obvious."""
    y, x = np.mgrid[0:size, 0:size].astype(np.float64)
    value = x / (size - 1)
    rgb = np.stack(
        [
            (30 + 200 * value).astype(np.uint8),
            np.full((size, size), 90, dtype=np.uint8),
            (200 - 150 * value).astype(np.uint8),
            np.full((size, size), 255, dtype=np.uint8),
        ],
        axis=-1,
    )
    return Image.fromarray(rgb, mode="RGBA")


def make_sprite_with_background(size: int = 128) -> Image.Image:
    """Opaque sprite on a solid magenta background (tests flood-fill removal)."""
    array = np.zeros((size, size, 4), dtype=np.uint8)
    array[:, :, :] = (255, 0, 255, 255)
    centre = size // 2
    radius = size // 3
    ys, xs = np.mgrid[0:size, 0:size]
    inside = (xs - centre) ** 2 + (ys - centre) ** 2 <= radius**2
    array[inside] = (90, 140, 80, 255)
    # a lighter highlight, still inside the sprite
    highlight = (xs - centre + 18) ** 2 + (ys - centre + 18) ** 2 <= (radius // 3) ** 2
    array[highlight] = (170, 210, 140, 255)
    return Image.fromarray(array, mode="RGBA")


# --------------------------------------------------------------------------
# GLB builder
# --------------------------------------------------------------------------
def build_glb(*, triangles: int = 2, with_uv: bool = True, version: int = 2, magic: bytes = b"glTF") -> bytes:
    """Minimal valid GLB: one mesh, one primitive, indexed triangles."""
    vertex_count = triangles * 3
    indices = list(range(vertex_count))
    index_bytes = struct.pack(f"<{len(indices)}H", *indices)
    positions = b"".join(struct.pack("<3f", float(i), 0.0, 0.0) for i in range(vertex_count))
    uvs = b"".join(struct.pack("<2f", 0.0, 0.0) for _ in range(vertex_count))
    normals = b"".join(struct.pack("<3f", 0.0, 1.0, 0.0) for _ in range(vertex_count))
    # align each bufferView to 4 bytes
    blob = b""
    offsets: list[tuple[int, int]] = []
    for chunk in (indices and index_bytes, positions, uvs if with_uv else b"", normals):
        if not chunk:
            continue
        offsets.append((len(blob), len(chunk)))
        blob += chunk
        padding = (-len(blob)) % 4
        blob += b"\x00" * padding
    buffer_views = [
        {"buffer": 0, "byteOffset": offset, "byteLength": length}
        for offset, length in offsets
    ]
    accessors: list[dict[str, Any]] = [
        {"bufferView": 0, "componentType": 5123, "count": len(indices), "type": "SCALAR"},
        {"bufferView": 1, "componentType": 5126, "count": vertex_count, "type": "VEC3",
         "min": [0.0, 0.0, 0.0], "max": [float(vertex_count), 0.0, 0.0]},
    ]
    attributes = {"POSITION": 1, "NORMAL": 3}
    if with_uv:
        accessors.append({"bufferView": 2, "componentType": 5126, "count": vertex_count, "type": "VEC2"})
        attributes["TEXCOORD_0"] = 2
        accessors.append({"bufferView": 3, "componentType": 5126, "count": vertex_count, "type": "VEC3"})
    else:
        accessors.append({"bufferView": 2, "componentType": 5126, "count": vertex_count, "type": "VEC3"})
    document = {
        "asset": {"version": "2.0", "generator": "nocterra-selftest"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0, "name": "test"}],
        "meshes": [
            {
                "name": "tri",
                "primitives": [
                    {"attributes": attributes, "indices": 0, "material": 0, "mode": 4}
                ],
            }
        ],
        "materials": [{"name": "mat", "pbrMetallicRoughness": {"baseColorTexture": {"index": 0}}}],
        "textures": [{"source": 0}],
        "images": [{"bufferView": 0, "mimeType": "image/png"}],
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(blob)}],
    }
    json_bytes = json.dumps(document, separators=(",", ":")).encode("utf-8")
    json_bytes += b" " * ((4 - len(json_bytes) % 4) % 4)
    bin_bytes = blob + b"\x00" * ((4 - len(blob) % 4) % 4)
    total = 12 + 8 + len(json_bytes) + 8 + len(bin_bytes)
    out = io.BytesIO()
    out.write(struct.pack("<4sII", magic, version, total))
    out.write(struct.pack("<II", len(json_bytes), 0x4E4F534A))
    out.write(json_bytes)
    out.write(struct.pack("<II", len(bin_bytes), 0x004E4942))
    out.write(bin_bytes)
    return out.getvalue()


def make_wav(path: Path, *, seconds: float = 0.6, rate: int = 44100, freq: float = 220.0) -> None:
    frames = int(seconds * rate)
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(rate)
        data = b"".join(
            struct.pack("<h", int(12000 * np.sin(2 * np.pi * freq * i / rate)))
            for i in range(frames)
        )
        handle.writeframes(data)


# --------------------------------------------------------------------------
# tests
# --------------------------------------------------------------------------
def test_dry_run_sends_no_http(cfg: Config, work: Path) -> None:
    section("1. DRY RUN must not send HTTP (transport patched to raise)")
    audit = work / "state" / "http_audit.jsonl"
    if audit.exists():
        audit.unlink()
    set_audit_log(audit)

    boom = AssertionError("HTTP transport was called during DRY RUN")

    def explode(*args: Any, **kwargs: Any) -> Any:
        raise boom

    names = ["texture_stone_tile", "model_forest_king", "audio_ambient_sting"]
    with mock.patch.object(Transport, "request", explode), mock.patch.object(
        Transport, "download", explode
    ), mock.patch("urllib.request.urlopen", explode):
        for name in names:
            spec = load_job(name)
            payload = dry_run_job(cfg, spec, persist=True)
            sent = payload.get("http_sent")
            check(
                f"dry-run {name}: http_sent=False",
                sent is False and payload.get("dry_run") is True,
                f"endpoint={(payload.get('create') or {}).get('endpoint') or payload.get('endpoint')}",
            )
    check("dry-run wrote no audit log entries", not audit.exists() or audit.stat().st_size == 0,
          "transport was never invoked")
    previews = list((work / "state" / "dryrun").glob("*.json"))
    check("dry-run preview JSON persisted", len(previews) >= len(names), f"{len(previews)} files")


def test_texture(work: Path) -> None:
    section("2. texture.py: nearest resize, alpha, background, quantise, tiling")
    src_dir = work / "textures"
    src_dir.mkdir(parents=True, exist_ok=True)

    tileable = make_tileable_texture(256)
    tileable_path = src_dir / "tileable_256.png"
    tileable.save(tileable_path)

    seamed = make_seamed_texture(256)
    seamed_path = src_dir / "seamed_256.png"
    seamed.save(seamed_path)

    sprite = make_sprite_with_background(128)
    sprite_path = src_dir / "sprite_bg_128.png"
    sprite.save(sprite_path)

    # -- unit level --------------------------------------------------------
    report_tileable = tileability(tileable)
    report_seamed = tileability(seamed)
    check(
        "tileability: periodic texture is seamless",
        report_tileable["verdict"] == "seamless",
        f"worst_ratio={report_tileable['worst_ratio']}",
    )
    check(
        "tileability: gradient texture is detected as seamed",
        report_seamed["verdict"] == "visible-seam",
        f"worst_ratio={report_seamed['worst_ratio']}",
    )

    # Nearest-neighbour must only ever pick existing source pixels (never blend).
    quad = Image.new("RGBA", (4, 4), (0, 0, 0, 255))
    palette = [(255, 0, 0, 255), (0, 255, 0, 255), (0, 0, 255, 255), (255, 255, 0, 255)]
    for index, color in enumerate(palette):
        x0, y0 = (index % 2) * 2, (index // 2) * 2
        for dx in range(2):
            for dy in range(2):
                quad.putpixel((x0 + dx, y0 + dy), color)
    small = nearest_resize(quad, 2)
    picked = {small.getpixel((x, y)) for x in range(2) for y in range(2)}
    check(
        "nearest_resize only picks existing source colours (no blending)",
        picked <= set(palette) and len(picked) >= 2,
        f"picked={sorted(picked)}",
    )
    check(
        "nearest_resize output has the requested size",
        small.size == (2, 2),
        str(small.size),
    )

    cut = alpha_cutout(Image.new("RGBA", (2, 1), (10, 20, 30, 100)))
    check("alpha_cutout makes 100/255 transparent", cut.getpixel((0, 0))[3] == 0)
    cut2 = alpha_cutout(Image.new("RGBA", (2, 1), (10, 20, 30, 200)))
    check("alpha_cutout makes 200/255 opaque", cut2.getpixel((0, 0))[3] == 255)

    quant = quantize_palette(make_tileable_texture(64), 8)
    colors = len({quant.getpixel((x, y))[:3] for x in range(0, 64, 3) for y in range(0, 64, 3)})
    check("quantize_palette reduces to <= N colours", colors <= 8, f"{colors} distinct sampled")
    check("quantize_palette preserves alpha", quant.getpixel((0, 0))[3] == 255)

    preview = tile_preview(nearest_resize(tileable, 16), repeat=2, scale=4)
    check("tile_preview produces the 2x2 grid", preview.size == (16 * 4 * 2, 16 * 4 * 2), f"{preview.size}")

    # -- full pipeline -----------------------------------------------------
    out_dir = work / "out" / "tileable"
    report = process_local_texture(
        _cfg_for(work),
        tileable_path,
        out_dir=out_dir,
        base_name="tileable",
        options={"sizes": (16, 32, 64), "alpha_mode": "cutout", "remove_background": False,
                 "quantize_colors": 16, "write_comparison": True},
    )
    sizes_ok = True
    for output in report["outputs"]:
        path = Path(output["path"])
        with Image.open(path) as img:
            sizes_ok &= img.size == (output["size"], output["size"])
        sizes_ok &= Path(output["tiled_preview"]).is_file()
        validation = validate_png(path, expected_sizes=(16, 32, 64))
        check(f"  {output['size']}px validates", validation.ok,
              f"{validation.details.get('width')}x{validation.details.get('height')} "
              f"mode={validation.details.get('mode')}")
    check("all requested sizes produced and square", sizes_ok, f"{[o['size'] for o in report['outputs']]}")
    check("comparison sheet exists", Path(report["comparison"]).is_file(), report["comparison"])

    sprite_out = work / "out" / "sprite"
    sprite_report = process_local_texture(
        _cfg_for(work),
        sprite_path,
        out_dir=sprite_out,
        base_name="sprite",
        options={"sizes": (16, 32), "remove_background": True, "bg_method": "flood",
                 "bg_tolerance": 30, "alpha_mode": "cutout", "quantize_colors": 12},
    )
    removal = sprite_report["background_removal"]
    check(
        "background removal cleared most of the solid background",
        0.25 < removal.get("removed_ratio", 0) < 0.95,
        f"removed_ratio={removal.get('removed_ratio')} key={removal.get('key_color_rgb')}",
    )
    with Image.open(sprite_report["outputs"][0]["path"]) as img:
        alpha = np.asarray(img.convert("RGBA"))[:, :, 3]
        corner_transparent = alpha[0, 0] == 0
    check("corner pixel is transparent after removal", corner_transparent,
          f"corner alpha={alpha[0, 0]}")


def test_glb(work: Path) -> None:
    section("3. validate.py: GLB parser and corruption detection")
    dir_ = work / "models"
    dir_.mkdir(parents=True, exist_ok=True)
    good = dir_ / "good.glb"
    good.write_bytes(build_glb(triangles=2, with_uv=True))
    result = validate_glb(good, max_triangles=200_000)
    details = result.details
    check("valid GLB accepted", result.ok, "; ".join(result.errors))
    check("magic/version parsed", details.get("magic") == "glTF" and details.get("version") == 2)
    check("JSON + BIN chunks found", len(details.get("chunks", [])) == 2,
          str([c["type_name"] for c in details.get("chunks", [])]))
    check("meshes/accessors counted", details.get("meshes") == 1 and details.get("accessors") == 4,
          f"meshes={details.get('meshes')} accessors={details.get('accessors')}")
    check("triangle estimate correct", details.get("triangle_estimate") == 2,
          f"tris={details.get('triangle_estimate')}")
    check("UV detected", details.get("has_uv") is True)

    high = dir_ / "highpoly.glb"
    high.write_bytes(build_glb(triangles=4))
    result_high = validate_glb(high, max_triangles=2)
    check("polygon budget produces a warning", any("budget" in w for w in result_high.warnings),
          str(result_high.warnings))

    no_uv = dir_ / "no_uv.glb"
    no_uv.write_bytes(build_glb(triangles=2, with_uv=False))
    result_no_uv = validate_glb(no_uv)
    check("missing UV warned about", any("TEXCOORD_0" in w for w in result_no_uv.warnings),
          str(result_no_uv.warnings))

    bad_magic = dir_ / "bad_magic.glb"
    bad_magic.write_bytes(build_glb(magic=b"glTX"))
    check("bad magic rejected", not validate_glb(bad_magic).ok)

    bad_version = dir_ / "bad_version.glb"
    bad_version.write_bytes(build_glb(version=1))
    bad_err = validate_glb(bad_version)
    check("GLB1 version rejected", not bad_err.ok, "; ".join(bad_err.errors))

    truncated = dir_ / "truncated.glb"
    truncated.write_bytes(build_glb()[:40])
    check("truncated GLB rejected", not validate_glb(truncated).ok)

    garbage = dir_ / "garbage.glb"
    garbage.write_bytes(b"not a glb at all")
    check("garbage rejected", not validate_glb(garbage).ok)


def test_audio(work: Path) -> None:
    section("4. audio: WAV -> Ogg Vorbis (ffmpeg) and container validation")
    dir_ = work / "audio"
    dir_.mkdir(parents=True, exist_ok=True)
    wav_path = dir_ / "tone.wav"
    make_wav(wav_path)
    wav_result = validate_audio(wav_path)
    check("WAV recognised", wav_result.ok and wav_result.details.get("container") == "wav",
          str(wav_result.details))
    check("WAV duration probed", (wav_result.details.get("duration_s") or 0) > 0.3,
          str(wav_result.details.get("duration_s")))

    exe = find_ffmpeg()
    check("ffmpeg discovered", bool(exe), exe or "not found")
    if not exe:
        check("ffmpeg conversion skipped", True, "ffmpeg missing -> WAV fallback documented")
        return
    print(f"      ffmpeg: {ffmpeg_version()}")
    ogg_path = dir_ / "tone.ogg"
    try:
        convert_to_ogg_vorbis(wav_path, ogg_path, quality=4)
    except FFmpegUnavailable as exc:  # pragma: no cover - defensive
        check("ffmpeg conversion", False, str(exc))
        return
    ogg_result = validate_audio(ogg_path)
    check("converted file recognised as Ogg", ogg_result.details.get("container") == "ogg",
          str(ogg_result.details))
    check(
        "converted codec is Vorbis (Minecraft-compatible)",
        ogg_result.details.get("codec") == "vorbis",
        f"codec={ogg_result.details.get('codec')}",
    )
    check("Ogg duration probed", (ogg_result.details.get("duration_s") or 0) > 0.3,
          str(ogg_result.details.get("duration_s")))
    non_audio = dir_ / "not_audio.ogg"
    non_audio.write_bytes(b"RIFF?no")
    check("non-audio rejected", not validate_audio(non_audio).ok)


def test_budget(work: Path) -> None:
    section("5. ledger.py: hard ceiling enforced BEFORE sending")
    db_path = work / "state_test" / "ledger.sqlite"
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()
    ledger = Ledger(db_path)
    try:
        estimate = CostEstimate(usd=3.0, detail="test", source="selftest")
        rid = ledger.check_and_reserve(
            job_id="job-a", service="openai", model="gpt-image-2.5-flare", kind="image",
            estimate=estimate, max_cost_usd=5.0,
        )
        check("reservation under the ceiling accepted", rid > 0, f"reservation id {rid}")
        check("reserved total counted", abs(ledger.outstanding_total() - 3.0) < 1e-9,
              f"{ledger.outstanding_total()}")

        blocked = False
        try:
            ledger.check_and_reserve(
                job_id="job-b", service="openai", model="gpt-image-2.5-flare", kind="image",
                estimate=CostEstimate(usd=2.5), max_cost_usd=5.0,
            )
        except BudgetExceeded as exc:
            blocked = True
            check("over-budget request refused with an explanatory message",
                  "ASSET_MAX_COST_USD" in str(exc), str(exc)[:110])
        check("over-budget request was refused", blocked)
        check("refused request left reserved total untouched",
              abs(ledger.outstanding_total() - 3.0) < 1e-9)

        ledger.release(rid, "selftest release")
        check("released reservation no longer counts", ledger.outstanding_total() == 0.0)

        unpriced = False
        try:
            ledger.check_and_reserve(
                job_id="job-c", service="tripo", model=None, kind="text_to_model",
                estimate=CostEstimate(usd=None, credits=20.0), max_cost_usd=5.0,
            )
        except BudgetExceeded as exc:
            unpriced = True
            check("unpriced request refused", "price unknown" in str(exc), str(exc)[:100])
        check("unpriced request was refused", unpriced)

        committed_id = ledger.check_and_reserve(
            job_id="job-d", service="openai", model="m", kind="image",
            estimate=CostEstimate(usd=1.25), max_cost_usd=5.0,
        )
        ledger.commit(committed_id, actual_usd=1.25)
        check("commit records actual spend", abs(ledger.committed_total() - 1.25) < 1e-9,
              f"{ledger.committed_total()}")
        check("commit clears the reservation", ledger.outstanding_total() == 0.0)
    finally:
        ledger.close()


def test_queue(work: Path) -> None:
    section("6. queue.py: dedupe, backoff, state machine, crash resume")
    db_path = work / "state_test" / "queue.sqlite"
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()
    queue = JobQueue(db_path)
    try:
        payload = {"prompt": "same request", "params": {"size": "1024x1024"}}
        jid1, created1 = queue.add_job(kind="texture", service="openai", model="m", payload=payload)
        jid2, created2 = queue.add_job(kind="texture", service="openai", model="m", payload=payload)
        check("identical request deduplicated", jid1 == jid2 and created1 and not created2,
              f"{jid1} / {jid2}")
        other, created3 = queue.add_job(
            kind="texture", service="openai", model="m", payload={"prompt": "different"}
        )
        check("different request gets a new job", other != jid1 and created3)

        claimed = queue.claim_specific(jid1)
        check("claim_specific promotes pending -> running",
              claimed is not None and claimed["status"] == "running" and claimed["attempts"] == 1,
              f"status={claimed and claimed['status']} attempts={claimed and claimed['attempts']}")

        outcome = queue.mark_failed(jid1, error="boom", retryable=True, base_s=0.5, cap_s=5.0)
        check("retryable failure schedules a retry", outcome["retry"] is True,
              f"next_run_in_s={outcome['next_run_in_s']}")
        job = queue.get(jid1)
        check("job went back to pending with backoff",
              job["status"] == "pending" and job["next_run_at"] > 0, f"next_run_at={job['next_run_at']:.0f}")

        queue.claim_specific(jid1)
        outcome2 = queue.mark_failed(jid1, error="boom again", retryable=True, base_s=0.1)
        check("second attempt still retryable (max_attempts=3)", outcome2["retry"] is True)
        queue.claim_specific(jid1)
        outcome3 = queue.mark_failed(jid1, error="final", retryable=True, base_s=0.1)
        check("attempt ceiling makes the job fail", outcome3["retry"] is False,
              f"attempts={outcome3['attempts']}/{outcome3['max_attempts']} status={queue.get(jid1)['status']}")

        # crash resume: a running job whose lease expired must be requeued
        jid_crash, _ = queue.add_job(
            kind="model", service="tripo", model="v", payload={"prompt": "crashy"}
        )
        queue.claim_specific(jid_crash)
        with queue._conn:  # noqa: SLF001 - simulating a dead worker
            queue._conn.execute(
                "UPDATE jobs SET lease_until=? WHERE id=?", (0.0, jid_crash)
            )
        recovered = queue.recover_stale()
        check("interrupted job recovered", jid_crash in recovered,
              f"recovered={recovered}")
        check("recovered job is pending again", queue.get(jid_crash)["status"] == "pending")

        # rate limiting actually waits
        waited = queue.acquire_rate_slot("openai", 0.25)
        waited2 = queue.acquire_rate_slot("openai", 0.25)
        check("rate limiter enforces the minimum interval", waited2 >= 0.15,
              f"second wait={waited2}s (first={waited}s)")

        stats = queue.stats()
        check("queue stats computed", stats["total"] >= 3, str(stats["by_status"]))
        check(
            "content hash is stable for identical requests",
            content_hash("texture", "openai", "m", payload)
            == content_hash("texture", "openai", "m", dict(payload)),
        )
        check(
            "content hash ignores underscore metadata",
            content_hash("texture", "openai", "m", {**payload, "_note": "x"})
            == content_hash("texture", "openai", "m", payload),
        )
    finally:
        queue.close()


def test_manifest(work: Path) -> None:
    section("7. manifest.py: provenance ledger")
    manifest_dir = work / "manifests"
    if manifest_dir.exists():
        shutil.rmtree(manifest_dir)
    sample = work / "textures" / "tileable_256.png"
    manifest = AssetManifest(manifest_dir)
    entry = manifest.add(
        asset_id="texture_demo_raw",
        kind="texture",
        path=sample,
        service="openai",
        model="gpt-image-2.5-flare",
        prompt="demo prompt",
        parameters={"size": "1024x1024"},
        stage="raw",
        cost={"estimated_usd": 0.07, "actual_usd": 0.07, "note": "estimate"},
        job_id="job-demo",
        license_note="selftest only",
    )
    check("manifest entry written", entry["asset_id"] == "texture_demo_raw" and entry["sha256"],
          f"sha={str(entry['sha256'])[:12]}")
    check("manifest file exists", (manifest_dir / "assets.json").is_file())
    found = manifest.find_by_sha(entry["sha256"])
    check("lookup by sha256 works", found is not None and found["asset_id"] == entry["asset_id"])
    processed = manifest.add_processed(
        asset_id="texture_demo_raw_16",
        path=sample,
        source_asset_id="texture_demo_raw",
        operation="nearest-resize",
        parameters={"size": 16},
    )
    check("processed asset records provenance",
          processed["stage"] == "processed" and processed["source_asset_id"] == "texture_demo_raw")
    summary = manifest.summary()
    check("manifest summary counts assets", summary["asset_count"] == 2, str(summary["by_stage"]))
    check("manifest total cost summed", abs(summary["total_actual_usd"] - 0.07) < 1e-9,
          f"{summary['total_actual_usd']}")

    # JSON validator
    good_json = work / "good.json"
    good_json.write_text('{"a": 1}', encoding="utf-8")
    bad_json = work / "bad.json"
    bad_json.write_text("{not json", encoding="utf-8")
    check("valid JSON accepted", validate_json(good_json, required_keys=["a"]).ok)
    check("invalid JSON rejected", not validate_json(bad_json).ok)
    check("missing required key rejected",
          not validate_json(good_json, required_keys=["missing"]).ok)


def test_image_models(work: Path) -> None:
    section("8. openai_image.py: request construction and constraint checks")
    from .openai_image import build_request

    spec = load_job("texture_stone_tile")
    request = build_request(spec)
    check("texture job request builds", request["model"] == "gpt-image-2.5-flare",
          json.dumps({k: request[k] for k in ("model", "size", "quality", "output_format")}))
    bad = load_job(
        """{"kind":"texture","prompt":"x","params":{"size":"1000x1000"}}"""
    )
    failed = False
    try:
        build_request(bad)
    except ValueError as exc:
        failed = True
        check("non-16-divisible arbitrary size rejected", "divisible by 16" in str(exc), str(exc)[:90])
    check("invalid size rejected", failed)

    bad_transparent = load_job(
        """{"kind":"texture","prompt":"x","params":{"background":"transparent","output_format":"jpeg"}}"""
    )
    failed2 = False
    try:
        build_request(bad_transparent)
    except ValueError as exc:
        failed2 = True
        check("transparent background requires png/webp", "requires output_format" in str(exc))
    check("transparent+jpeg rejected", failed2)

    bad_key = load_job("""{"kind":"texture","prompt":"x","params":{"siz":"1024x1024"}}""")
    failed3 = False
    try:
        build_request(bad_key)
    except ValueError as exc:
        failed3 = True
        check("typo in parameter name rejected", "unknown image parameter" in str(exc),
              str(exc)[:90])
    check("unknown parameter rejected", failed3)


def test_tripo_request(work: Path) -> None:
    section("9. tripo.py: request construction (no task created)")
    from .tripo import TripoClient, TripoError, _unwrap

    cfg = _cfg_for(work)
    client = TripoClient(cfg)
    spec = load_job("model_forest_king")
    body = client.build_task_request(spec)
    check("text_to_model body built", body.get("prompt") and body.get("model"),
          json.dumps({k: body[k] for k in ("model", "face_limit", "texture", "export_uv")}))
    check("face_limit forwarded", body.get("face_limit") == 12000)

    image_spec = load_job(
        json.dumps(
            {
                "id": "img2model_demo",
                "kind": "model",
                "service": "tripo",
                "mode": "image_to_model",
                "model": "v2.5-20250123",
                "prompt": "turn this into a low-poly model",
                "input_image": str(work / "textures" / "sprite_bg_128.png"),
                "params": {"face_limit": 8000},
            }
        )
    )
    preview = client.preview(image_spec, job_id="img2model_demo")
    check("image_to_model preview includes the upload step",
          preview["upload"]["endpoint"].endswith("/v3/files") and preview["upload"]["field"] == "file",
          preview["upload"]["endpoint"])
    check("upload preview records the file hash",
          bool(preview["upload"]["file_sha256"]), str(preview["upload"]["file_sha256"])[:12])
    check("image_to_model request uses the input field",
          preview["create"]["request"].get("input") == image_spec.data["input_image"])

    unwrapped = _unwrap({"code": 0, "status": "success", "data": {"task_id": "t1"}})
    check("Tripo envelope unwrapped", unwrapped == {"task_id": "t1"})
    try:
        _unwrap({"code": 2001, "status": "error", "message": "The task is not found"})
        check("Tripo error envelope raised", False)
    except TripoError as exc:
        check("Tripo error envelope surfaced", "code=2001" in str(exc), str(exc)[:80])


def test_fish_request(work: Path) -> None:
    section("10. fish.py: TTS request construction and format policy")
    from .fish import FishClient, TTS_MODELS

    cfg = _cfg_for(work)
    client = FishClient(cfg)
    spec = load_job("audio_ambient_sting")
    headers, body = client.build_request(spec)
    check("TTS model sent as the 'model' header", headers["model"] == "s2.1-pro", headers["model"])
    check("TTS body has text and format", body["text"] and body["format"] == "wav",
          f"format={body['format']} chars={len(body['text'])}")
    check("TTS model is a documented enum value", headers["model"] in TTS_MODELS)
    check("fish is treated as unpriced by default", client.estimate(spec).usd is None,
          "set ASSET_FISH_USD_PER_1K_CHARS after verifying pricing")

    bad = load_job(
        json.dumps(
            {
                "kind": "audio",
                "prompt": "hi",
                "audio": {"tts_model": "not-a-real-model"},
            }
        )
    )
    failed = False
    try:
        client.build_request(bad)
    except ValueError as exc:
        failed = True
        check("unknown TTS model rejected", "tts_model must be one of" in str(exc))
    check("invalid TTS model rejected", failed)


# --------------------------------------------------------------------------
# harness
# --------------------------------------------------------------------------
def _cfg_for(work: Path) -> Config:
    """Hermetic config for tests: never inherits the live-spend .env settings.

    The self-test must stay safe even when the repository is in
    LIVE-SPEND-ENABLED mode, so the gates and prices are forced to their safe
    defaults here regardless of what .env says.
    """
    cfg = Config.load(envmod.Env(repo_root=REPO_ROOT))
    cfg.state_dir = work / "state"
    cfg.raw_dir = work / "raw"
    cfg.processed_dir = work / "processed"
    cfg.manifests_dir = work / "manifests"
    # hard safety: no billable call can ever pass the gates inside the self-test
    cfg.dry_run = True
    cfg.auto_spend = False
    cfg.max_cost_usd = 0.0
    cfg.fish_usd_per_1k_chars = None
    cfg.tripo_credit_usd = None
    cfg.warnings = [w for w in cfg.warnings if "unpriced" in w or "ASSET_" in w]
    cfg.ensure_state_dirs()
    return cfg


def main(argv: list[str] | None = None) -> int:
    args = list(sys.argv[1:] if argv is None else argv)
    work = Path(args[0]).resolve() if args else DEFAULT_WORK
    if work.exists():
        shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True, exist_ok=True)
    set_audit_log(work / "state" / "http_audit.jsonl")
    cfg = _cfg_for(work)

    print("NOCTERRA asset pipeline self-test (offline, no billable API calls)")
    print(f"work dir: {work}")
    print(f"python  : {sys.version.split()[0]}")
    print(f"mode    : {cfg.mode}")

    test_dry_run_sends_no_http(cfg, work)
    test_texture(work)
    test_glb(work)
    test_audio(work)
    test_budget(work)
    test_queue(work)
    test_manifest(work)
    test_image_models(work)
    test_tripo_request(work)
    test_fish_request(work)

    passed = sum(1 for _, ok, _ in CHECKS if ok)
    failed = len(CHECKS) - passed
    print("\n" + "=" * 78)
    print(f"RESULT: {passed} passed, {failed} failed, {len(CHECKS)} checks")
    if failed:
        print("\nFailed checks:")
        for name, ok, detail in CHECKS:
            if not ok:
                print(f"  - {name} ({detail})")
    print("=" * 78)
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

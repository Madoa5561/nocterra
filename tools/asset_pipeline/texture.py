"""Game-texture post-processing (Pillow + numpy).

Powers the texture jobs:

* nearest-neighbour downscale to a Minecraft-friendly size (16/32/64),
* alpha handling (keep / hard cutout / threshold blend),
* solid-background removal by flood fill from the border (or by exact key colour),
* palette quantisation that preserves the alpha mask,
* tileability inspection (left/right and top/bottom edge continuity, with a seam
  score and a tiled preview),
* before/after comparison sheets for human review.

All functions are pure/local: no network, no API keys.
"""

from __future__ import annotations

import math
from pathlib import Path
from typing import Any, Iterable, Sequence

import numpy as np
from PIL import Image, ImageDraw

__all__ = [
    "TextureReport",
    "alpha_cutout",
    "comparison_sheet",
    "detect_background_color",
    "nearest_resize",
    "open_rgba",
    "process_texture",
    "quantize_palette",
    "remove_background",
    "tileability",
    "tile_preview",
]

#: Sentinel colours used while flood-filling; the least-likely one wins.
_SENTINELS = ((255, 0, 255), (0, 255, 255), (255, 255, 0), (1, 2, 3), (254, 1, 128))

TARGET_SIZES = (16, 32, 64)


class TextureReport(dict):
    """Plain dict subclass so reports stay JSON-serialisable."""


def open_rgba(path: str | Path) -> Image.Image:
    with Image.open(path) as img:
        return img.convert("RGBA")


# --------------------------------------------------------------------------
# resize / alpha
# --------------------------------------------------------------------------
def nearest_resize(img: Image.Image, size: int) -> Image.Image:
    """Nearest-neighbour resize to *size* x *size* (never bilinear: pixel art)."""
    if size <= 0:
        raise ValueError("size must be positive")
    src = img if img.mode == "RGBA" else img.convert("RGBA")
    return src.resize((size, size), Image.Resampling.NEAREST)


def alpha_cutout(img: Image.Image, threshold: int = 128) -> Image.Image:
    """Hard alpha: fully opaque or fully transparent (Minecraft cutout textures)."""
    src = img.convert("RGBA")
    alpha = src.getchannel("A").point(lambda value: 255 if value >= threshold else 0)
    src.putalpha(alpha)
    return src


def alpha_threshold_blend(img: Image.Image, low: int = 16, high: int = 96) -> Image.Image:
    """Clamp partial alpha into ``[0, 255]`` while keeping a soft edge band."""
    if not 0 <= low < high <= 255:
        raise ValueError("expected 0 <= low < high <= 255")
    src = img.convert("RGBA")
    alpha = src.getchannel("A").point(
        lambda value: 0 if value <= low else 255 if value >= high else int((value - low) * 255 / (high - low))
    )
    src.putalpha(alpha)
    return src


def apply_alpha_mode(img: Image.Image, mode: str = "cutout", threshold: int = 128) -> Image.Image:
    if mode == "keep":
        return img.convert("RGBA")
    if mode == "cutout":
        return alpha_cutout(img, threshold)
    if mode == "blend":
        return alpha_threshold_blend(img)
    raise ValueError(f"unknown alpha mode {mode!r} (expected keep|cutout|blend)")


# --------------------------------------------------------------------------
# background removal
# --------------------------------------------------------------------------
def _border_pixels(img: Image.Image) -> np.ndarray:
    array = np.asarray(img.convert("RGBA"))
    top = array[0, :, :]
    bottom = array[-1, :, :]
    left = array[:, 0, :]
    right = array[:, -1, :]
    return np.concatenate([top, bottom, left, right], axis=0)


def detect_background_color(img: Image.Image) -> tuple[tuple[int, int, int, int], float]:
    """Most common opaque-ish colour along the border and its share of the border."""
    border = _border_pixels(img)
    opaque = border[border[:, 3] > 8]
    if len(opaque) == 0:
        return (0, 0, 0, 0), 0.0
    colors, counts = np.unique(opaque[:, :3], axis=0, return_counts=True)
    index = int(np.argmax(counts))
    rgb = tuple(int(v) for v in colors[index])
    return (*rgb, 255), float(counts[index]) / float(len(border))


def _pick_sentinel(array: np.ndarray) -> tuple[int, int, int]:
    for candidate in _SENTINELS:
        if not np.any(np.all(array[:, :, :3] == np.array(candidate, dtype=np.uint8), axis=-1)):
            return candidate
    raise RuntimeError("could not find an unused flood-fill sentinel colour")


def remove_background(
    img: Image.Image,
    *,
    method: str = "flood",
    tolerance: int = 24,
    key_color: tuple[int, int, int] | None = None,
) -> tuple[Image.Image, dict[str, Any]]:
    """Make a solid background transparent.

    ``method='flood'`` floods inward from the four corners (keeps interior pixels of
    the same colour, which is what you want for pixel art). ``method='color'`` removes
    every pixel within *tolerance* of *key_color* anywhere in the image.
    """
    src = img.convert("RGBA")
    array = np.asarray(src)
    info: dict[str, Any] = {"method": method, "tolerance": tolerance}
    if key_color is None:
        detected, share = detect_background_color(src)
        key_rgb = detected[:3]
        info["detected_background_rgba"] = list(detected)
        info["border_share"] = round(share, 4)
    else:
        key_rgb = tuple(int(v) for v in key_color[:3])
        info["key_color"] = list(key_rgb)
    info["key_color_rgb"] = list(key_rgb)

    if method == "color":
        diff = np.abs(array[:, :, :3].astype(np.int16) - np.array(key_rgb, dtype=np.int16))
        match = np.all(diff <= tolerance, axis=-1)
        out = array.copy()
        out[match, 3] = 0
        info["removed_pixels"] = int(match.sum())
    elif method == "flood":
        sentinel = _pick_sentinel(array)
        work = src.copy()
        width, height = work.size
        corners = ((0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1))
        filled = 0
        draw = ImageDraw.Draw(work)
        for x, y in corners:
            current = work.getpixel((x, y))
            if len(current) == 4 and current[3] == 0:
                continue
            before = np.asarray(work)
            prior = np.count_nonzero(np.all(before[:, :, :3] == np.array(sentinel, np.uint8), axis=-1))
            ImageDraw.floodfill(work, (x, y), (*sentinel, 255), thresh=int(tolerance))
            after = np.asarray(work)
            now = np.count_nonzero(np.all(after[:, :, :3] == np.array(sentinel, np.uint8), axis=-1))
            filled += max(0, now - prior)
            draw = ImageDraw.Draw(work)
        mask = np.all(np.asarray(work)[:, :, :3] == np.array(sentinel, np.uint8), axis=-1)
        out = array.copy()
        out[mask, 3] = 0
        info["removed_pixels"] = int(mask.sum())
        info["corner_flood_fills"] = filled
    else:
        raise ValueError(f"unknown method {method!r} (expected flood|color)")

    result = Image.fromarray(out, mode="RGBA")
    total = array.shape[0] * array.shape[1]
    info["removed_ratio"] = round(float(np.count_nonzero(np.asarray(result)[:, :, 3] == 0)) / total, 4)
    return result, info


# --------------------------------------------------------------------------
# palette quantisation
# --------------------------------------------------------------------------
def quantize_palette(
    img: Image.Image,
    colors: int = 16,
    *,
    dither: bool = False,
    method: int = Image.Quantize.MEDIANCUT,
) -> Image.Image:
    """Reduce to *colors* RGB colours while keeping the original alpha channel."""
    if colors < 2:
        raise ValueError("colors must be >= 2")
    src = img.convert("RGBA")
    alpha = src.getchannel("A")
    rgb = src.convert("RGB")
    quantized = rgb.quantize(colors=colors, method=method, dither=Image.Dither.FLOYDSTEINBERG if dither else Image.Dither.NONE)
    out = quantized.convert("RGBA")
    out.putalpha(alpha)
    return out


# --------------------------------------------------------------------------
# tileability
# --------------------------------------------------------------------------
def _zero_transparent_rgb(array: np.ndarray) -> np.ndarray:
    out = array.astype(np.float32)
    transparent = out[:, :, 3] == 0
    out[transparent, :3] = 0.0
    return out


def tileability(img: Image.Image, *, band: int = 1) -> dict[str, Any]:
    """Measure how well the texture wraps.

    For each axis we compare the *seam* difference (first vs last edge) against the
    *interior* neighbour difference (first vs second edge). A seamless tile has a
    ratio near 1.0 (the wrap looks like any other adjacent pixel pair).
    """
    src = img.convert("RGBA")
    array = _zero_transparent_rgb(np.asarray(src))
    band = max(1, int(band))
    height, width = array.shape[:2]
    if width < 2 or height < 2:
        return {"ok": False, "reason": "image too small (need at least 2x2)"}

    left = array[:, :band, :]
    right = array[:, -band:, :]
    inner_x = array[:, band : band * 2, :] if width > band * 2 else array[:, band:, :]
    seam_x = float(np.mean(np.abs(left - right)))
    interior_x = float(np.mean(np.abs(array[:, :band, :] - inner_x))) if inner_x.size else 0.0

    top = array[:band, :, :]
    bottom = array[-band:, :, :]
    inner_y = array[band : band * 2, :, :] if height > band * 2 else array[band:, :, :]
    seam_y = float(np.mean(np.abs(top - bottom)))
    interior_y = float(np.mean(np.abs(array[:band, :, :] - inner_y))) if inner_y.size else 0.0

    def ratio(seam: float, interior: float) -> float:
        baseline = max(interior, 0.5)  # avoid dividing by ~0 on flat textures
        return seam / baseline

    ratio_x = ratio(seam_x, interior_x)
    ratio_y = ratio(seam_y, interior_y)
    worst = max(ratio_x, ratio_y)

    # A centred sprite on a transparent background has identical (empty) edges; the
    # seam metric is meaningless there, so say so instead of calling it "seamless".
    border_alpha = np.concatenate(
        [array[0, :, 3], array[-1, :, 3], array[:, 0, 3], array[:, -1, 3]]
    )
    transparent_border_ratio = float(np.mean(border_alpha <= 0.0))
    tiling_relevant = transparent_border_ratio < 0.9

    if not tiling_relevant:
        verdict = "non-tiling (transparent edges)"
    elif worst <= 1.5:
        verdict = "seamless"
    elif worst <= 3.0:
        verdict = "marginal"
    else:
        verdict = "visible-seam"

    return {
        "ok": tiling_relevant and verdict == "seamless",
        "tiling_relevant": tiling_relevant,
        "transparent_border_ratio": round(transparent_border_ratio, 4),
        "verdict": verdict,
        "seam_x": round(seam_x, 4),
        "seam_y": round(seam_y, 4),
        "interior_x": round(interior_x, 4),
        "interior_y": round(interior_y, 4),
        "ratio_x": round(ratio_x, 4),
        "ratio_y": round(ratio_y, 4),
        "worst_ratio": round(worst, 4),
        "thresholds": {"seamless": 1.5, "marginal": 3.0},
        "note": "ratio = edge-seam difference / typical neighbour difference; lower is better",
    }


def tile_preview(img: Image.Image, repeat: int = 2, scale: int = 1) -> Image.Image:
    """Repeat the texture into a grid so seams become obvious to the eye."""
    src = img.convert("RGBA")
    if scale > 1:
        src = src.resize((src.width * scale, src.height * scale), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (src.width * repeat, src.height * repeat), (0, 0, 0, 255))
    for y in range(repeat):
        for x in range(repeat):
            canvas.paste(src, (x * src.width, y * src.height), src)
    return canvas


# --------------------------------------------------------------------------
# comparison sheet
# --------------------------------------------------------------------------
def comparison_sheet(
    before: Image.Image,
    after: Image.Image,
    *,
    path: str | Path | None = None,
    scale: int = 8,
    labels: Sequence[str] = ("before", "after"),
    gap: int = 8,
) -> Image.Image:
    """Side-by-side nearest-neighbour upscale of two textures (+ checker backdrop)."""
    tiles: list[Image.Image] = []
    for image in (before, after):
        src = image.convert("RGBA")
        tiles.append(
            src.resize((src.width * scale, src.height * scale), Image.Resampling.NEAREST)
        )
    width = sum(tile.width for tile in tiles) + gap
    height = max(tile.height for tile in tiles)
    canvas = Image.new("RGBA", (width, height), (32, 32, 32, 255))
    x = 0
    for tile in tiles:
        canvas.alpha_composite(tile, (x, 0))
        x += tile.width + gap
    if labels:
        draw = ImageDraw.Draw(canvas)
        y = 2
        x = 0
        for label, tile in zip(labels, tiles):
            draw.text((x + 2, y), str(label), fill=(255, 255, 255, 255))
            x += tile.width + gap
    if path is not None:
        destination = Path(path)
        destination.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(destination)
    return canvas


# --------------------------------------------------------------------------
# full pipeline
# --------------------------------------------------------------------------
def process_texture(
    source: str | Path,
    out_dir: str | Path,
    *,
    base_name: str | None = None,
    sizes: Iterable[int] = TARGET_SIZES,
    alpha_mode: str = "cutout",
    alpha_threshold: int = 128,
    remove_bg: bool = True,
    bg_method: str = "flood",
    bg_tolerance: int = 24,
    key_color: tuple[int, int, int] | None = None,
    quantize_colors: int | None = 16,
    dither: bool = False,
    comparison_scale: int = 8,
    write_comparison: bool = True,
) -> TextureReport:
    """Run the texture chain and write the game-ready PNGs plus review images."""
    src_path = Path(source)
    destination = Path(out_dir)
    destination.mkdir(parents=True, exist_ok=True)
    name = base_name or src_path.stem

    original = open_rgba(src_path)
    working = original.copy()
    report: dict[str, Any] = {
        "source": str(src_path),
        "source_size": list(original.size),
        "outputs": [],
        "warnings": [],
    }

    if remove_bg:
        working, bg_info = remove_background(
            working, method=bg_method, tolerance=bg_tolerance, key_color=key_color
        )
        report["background_removal"] = bg_info
        if bg_info.get("removed_ratio", 0) == 0:
            report["warnings"].append("background removal removed no pixels")
    else:
        report["background_removal"] = {"skipped": True}

    if quantize_colors:
        working = quantize_palette(working, int(quantize_colors), dither=dither)
        report["quantization"] = {"colors": int(quantize_colors), "dither": dither}
    else:
        report["quantization"] = {"skipped": True}

    working = apply_alpha_mode(working, alpha_mode, alpha_threshold)
    report["alpha"] = {"mode": alpha_mode, "threshold": alpha_threshold}

    report["tileability_source"] = tileability(working)
    if write_comparison:
        comparison_path = destination / f"{name}_compare.png"
        comparison_sheet(
            original,
            working,
            path=comparison_path,
            scale=comparison_scale,
            labels=("source", "processed"),
        )
        report["comparison"] = str(comparison_path)

    for size in sizes:
        size = int(size)
        resized = nearest_resize(working, size)
        resized = apply_alpha_mode(resized, alpha_mode, alpha_threshold)
        out_path = destination / f"{name}_{size}.png"
        resized.save(out_path, format="PNG", optimize=False)
        tiling = tileability(resized)
        preview = tile_preview(resized, repeat=2, scale=max(1, 64 // size))
        preview_path = destination / f"{name}_{size}_tiled.png"
        preview.save(preview_path)
        report["outputs"].append(
            {
                "size": size,
                "path": str(out_path),
                "tiled_preview": str(preview_path),
                "bytes": out_path.stat().st_size,
                "tileability": tiling,
            }
        )
        if tiling.get("tiling_relevant", True) and tiling["verdict"] != "seamless":
            report["warnings"].append(f"{size}px: tileability {tiling['verdict']} (worst ratio {tiling['worst_ratio']})")

    return report

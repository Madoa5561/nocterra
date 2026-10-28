"""GLB -> voxel grid -> GeckoLib (Bedrock) ``.geo.json`` converter.

This is the "challenge" part of the Tripo workflow. It is deliberately a *surface*
voxeliser: the Tripo PBR mesh is sampled along its triangles, the base-colour texture
is sampled at each hit's UV, and the resulting shell is emitted as merged cubes into a
single-texture palette atlas.

Honest limitations (also recorded in the manifest and the report):
  * static geometry only - one ``root`` bone, no skeleton/animations,
  * UVs are replaced by a flat palette (PBR/normal maps are dropped),
  * the orientation/scale is an approximation that must be checked in-game,
  * small details below one voxel are lost.

If the GLB cannot be parsed or the mesh is empty, the converter raises
:class:`VoxelizeError` and the caller must report failure - never success.
"""

from __future__ import annotations

import json
import struct
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw

__all__ = ["VoxelizeError", "glb_to_geckolib", "parse_glb", "validate_geckolib", "voxelize_mesh"]

GLB_JSON_CHUNK = 0x4E4F534A
GLB_BIN_CHUNK = 0x004E4942

_COMPONENT = {
    5120: (np.int8, 1),
    5121: (np.uint8, 1),
    5122: (np.int16, 2),
    5123: (np.uint16, 2),
    5125: (np.uint32, 4),
    5126: (np.float32, 4),
}
_NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}

DEFAULT_RESOLUTION = 20
MAX_CUBES = 2600
TARGET_HEIGHT_PX = 40.0  # 2.5 Minecraft blocks


class VoxelizeError(RuntimeError):
    pass


# --------------------------------------------------------------------------
# GLB parsing
# --------------------------------------------------------------------------
def parse_glb(path: str | Path) -> tuple[dict[str, Any], bytes]:
    raw = Path(path).read_bytes()
    if len(raw) < 20 or raw[:4] != b"glTF":
        raise VoxelizeError("not a GLB file (bad magic)")
    version, _declared = struct.unpack("<II", raw[4:12])
    if version != 2:
        raise VoxelizeError(f"unsupported GLB version {version}")
    offset = 12
    document: dict[str, Any] | None = None
    blob = b""
    while offset + 8 <= len(raw):
        length, kind = struct.unpack("<II", raw[offset : offset + 8])
        offset += 8
        payload = raw[offset : offset + length]
        offset += length
        if kind == GLB_JSON_CHUNK and document is None:
            document = json.loads(payload.decode("utf-8").rstrip("\x00 "))
        elif kind == GLB_BIN_CHUNK:
            blob = payload
    if document is None:
        raise VoxelizeError("GLB has no JSON chunk")
    return document, blob


def _accessor(document: dict[str, Any], blob: bytes, index: int) -> np.ndarray:
    accessor = document["accessors"][index]
    view = document["bufferViews"][accessor["bufferView"]]
    dtype, itemsize = _COMPONENT[accessor["componentType"]]
    ncomp = _NCOMP[accessor["type"]]
    count = int(accessor["count"])
    base = int(view.get("byteOffset", 0)) + int(accessor.get("byteOffset", 0))
    stride = int(view.get("byteStride") or itemsize * ncomp)
    if stride == itemsize * ncomp:
        array = np.frombuffer(blob, dtype=dtype, count=count * ncomp, offset=base).reshape(count, ncomp)
    else:
        array = np.empty((count, ncomp), dtype=dtype)
        for row in range(count):
            start = base + row * stride
            array[row] = np.frombuffer(blob, dtype=dtype, count=ncomp, offset=start)
    if accessor.get("normalized") and dtype in (np.uint8, np.uint16):
        array = array.astype(np.float32) / float(np.iinfo(dtype).max)
    return array


def _image_from_glb(document: dict[str, Any], blob: bytes, image_index: int) -> Image.Image | None:
    images = document.get("images") or []
    if not 0 <= image_index < len(images):
        return None
    image = images[image_index]
    if image.get("bufferView") is None:
        return None  # external URI: not supported here
    view = document["bufferViews"][image["bufferView"]]
    start = int(view.get("byteOffset", 0))
    data = blob[start : start + int(view["byteLength"])]
    try:
        import io

        return Image.open(io.BytesIO(data)).convert("RGBA")
    except Exception as exc:  # noqa: BLE001
        raise VoxelizeError(f"could not decode embedded texture: {exc}") from exc


def _base_color_image(document: dict[str, Any], blob: bytes, primitive: dict[str, Any]) -> Image.Image | None:
    materials = document.get("materials") or []
    textures = document.get("textures") or []
    material_index = primitive.get("material")
    candidates: list[int] = []
    if material_index is not None and 0 <= material_index < len(materials):
        pbr = materials[material_index].get("pbrMetallicRoughness") or {}
        texture = pbr.get("baseColorTexture") or {}
        if texture.get("index") is not None and 0 <= texture["index"] < len(textures):
            candidates.append(int(textures[texture["index"]].get("source", 0)))
    # fallbacks: first embedded image
    candidates.extend(range(len(document.get("images") or [])))
    for index in candidates:
        image = _image_from_glb(document, blob, index)
        if image is not None:
            return image
    return None


def _sample_texture(texture: np.ndarray, u: np.ndarray, v: np.ndarray) -> np.ndarray:
    height, width = texture.shape[:2]
    px = np.clip((u % 1.0) * (width - 1), 0, width - 1).astype(np.int32)
    py = np.clip((1.0 - (v % 1.0)) * (height - 1), 0, height - 1).astype(np.int32)
    return texture[py, px]


# --------------------------------------------------------------------------
# voxelisation
# --------------------------------------------------------------------------
def voxelize_mesh(
    positions: np.ndarray,
    indices: np.ndarray,
    uvs: np.ndarray | None,
    texture: Image.Image | None,
    *,
    resolution: int = DEFAULT_RESOLUTION,
    step: float = 0.5,
) -> tuple[dict[tuple[int, int, int], tuple[int, int, int]], dict[str, Any]]:
    """Return ``{(i,j,k): (r,g,b)}`` for the mesh surface plus statistics."""
    if len(positions) == 0 or len(indices) == 0:
        raise VoxelizeError("mesh has no vertices/indices")
    minimum = positions.min(axis=0)
    maximum = positions.max(axis=0)
    extent = maximum - minimum
    longest = float(extent.max())
    if longest <= 0:
        raise VoxelizeError("degenerate mesh bounding box")
    scale = resolution / longest
    grid = (positions - minimum) * scale
    texture_array = np.asarray(texture) if texture is not None else None

    voxels: dict[tuple[int, int, int], tuple[int, int, int]] = {}
    fallback = np.array([150, 150, 150], dtype=np.uint8)
    triangles = indices.reshape(-1, 3)
    sampled = 0
    for triangle in triangles:
        if triangle.max() >= len(positions):
            continue
        p0, p1, p2 = grid[triangle[0]], grid[triangle[1]], grid[triangle[2]]
        longest_edge = max(
            float(np.linalg.norm(p1 - p0)),
            float(np.linalg.norm(p2 - p1)),
            float(np.linalg.norm(p0 - p2)),
        )
        steps = max(1, min(64, int(np.ceil(longest_edge / step))))
        for a in range(steps + 1):
            for b in range(steps + 1 - a):
                wa, wb = a / steps, b / steps
                wc = 1.0 - wa - wb
                point = wa * p0 + wb * p1 + wc * p2
                cell = (
                    int(np.clip(np.floor(point[0]), 0, resolution - 1)),
                    int(np.clip(np.floor(point[1]), 0, resolution - 1)),
                    int(np.clip(np.floor(point[2]), 0, resolution - 1)),
                )
                if texture_array is not None and uvs is not None:
                    uv = wa * uvs[triangle[0]] + wb * uvs[triangle[1]] + wc * uvs[triangle[2]]
                    rgba = _sample_texture(texture_array, np.array([uv[0]]), np.array([uv[1]]))[0]
                    if rgba[3] < 96:
                        continue
                    color = (int(rgba[0]), int(rgba[1]), int(rgba[2]))
                else:
                    color = tuple(int(c) for c in fallback)
                voxels[cell] = color
                sampled += 1
    stats = {
        "resolution": resolution,
        "surface_voxels": len(voxels),
        "samples": sampled,
        "triangles": int(len(triangles)),
        "extent": [round(float(v), 5) for v in extent],
        "scale": round(float(scale), 5),
    }
    return voxels, stats


# --------------------------------------------------------------------------
# palette + geometry
# --------------------------------------------------------------------------
def _merge_along_x(
    voxels: dict[tuple[int, int, int], int]
) -> list[tuple[int, int, int, int, int]]:
    """Greedy merge: one cube per run of identical colour along X."""
    rows: dict[tuple[int, int, int], list[int]] = {}
    for (i, j, k), color in voxels.items():
        rows.setdefault((j, k, color), []).append(i)
    cubes: list[tuple[int, int, int, int, int]] = []
    for (j, k, color), xs in rows.items():
        xs.sort()
        start = previous = xs[0]
        for x in xs[1:]:
            if x == previous + 1:
                previous = x
                continue
            cubes.append((start, j, k, previous - start + 1, color))
            start = previous = x
        cubes.append((start, j, k, previous - start + 1, color))
    return cubes


def build_geometry(
    voxels: dict[tuple[int, int, int], tuple[int, int, int]],
    *,
    boss_id: str,
    resolution: int,
    height_px: float = TARGET_HEIGHT_PX,
) -> tuple[dict[str, Any], list[tuple[int, int, int]], list[tuple[int, int, int, int, int]], dict[str, Any]]:
    if not voxels:
        raise VoxelizeError("no voxels generated")
    # 5-bit colour buckets -> at most 256 palette entries, ordered by luminance.
    counts: dict[tuple[int, int, int], int] = {}
    for color in voxels.values():
        bucket = (color[0] // 8 * 8, color[1] // 8 * 8, color[2] // 8 * 8)
        counts[bucket] = counts.get(bucket, 0) + 1
    ranked = sorted(counts.items(), key=lambda item: item[1], reverse=True)[:256]
    palette = sorted((color for color, _count in ranked), key=lambda c: (sum(c), c))
    palette_index = {color: index for index, color in enumerate(palette)}
    indexed = {
        cell: palette_index.get((color[0] // 8 * 8, color[1] // 8 * 8, color[2] // 8 * 8), 0)
        for cell, color in voxels.items()
    }
    cubes = _merge_along_x(indexed)

    voxel_px = height_px / (resolution - 1 if resolution > 1 else 1)
    # centre X/Z, put feet at Y=0, flip Z so the model faces the same way as Minecraft
    cx = (resolution - 1) / 2.0
    cz = (resolution - 1) / 2.0
    max_run = max((length for _i, _j, _k, length, _c in cubes), default=1)
    max_sx = max_run * voxel_px
    max_sy = voxel_px
    max_sz = voxel_px

    # Palette atlas: one solid block per colour, big enough that a cube's whole UV
    # footprint (2*size_x+2*size_z wide, size_y+size_z tall) stays inside its block.
    cols = 8
    block_w = int(np.ceil(2.0 * (max_sx + max_sz))) + 4
    block_h = int(np.ceil(max_sy + max_sz)) + 4
    block_w = max(32, int(np.ceil(block_w / 8.0) * 8))
    block_h = max(8, int(np.ceil(block_h / 8.0) * 8))
    rows = max(1, int(np.ceil(len(palette) / cols)))
    atlas_w = cols * block_w
    atlas_h = rows * block_h
    if atlas_w > 2048 or atlas_h > 2048:
        raise VoxelizeError(f"palette atlas too large ({atlas_w}x{atlas_h})")

    geometry_cubes: list[dict[str, Any]] = []
    for (i, j, k, length, color_index) in cubes:
        uv = [(color_index % cols) * block_w, (color_index // cols) * block_h]
        origin = [
            round((i - cx) * voxel_px, 3),
            round(j * voxel_px, 3),
            round(-(k - cz) * voxel_px, 3) - voxel_px,
        ]
        size = [round(length * voxel_px, 3), round(voxel_px, 3), round(voxel_px, 3)]
        geometry_cubes.append({"origin": origin, "size": size, "uv": uv})
    geometry_cubes.sort(key=lambda cube: (cube["origin"][1], cube["origin"][2], cube["origin"][0]))

    document = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.nocterra.{boss_id}",
                    "texture_width": atlas_w,
                    "texture_height": atlas_h,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 5,
                    "visible_bounds_offset": [0.0, 1.5, 0.0],
                },
                "bones": [
                    {
                        "name": "body",
                        "pivot": [0.0, 0.0, 0.0],
                        "cubes": geometry_cubes,
                    }
                ],
            }
        ],
    }
    stats = {
        "cubes": len(geometry_cubes),
        "palette_colors": len(palette),
        "voxel_px": round(voxel_px, 3),
        "height_px": height_px,
        "max_run_voxels": int(max_run),
        "atlas": {
            "width": atlas_w,
            "height": atlas_h,
            "block_w": block_w,
            "block_h": block_h,
            "columns": cols,
        },
        "uv_footprint_px": {
            "width": round(2.0 * (max_sx + max_sz), 2),
            "height": round(max_sy + max_sz, 2),
        },
    }
    return document, palette, geometry_cubes, stats


def render_voxel_preview(
    voxels: dict[tuple[int, int, int], tuple[int, int, int]],
    path: str | Path,
    *,
    resolution: int | None = None,
    height_px: int = 360,
) -> Path:
    """Orthographic front/side preview (cropped to the model) for human sanity checks."""
    if not voxels:
        raise VoxelizeError("no voxels to preview")
    xs = [cell[0] for cell in voxels]
    ys = [cell[1] for cell in voxels]
    zs = [cell[2] for cell in voxels]
    x0, x1 = min(xs), max(xs)
    y0, y1 = min(ys), max(ys)
    z0, z1 = min(zs), max(zs)
    cells_y = y1 - y0 + 1
    scale = max(1, int(round(height_px / max(1, cells_y))))
    front_w = (x1 - x0 + 1) * scale
    side_w = (z1 - z0 + 1) * scale
    gap = 10
    canvas = Image.new("RGB", (front_w + gap + side_w, cells_y * scale), (24, 24, 28))
    draw = ImageDraw.Draw(canvas)
    ordered = sorted(voxels.items(), key=lambda item: item[0][2])
    depth = max(1, z1 - z0)
    for (i, j, k), color in ordered:
        shade = 0.72 + 0.28 * ((k - z0) / depth)
        rgb = tuple(int(c * shade) for c in color)
        top = (y1 - j) * scale
        draw.rectangle(
            [(i - x0) * scale, top, (i - x0 + 1) * scale - 1, top + scale - 1],
            fill=rgb,
        )
        offset = front_w + gap
        draw.rectangle(
            [offset + (k - z0) * scale, top, offset + (k - z0 + 1) * scale - 1, top + scale - 1],
            fill=rgb,
        )
    out = Path(path)
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out)
    return out


def validate_geckolib(geo_path: str | Path, texture_path: str | Path) -> dict[str, Any]:
    """Structural check of the produced Bedrock/GeckoLib geometry + palette atlas."""
    errors: list[str] = []
    warnings: list[str] = []
    try:
        document = json.loads(Path(geo_path).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return {"ok": False, "errors": [f"cannot read geo.json: {exc}"], "warnings": []}
    if document.get("format_version") != "1.12.0":
        warnings.append(f"format_version={document.get('format_version')!r} (expected '1.12.0')")
    geometry_list = document.get("minecraft:geometry")
    if not isinstance(geometry_list, list) or not geometry_list:
        return {"ok": False, "errors": ["missing minecraft:geometry array"], "warnings": warnings}
    geometry = geometry_list[0]
    description = geometry.get("description") or {}
    for key in ("identifier", "texture_width", "texture_height"):
        if key not in description:
            errors.append(f"description missing {key}")
    if not str(description.get("identifier", "")).startswith("geometry.nocterra."):
        warnings.append("identifier does not follow the geometry.nocterra.* convention")
    bones = geometry.get("bones") or []
    if not bones:
        errors.append("no bones")
    cubes: list[dict[str, Any]] = []
    for bone in bones:
        for cube in bone.get("cubes") or []:
            cubes.append(cube)
    if not cubes:
        errors.append("no cubes")
    texture_width = int(description.get("texture_width", 0))
    texture_height = int(description.get("texture_height", 0))
    try:
        with Image.open(texture_path) as image:
            atlas_size = image.size
    except Exception as exc:  # noqa: BLE001
        return {"ok": False, "errors": [f"cannot read texture: {exc}"], "warnings": warnings}
    if list(atlas_size) != [texture_width, texture_height]:
        errors.append(
            f"texture size {atlas_size} != description texture_width/height "
            f"{texture_width}x{texture_height}"
        )
    uv_min = [10**9, 10**9]
    uv_max = [-10**9, -10**9]
    for index, cube in enumerate(cubes):
        origin = cube.get("origin")
        size = cube.get("size")
        uv = cube.get("uv")
        if not (isinstance(origin, list) and len(origin) == 3):
            errors.append(f"cube {index}: bad origin")
            continue
        if not (isinstance(size, list) and len(size) == 3) or min(size) <= 0:
            errors.append(f"cube {index}: bad size {size!r}")
        if not (isinstance(uv, list) and len(uv) == 2):
            errors.append(f"cube {index}: bad uv")
            continue
        uv_min = [min(uv_min[0], uv[0]), min(uv_min[1], uv[1])]
        uv_max = [max(uv_max[0], uv[0]), max(uv_max[1], uv[1])]
        # the UV footprint of a box must stay inside the atlas for every face
        footprint_w = 2 * float(size[0]) + 2 * float(size[2])
        footprint_h = float(size[1]) + float(size[2])
        if uv[0] + footprint_w > texture_width or uv[1] + footprint_h > texture_height:
            errors.append(
                f"cube {index}: uv {uv} + footprint ({footprint_w:.1f},{footprint_h:.1f}) "
                f"exceeds the {texture_width}x{texture_height} atlas"
            )
            if len(errors) > 12:
                break
    if len(cubes) > MAX_CUBES:
        warnings.append(f"{len(cubes)} cubes exceeds the recommended {MAX_CUBES} budget")
    return {
        "ok": not errors,
        "errors": errors,
        "warnings": warnings,
        "details": {
            "format_version": document.get("format_version"),
            "identifier": description.get("identifier"),
            "texture_width": texture_width,
            "texture_height": texture_height,
            "bones": len(bones),
            "cubes": len(cubes),
            "uv_range": [uv_min, uv_max],
        },
    }


# --------------------------------------------------------------------------
# entry point
# --------------------------------------------------------------------------
def glb_to_geckolib(
    glb_path: str | Path,
    out_dir: str | Path,
    boss_id: str,
    *,
    resolution: int = DEFAULT_RESOLUTION,
    max_cubes: int = MAX_CUBES,
    preview_path: str | Path | None = None,
) -> dict[str, Any]:
    document, blob = parse_glb(glb_path)
    meshes = document.get("meshes") or []
    if not meshes:
        raise VoxelizeError("GLB contains no meshes")
    primitive = meshes[0]["primitives"][0]
    attributes = primitive.get("attributes") or {}
    if "POSITION" not in attributes:
        raise VoxelizeError("primitive has no POSITION attribute")
    positions = _accessor(document, blob, attributes["POSITION"]).astype(np.float64)
    indices = _accessor(document, blob, primitive["indices"]).ravel().astype(np.int64) \
        if "indices" in primitive else np.arange(len(positions), dtype=np.int64)
    uvs = _accessor(document, blob, attributes["TEXCOORD_0"]).astype(np.float64) \
        if "TEXCOORD_0" in attributes else None
    texture = _base_color_image(document, blob, primitive)

    attempts: list[dict[str, Any]] = []
    chosen: dict[str, Any] | None = None
    candidates = sorted({int(resolution), 20, 16, 12}, reverse=True)
    candidates = [value for value in candidates if value <= int(resolution)]
    if not candidates:
        candidates = [int(resolution)]
    for candidate in candidates:
        voxels, stats = voxelize_mesh(positions, indices, uvs, texture, resolution=candidate)
        geometry, palette, cubes, geo_stats = build_geometry(
            voxels, boss_id=boss_id, resolution=candidate
        )
        attempts.append({"resolution": candidate, **geo_stats})
        if len(cubes) <= max_cubes or candidate <= 12:
            chosen = {
                "document": geometry,
                "palette": palette,
                "stats": {**stats, **geo_stats},
                "voxels": voxels,
            }
            break
    if chosen is None:
        raise VoxelizeError("could not build geometry within the cube budget")

    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    geo_path = out / f"{boss_id}.geo.json"
    geo_path.write_text(
        json.dumps(chosen["document"], ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    atlas = chosen["stats"]["atlas"]
    palette_image = Image.new("RGBA", (atlas["width"], atlas["height"]), (128, 128, 128, 255))
    draw = ImageDraw.Draw(palette_image)
    for index, color in enumerate(chosen["palette"][:256]):
        bx = (index % atlas["columns"]) * atlas["block_w"]
        by = (index // atlas["columns"]) * atlas["block_h"]
        draw.rectangle(
            [bx, by, bx + atlas["block_w"] - 1, by + atlas["block_h"] - 1],
            fill=(*color, 255),
        )
    texture_path = out / f"{boss_id}.png"
    palette_image.save(texture_path)

    preview = None
    if preview_path is not None:
        preview = render_voxel_preview(
            chosen["voxels"], preview_path, resolution=chosen["stats"]["resolution"]
        )
    return {
        "status": "success",
        "boss_id": boss_id,
        "geo_path": str(geo_path),
        "texture_path": str(texture_path),
        "preview_path": str(preview) if preview else None,
        "stats": chosen["stats"],
        "attempts": attempts,
        "reason": None,
    }

"""Format validators for generated assets: GLB, PNG, audio, JSON.

The GLB validator is a real parser (magic/version/length, chunk walk, JSON chunk
parse, accessor & primitive accounting, triangle estimate, UV detection). It is the
gate used by :mod:`asset_pipeline.tripo` before a model is accepted.
"""

from __future__ import annotations

import json
import struct
from pathlib import Path
from typing import Any

__all__ = [
    "ValidationResult",
    "detect_kind",
    "validate_audio",
    "validate_glb",
    "validate_json",
    "validate_path",
    "validate_png",
]

GLB_MAGIC = b"glTF"
GLB_JSON_CHUNK = 0x4E4F534A  # 'JSON'
GLB_BIN_CHUNK = 0x004E4942   # 'BIN\0'


class ValidationResult:
    def __init__(self, kind: str, path: str | Path) -> None:
        self.kind = kind
        self.path = str(path)
        self.ok = True
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.details: dict[str, Any] = {}

    def error(self, message: str) -> None:
        self.ok = False
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def as_dict(self) -> dict[str, Any]:
        return {
            "ok": self.ok,
            "kind": self.kind,
            "path": self.path,
            "errors": self.errors,
            "warnings": self.warnings,
            "details": self.details,
        }


# --------------------------------------------------------------------------
# GLB
# --------------------------------------------------------------------------
def validate_glb(path: str | Path, *, max_triangles: int | None = None) -> ValidationResult:
    result = ValidationResult("glb", path)
    file_path = Path(path)
    if not file_path.is_file():
        result.error("file not found")
        return result
    size = file_path.stat().st_size
    result.details["file_size"] = size
    if size < 20:
        result.error(f"file too small for a GLB header ({size} bytes)")
        return result

    with file_path.open("rb") as handle:
        header = handle.read(12)
        magic, version, declared_length = struct.unpack("<4sII", header)
        result.details.update(magic=magic.decode("latin-1"), version=version, declared_length=declared_length)
        if magic != GLB_MAGIC:
            result.error(f"bad magic {magic!r}; expected b'glTF'")
            return result
        if version != 2:
            result.error(f"unsupported GLB version {version}; expected 2")
        if declared_length != size:
            result.warn(f"header length {declared_length} != file size {size}")

        chunks: list[dict[str, Any]] = []
        offset = 12
        json_doc: dict[str, Any] | None = None
        bin_bytes = 0
        while offset + 8 <= size:
            handle.seek(offset)
            chunk_len, chunk_type = struct.unpack("<II", handle.read(8))
            offset += 8
            if offset + chunk_len > size:
                result.error(
                    f"chunk at offset {offset - 8} declares {chunk_len} bytes but only "
                    f"{size - offset} remain"
                )
                break
            payload = handle.read(chunk_len)
            offset += chunk_len
            entry: dict[str, Any] = {
                "type": hex(chunk_type),
                "type_name": (
                    "JSON" if chunk_type == GLB_JSON_CHUNK else "BIN" if chunk_type == GLB_BIN_CHUNK else "UNKNOWN"
                ),
                "length": chunk_len,
            }
            chunks.append(entry)
            if chunk_type == GLB_JSON_CHUNK and json_doc is None:
                try:
                    json_doc = json.loads(payload.decode("utf-8").rstrip("\x00 "))
                except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                    result.error(f"JSON chunk is not valid JSON: {exc}")
            elif chunk_type == GLB_BIN_CHUNK:
                bin_bytes += chunk_len
        result.details["chunks"] = chunks
        result.details["bin_bytes"] = bin_bytes
        if offset != size:
            result.warn(f"{size - offset} trailing byte(s) after the last chunk")

    if json_doc is None:
        result.error("no JSON chunk found")
        return result

    asset = json_doc.get("asset") or {}
    result.details["asset"] = asset
    if not asset.get("version"):
        result.error("JSON chunk has no asset.version")
    if asset.get("version") and str(asset["version"]).split(".")[0] != "2":
        result.warn(f"asset.version={asset.get('version')} (GLB2 loader expects 2.x)")

    meshes = json_doc.get("meshes") or []
    accessors = json_doc.get("accessors") or []
    buffer_views = json_doc.get("bufferViews") or []
    materials = json_doc.get("materials") or []
    textures = json_doc.get("textures") or []
    images = json_doc.get("images") or []
    nodes = json_doc.get("nodes") or []
    skins = json_doc.get("skins") or []
    animations = json_doc.get("animations") or []

    triangles = 0.0
    primitive_count = 0
    has_uv = False
    has_normals = False
    has_vertex_colors = False
    has_tangents = False
    modes: set[int] = set()
    for mesh in meshes:
        for primitive in mesh.get("primitives") or []:
            primitive_count += 1
            attributes = primitive.get("attributes") or {}
            if "TEXCOORD_0" in attributes:
                has_uv = True
            if "NORMAL" in attributes:
                has_normals = True
            if "COLOR_0" in attributes:
                has_vertex_colors = True
            if "TANGENT" in attributes:
                has_tangents = True
            mode = int(primitive.get("mode", 4))
            modes.add(mode)
            count = 0
            if "indices" in primitive:
                idx = int(primitive["indices"])
                if 0 <= idx < len(accessors):
                    count = int(accessors[idx].get("count", 0))
            elif "POSITION" in attributes:
                pos = int(attributes["POSITION"])
                if 0 <= pos < len(accessors):
                    count = int(accessors[pos].get("count", 0))
            if mode == 4:  # TRIANGLES
                triangles += count / 3.0
            elif mode == 5:  # TRIANGLE_STRIP
                triangles += max(0, count - 2)
            elif mode == 6:  # TRIANGLE_FAN
                triangles += max(0, count - 2)
    triangle_estimate = int(round(triangles))
    result.details.update(
        meshes=len(meshes),
        primitives=primitive_count,
        accessors=len(accessors),
        buffer_views=len(buffer_views),
        materials=len(materials),
        textures=len(textures),
        images=len(images),
        nodes=len(nodes),
        skins=len(skins),
        animations=len(animations),
        triangle_estimate=triangle_estimate,
        primitive_modes=sorted(modes),
        has_uv=has_uv,
        has_normals=has_normals,
        has_vertex_colors=has_vertex_colors,
        has_tangents=has_tangents,
    )

    if not meshes:
        result.error("GLB contains no meshes")
    if not accessors:
        result.error("GLB contains no accessors")
    if primitive_count and not has_uv:
        result.warn("no TEXCOORD_0 attribute: the model is untextured / not UV mapped")
    if not images:
        result.warn("no embedded images (textures are empty or external)")
    if not has_normals:
        result.warn("no NORMAL attribute: flat shading only")
    if triangle_estimate == 0 and meshes:
        result.warn("triangle estimate is 0 (unusual topology or missing indices)")
    if max_triangles is not None and triangle_estimate > max_triangles:
        result.warn(
            f"triangle estimate {triangle_estimate:,} exceeds the {max_triangles:,} budget "
            "for a Minecraft/GeckoLib model (consider Tripo face_limit / conversion)"
        )
    return result


# --------------------------------------------------------------------------
# PNG
# --------------------------------------------------------------------------
def validate_png(
    path: str | Path,
    *,
    expected_sizes: tuple[int, ...] = (16, 32, 64),
    require_square: bool = False,
) -> ValidationResult:
    result = ValidationResult("png", path)
    file_path = Path(path)
    if not file_path.is_file():
        result.error("file not found")
        return result
    raw = file_path.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        result.error("missing PNG signature")
        return result
    # IHDR is the first chunk: length(4) type(4) width(4) height(4) bitdepth(1) colortype(1)...
    if len(raw) < 33:
        result.error("truncated PNG (no complete IHDR)")
        return result
    ihdr_len, ihdr_type = struct.unpack(">I4s", raw[8:16])
    if ihdr_type != b"IHDR" or ihdr_len != 13:
        result.error(f"first chunk is not a valid IHDR ({ihdr_type!r})")
        return result
    width, height, bit_depth, color_type = struct.unpack(">IIBB", raw[16:26])
    result.details.update(
        width=width,
        height=height,
        bit_depth=bit_depth,
        color_type=color_type,
        file_size=len(raw),
    )
    try:
        from PIL import Image  # noqa: PLC0415 - optional at import time

        with Image.open(file_path) as img:
            img.load()
            result.details.update(mode=img.mode, format=img.format)
            result.details["has_alpha"] = img.mode in ("RGBA", "LA", "PA") or (
                img.mode == "P" and "transparency" in img.info
            )
            if img.mode == "RGBA":
                alpha = img.getchannel("A")
                extrema = alpha.getextrema()
                result.details["alpha_extrema"] = list(extrema)
    except ImportError:
        result.warn("Pillow not installed: pixel-level checks skipped")
    except Exception as exc:  # noqa: BLE001 - PIL raises varied errors
        result.error(f"Pillow could not decode the PNG: {exc}")

    if require_square and width != height:
        result.error(f"texture must be square, got {width}x{height}")
    if width != height:
        result.warn(f"non-square texture {width}x{height}")
    if width & (width - 1) or height & (height - 1):
        result.warn(f"{width}x{height} is not a power of two")
    if expected_sizes and width not in expected_sizes:
        result.warn(f"width {width} not in the usual Minecraft sizes {expected_sizes}")
    if not raw.rstrip(b"\x00").endswith(b"IEND\xaeB`\x82"):
        if b"IEND" not in raw[-16:]:
            result.warn("missing IEND chunk (PNG may be truncated)")
    return result


# --------------------------------------------------------------------------
# Audio
# --------------------------------------------------------------------------
def validate_audio(path: str | Path, *, probe_duration: bool = True) -> ValidationResult:
    result = ValidationResult("audio", path)
    file_path = Path(path)
    if not file_path.is_file():
        result.error("file not found")
        return result
    raw = file_path.read_bytes()
    result.details["file_size"] = len(raw)
    head = raw[:64]
    container = "unknown"
    codec = "unknown"
    if head[:4] == b"OggS":
        container = "ogg"
        if b"\x01vorbis" in raw[:256]:
            codec = "vorbis"
        elif b"OpusHead" in raw[:256]:
            codec = "opus"
        else:
            codec = "unknown-ogg"
    elif head[:4] == b"RIFF" and head[8:12] == b"WAVE":
        container, codec = "wav", "pcm"
    elif head[:3] == b"ID3":
        container, codec = "mp3", "mp3"
    elif len(head) > 1 and head[0] == 0xFF and (head[1] & 0xE0) == 0xE0:
        container, codec = "mp3", "mp3"
    elif head[:4] == b"fLaC":
        container, codec = "flac", "flac"
    else:
        result.error("unrecognised audio container (expected Ogg/RIFF-WAVE/MP3/FLAC)")
    result.details.update(container=container, codec=codec)
    if container == "ogg" and codec == "opus":
        result.warn("Ogg/Opus is not playable by Minecraft's Ogg *Vorbis* loader; convert to Vorbis")
    if container == "ogg" and codec == "unknown-ogg":
        result.warn("Ogg stream codec not identified")

    if probe_duration:
        try:
            from .audio_tools import ffprobe_duration  # noqa: PLC0415

            info = ffprobe_duration(file_path)
            if info:
                result.details.update(info)
        except Exception as exc:  # noqa: BLE001
            result.warn(f"ffprobe unavailable: {exc}")
    return result


# --------------------------------------------------------------------------
# JSON
# --------------------------------------------------------------------------
def validate_json(path: str | Path, *, required_keys: list[str] | None = None) -> ValidationResult:
    result = ValidationResult("json", path)
    file_path = Path(path)
    if not file_path.is_file():
        result.error("file not found")
        return result
    try:
        data = json.loads(file_path.read_text(encoding="utf-8-sig"))
    except json.JSONDecodeError as exc:
        result.error(f"invalid JSON: {exc}")
        return result
    result.details["top_level_type"] = type(data).__name__
    if isinstance(data, dict):
        result.details["keys"] = list(data.keys())
    for key in required_keys or []:
        if not isinstance(data, dict) or key not in data:
            result.error(f"missing required key {key!r}")
    return result


# --------------------------------------------------------------------------
# dispatch
# --------------------------------------------------------------------------
_KIND_BY_SUFFIX = {
    ".glb": "glb",
    ".gltf": "glb",
    ".png": "png",
    ".ogg": "audio",
    ".wav": "audio",
    ".mp3": "audio",
    ".flac": "audio",
    ".json": "json",
}


def detect_kind(path: str | Path) -> str | None:
    return _KIND_BY_SUFFIX.get(Path(path).suffix.lower())


def validate_path(
    path: str | Path,
    *,
    max_triangles: int | None = None,
    expected_sizes: tuple[int, ...] = (16, 32, 64),
) -> ValidationResult:
    kind = detect_kind(path)
    if kind == "glb":
        return validate_glb(path, max_triangles=max_triangles)
    if kind == "png":
        return validate_png(path, expected_sizes=expected_sizes)
    if kind == "audio":
        return validate_audio(path)
    if kind == "json":
        return validate_json(path)
    result = ValidationResult("unknown", path)
    result.error(f"unsupported file extension {Path(path).suffix!r}")
    return result

"""Audio helpers: ffmpeg discovery, WAV -> Ogg/Vorbis conversion, ffprobe metadata.

ffmpeg **is** available on this machine (verified), but the pipeline must still work
without it: in that case TTS output stays as WAV and :func:`convert_to_ogg_vorbis`
raises :class:`FFmpegUnavailable` with installation instructions.

Minecraft needs **Ogg Vorbis**; Fish Audio's ``opus`` output is *Ogg Opus*, which the
vanilla Ogg loader does not decode, so an explicit Vorbis transcode is required.
"""

from __future__ import annotations

import json
import shutil
import subprocess
import wave
from pathlib import Path
from typing import Any

__all__ = [
    "FFmpegUnavailable",
    "convert_to_ogg_vorbis",
    "ffmpeg_version",
    "ffprobe_duration",
    "find_ffmpeg",
    "find_ffprobe",
    "wav_duration",
]

#: Well-known locations on Windows (winget install path) as a fallback.
_EXTRA_PATHS = [
    Path.home() / "AppData/Local/Microsoft/WinGet/Packages",
]


class FFmpegUnavailable(RuntimeError):
    """ffmpeg is not installed / not on PATH."""


def _search_winget(name: str) -> str | None:
    for root in _EXTRA_PATHS:
        if not root.is_dir():
            continue
        try:
            for candidate in root.glob(f"Gyan.FFmpeg*/**/bin/{name}.exe"):
                return str(candidate)
        except OSError:
            continue
    return None


def find_ffmpeg() -> str | None:
    found = shutil.which("ffmpeg")
    return found or _search_winget("ffmpeg")


def find_ffprobe() -> str | None:
    found = shutil.which("ffprobe")
    return found or _search_winget("ffprobe")


def ffmpeg_version(timeout_s: float = 15.0) -> str | None:
    exe = find_ffmpeg()
    if not exe:
        return None
    try:
        proc = subprocess.run(
            [exe, "-version"], capture_output=True, text=True, timeout=timeout_s, check=False
        )
        return (proc.stdout or "").splitlines()[0] if proc.stdout else None
    except (OSError, subprocess.SubprocessError):
        return None


def wav_duration(path: str | Path) -> float | None:
    try:
        with wave.open(str(path), "rb") as handle:
            frames = handle.getnframes()
            rate = handle.getframerate() or 1
            return round(frames / float(rate), 3)
    except (wave.Error, OSError):
        return None


def ffprobe_duration(path: str | Path, timeout_s: float = 20.0) -> dict[str, Any] | None:
    exe = find_ffprobe()
    if not exe:
        return None
    try:
        proc = subprocess.run(
            [
                exe,
                "-v",
                "error",
                "-select_streams",
                "a:0",
                "-show_entries",
                "stream=codec_name,sample_rate,channels,duration:format=duration,format_name",
                "-of",
                "json",
                str(path),
            ],
            capture_output=True,
            text=True,
            timeout=timeout_s,
            check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return None
    if proc.returncode != 0 or not proc.stdout:
        return None
    try:
        data = json.loads(proc.stdout)
    except json.JSONDecodeError:
        return None
    stream = (data.get("streams") or [{}])[0]
    fmt = data.get("format") or {}
    duration = stream.get("duration") or fmt.get("duration")
    return {
        "codec": stream.get("codec_name"),
        "sample_rate": _to_int(stream.get("sample_rate")),
        "channels": _to_int(stream.get("channels")),
        "duration_s": round(float(duration), 3) if duration else None,
        "format": fmt.get("format_name"),
    }


def _to_int(value: Any) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def convert_to_ogg_vorbis(
    source: str | Path,
    destination: str | Path,
    *,
    quality: int = 5,
    sample_rate: int | None = None,
    mono: bool = False,
    overwrite: bool = True,
) -> Path:
    """Transcode *source* to Ogg/Vorbis (Minecraft-compatible).

    Raises :class:`FFmpegUnavailable` when ffmpeg is missing.
    """
    exe = find_ffmpeg()
    if not exe:
        raise FFmpegUnavailable(
            "ffmpeg not found. Install it (e.g. `winget install Gyan.FFmpeg`) or keep the "
            "WAV output; Minecraft needs Ogg Vorbis, so a converter is required for audio."
        )
    src = Path(source)
    dst = Path(destination)
    dst.parent.mkdir(parents=True, exist_ok=True)
    command = [exe, "-hide_banner", "-loglevel", "error", "-y" if overwrite else "-n", "-i", str(src)]
    if mono:
        command += ["-ac", "1"]
    if sample_rate:
        command += ["-ar", str(sample_rate)]
    command += ["-c:a", "libvorbis", "-q:a", str(quality), str(dst)]
    proc = subprocess.run(command, capture_output=True, text=True, timeout=300, check=False)
    if proc.returncode != 0:
        raise RuntimeError(f"ffmpeg failed ({proc.returncode}): {(proc.stderr or '').strip()[:500]}")
    if not dst.is_file() or dst.stat().st_size == 0:
        raise RuntimeError(f"ffmpeg produced no output for {dst}")
    return dst


def convert_to_ogg_vorbis_or_none(source: str | Path, destination: str | Path) -> tuple[Path | None, str]:
    """Best-effort conversion returning ``(path|None, status_message)``."""
    try:
        return convert_to_ogg_vorbis(source, destination), "converted to Ogg/Vorbis"
    except FFmpegUnavailable as exc:
        return None, f"skipped: {exc}"
    except RuntimeError as exc:
        return None, f"failed: {exc}"

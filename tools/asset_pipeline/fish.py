"""Fish Audio TTS client (``POST /v1/tts``).

Facts verified against the official docs page (``docs.fish.audio`` OpenAPI section,
fetched 2026-10-28) and a live non-billable ``GET /model`` call:

* base URL ``https://api.fish.audio``, auth ``Authorization: Bearer <FISH>``
* ``POST /v1/tts`` accepts ``application/json`` (or msgpack), required body field ``text``
* the TTS model is selected with the **``model`` request header**
  (``s1``, ``s2-pro``, ``s2.1-pro``, ``s2.1-pro-free``, ``drama-3``; default ``s2.1-pro``)
* output ``format``: ``wav`` | ``pcm`` | ``mp3`` | ``opus`` (48 kHz); there is **no ogg
  vorbis option**, so a Vorbis transcode is required for Minecraft.
* ``GET /model?page_size=N`` lists voice models (``_id``/``title``/``tags``/``languages``)

TTS is billable, so it goes through the same spend gate as the other services.
"""

from __future__ import annotations

import base64
import hashlib
import json
from pathlib import Path
from typing import Any

from .audio_tools import (
    FFmpegUnavailable,
    convert_to_ogg_vorbis_or_none,
    find_ffmpeg,
    ffmpeg_version,
    ffprobe_duration,
)
from .config import Config, CostEstimate
from .env import mask
from .http_client import HttpResponse, Transport
from .openai_image import SpendLocked
from .util import save_dry_run_preview

__all__ = ["DEFAULT_TTS_MODEL", "TTS_MODELS", "FishClient", "FishError"]

DEFAULT_TTS_MODEL = "s2.1-pro"  # documented header default / fallback
TTS_MODELS = ("s1", "s2-pro", "s2.1-pro", "s2.1-pro-free", "drama-3")
OUTPUT_FORMATS = ("wav", "pcm", "mp3", "opus")

#: Body keys copied from the job's ``audio`` section (documented in TTSRequest).
ALLOWED_TTS_PARAMS = {
    "tts_model",  # sent in the `model` header, not the body (documented)
    "reference_id",
    "format",
    "sample_rate",
    "mp3_bitrate",
    "opus_bitrate",
    "latency",
    "temperature",
    "top_p",
    "chunk_length",
    "normalize",
    "language",
    "prosody",
    "max_new_tokens",
    "repetition_penalty",
    "min_chunk_length",
    "condition_on_previous_chunks",
}


class FishError(RuntimeError):
    def __init__(self, message: str, *, response: HttpResponse | None = None) -> None:
        super().__init__(message)
        self.response = response
        self.ambiguous = bool(response and response.ambiguous)


class FishClient:
    def __init__(self, cfg: Config, *, transport: Transport | None = None) -> None:
        self.cfg = cfg
        self.transport = transport or Transport(timeout_s=180.0, max_retries=3)

    # ---------------------------------------------------------------- info
    @property
    def endpoint(self) -> str:
        return f"{self.cfg.fish_base_url}/v1/tts"

    @property
    def models_endpoint(self) -> str:
        return f"{self.cfg.fish_base_url}/model"

    def _auth_headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.cfg.env.secret('FISH')}"}

    def redacted_headers(self, tts_model: str) -> dict[str, str]:
        return {
            "Authorization": "Bearer ***REDACTED***",
            "Content-Type": "application/json",
            "model": tts_model,
        }

    # -------------------------------------------------------------- request
    def build_request(self, spec: Any) -> tuple[dict[str, str], dict[str, Any]]:
        audio = spec.audio
        unknown = set(audio) - ALLOWED_TTS_PARAMS
        if unknown:
            raise ValueError(
                f"unknown audio parameter(s) {sorted(unknown)}; allowed: {sorted(ALLOWED_TTS_PARAMS)}"
            )
        body: dict[str, Any] = {"text": spec.prompt}
        for key, value in audio.items():
            if value is not None and key not in ("format", "tts_model"):
                body[key] = value
        output_format = str(audio.get("format", "wav"))
        if output_format not in OUTPUT_FORMATS:
            raise ValueError(f"audio.format must be one of {OUTPUT_FORMATS}, got {output_format!r}")
        body["format"] = output_format
        tts_model = str(audio.get("tts_model") or spec.model or DEFAULT_TTS_MODEL)
        if tts_model not in TTS_MODELS:
            # The API falls back silently; we surface it instead of guessing.
            raise ValueError(f"audio.tts_model must be one of {TTS_MODELS}, got {tts_model!r}")
        headers = self.redacted_headers(tts_model)
        return headers, body

    def estimate(self, spec: Any) -> CostEstimate:
        return self.cfg.fish_estimate(spec.prompt)

    # -------------------------------------------------------------- dry run
    def preview(self, spec: Any, *, job_id: str = "", persist: bool = False) -> dict[str, Any]:
        headers, body = self.build_request(spec)
        estimate = self.estimate(spec)
        ffmpeg = find_ffmpeg()
        warnings: list[str] = []
        if ffmpeg is None:
            warnings.append(
                "ffmpeg not found: output stays WAV; Minecraft needs Ogg Vorbis, so audio "
                "cannot be shipped until ffmpeg is installed"
            )
        if "reference_id" not in body and "references" not in body:
            warnings.append(
                "no reference_id: the API will use its default voice (UNVERIFIED behaviour); "
                "run `fish voices` to list available voice model ids"
            )
        if estimate.usd is None:
            warnings.append(
                "Fish TTS pricing is UNVERIFIED: ASSET_FISH_USD_PER_1K_CHARS is unset, so a "
                "live call would be refused by the budget guard"
            )
        payload: dict[str, Any] = {
            "service": "fish",
            "kind": spec.kind,
            "job_id": job_id or spec.job_id,
            "dry_run": True,
            "http_sent": False,
            "method": "POST",
            "endpoint": self.endpoint,
            "headers": headers,
            "request": body,
            "estimate": estimate.as_dict(),
            "gate": {
                "mode": self.cfg.mode,
                "spend_locked": self.cfg.spend_locked,
                "max_cost_usd": self.cfg.max_cost_usd,
            },
            "output": {
                "raw_dir": str(self.cfg.raw_dir / "fish"),
                "requested_format": body.get("format"),
                "game_format": "ogg/vorbis",
                "ffmpeg": ffmpeg,
                "ffmpeg_version": ffmpeg_version() if ffmpeg else None,
            },
            "warnings": warnings,
            "key": {"value": mask(self.cfg.env.values.get("FISH")), "source": str(self.cfg.env.dotenv_path)},
        }
        if persist:
            payload["preview_saved_to"] = str(
                save_dry_run_preview(self.cfg.state_dir, f"fish_{job_id or 'job'}", payload)
            )
        return payload

    # ------------------------------------------------------------- metadata
    def list_models(self, *, page_size: int = 20, page_number: int = 1) -> list[dict[str, Any]]:
        """Non-billable voice model listing (``GET /model``)."""
        url = f"{self.models_endpoint}?page_size={int(page_size)}&page_number={int(page_number)}"
        response = self.transport.request("GET", url, headers=self._auth_headers(), retry=True)
        if not response.ok:
            raise FishError(f"GET /model failed: HTTP {response.status} {response.text(300)}", response=response)
        payload = response.json()
        items = payload.get("items") if isinstance(payload, dict) else payload
        out: list[dict[str, Any]] = []
        for item in items or []:
            if not isinstance(item, dict):
                continue
            out.append(
                {
                    "id": item.get("_id"),
                    "title": item.get("title"),
                    "state": item.get("state"),
                    "train_mode": item.get("train_mode"),
                    "languages": item.get("languages"),
                    "tags": item.get("tags"),
                    "tasks": item.get("tasks"),
                }
            )
        return out

    # ---------------------------------------------------------------- live
    def synthesize(
        self,
        spec: Any,
        *,
        job_id: str,
        ledger: Any | None = None,
        convert: bool = True,
    ) -> dict[str, Any]:
        if self.cfg.spend_locked:
            raise SpendLocked(f"refusing to call Fish TTS while spend is locked ({self.cfg.mode})")
        headers, body = self.build_request(spec)
        estimate = self.estimate(spec)

        reservation_id: int | None = None
        if ledger is not None:
            reservation_id = ledger.check_and_reserve(
                job_id=job_id,
                service="fish",
                model=body.get("reference_id") or DEFAULT_TTS_MODEL,
                kind="tts",
                estimate=estimate,
                max_cost_usd=self.cfg.max_cost_usd,
                service_budget_usd=self.cfg.service_budget_usd.get("fish"),
                units=len(body.get("text", "")),
                unit="chars",
            )
        live_headers = {
            **self._auth_headers(),
            "Content-Type": "application/json",
            "model": headers["model"],
        }
        try:
            response = self.transport.request(
                "POST", self.endpoint, headers=live_headers, json_body=body, retry=False, billable=True
            )
            if not response.ok:
                raise FishError(
                    f"POST /v1/tts failed: HTTP {response.status} {response.text(800)}",
                    response=response,
                )
            raw, extension = self._extract_audio(response, body.get("format", "wav"))
            digest = hashlib.sha256(raw).hexdigest()
            raw_dir = self.cfg.raw_dir / "fish"
            raw_dir.mkdir(parents=True, exist_ok=True)
            raw_path = raw_dir / f"{digest}.{extension}"
            if not raw_path.exists():
                raw_path.write_bytes(raw)
            result: dict[str, Any] = {
                "service": "fish",
                "kind": spec.kind,
                "job_id": job_id,
                "dry_run": False,
                "http_sent": True,
                "endpoint": self.endpoint,
                "http": response.summary(),
                "request": {k: v for k, v in body.items() if k != "text"} | {"text_chars": len(body.get("text", ""))},
                "raw_artifact": {
                    "path": str(raw_path),
                    "sha256": digest,
                    "bytes": len(raw),
                    "format": extension,
                },
                "tts_model": live_headers.get("model"),
                "estimate": estimate.as_dict(),
            }
            from .validate import validate_audio  # local import

            result["raw_validation"] = validate_audio(raw_path).as_dict()
            if convert:
                result["conversion"] = self._convert_for_game(raw_path, digest, spec)
            if ledger is not None and reservation_id is not None:
                ledger.commit(reservation_id, actual_usd=estimate.usd, usage={"chars": len(body.get("text", ""))})
            return result
        except Exception as exc:
            if ledger is not None and reservation_id is not None:
                if not getattr(exc, "ambiguous", False):
                    ledger.release(reservation_id, f"tts failed: {type(exc).__name__}")
            raise

    # -------------------------------------------------------------- output
    def _extract_audio(self, response: HttpResponse, requested_format: str) -> tuple[bytes, str]:
        content_type = response.content_type.lower()
        raw = response.body
        if not raw:
            raise FishError("Fish returned an empty body", response=response)
        if content_type.startswith("audio") or _looks_like_audio(raw):
            return raw, _extension_for(requested_format, raw)
        # Some deployments wrap the audio in JSON: accept base64 fields defensively.
        try:
            payload = json.loads(raw.decode("utf-8", "replace"))
        except json.JSONDecodeError as exc:
            raise FishError(
                f"unexpected Fish response (content-type {content_type!r})", response=response
            ) from exc
        for key in ("audio", "audio_base64", "b64_json", "data"):
            value = payload.get(key) if isinstance(payload, dict) else None
            if isinstance(value, str) and value:
                try:
                    return base64.b64decode(value), _extension_for(requested_format, b"")
                except Exception as exc:  # noqa: BLE001
                    raise FishError(f"could not decode {key} from Fish response", response=response) from exc
        raise FishError(f"Fish response contained no audio: keys={sorted(payload) if isinstance(payload, dict) else '?'}", response=response)

    def _convert_for_game(self, raw_path: Path, digest: str, spec: Any) -> dict[str, Any]:
        processed_dir = self.cfg.processed_dir / "audio"
        processed_dir.mkdir(parents=True, exist_ok=True)
        target = processed_dir / f"{digest}.ogg"
        converted, message = convert_to_ogg_vorbis_or_none(raw_path, target)
        info: dict[str, Any] = {"status": message, "game_path": str(converted) if converted else None}
        if converted is None:
            info["fallback"] = (
                "kept WAV at the raw path; install ffmpeg (`winget install Gyan.FFmpeg`) and "
                "re-run `python -m asset_pipeline convert-audio <wav>` to produce Ogg Vorbis"
            )
            return info
        from .validate import validate_audio  # local import

        info["validation"] = validate_audio(converted).as_dict()
        info["bytes"] = converted.stat().st_size
        return info


def _looks_like_audio(raw: bytes) -> bool:
    head = raw[:12]
    return (
        head[:4] == b"OggS"
        or (head[:4] == b"RIFF" and head[8:12] == b"WAVE")
        or head[:3] == b"ID3"
        or (len(head) > 1 and head[0] == 0xFF and (head[1] & 0xE0) == 0xE0)
        or head[:4] == b"fLaC"
    )


def _extension_for(requested_format: str, raw: bytes) -> str:
    if raw[:4] == b"OggS":
        return "opus" if b"OpusHead" in raw[:256] else "ogg"
    if raw[:4] == b"RIFF" and raw[8:12] == b"WAVE":
        return "wav"
    if raw[:3] == b"ID3" or (len(raw) > 1 and raw[0] == 0xFF and (raw[1] & 0xE0) == 0xE0):
        return "mp3"
    return {"wav": "wav", "mp3": "mp3", "opus": "opus", "pcm": "pcm"}.get(requested_format, "bin")

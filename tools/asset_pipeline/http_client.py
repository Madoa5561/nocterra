"""Minimal stdlib HTTP client with auditing and secret redaction.

Design notes
------------
* No third-party dependency (``urllib`` only).
* **Billable POSTs are never retried automatically.** A 5xx after the request body
  was transmitted is *ambiguous*: the provider may have generated (and charged for)
  the asset. Retrying could double-charge, so we surface ``ambiguous=True`` and let a
  human decide. Only ``429`` (explicitly "not processed") and pre-send connection
  failures are retried, and only when ``retry=True``.
* Every outbound request is appended to an audit log (method, host, path, status,
  bytes). Headers are redacted, bodies are never logged. In DRY RUN nothing is
  written, which is how the self-test *proves* no HTTP was sent.
"""

from __future__ import annotations

import json
import os
import socket
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Mapping

__all__ = [
    "HttpResponse",
    "HttpTransportError",
    "Transport",
    "redact_headers",
    "set_audit_log",
]

USER_AGENT = "nocterra-asset-pipeline/0.1 (+https://github.com/; python-urllib)"

_AUDIT_PATH: Path | None = None

_SECRET_HEADERS = {"authorization", "x-api-key", "api-key", "cookie", "proxy-authorization"}


def set_audit_log(path: str | Path | None) -> None:
    """Point the audit log at *path* (or disable with ``None``)."""
    global _AUDIT_PATH
    _AUDIT_PATH = Path(path) if path else None
    if _AUDIT_PATH:
        _AUDIT_PATH.parent.mkdir(parents=True, exist_ok=True)


def redact_headers(headers: Mapping[str, str] | None) -> dict[str, str]:
    out: dict[str, str] = {}
    for key, value in (headers or {}).items():
        if key.lower() in _SECRET_HEADERS:
            out[key] = "Bearer ***REDACTED***" if key.lower() == "authorization" else "***REDACTED***"
        else:
            out[key] = value
    return out


@dataclass
class HttpResponse:
    status: int
    headers: dict[str, str]
    body: bytes
    url: str
    elapsed_s: float
    method: str
    ambiguous: bool = False
    error: str | None = None

    @property
    def ok(self) -> bool:
        return 200 <= self.status < 300

    @property
    def content_type(self) -> str:
        return self.headers.get("content-type", "")

    def text(self, limit: int = 4000) -> str:
        try:
            value = self.body.decode("utf-8", "replace")
        except Exception:  # noqa: BLE001 - defensive
            return "<binary>"
        return value if len(value) <= limit else value[:limit] + "...[truncated]"

    def json(self) -> Any:
        return json.loads(self.body.decode("utf-8", "replace"))

    def summary(self) -> dict[str, Any]:
        return {
            "method": self.method,
            "url": self.url,
            "status": self.status,
            "elapsed_s": round(self.elapsed_s, 3),
            "content_type": self.content_type,
            "bytes": len(self.body),
            "ambiguous_billing": self.ambiguous,
            "error": self.error,
        }


class HttpTransportError(RuntimeError):
    """Raised for transport-level failures (DNS, TLS, timeout, reset)."""


class Transport:
    """Thin wrapper around :mod:`urllib.request`."""

    def __init__(
        self,
        *,
        timeout_s: float = 120.0,
        max_retries: int = 3,
        user_agent: str = USER_AGENT,
    ) -> None:
        self.timeout_s = timeout_s
        self.max_retries = max_retries
        self.user_agent = user_agent
        self._ssl_context = ssl.create_default_context()

    # ------------------------------------------------------------------ audit
    def _audit(self, record: dict[str, Any]) -> None:
        if _AUDIT_PATH is None:
            return
        try:
            with _AUDIT_PATH.open("a", encoding="utf-8") as handle:
                handle.write(json.dumps(record, ensure_ascii=False) + "\n")
        except OSError:
            pass  # auditing must never break a request

    # ---------------------------------------------------------------- request
    def request(
        self,
        method: str,
        url: str,
        *,
        headers: Mapping[str, str] | None = None,
        json_body: Mapping[str, Any] | list[Any] | None = None,
        data: bytes | None = None,
        timeout_s: float | None = None,
        retry: bool = False,
        billable: bool = False,
    ) -> HttpResponse:
        """Perform one HTTP request.

        ``billable=True`` disables retries after the body has been transmitted.
        """
        send_headers = {"User-Agent": self.user_agent, "Accept": "*/*"}
        send_headers.update({k: v for k, v in (headers or {}).items()})
        body = data
        if json_body is not None:
            body = json.dumps(json_body, ensure_ascii=False).encode("utf-8")
            send_headers.setdefault("Content-Type", "application/json")

        attempts = self.max_retries if retry else 1
        if billable:
            attempts = 1  # never replay a potentially charged request
        last_error: str | None = None
        for attempt in range(1, attempts + 1):
            request = urllib.request.Request(url, data=body, method=method.upper())
            for key, value in send_headers.items():
                request.add_header(key, value)
            started = time.monotonic()
            parse = urllib.parse.urlsplit(url)
            audit_base = {
                "ts": time.strftime("%Y-%m-%dT%H:%M:%S%z", time.localtime()),
                "method": method.upper(),
                "host": parse.netloc,
                "path": parse.path,
                "billable": billable,
                "attempt": attempt,
            }
            try:
                with urllib.request.urlopen(
                    request, timeout=timeout_s or self.timeout_s, context=self._ssl_context
                ) as response:
                    payload = response.read()
                    result = HttpResponse(
                        status=response.status,
                        headers={k.lower(): v for k, v in response.headers.items()},
                        body=payload,
                        url=url,
                        elapsed_s=time.monotonic() - started,
                        method=method.upper(),
                    )
                    self._audit({**audit_base, "status": result.status, "bytes": len(payload)})
                    return result
            except urllib.error.HTTPError as exc:
                payload = exc.read() if exc.fp else b""
                result = HttpResponse(
                    status=exc.code,
                    headers={k.lower(): v for k, v in (exc.headers or {}).items()},
                    body=payload,
                    url=url,
                    elapsed_s=time.monotonic() - started,
                    method=method.upper(),
                    # HTTPError means the server answered; 5xx/429 may or may not
                    # have been processed for a billable POST.
                    ambiguous=billable and exc.code >= 500,
                    error=f"HTTP {exc.code}",
                )
                self._audit(
                    {**audit_base, "status": exc.code, "bytes": len(payload), "error": True}
                )
                if exc.code == 429 and attempt < attempts and retry:
                    retry_after = self._retry_after(result.headers)
                    time.sleep(min(retry_after, 60.0))
                    continue
                if 500 <= exc.code < 600 and attempt < attempts and retry:
                    time.sleep(min(2.0 * attempt, 30.0))
                    continue
                return result
            except (urllib.error.URLError, socket.timeout, ssl.SSLError, TimeoutError, OSError) as exc:
                last_error = f"{type(exc).__name__}: {exc}"
                self._audit({**audit_base, "status": None, "error": last_error, "sent": True})
                if attempt < attempts and retry:
                    time.sleep(min(2.0 * attempt, 30.0))
                    continue
                raise HttpTransportError(
                    f"{method.upper()} {url} failed before a usable response: {last_error}"
                ) from exc
        raise HttpTransportError(f"{method.upper()} {url} failed: {last_error}")

    @staticmethod
    def _retry_after(headers: Mapping[str, str]) -> float:
        raw = headers.get("retry-after")
        if not raw:
            return 5.0
        try:
            return max(0.0, float(raw))
        except ValueError:
            return 5.0

    # --------------------------------------------------------------- download
    def download(
        self,
        url: str,
        destination: str | Path,
        *,
        max_bytes: int = 256 * 1024 * 1024,
        timeout_s: float = 120.0,
    ) -> Path:
        """Download *url* straight to *destination* (signed URLs expire quickly)."""
        dest = Path(destination)
        dest.parent.mkdir(parents=True, exist_ok=True)
        parse = urllib.parse.urlsplit(url)
        request = urllib.request.Request(url, method="GET")
        request.add_header("User-Agent", self.user_agent)
        started = time.monotonic()
        try:
            with urllib.request.urlopen(
                request, timeout=timeout_s, context=self._ssl_context
            ) as response, dest.open("wb") as handle:
                written = 0
                while True:
                    chunk = response.read(1024 * 256)
                    if not chunk:
                        break
                    written += len(chunk)
                    if written > max_bytes:
                        raise HttpTransportError(
                            f"download exceeded {max_bytes} bytes; aborting {parse.path}"
                        )
                    handle.write(chunk)
            self._audit(
                {
                    "ts": time.strftime("%Y-%m-%dT%H:%M:%S%z", time.localtime()),
                    "method": "GET",
                    "host": parse.netloc,
                    "path": parse.path,
                    "billable": False,
                    "status": 200,
                    "bytes": dest.stat().st_size,
                    "elapsed_s": round(time.monotonic() - started, 3),
                }
            )
            return dest
        except urllib.error.HTTPError as exc:
            self._audit(
                {
                    "ts": time.strftime("%Y-%m-%dT%H:%M:%S%z", time.localtime()),
                    "method": "GET",
                    "host": parse.netloc,
                    "path": parse.path,
                    "billable": False,
                    "status": exc.code,
                    "error": True,
                }
            )
            raise HttpTransportError(
                f"download {url} failed with HTTP {exc.code}"
            ) from exc
        except (urllib.error.URLError, OSError, TimeoutError) as exc:
            raise HttpTransportError(f"download {url} failed: {exc}") from exc


def probe_route_available(transport: Transport, url: str, headers: Mapping[str, str]) -> dict[str, Any]:
    """Non-billable route check: a POST-only endpoint answers GET with 405."""
    response = transport.request("GET", url, headers=headers, retry=False)
    return {
        "url": url,
        "status": response.status,
        "exists": response.status in (405, 401, 403, 422),
        "detail": response.text(300),
    }

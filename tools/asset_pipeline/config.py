"""Configuration, safety gates, budgets, pricing and paths.

Everything financial is decided here, before any HTTP call:

===========================================  =========================================
Guard                                        Behaviour
===========================================  =========================================
``ASSET_DRY_RUN``             (default True) No billable request is ever sent while true.
``ASSET_AUTO_SPEND``          (default False) Second, independent lock. Real spend needs
                                              ``DRY_RUN=false`` AND ``AUTO_SPEND=true``.
``ASSET_MAX_COST_USD``        (default 0)    Hard ceiling. A request is refused *before*
                                              it is sent when committed + estimate > cap.
``ASSET_BUDGET_<SERVICE>_USD`` (optional)    Per-service sub-ceiling.
unknown price for a model                    Real spend is refused (cannot prove budget).
===========================================  =========================================
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Mapping

from . import env as envmod

# --------------------------------------------------------------------------
# Defaults. All are overridable from .env / process env.
# --------------------------------------------------------------------------
DEFAULT_DRY_RUN = True          # safe default
DEFAULT_AUTO_SPEND = False      # safe default
DEFAULT_MAX_COST_USD = 0.0      # safe default: nothing may be spent
DEFAULT_MAX_TRIPO_TRIANGLES = 200_000

#: Default state directory: ``tools/asset_pipeline/state`` (inside the owned tree,
#: self-ignoring via its own .gitignore). Override with ``ASSET_STATE_DIR``.
DEFAULT_STATE_SUBDIR = Path("tools") / "asset_pipeline" / "state"

SERVICES = ("openai", "tripo", "fish")

#: Default rate limits (seconds between two calls per service).
DEFAULT_MIN_INTERVAL_S = {"openai": 5.0, "tripo": 1.0, "fish": 1.0}

#: Estimated USD per generated image for GPT Image models.
#:
#: !! UNVERIFIED for gpt-image-2.5-* !!
#: These are *proxy* figures taken from public gpt-image-1 list prices and are kept
#: conservative. They are configurable (``ASSET_PRICE_JSON``) precisely because the
#: real gpt-image-2.5 price list has NOT been verified against the account. The
#: pipeline refuses real spend when a price is ``None`` and warns when a price is
#: flagged ``unverified``.
_OPENAI_IMAGE_USD_PROXY: dict[str, float] = {
    "low": 0.02,
    "medium": 0.07,
    "high": 0.25,
    "xhigh": 0.40,
    "max": 0.60,
    "auto": 0.07,
}

#: Tripo credit estimates per task type (UNVERIFIED - credit costs are not published
#: in the docs we could fetch; treat as unknown unless set via .env).
DEFAULT_TRIPO_CREDITS: dict[str, float] = {
    "text_to_model": 20.0,
    "image_to_model": 20.0,
    "texture_model": 10.0,
    "convert_model": 5.0,
}

#: Fish Audio pricing: UNVERIFIED (per-character billing not confirmed). A TTS call
#: is therefore "unpriced" and can never be sent while the budget guard is strict.
DEFAULT_FISH_USD_PER_1K_CHARS: float | None = None


class ConfigError(RuntimeError):
    """Invalid or unsafe configuration."""


@dataclass(frozen=True)
class CostEstimate:
    """Cost of one request. ``usd is None`` means "unknown / unpriced"."""

    usd: float | None
    credits: float | None = None
    detail: str = ""
    source: str = "unverified"
    billable: bool = True

    @property
    def priced(self) -> bool:
        return self.usd is not None

    def as_dict(self) -> dict[str, Any]:
        return {
            "usd": self.usd,
            "credits": self.credits,
            "detail": self.detail,
            "source": self.source,
            "priced": self.priced,
            "billable": self.billable,
        }


@dataclass(frozen=True)
class SpendGate:
    """Result of the pre-flight spend check."""

    allowed: bool
    reasons: list[str]
    dry_run: bool
    auto_spend: bool
    max_cost_usd: float
    committed_usd: float = 0.0
    estimate_usd: float | None = None
    service: str | None = None
    service_budget_usd: float | None = None
    service_spent_usd: float | None = None

    def as_dict(self) -> dict[str, Any]:
        return {
            "allowed": self.allowed,
            "reasons": self.reasons,
            "dry_run": self.dry_run,
            "auto_spend": self.auto_spend,
            "max_cost_usd": self.max_cost_usd,
            "committed_usd": round(self.committed_usd, 6),
            "estimate_usd": self.estimate_usd,
            "service": self.service,
            "service_budget_usd": self.service_budget_usd,
            "service_spent_usd": self.service_spent_usd,
        }


@dataclass
class Config:
    repo_root: Path
    env: envmod.Env
    dry_run: bool = DEFAULT_DRY_RUN
    auto_spend: bool = DEFAULT_AUTO_SPEND
    max_cost_usd: float = DEFAULT_MAX_COST_USD
    service_budget_usd: dict[str, float | None] = field(default_factory=dict)
    min_interval_s: dict[str, float] = field(default_factory=dict)
    max_attempts: int = 3
    backoff_base_s: float = 5.0
    backoff_cap_s: float = 900.0
    lease_seconds: float = 900.0
    max_tripo_triangles: int = DEFAULT_MAX_TRIPO_TRIANGLES
    pricing: dict[str, Any] = field(default_factory=dict)
    tripo_credits: dict[str, float] = field(default_factory=lambda: dict(DEFAULT_TRIPO_CREDITS))
    tripo_credit_usd: float | None = None
    fish_usd_per_1k_chars: float | None = None
    openai_base_url: str = "https://api.openai.com/v1"
    tripo_base_url: str = "https://openapi.tripo3d.ai"
    tripo_v2_base_url: str = "https://api.tripo3d.ai/v2/openapi"
    fish_base_url: str = "https://api.fish.audio"
    state_dir: Path = Path()
    raw_dir: Path = Path()
    processed_dir: Path = Path()
    manifests_dir: Path = Path()
    ffmpeg_path: str | None = None
    warnings: list[str] = field(default_factory=list)

    # ------------------------------------------------------------------ load
    @classmethod
    def load(
        cls,
        env: envmod.Env | None = None,
        repo_root: str | None = None,
    ) -> "Config":
        environment = env or envmod.Env(repo_root=repo_root)
        root = environment.repo_root
        cfg = cls(repo_root=root, env=environment)

        cfg.dry_run = environment.get_bool("ASSET_DRY_RUN", DEFAULT_DRY_RUN)
        cfg.auto_spend = environment.get_bool("ASSET_AUTO_SPEND", DEFAULT_AUTO_SPEND)
        cfg.max_cost_usd = float(
            environment.get_float("ASSET_MAX_COST_USD", DEFAULT_MAX_COST_USD) or 0.0
        )

        for service in SERVICES:
            key = f"ASSET_BUDGET_{service.upper()}_USD"
            cfg.service_budget_usd[service] = environment.get_float(key, None)
            interval = environment.get_float(
                f"ASSET_RATE_{service.upper()}_MIN_INTERVAL_S",
                DEFAULT_MIN_INTERVAL_S[service],
            )
            cfg.min_interval_s[service] = max(0.0, float(interval or 0.0))

        cfg.max_attempts = max(1, environment.get_int("ASSET_MAX_ATTEMPTS", 3) or 3)
        cfg.backoff_base_s = float(
            environment.get_float("ASSET_BACKOFF_BASE_S", 5.0) or 5.0
        )
        cfg.backoff_cap_s = float(environment.get_float("ASSET_BACKOFF_CAP_S", 900.0) or 900.0)
        cfg.lease_seconds = float(environment.get_float("ASSET_LEASE_SECONDS", 900.0) or 900.0)
        cfg.max_tripo_triangles = int(
            environment.get_int("ASSET_MAX_TRIPO_TRI", DEFAULT_MAX_TRIPO_TRIANGLES)
            or DEFAULT_MAX_TRIPO_TRIANGLES
        )
        cfg.tripo_credit_usd = environment.get_float("ASSET_TRIPO_CREDIT_USD", None)
        cfg.fish_usd_per_1k_chars = environment.get_float("ASSET_FISH_USD_PER_1K_CHARS", None)

        # Pricing overrides: JSON mapping merged over the built-in proxy table.
        raw_pricing = environment.get("ASSET_PRICE_JSON")
        pricing = {
            "openai_image": dict(_OPENAI_IMAGE_USD_PROXY),
            "source": "unverified proxy (gpt-image-1 public list price)",
        }
        if raw_pricing:
            try:
                override = json.loads(raw_pricing)
            except json.JSONDecodeError as exc:
                raise ConfigError(f"ASSET_PRICE_JSON is not valid JSON: {exc}") from exc
            if not isinstance(override, dict):
                raise ConfigError("ASSET_PRICE_JSON must be a JSON object")
            pricing["openai_image"].update(override)
            pricing["source"] = "ASSET_PRICE_JSON override (operator supplied)"
        cfg.pricing = pricing

        raw_credits = environment.get("ASSET_TRIPO_CREDITS_JSON")
        if raw_credits:
            try:
                parsed = json.loads(raw_credits)
            except json.JSONDecodeError as exc:
                raise ConfigError(f"ASSET_TRIPO_CREDITS_JSON is not valid JSON: {exc}") from exc
            if isinstance(parsed, dict):
                cfg.tripo_credits.update({str(k): float(v) for k, v in parsed.items()})

        cfg.openai_base_url = (
            environment.get("ASSET_OPENAI_BASE_URL", cfg.openai_base_url) or cfg.openai_base_url
        ).rstrip("/")
        cfg.tripo_base_url = (
            environment.get("ASSET_TRIPO_BASE_URL", cfg.tripo_base_url) or cfg.tripo_base_url
        ).rstrip("/")
        cfg.tripo_v2_base_url = (
            environment.get("ASSET_TRIPO_V2_BASE_URL", cfg.tripo_v2_base_url)
            or cfg.tripo_v2_base_url
        ).rstrip("/")
        cfg.fish_base_url = (
            environment.get("ASSET_FISH_BASE_URL", cfg.fish_base_url) or cfg.fish_base_url
        ).rstrip("/")

        state_raw = environment.get("ASSET_STATE_DIR")
        cfg.state_dir = (
            Path(state_raw).expanduser()
            if state_raw
            else (root / DEFAULT_STATE_SUBDIR)
        ).resolve()
        cfg.raw_dir = Path(
            environment.get("ASSET_RAW_DIR", str(root / "generated_assets" / "raw"))
        ).resolve()
        cfg.processed_dir = Path(
            environment.get("ASSET_PROCESSED_DIR", str(root / "generated_assets" / "processed"))
        ).resolve()
        cfg.manifests_dir = Path(
            environment.get("ASSET_MANIFESTS_DIR", str(root / "generated_assets" / "manifests"))
        ).resolve()

        cfg.warnings = cfg._consistency_warnings()
        return cfg

    # ------------------------------------------------------------- helpers
    def _consistency_warnings(self) -> list[str]:
        warnings: list[str] = []
        if not self.dry_run and not self.auto_spend:
            warnings.append(
                "ASSET_DRY_RUN=false but ASSET_AUTO_SPEND=false: real spend stays blocked "
                "(set both to enable billable requests)."
            )
        if self.auto_spend and self.dry_run:
            warnings.append(
                "ASSET_AUTO_SPEND=true is ignored while ASSET_DRY_RUN=true."
            )
        if not self.dry_run and self.auto_spend and self.max_cost_usd <= 0:
            warnings.append(
                "Real spend would be enabled but ASSET_MAX_COST_USD<=0: every request "
                "will be refused by the budget guard."
            )
        if self.tripo_credit_usd is None:
            warnings.append(
                "ASSET_TRIPO_CREDIT_USD is not set: Tripo tasks are unpriced, so real "
                "Tripo spend is refused by the budget guard."
            )
        if self.fish_usd_per_1k_chars is None:
            warnings.append(
                "ASSET_FISH_USD_PER_1K_CHARS is not set: Fish TTS is unpriced, so real "
                "Fish spend is refused by the budget guard."
            )
        return warnings

    @property
    def spend_locked(self) -> bool:
        """True when real (billable) HTTP must not be sent."""
        return self.dry_run or not self.auto_spend

    @property
    def mode(self) -> str:
        if self.dry_run:
            return "DRY-RUN"
        if not self.auto_spend:
            return "LOCKED(auto_spend=false)"
        return "LIVE-SPEND-ENABLED"

    def ensure_state_dirs(self) -> dict[str, Path]:
        """Create the local state directory (queue/ledger DBs). Idempotent."""
        self.state_dir.mkdir(parents=True, exist_ok=True)
        ignore = self.state_dir / ".gitignore"
        if not ignore.exists():
            ignore.write_text(
                "# runtime state of tools/asset_pipeline - never commit\n*\n!.gitignore\n",
                encoding="utf-8",
            )
        return {"state": self.state_dir}

    def ensure_output_dirs(self) -> dict[str, Path]:
        """Create raw/processed/manifest output directories (real-run paths only)."""
        created = self.ensure_state_dirs()
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self.processed_dir.mkdir(parents=True, exist_ok=True)
        self.manifests_dir.mkdir(parents=True, exist_ok=True)
        created.update(
            raw=self.raw_dir, processed=self.processed_dir, manifests=self.manifests_dir
        )
        return created

    # -------------------------------------------------------------- pricing
    def openai_image_estimate(
        self, *, quality: str = "auto", size: str = "1024x1024", n: int = 1
    ) -> CostEstimate:
        table: Mapping[str, float] = self.pricing.get("openai_image", {})
        unit = table.get(quality)
        if unit is None:
            return CostEstimate(
                usd=None,
                detail=f"no price entry for quality={quality!r}",
                source="missing",
            )
        total = round(float(unit) * max(1, int(n)), 6)
        return CostEstimate(
            usd=total,
            detail=f"{n} x ${unit:.3f} @ quality={quality}, size={size}",
            source=str(self.pricing.get("source", "unverified")),
        )

    def tripo_estimate(self, task_type: str) -> CostEstimate:
        credits = self.tripo_credits.get(task_type)
        if credits is None:
            return CostEstimate(usd=None, credits=None, detail=f"no credit estimate for {task_type!r}")
        usd = None if self.tripo_credit_usd is None else round(credits * self.tripo_credit_usd, 6)
        return CostEstimate(
            usd=usd,
            credits=credits,
            detail=f"{credits:g} credits @ ${self.tripo_credit_usd or 0:.4f}/credit",
            source="unverified" if self.tripo_credit_usd is None else "operator-supplied",
        )

    def fish_estimate(self, text: str) -> CostEstimate:
        chars = len(text or "")
        if self.fish_usd_per_1k_chars is None:
            return CostEstimate(
                usd=None,
                detail=f"{chars} chars (no price configured)",
                source="missing",
            )
        usd = round(chars / 1000.0 * self.fish_usd_per_1k_chars, 6)
        return CostEstimate(
            usd=usd,
            detail=f"{chars} chars @ ${self.fish_usd_per_1k_chars:.4f}/1k chars",
            source="operator-supplied",
        )

    # ------------------------------------------------------------ diagnostics
    def describe(self) -> dict[str, Any]:
        return {
            "mode": self.mode,
            "dry_run": self.dry_run,
            "auto_spend": self.auto_spend,
            "spend_locked": self.spend_locked,
            "max_cost_usd": self.max_cost_usd,
            "service_budget_usd": self.service_budget_usd,
            "min_interval_s": self.min_interval_s,
            "max_attempts": self.max_attempts,
            "lease_seconds": self.lease_seconds,
            "max_tripo_triangles": self.max_tripo_triangles,
            "pricing_source": self.pricing.get("source"),
            "tripo_credit_usd": self.tripo_credit_usd,
            "fish_usd_per_1k_chars": self.fish_usd_per_1k_chars,
            "endpoints": {
                "openai": self.openai_base_url,
                "tripo": self.tripo_base_url,
                "tripo_v2": self.tripo_v2_base_url,
                "fish": self.fish_base_url,
            },
            "paths": {
                "state_dir": str(self.state_dir),
                "raw_dir": str(self.raw_dir),
                "processed_dir": str(self.processed_dir),
                "manifests_dir": str(self.manifests_dir),
            },
            "ffmpeg": self.ffmpeg_path,
            "warnings": self.warnings,
        }

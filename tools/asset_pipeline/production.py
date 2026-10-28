"""Production run: actually generate the NOCTERRA assets that were approved for spend.

Groups / priorities (see ``docs/ASSET_PIPELINE.md``):
  1. ``hud``      - 12 boss portraits (128x128), boss frame (512x128, 9-slice), bar fill (256x32)
  2. ``textures`` - 16x16 block textures + item icons
  3. ``audio``    - 12 bosses x 2 Japanese voice lines -> Ogg Vorbis
  4. ``tripo``    - full-body concept -> image_to_model GLB -> voxel/GeckoLib attempt

Every paid call goes through :class:`OpenAIImageClient` / :class:`FishClient` /
:class:`TripoClient`, which reserve budget in the ledger *before* the HTTP request and
commit/release afterwards. This module additionally checks a spend ceiling between
assets so a long batch stops before it can overshoot.

Licensing: every manifest entry carries an explicit UNVERIFIED licence note; the
provider terms have NOT been verified and must be confirmed before public release.
"""

from __future__ import annotations

import shutil
import time
from pathlib import Path
from typing import Any, Callable, Iterable

import numpy as np
from PIL import Image

from .config import Config
from .fish import FishClient
from .jobs import JobSpec
from .ledger import BudgetExceeded, Ledger
from .manifest import AssetManifest, sha256_file
from .openai_image import OpenAIImageClient, OpenAIImageError, build_request
from .queue import JobQueue
from .texture import (
    apply_alpha_mode,
    ensure_multiple_of_16,
    fit_square,
    make_center_tileable,
    nine_slice_report,
    open_rgba,
    quantize_palette,
    remove_background,
    resize_to,
    tile_seam_correct,
    tileability,
    trim_alpha,
)
from .tripo import TripoClient, TripoError
from .util import atomic_write_json, human_usd
from .validate import validate_audio, validate_glb, validate_png
from .voxelize import VoxelizeError, glb_to_geckolib, validate_geckolib

LICENSE_OPENAI = (
    "AI-generated with OpenAI Images for NOCTERRA. Provider terms / commercial-use "
    "rights are UNVERIFIED by this pipeline - confirm before public release."
)
LICENSE_TRIPO = (
    "AI-generated with Tripo3D for NOCTERRA. Provider terms / commercial-use rights are "
    "UNVERIFIED by this pipeline - confirm before public release."
)
LICENSE_FISH = (
    "AI-generated with Fish Audio (default voice, no third-party clone). Provider terms / "
    "commercial-use rights are UNVERIFIED - confirm before public release."
)
PRICE_NOTE = (
    "API returns no itemised price; amount computed from the operator-supplied "
    "ASSET_PRICE_JSON / credit / character price table (not invoice-verified)."
)

# --------------------------------------------------------------------------
# content definitions
# --------------------------------------------------------------------------
PORTRAIT_STYLE = (
    "Dark fantasy boss portrait, bust shot from the chest up, centered, facing the viewer, "
    "symmetric framing, painterly digital illustration, dramatic violet rim light, ash and "
    "obsidian colour palette, highly detailed, isolated on a fully transparent background "
    "(alpha channel), no background scene, no text, no border, no frame, square composition"
)

BOSSES: list[dict[str, Any]] = [
    {
        "id": "rotwood_king",
        "name": "腐森の王",
        "subject": "an ancient rotting forest king, bark-plated skull-like face, antler crown of "
        "blackened oak, moss and pale fungi growing from the shoulders, hollow amber eyes, "
        "tattered dark green cloak",
    },
    {
        "id": "sorrow_saint",
        "name": "悲嘆の聖者",
        "subject": "a weeping saint wearing a cracked porcelain mask, tears of black ichor, "
        "hooded ash-white robes stained grey, broken glass halo, expression of endless sorrow",
    },
    {
        "id": "condemned_knight_lord",
        "name": "断罪の騎士長",
        "subject": "a condemned knight lord, rusted black plate armour bound in violet rune "
        "chains, horned great helm with a shattered visor, tattered crimson tabard, "
        "executioner aura",
    },
    {
        "id": "sandglass_twin_kings",
        "name": "砂時計の双王",
        "subject": "twin kings fused at the shoulders, one young and one withered, hourglass "
        "crowns filled with falling sand, cracked gilded armour, violet dust drifting",
    },
    {
        "id": "abyssal_sea_emperor",
        "name": "深淵海の皇帝",
        "subject": "an abyssal sea emperor, pressure armour of barnacled black coral, "
        "bioluminescent violet eyes, crown of drowned shells, shadowy tendrils rising behind "
        "the shoulders",
    },
    {
        "id": "astel_fallen_star_dragon",
        "name": "堕星竜アステル",
        "subject": "a fallen star dragon in humanoid form, cracked obsidian scales with molten "
        "violet seams, draconic horned head, broken halo of cold starlight, cosmic ash",
    },
    {
        "id": "silver_executioner",
        "name": "白銀の処刑人",
        "subject": "a silver executioner, mirror-polished mask with no eyes, heavy white-grey "
        "plate armour, silver chains over the shoulders, cold violet glow, immense cleaver "
        "silhouette behind",
    },
    {
        "id": "molten_steel_colossus",
        "name": "溶鋼の巨像",
        "subject": "a molten steel colossus, riveted dark iron plates, glowing orange-white "
        "seams and furnace vents, grill-like face, ember plumes and smoke, massive shoulders",
    },
    {
        "id": "black_crowned_king",
        "name": "黒冠の王",
        "subject": "a black-crowned king, tall iron crown set with black gems, pale gaunt face, "
        "void-black regal robes with violet lining, rings of dark authority, cold commanding stare",
    },
    {
        "id": "ember_incarnation",
        "name": "残火の化身",
        "subject": "an incarnation of embers, humanoid form of cooling slag and living charcoal, "
        "cracks of orange and violet fire, crown of embers, halo of drifting ash",
    },
    {
        "id": "forgotten_knight",
        "name": "忘れられた騎士",
        "subject": "a forgotten knight, empty great helm with a faint violet light inside, "
        "moss-covered grey armour, broken sword resting at the shoulder, dust and cobwebs",
    },
    {
        "id": "nameless_observer",
        "name": "名無しの観測者",
        "subject": "a nameless observer, faceless hooded figure with a smooth dark mask, many "
        "small violet eyes floating around the head, ornate tattered robes, ink-black tendrils",
    },
]

#: (boss_id, 1=roar/battle-cry, 2=spoken line); all 15-40 Japanese characters, original text.
VOICE_LINES: dict[str, dict[int, str]] = {
    "rotwood_king": {1: "我が根は、お前の骨まで喰らい尽くす", 2: "この森に足を踏み入れたことを、永遠に悔やむがいい"},
    "sorrow_saint": {1: "涙は尽きた。あとはお前の血だけだ", 2: "救いを乞う声も、ここでは祈りに届かぬ"},
    "condemned_knight_lord": {1: "我が断罪の刃、その身で受けてみよ", 2: "騎士の誓いは破られた。ならば罰だけが残る"},
    "sandglass_twin_kings": {1: "時は既に、お前の側で尽きている", 2: "双子の王が紡ぐ砂は、二度と戻らぬ"},
    "abyssal_sea_emperor": {1: "深淵の底まで、その魂ごと沈めてやろう", 2: "海はすべてを呑み、沈黙だけを返す"},
    "astel_fallen_star_dragon": {1: "堕ちた星の業火、その身で味わうがいい", 2: "空を失った竜の怒りは、地をも焼く"},
    "silver_executioner": {1: "裁きは下った。静かに首を差し出せ", 2: "白銀の刃に、一片の情けもありはしない"},
    "molten_steel_colossus": {1: "熔けた鋼が、その身ごと呑み込む", 2: "我が装甲は熱く、心はもっと熱い"},
    "black_crowned_king": {1: "王冠は黒い。ゆえに王は絶えることがない", 2: "跪け。ここは我が玉座の間である"},
    "ember_incarnation": {1: "残り火が、お前を巻いて最後の焔となる", 2: "灰になっても、この火は決して消えぬ"},
    "forgotten_knight": {1: "誰も憶えておらぬ。だからこそ斬る", 2: "名を忘れられても、この剣は忘れぬ"},
    "nameless_observer": {1: "観測している。お前の終わりを、ずっと", 2: "名もなき者が、すべてを見届ける"},
}

#: ``tileable`` blocks get seam correction + tiling verification; item icons are sprites.
TEXTURES: list[dict[str, Any]] = [
    {
        "name": "gate_frame",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of an ancient gate frame made of blackened "
        "stone with obsidian inlay and faint glowing violet runes, dark fantasy Minecraft block, "
        "flat top-down orthographic view, no border, no text, no lighting gradient",
    },
    {
        "name": "ashen_portal",
        "category": "block",
        "size": 32,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of a swirling ashen portal surface, pale "
        "violet and grey smoke vortex with drifting embers and a dark centre, flat orthographic "
        "top-down view, dark fantasy Minecraft block, no text",
    },
    {
        "name": "ashstone",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of ashen grey volcanic stone with fine "
        "cracks and pale ash dust on the surface, dark fantasy Minecraft block, flat orthographic "
        "top-down view, no text, no lighting gradient",
    },
    {
        "name": "ash_coal_ore",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of dark grey ash stone with embedded "
        "glowing ember coal veins in orange and violet, dark fantasy Minecraft ore block, flat "
        "orthographic top-down view, no text",
    },
    {
        "name": "ashwood_log_side",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of charred ashwood tree bark, vertical dark "
        "grey-brown ridges with faint ember cracks, Minecraft log side texture, flat orthographic "
        "view, no text",
    },
    {
        "name": "ashwood_log_top",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of an ashwood log top: concentric dark "
        "growth rings with a pale ash centre and charred outer edge, Minecraft log top texture, "
        "flat orthographic view, no text",
    },
    {
        "name": "ashwood_planks",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of charred ashwood planks, clearly visible "
        "horizontal plank seams and warm dark brown wood grain, mid-tone contrast, a few pale ash "
        "streaks across the boards, Minecraft planks texture, flat orthographic view, no text",
    },
    {
        "name": "rotwood_altar",
        "category": "block",
        "size": 16,
        "tileable": True,
        "prompt": "Seamless tileable pixel-art texture of a rotwood altar top: dark rotten wood "
        "with moss, carved violet ritual sigils and a faint ember glow in the centre, dark "
        "fantasy Minecraft block, flat orthographic top-down view, no text",
    },
    {
        "name": "ember_shard",
        "category": "item",
        "size": 16,
        "tileable": False,
        "prompt": "Pixel-art item icon of a single glowing ember shard, faceted crimson-violet "
        "crystal with tiny sparks, centred on a fully transparent background, Minecraft item "
        "sprite, flat lighting, no text, no border",
    },
    {
        "name": "ashwood_blade",
        "category": "item",
        "size": 16,
        "tileable": False,
        "prompt": "Pixel-art item icon of an ashwood blade sword, charred wooden grip, dark steel "
        "blade with a violet glowing edge, diagonal composition, centred on a fully transparent "
        "background, Minecraft item sprite, no text, no border",
    },
    {
        "name": "grave_greatsword",
        "category": "item",
        "size": 16,
        "tileable": False,
        "prompt": "Pixel-art item icon of a massive grave greatsword, bright worn silver blade with "
        "a dark cutting edge, gravestone-shaped stone crossguard, violet rune inlay on the guard, "
        "strong high-contrast silhouette readable at 16x16, centred on a fully transparent "
        "background, Minecraft item sprite, no text, no border",
    },
    {
        "name": "mourning_scythe",
        "category": "item",
        "size": 16,
        "tileable": False,
        "prompt": "Pixel-art item icon of a mourning scythe, blackened curved blade with a bright "
        "silver cutting edge, pale bone handle, violet spectral mist, strong high-contrast "
        "silhouette readable at 16x16, centred on a fully transparent background, Minecraft item "
        "sprite, no text, no border",
    },
]

#: 1-2 Tripo models (priority last; failure is acceptable and will be reported as such).
TRIPO_BOSSES = ["rotwood_king", "black_crowned_king"]

BAR_FILL_LEFT = (176, 64, 255)
BAR_FILL_RIGHT = (42, 8, 70)


# --------------------------------------------------------------------------
# studio
# --------------------------------------------------------------------------
class Studio:
    def __init__(
        self,
        cfg: Config,
        ledger: Ledger,
        queue: JobQueue,
        *,
        force: bool = False,
        only: Iterable[str] | None = None,
        on_event: Callable[[str], None] | None = None,
    ) -> None:
        self.cfg = cfg
        self.ledger = ledger
        self.queue = queue
        self.force = force
        self.only = {item.strip() for item in (only or []) if item.strip()}
        self.on_event = on_event or (lambda message: print(message, flush=True))
        cfg.ensure_output_dirs()
        self.manifest = AssetManifest(cfg.manifests_dir)
        self.openai = OpenAIImageClient(cfg)
        self.fish = FishClient(cfg)
        self.tripo = TripoClient(cfg)
        self.report: dict[str, Any] = {
            "started_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
            "mode": cfg.mode,
            "images": [],
            "audio": [],
            "models": [],
            "skipped": [],
            "errors": [],
            "missing": [],
        }
        self._seq = 0

    # ------------------------------------------------------------- budget
    def _spent(self) -> float:
        return self.ledger.committed_total()

    def _left(self) -> float:
        return round(
            self.cfg.max_cost_usd
            - self.ledger.committed_total()
            - self.ledger.outstanding_total(),
            6,
        )

    def _service_left(self, service: str) -> float | None:
        budget = self.cfg.service_budget_usd.get(service)
        if budget is None:
            return None
        return round(
            budget - self.ledger.committed_total(service) - self.ledger.outstanding_total(service),
            6,
        )

    def _guard(self, estimate_usd: float | None, label: str, service: str) -> None:
        """Cheap pre-flight so a batch stops before it can overshoot the ceiling."""
        if estimate_usd is None:
            raise BudgetExceeded(f"refusing {label}: price unknown, cannot prove the budget")
        left = self._left()
        if estimate_usd > left:
            raise BudgetExceeded(
                f"refusing {label}: estimate {human_usd(estimate_usd)} would exceed the remaining "
                f"global budget {human_usd(left)} (ASSET_MAX_COST_USD={self.cfg.max_cost_usd})"
            )
        service_left = self._service_left(service)
        if service_left is not None and estimate_usd > service_left:
            raise BudgetExceeded(
                f"refusing {label}: estimate {human_usd(estimate_usd)} exceeds the remaining "
                f"{service} budget {human_usd(service_left)}"
            )

    def _progress(self, label: str, estimate_usd: float | None) -> None:
        self._seq += 1
        self.on_event(
            f"[{self._seq:02d}] {label:<42} est={human_usd(estimate_usd):>9} "
            f"spent={human_usd(self._spent())}/{human_usd(self.cfg.max_cost_usd)}"
        )

    # ------------------------------------------------------------ helpers
    def _selected(self, *keys: str) -> bool:
        """``--only`` filter: match any of the given names/ids."""
        if not self.only:
            return True
        return any(key in self.only for key in keys)

    def _path(self, *parts: str) -> Path:
        return self.cfg.processed_dir.joinpath(*parts)

    def _reuse_raw(self, raw_id: str) -> dict[str, Any] | None:
        entry = self.manifest.find(raw_id)
        if self.force or not entry:
            return None
        path = self.cfg.repo_root / entry["path"]
        if path.is_file() and path.stat().st_size > 0:
            return entry
        return None

    def _save_processed(self, image: Image.Image, path: Path) -> Path:
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=False)
        return path

    # ------------------------------------------------------------- images
    def _image_spec(
        self,
        asset_id: str,
        prompt: str,
        params: dict[str, Any],
        model: str,
    ) -> JobSpec:
        return JobSpec(
            {
                "id": asset_id,
                "kind": "texture",
                "service": "openai",
                "model": model,
                "prompt": prompt,
                "params": params,
            },
            source="<production>",
        )

    def generate_image(
        self,
        *,
        asset_id: str,
        prompt: str,
        params: dict[str, Any],
        model: str = "gpt-image-2.5-sunburst",
        label: str = "",
    ) -> tuple[Path, dict[str, Any], dict[str, Any]]:
        """Generate (or reuse) one image. Returns (raw_path, raw_manifest_entry, result)."""
        raw_id = f"raw_{asset_id}"
        reused = self._reuse_raw(raw_id)
        if reused:
            path = self.cfg.repo_root / reused["path"]
            self.report["skipped"].append({"asset_id": asset_id, "reason": "raw already generated"})
            self.on_event(f"[--] {label or asset_id:<42} reused  {path.name}")
            return path, reused, {"reused": True, "cost_usd": 0.0}

        spec = self._image_spec(asset_id, prompt, params, model)
        request = build_request(spec)
        estimate = self.openai.estimate(request)
        self._guard(estimate.usd, label or asset_id, "openai")
        self._progress(label or asset_id, estimate.usd)
        self.queue.acquire_rate_slot("openai", self.cfg.min_interval_s.get("openai", 0.0))

        last_error: Exception | None = None
        for attempt in range(1, 4):
            try:
                result = self.openai.generate(spec, job_id=f"prod_{asset_id}", ledger=self.ledger)
                break
            except OpenAIImageError as exc:
                last_error = exc
                if exc.response is not None and exc.response.status == 429 and attempt < 3:
                    wait = 15.0 * attempt
                    self.on_event(f"     429 rate limited; waiting {wait:.0f}s then retrying")
                    time.sleep(wait)
                    continue
                raise
        else:  # pragma: no cover - loop always breaks/raises
            raise last_error or RuntimeError("image generation failed")

        artifacts = result.get("artifacts") or []
        if not artifacts:
            raise RuntimeError(f"{asset_id}: OpenAI returned no artifacts")
        raw = artifacts[0]
        raw_path = Path(raw["path"])
        entry = self.manifest.add(
            asset_id=raw_id,
            kind="hud" if asset_id.startswith("hud_") else "texture",
            path=raw_path,
            service="openai",
            model=str(request.get("model")),
            prompt=prompt,
            parameters=request,
            stage="raw",
            cost={
                "estimated_usd": estimate.usd,
                "actual_usd": estimate.usd,
                "note": PRICE_NOTE,
            },
            job_id=f"prod_{asset_id}",
            license_note=LICENSE_OPENAI,
            validation=raw.get("validation"),
            warnings=result.get("warnings") or [],
        )
        self.report["images"].append(
            {
                "asset_id": asset_id,
                "raw_path": str(raw_path),
                "prompt": prompt,
                "request": request,
                "estimated_usd": estimate.usd,
                "sha256": raw.get("sha256"),
            }
        )
        return raw_path, entry, result

    # ----------------------------------------------------------- portraits
    def produce_portraits(self) -> None:
        out_dir = self._path("hud", "boss_portrait")
        total = len(BOSSES)
        for index, boss in enumerate(BOSSES, 1):
            asset_id = f"hud_portrait_{boss['id']}"
            if not self._selected(boss["id"], asset_id):
                continue
            target = out_dir / f"{boss['id']}.png"
            if target.is_file() and not self.force and self.manifest.find(asset_id):
                self.report["skipped"].append({"asset_id": asset_id, "reason": "portrait exists"})
                self.on_event(f"[{index:02d}/{total}] portrait {boss['id']:<30} exists, skipped")
                continue
            prompt = f"{boss['subject']}. {PORTRAIT_STYLE}"
            try:
                raw_path, raw_entry, _ = self.generate_image(
                    asset_id=asset_id,
                    prompt=prompt,
                    params={
                        "size": "1024x1024",
                        "quality": "medium",
                        "background": "transparent",
                        "output_format": "png",
                        "n": 1,
                    },
                    model="gpt-image-2.5-sunburst",
                    label=f"portrait/{boss['id']}",
                )
                processed = self._post_portrait(raw_path, boss["id"], asset_id, raw_entry)
                self.report["images"].append(
                    {"asset_id": asset_id, "processed": str(processed), "boss": boss["id"]}
                )
            except BudgetExceeded:
                raise
            except Exception as exc:  # noqa: BLE001 - continue the batch
                self.report["errors"].append(
                    {"asset_id": asset_id, "error": f"{type(exc).__name__}: {exc}"}
                )
                self.on_event(f"[!!] portrait {boss['id']} failed: {type(exc).__name__}: {exc}")

    def _post_portrait(
        self, raw_path: Path, boss_id: str, asset_id: str, raw_entry: dict[str, Any]
    ) -> Path:
        image = open_rgba(raw_path)
        array = np.asarray(image)
        alpha = array[:, :, 3]
        alpha_zero_fraction = float((alpha == 0).mean())
        top_corners = [int(array[0, 0, 3]), int(array[0, -1, 3])]
        bottom_corners = [int(array[-1, 0, 3]), int(array[-1, -1, 3])]
        bg_note: dict[str, Any] = {
            "method": "none (background alpha already present)",
            "alpha_zero_fraction": round(alpha_zero_fraction, 4),
        }
        # Only remove a background when the model ignored the transparency request:
        # a transparent *background* means the TOP corners are transparent; the bottom
        # of a chest-up bust is legitimately opaque (the body reaches the frame edge).
        if max(top_corners) > 8 and alpha_zero_fraction < 0.02:
            image, removed_info = remove_background(image, method="flood", tolerance=30)
            bg_note = {"method": "flood-fill from corners", **removed_info}
        trimmed = trim_alpha(image, padding_ratio=0.02)
        fitted = fit_square(trimmed, 128, resample=Image.Resampling.BILINEAR)
        fitted = apply_alpha_mode(fitted, "blend")
        target = self._save_processed(fitted, self._path("hud", "boss_portrait", f"{boss_id}.png"))
        validation = validate_png(target, expected_sizes=(128,), require_square=True)
        final = np.asarray(fitted)
        out_alpha_zero = float((final[:, :, 3] == 0).mean())
        out_top = [int(final[0, 0, 3]), int(final[0, -1, 3])]
        out_bottom = [int(final[-1, 0, 3]), int(final[-1, -1, 3])]
        background_ok = max(out_top) == 0 and out_alpha_zero > 0.02
        details = {
            "operation": "trim-alpha + fit-square(bilinear) + alpha-blend",
            "source_size": list(Image.open(raw_path).size),
            "target_size": [128, 128],
            "background_handling": bg_note,
            "alpha_zero_fraction": round(out_alpha_zero, 4),
            "top_corner_alpha": out_top,
            "bottom_corner_alpha": out_bottom,
            "note": "top corners = background (must be transparent); bottom = bust body (may be opaque)",
            "sha256": sha256_file(target),
            "bytes": target.stat().st_size,
        }
        warnings: list[str] = []
        if not validation.ok:
            warnings.append("png validation failed")
        if not background_ok:
            warnings.append("background is not fully transparent at the top corners")
        self.manifest.add_processed(
            asset_id=asset_id,
            path=target,
            source_asset_id=raw_entry["asset_id"],
            operation="portrait-postprocess",
            parameters=details,
            validation=validation.as_dict(),
            warnings=warnings,
        )
        self.on_event(
            f"      -> {target.relative_to(self.cfg.repo_root)} "
            f"bg_ok={background_ok} top_alpha={out_top} alpha0={out_alpha_zero:.2f} ok={validation.ok}"
        )
        return target

    # ---------------------------------------------------------- HUD frame
    def produce_frame(self) -> None:
        asset_id = "hud_boss_frame"
        if not self._selected("boss_frame", asset_id, "frame"):
            return
        target = self._path("hud", "boss_frame.png")
        if target.is_file() and not self.force and self.manifest.find(asset_id):
            self.report["skipped"].append({"asset_id": asset_id, "reason": "frame exists"})
            self.on_event("[--] boss frame exists, skipped")
            return
        prompt = (
            "Ornate horizontal boss health-bar frame for a dark fantasy game HUD, made of "
            "blackened iron and obsidian with silver filigree, sharp decorative caps at the left "
            "and right ends, a calm repeatable dark metal centre band, violet magical aura "
            "trim, isolated on a fully transparent background (alpha), 9-slice friendly: the "
            "centre region is a simple horizontal metal strip without focal elements, the two "
            "end caps contain the decoration, no text, no letters, no numbers, wide aspect ratio"
        )
        raw_path, raw_entry, _ = self.generate_image(
            asset_id=asset_id,
            prompt=prompt,
            params={
                "size": "1536x512",
                "quality": "high",
                "background": "transparent",
                "output_format": "png",
                "n": 1,
            },
            model="gpt-image-2.5-sunburst",
            label="frame/boss_frame",
        )
        return self._post_frame(raw_path, raw_entry)

    def _post_frame(self, raw_path: Path, raw_entry: dict[str, Any]) -> Path:
        asset_id = "hud_boss_frame"
        target = self._path("hud", "boss_frame.png")
        image = open_rgba(raw_path)
        array = np.asarray(image)
        if max(int(array[0, 0, 3]), int(array[0, -1, 3])) > 8:
            image, _ = remove_background(image, method="flood", tolerance=26)
        resized = resize_to(image, (512, 128), resample=Image.Resampling.BILINEAR)
        # The generated frame already has clean decorative caps and a calm centre.
        # Only force a 16 px repeat when the measured centre seam is actually bad.
        measured = nine_slice_report(resized, corner=16, center=16)
        forced = False
        if measured["center_ratio_x"] > 3.0:
            resized = make_center_tileable(resized, corner=16, tile_width=16)
            forced = True
        resized = apply_alpha_mode(resized, "blend")
        target = self._save_processed(resized, target)
        nine = nine_slice_report(resized, corner=16, center=16)
        validation = validate_png(target, expected_sizes=(), require_square=False)
        self.manifest.add_processed(
            asset_id=asset_id,
            path=target,
            source_asset_id=raw_entry["asset_id"],
            operation="resize(512x128,bilinear)"
            + (" + forced 16px centre repeat" if forced else " + measured 9-slice (centre kept)"),
            parameters={
                "source_size": list(Image.open(raw_path).size),
                "nine_slice": nine,
                "centre_forced": forced,
                "size": [512, 128],
                "sha256": sha256_file(target),
            },
            validation=validation.as_dict(),
            warnings=[] if nine["ok"] and validation.ok else ["9-slice contract not fully met"],
        )
        self.report["images"].append({"asset_id": asset_id, "processed": str(target), "nine_slice": nine})
        self.on_event(
            f"      -> hud/boss_frame.png 9slice_ok={nine['ok']} "
            f"center_ratio_x={nine['center_ratio_x']} caps_decorated={nine['caps_decorated']} "
            f"forced={forced}"
        )
        return target

    # ---------------------------------------------------------- bar fill
    def produce_bar_fill(self) -> None:
        """Fully specified horizontal gradient: produced procedurally (zero API cost)."""
        asset_id = "hud_boss_bar_fill"
        if not self._selected("bar_fill", asset_id):
            return
        target = self._path("hud", "boss_bar_fill.png")
        if target.is_file() and not self.force and self.manifest.find(asset_id):
            self.report["skipped"].append({"asset_id": asset_id, "reason": "bar fill exists"})
            self.on_event("[--] bar fill exists, skipped")
            return
        width, height = ensure_multiple_of_16(256, 32)
        x = np.linspace(0.0, 1.0, width, dtype=np.float32)[None, :, None]
        left = np.array(BAR_FILL_LEFT, dtype=np.float32)
        right = np.array(BAR_FILL_RIGHT, dtype=np.float32)
        gradient = left * (1.0 - x) + right * x
        # gentle vertical sheen so it does not look like a flat band in-game
        y = np.linspace(-1.0, 1.0, height, dtype=np.float32)[:, None, None]
        sheen = 1.0 + 0.16 * y
        rgb = np.clip(gradient * sheen, 0, 255).astype(np.uint8)
        alpha = np.full((height, width, 1), 255, dtype=np.uint8)
        canvas = np.concatenate([rgb, alpha], axis=-1)
        target.parent.mkdir(parents=True, exist_ok=True)
        image = Image.fromarray(canvas, mode="RGBA")
        image.save(target, format="PNG")
        validation = validate_png(target, expected_sizes=(256,), require_square=False)
        self.manifest.add(
            asset_id=asset_id,
            kind="hud",
            path=target,
            service="local",
            model=None,
            prompt=f"horizontal gradient {BAR_FILL_LEFT} -> {BAR_FILL_RIGHT} with vertical sheen",
            parameters={"operation": "procedural-gradient", "size": [width, height]},
            stage="processed",
            cost={"estimated_usd": 0.0, "actual_usd": 0.0, "note": "local Pillow/numpy generation"},
            validation=validation.as_dict(),
            license_note="Procedurally generated inside this repository (no third-party content).",
        )
        self.report["images"].append({"asset_id": asset_id, "processed": str(target), "cost_usd": 0.0})
        self.on_event(f"[--] hud/boss_bar_fill.png procedural (free) ok={validation.ok}")

    # ----------------------------------------------------------- textures
    def produce_textures(self) -> None:
        total = len(TEXTURES)
        for index, spec in enumerate(TEXTURES, 1):
            asset_id = f"texture_{spec['category']}_{spec['name']}"
            if not self._selected(spec["name"], asset_id):
                continue
            target = self._path("textures", spec["category"], f"{spec['name']}.png")
            if target.is_file() and not self.force and self.manifest.find(asset_id):
                self.report["skipped"].append({"asset_id": asset_id, "reason": "texture exists"})
                self.on_event(f"[{index:02d}/{total}] {spec['category']}/{spec['name']:<28} exists, skipped")
                continue
            try:
                prompt = spec["prompt"]
                if spec["tileable"]:
                    prompt += ", perfectly seamless tileable edges"
                raw_path, raw_entry, _ = self.generate_image(
                    asset_id=asset_id,
                    prompt=prompt,
                    params={
                        "size": "1024x1024",
                        "quality": "low",
                        "background": "transparent",
                        "output_format": "png",
                        "n": 1,
                    },
                    model="gpt-image-2.5-flare",
                    label=f"texture/{spec['category']}/{spec['name']}",
                )
                self._post_texture(raw_path, spec, asset_id, raw_entry)
            except BudgetExceeded:
                raise
            except Exception as exc:  # noqa: BLE001
                self.report["errors"].append(
                    {"asset_id": asset_id, "error": f"{type(exc).__name__}: {exc}"}
                )
                self.on_event(f"[!!] texture {spec['name']} failed: {type(exc).__name__}: {exc}")

    def _post_texture(
        self,
        raw_path: Path,
        spec: dict[str, Any],
        asset_id: str,
        raw_entry: dict[str, Any],
    ) -> Path:
        size = int(spec["size"])
        image = open_rgba(raw_path)
        array = np.asarray(image)
        if max(int(array[0, 0, 3]), int(array[0, -1, 3])) > 8:
            image, _ = remove_background(image, method="flood", tolerance=26)
        details: dict[str, Any] = {}
        if spec["tileable"]:
            # downscale smoothly, remove the wrap seam, then reduce to pixel art
            intermediate = max(size * 4, 64)
            working = resize_to(image, (intermediate, intermediate), resample=Image.Resampling.BILINEAR)
            working = tile_seam_correct(working)
            working = resize_to(working, (size, size), resample=Image.Resampling.NEAREST)
            working = quantize_palette(working, 16, dither=False)
            tiling = tileability(working)
            if tiling.get("tiling_relevant", True) and tiling["worst_ratio"] > 3.0:
                working = tile_seam_correct(working)
                tiling = tileability(working)
            details["tileability"] = tiling
            details["seam_corrected"] = True
        else:
            trimmed = trim_alpha(image, padding_ratio=0.02)
            inner = fit_square(trimmed, max(2, size - 2), resample=Image.Resampling.BILINEAR)
            inner = apply_alpha_mode(inner, "cutout", threshold=96)
            working = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            working.alpha_composite(inner, (1, 1))
            working = quantize_palette(working, 14, dither=False)
            working = apply_alpha_mode(working, "cutout", threshold=96)
            tiling = tileability(working)
            details["tileability"] = tiling
            details["note"] = "item sprite: 1px transparent margin, tiling not expected"
        target = self._save_processed(
            working, self._path("textures", spec["category"], f"{spec['name']}.png")
        )
        validation = validate_png(target, expected_sizes=(size,), require_square=True)
        details.update(
            request_size=list(Image.open(raw_path).size),
            target_size=[size, size],
            sha256=sha256_file(target),
            bytes=target.stat().st_size,
        )
        warnings: list[str] = []
        if spec["tileable"] and details["tileability"]["verdict"] != "seamless":
            warnings.append(f"tileability {details['tileability']['verdict']}")
        if not validation.ok:
            warnings.append("png validation failed")
        self.manifest.add_processed(
            asset_id=asset_id,
            path=target,
            source_asset_id=raw_entry["asset_id"],
            operation="tile-seam-correct + nearest-resize + quantize(16)"
            if spec["tileable"]
            else "trim-alpha + fit-square + cutout + quantize(14)",
            parameters={"name": spec["name"], "category": spec["category"], **details},
            validation=validation.as_dict(),
            warnings=warnings,
        )
        self.report["images"].append(
            {
                "asset_id": asset_id,
                "processed": str(target),
                "size": size,
                "tileable": spec["tileable"],
                "tileability": details["tileability"]["verdict"],
                "tile_ratio": details["tileability"].get("worst_ratio"),
            }
        )
        self.on_event(
            f"      -> textures/{spec['category']}/{spec['name']}.png {size}x{size} "
            f"tile={details['tileability']['verdict']} ok={validation.ok}"
        )

    # -------------------------------------------------------------- audio
    def produce_audio(self) -> None:
        out_dir = self._path("audio", "boss")
        out_dir.mkdir(parents=True, exist_ok=True)
        total = len(BOSSES) * 2
        done = 0
        for boss in BOSSES:
            if not self._selected(boss["id"], f"audio_boss_{boss['id']}"):
                continue
            for number, text in sorted(VOICE_LINES[boss["id"]].items()):
                done += 1
                asset_id = f"audio_boss_{boss['id']}_{number}"
                target = out_dir / f"{boss['id']}_{number}.ogg"
                if target.is_file() and not self.force and self.manifest.find(asset_id):
                    self.report["skipped"].append({"asset_id": asset_id, "reason": "voice exists"})
                    self.on_event(f"[{done:02d}/{total}] voice {boss['id']}_{number} exists, skipped")
                    continue
                chars = len(text)
                if not 15 <= chars <= 40:
                    self.report["errors"].append(
                        {"asset_id": asset_id, "error": f"line length {chars} outside 15-40"}
                    )
                    continue
                try:
                    self._produce_voice(asset_id, boss["id"], number, text, target)
                except BudgetExceeded:
                    raise
                except Exception as exc:  # noqa: BLE001
                    self.report["errors"].append(
                        {"asset_id": asset_id, "error": f"{type(exc).__name__}: {exc}"}
                    )
                    self.on_event(f"[!!] voice {boss['id']}_{number} failed: {type(exc).__name__}: {exc}")

    def _produce_voice(
        self, asset_id: str, boss_id: str, number: int, text: str, target: Path
    ) -> None:
        spec = JobSpec(
            {
                "id": f"prod_{asset_id}",
                "kind": "audio",
                "service": "fish",
                "model": "s2.1-pro",
                "prompt": text,
                "audio": {
                    "tts_model": "s2.1-pro",
                    "format": "wav",
                    "sample_rate": 44100,
                    "language": "ja",
                    "normalize": True,
                    "latency": "normal",
                },
            },
            source="<production>",
        )
        estimate = self.fish.estimate(spec)
        self._guard(estimate.usd, f"voice/{boss_id}_{number}", "fish")
        self._progress(f"voice/{boss_id}_{number} ({len(text)} chars)", estimate.usd)
        self.queue.acquire_rate_slot("fish", self.cfg.min_interval_s.get("fish", 0.0))
        result = self.fish.synthesize(spec, job_id=f"prod_{asset_id}", ledger=self.ledger)
        conversion = result.get("conversion") or {}
        game_path = conversion.get("game_path")
        if not game_path or not Path(game_path).is_file():
            raise RuntimeError(f"ogg conversion failed: {conversion.get('status')}")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(game_path, target)
        try:
            Path(game_path).unlink(missing_ok=True)  # keep only the canonical boss path
        except OSError:
            pass
        validation = validate_audio(target)
        raw = result["raw_artifact"]
        entry = self.manifest.add(
            asset_id=asset_id,
            kind="audio",
            path=target,
            service="fish",
            model=result.get("tts_model"),
            prompt=text,
            parameters={
                "boss": boss_id,
                "line_type": "battle_cry" if number == 1 else "spoken_line",
                "characters": len(text),
                "requested_format": "wav",
                "game_format": "ogg/vorbis",
                "source_raw": raw["path"],
                "source_raw_sha256": raw["sha256"],
                "sha256": sha256_file(target),
            },
            stage="processed",
            cost={
                "estimated_usd": estimate.usd,
                "actual_usd": estimate.usd,
                "note": PRICE_NOTE,
            },
            job_id=f"prod_{asset_id}",
            license_note=LICENSE_FISH,
            validation=validation.as_dict(),
            warnings=[] if validation.ok and validation.details.get("codec") == "vorbis" else [
                "audio validation/codec issue"
            ],
        )
        self.report["audio"].append(
            {
                "asset_id": asset_id,
                "boss": boss_id,
                "line": text,
                "characters": len(text),
                "path": str(target),
                "estimated_usd": estimate.usd,
                "codec": validation.details.get("codec"),
                "duration_s": validation.details.get("duration_s"),
            }
        )
        self.on_event(
            f"      -> {target.relative_to(self.cfg.repo_root)} "
            f"codec={validation.details.get('codec')} dur={validation.details.get('duration_s')}s "
            f"ok={validation.ok}"
        )

    # -------------------------------------------------------------- tripo
    def produce_tripo(self) -> None:
        """image_to_model for 1-2 bosses; failure is reported as failure, never as success."""
        out_dir = self._path("geckolib")
        out_dir.mkdir(parents=True, exist_ok=True)
        for boss_id in TRIPO_BOSSES:
            asset_id = f"model_{boss_id}"
            if not self._selected(boss_id, asset_id):
                continue
            model_target = out_dir / f"{boss_id}.glb"
            if model_target.is_file() and not self.force and self.manifest.find(asset_id):
                self.report["skipped"].append({"asset_id": asset_id, "reason": "glb exists"})
                self.on_event(f"[--] model {boss_id} exists, skipped")
                continue
            try:
                self._produce_model(boss_id, asset_id, model_target)
            except BudgetExceeded:
                raise
            except Exception as exc:  # noqa: BLE001
                self.report["models"].append(
                    {
                        "asset_id": asset_id,
                        "status": "failed",
                        "error": f"{type(exc).__name__}: {exc}",
                    }
                )
                self.report["errors"].append(
                    {"asset_id": asset_id, "error": f"{type(exc).__name__}: {exc}"}
                )
                self.on_event(f"[!!] model {boss_id} failed: {type(exc).__name__}: {exc}")

    def _produce_model(self, boss_id: str, asset_id: str, model_target: Path) -> None:
        boss = next(b for b in BOSSES if b["id"] == boss_id)
        concept_prompt = (
            f"Full body character reference of {boss['subject']}. Standing upright in a neutral "
            "A-pose, arms slightly away from the body, legs apart, full body from head to toe "
            "visible and centered, clean readable silhouette, plain light grey studio background, "
            "dark fantasy game character concept art, no text, no labels, no props on the ground"
        )
        concept_path, concept_entry, _ = self.generate_image(
            asset_id=f"concept_{boss_id}",
            prompt=concept_prompt,
            params={
                "size": "1024x1536",
                "quality": "medium",
                "background": "opaque",
                "output_format": "png",
                "n": 1,
            },
            model="gpt-image-2.5-sunburst",
            label=f"concept/{boss_id} (tripo input)",
        )
        spec = JobSpec(
            {
                "id": f"prod_{asset_id}",
                "kind": "model",
                "service": "tripo",
                "mode": "image_to_model",
                "model": "v2.5-20250123",
                "prompt": f"low-poly game boss model of {boss_id}, "
                f"{(boss['subject'])[:180]}",
                "input_image": str(concept_path),
                "params": {
                    "model_version": "v2.5-20250123",
                    "face_limit": 12000,
                    "texture": True,
                    "pbr": True,
                    "texture_quality": "standard",
                    "geometry_quality": "standard",
                    "export_uv": True,
                    "quad": False,
                    "auto_size": False,
                },
            },
            source="<production>",
        )
        estimate = self.tripo.estimate("image_to_model")
        self._guard(estimate.usd, f"model/{boss_id}", "tripo")
        self._progress(f"model/{boss_id} (image_to_model)", estimate.usd)
        self.queue.acquire_rate_slot("tripo", self.cfg.min_interval_s.get("tripo", 0.0))
        balance_before = self._tripo_balance()
        job_id, created = self.queue.add_job(
            kind="model",
            service="tripo",
            model="v2.5-20250123",
            payload=spec.hash_payload(),
            job_id=f"prod_{asset_id}",
            max_attempts=1,
        )
        existing_job = self.queue.get(job_id) or {}
        if not created and existing_job.get("status") == "failed":
            self.queue.requeue(job_id, reset_attempts=True)
        self.queue.claim_specific(job_id)
        created_task = self.tripo.create_task(spec, job_id=job_id, ledger=self.ledger)
        task_id = created_task["task_id"]
        reservation_id = created_task.get("reservation_id")
        self.queue.set_remote_task_id(job_id, task_id)
        self.on_event(f"      tripo task_id={task_id}; polling...")
        try:
            task = self.tripo.wait_for_task(
                task_id,
                interval_s=3.0,
                timeout_s=420.0,
                on_progress=lambda task: self.on_event(
                    f"      tripo {task.get('status')} {task.get('progress', 0)}%"
                ),
            )
            if str(task.get("status", "")).lower() != "success":
                raise TripoError(f"task ended with status {task.get('status')!r}")
            artifact = self.tripo.download_output(task, destination_dir=model_target.parent)
            shutil.copy2(artifact["model_path"], model_target)
            validation = validate_glb(model_target, max_triangles=self.cfg.max_tripo_triangles)
            if not validation.ok:
                raise TripoError("GLB validation failed: " + "; ".join(validation.errors))
            # Measure the real credit consumption from the (non-billable) balance delta.
            balance_after = self._tripo_balance()
            measured_credits: float | None = None
            if balance_before is not None and balance_after is not None:
                delta = round(float(balance_before) - float(balance_after), 4)
                if delta > 0:
                    measured_credits = delta
            credits_actual = measured_credits if measured_credits is not None else estimate.credits
            usd_actual = (
                round(float(credits_actual) * float(self.cfg.tripo_credit_usd), 6)
                if (credits_actual is not None and self.cfg.tripo_credit_usd is not None)
                else estimate.usd
            )
            if reservation_id is not None:
                self.ledger.commit(
                    reservation_id,
                    actual_usd=usd_actual,
                    credits=credits_actual,
                    note=(
                        f"measured credit delta {measured_credits} from /account/balance; "
                        "USD from operator credit price"
                        if measured_credits is not None
                        else "balance delta unavailable; estimated credits used"
                    ),
                )
            entry = self.manifest.add(
                asset_id=asset_id,
                kind="model",
                path=model_target,
                service="tripo",
                model=str(task.get("model") or "v2.5-20250123"),
                prompt=spec.prompt,
                parameters={
                    "mode": "image_to_model",
                    "concept_image": str(concept_path),
                    "task_id": task_id,
                    "output_keys": sorted((task.get("output") or {}).keys()),
                    "triangle_estimate": validation.details.get("triangle_estimate"),
                    "has_uv": validation.details.get("has_uv"),
                    "sha256": sha256_file(model_target),
                    "tripo_balance_before": balance_before,
                    "tripo_balance_after": balance_after,
                    "credits_measured": measured_credits,
                },
                stage="raw",
                cost={
                    "estimated_usd": estimate.usd,
                    "actual_usd": usd_actual,
                    "credits": credits_actual,
                    "note": PRICE_NOTE,
                },
                job_id=f"prod_{asset_id}",
                license_note=LICENSE_TRIPO,
                validation=validation.as_dict(),
                warnings=validation.warnings,
                extra={"source_url": artifact.get("model_url")},
            )
            self.report["models"].append(
                {
                    "asset_id": asset_id,
                    "status": "success",
                    "path": str(model_target),
                    "task_id": task_id,
                    "triangles": validation.details.get("triangle_estimate"),
                    "has_uv": validation.details.get("has_uv"),
                    "estimated_usd": estimate.usd,
                    "actual_usd": usd_actual,
                    "credits": credits_actual,
                    "credits_measured": measured_credits,
                    "balance_before": balance_before,
                    "balance_after": balance_after,
                    "validation": validation.as_dict(),
                }
            )
            self.queue.mark_done(
                job_id,
                result={
                    "asset_id": asset_id,
                    "triangles": validation.details.get("triangle_estimate"),
                },
                artifact_path=str(model_target),
                remote_task_id=task_id,
                source_url=artifact.get("model_url"),
            )
            self.on_event(
                f"      -> {model_target.relative_to(self.cfg.repo_root)} "
                f"tris={validation.details.get('triangle_estimate')} "
                f"uv={validation.details.get('has_uv')} ok={validation.ok}"
            )
        except Exception as exc:
            if reservation_id is not None:
                self.ledger.release(reservation_id, "tripo task did not complete successfully")
            self.queue.mark_failed(job_id, error=f"{type(exc).__name__}: {exc}", retryable=False)
            raise

    # ------------------------------------------------------- voxelize GLB
    def produce_voxelize(self) -> None:
        """GLB -> voxel -> GeckoLib geo.json; failure is recorded as failure."""
        out_dir = self._path("geckolib")
        out_dir.mkdir(parents=True, exist_ok=True)
        preview_dir = self.cfg.state_dir / "previews"
        preview_dir.mkdir(parents=True, exist_ok=True)
        for boss_id in TRIPO_BOSSES:
            source_asset = f"model_{boss_id}"
            asset_id = f"geckolib_{boss_id}"
            if not self._selected(boss_id, asset_id):
                continue
            geo_target = out_dir / f"{boss_id}.geo.json"
            png_target = out_dir / f"{boss_id}.png"
            source = self.manifest.find(source_asset)
            if source is None:
                self.report["missing"].append(
                    {"asset_id": asset_id, "reason": f"{source_asset} has not been generated"}
                )
                self.on_event(f"[--] voxelize {boss_id}: no GLB available")
                continue
            if (
                geo_target.is_file()
                and png_target.is_file()
                and self.manifest.find(asset_id)
                and not self.force
            ):
                validation = validate_geckolib(geo_target, png_target)
                self.report["models"].append(
                    {"asset_id": asset_id, "status": "exists", "validation": validation}
                )
                self.on_event(
                    f"[--] voxelize {boss_id}: exists "
                    f"cubes={validation.get('details', {}).get('cubes')} ok={validation['ok']}"
                )
                continue
            glb_path = self.cfg.repo_root / source["path"]
            try:
                result = glb_to_geckolib(
                    glb_path,
                    out_dir,
                    boss_id,
                    resolution=32,
                    preview_path=preview_dir / f"voxel_{boss_id}.png",
                )
                validation = validate_geckolib(result["geo_path"], result["texture_path"])
                if not validation["ok"]:
                    raise VoxelizeError("geo.json validation failed: " + "; ".join(validation["errors"]))
                caveats = [
                    "static geometry only (single 'body' bone, no skeleton/animations)",
                    "PBR/normal maps replaced by a flat palette atlas",
                    "scale (40 px tall) and orientation are approximations - verify in-game",
                    "details below one voxel are lost",
                ]
                self.manifest.add(
                    asset_id=asset_id,
                    kind="model",
                    path=geo_target,
                    service="local",
                    model=None,
                    prompt=None,
                    parameters={
                        "converter": "surface voxelizer + greedy X merge",
                        "source_glb": source["path"],
                        "resolution": result["stats"]["resolution"],
                        "cubes": result["stats"]["cubes"],
                        "palette_colors": result["stats"]["palette_colors"],
                        "voxel_px": result["stats"]["voxel_px"],
                        "height_px": result["stats"]["height_px"],
                        "atlas": result["stats"]["atlas"],
                        "attempts": result["attempts"],
                        "caveats": caveats,
                    },
                    stage="processed",
                    source_asset_id=source_asset,
                    cost={"estimated_usd": 0.0, "actual_usd": 0.0, "note": "local conversion"},
                    validation=validation,
                    warnings=caveats,
                    extra={"preview": str(preview_dir / f"voxel_{boss_id}.png")},
                )
                self.manifest.add(
                    asset_id=f"{asset_id}_texture",
                    kind="model",
                    path=png_target,
                    service="local",
                    model=None,
                    prompt=None,
                    parameters={
                        "role": "palette atlas for the voxel geometry",
                        "atlas": result["stats"]["atlas"],
                        "palette_colors": result["stats"]["palette_colors"],
                    },
                    stage="processed",
                    source_asset_id=asset_id,
                    cost={"estimated_usd": 0.0, "actual_usd": 0.0, "note": "local conversion"},
                    validation={"ok": True, "kind": "png", "note": "palette atlas"},
                )
                self.report["models"].append(
                    {
                        "asset_id": asset_id,
                        "status": "success",
                        "geo_path": str(geo_target),
                        "texture_path": str(png_target),
                        "preview_path": result.get("preview_path"),
                        "stats": result["stats"],
                        "validation": validation,
                    }
                )
                self.on_event(
                    f"[++] voxelize {boss_id}: cubes={result['stats']['cubes']} "
                    f"palette={result['stats']['palette_colors']} "
                    f"atlas={validation['details']['texture_width']}x"
                    f"{validation['details']['texture_height']} ok={validation['ok']}"
                )
            except Exception as exc:  # noqa: BLE001
                self.report["models"].append(
                    {
                        "asset_id": asset_id,
                        "status": "failed",
                        "error": f"{type(exc).__name__}: {exc}",
                    }
                )
                self.report["errors"].append(
                    {"asset_id": asset_id, "error": f"{type(exc).__name__}: {exc}"}
                )
                self.on_event(f"[!!] voxelize {boss_id} FAILED: {type(exc).__name__}: {exc}")

    # ------------------------------------------------------------- report
    def _tripo_balance(self) -> float | None:
        """Non-billable balance probe used to measure real credit consumption."""
        try:
            payload = self.tripo.balance(api="v3")
            data = payload.get("data") if isinstance(payload, dict) else None
            if isinstance(data, dict) and data.get("balance") is not None:
                return float(data["balance"])
        except Exception:  # noqa: BLE001 - balance is best-effort observability
            return None
        return None

    def summary(self) -> dict[str, Any]:
        totals = self.ledger.totals()
        images_cost = sum(
            i.get("estimated_usd") or 0.0 for i in self.report["images"] if "estimated_usd" in i
        )
        audio_cost = sum(a.get("estimated_usd") or 0.0 for a in self.report["audio"])
        model_cost = sum(
            (m.get("actual_usd") if m.get("actual_usd") is not None else m.get("estimated_usd")) or 0.0
            for m in self.report["models"]
            if m.get("status") == "success"
        )
        self.report["cost"] = {
            "images_usd": round(images_cost, 4),
            "audio_usd": round(audio_cost, 4),
            "models_usd": round(model_cost, 4),
            "total_usd": round(images_cost + audio_cost + model_cost, 4),
            "ledger_committed_usd": totals["committed_usd"],
            "ledger_reserved_usd": totals["reserved_usd"],
            "by_service": totals["by_service"],
            "max_cost_usd": self.cfg.max_cost_usd,
            "remaining_usd": round(self.cfg.max_cost_usd - totals["committed_usd"] - totals["reserved_usd"], 4),
            "note": PRICE_NOTE,
        }
        self.report["finished_at"] = time.strftime("%Y-%m-%dT%H:%M:%S%z")
        self.report["manifest"] = self.manifest.summary()
        return self.report

    def write_report(self) -> Path:
        stamp = time.strftime("%Y%m%dT%H%M%SZ")
        payload = self.summary()
        atomic_write_json(self.cfg.manifests_dir / "production_report.json", payload)
        return atomic_write_json(self.cfg.manifests_dir / f"production_report_{stamp}.json", payload)


def run_group(
    cfg: Config,
    group: str,
    ledger: Ledger,
    queue: JobQueue,
    *,
    force: bool = False,
    only: Iterable[str] | None = None,
    on_event: Callable[[str], None] | None = None,
) -> dict[str, Any]:
    """Run one production group. ``group`` is hud | textures | audio | tripo | all."""
    if cfg.spend_locked:
        raise RuntimeError(
            f"spend gate is closed ({cfg.mode}); the Lead must enable ASSET_DRY_RUN=false and "
            "ASSET_AUTO_SPEND=true before production"
        )
    studio = Studio(cfg, ledger, queue, force=force, only=only, on_event=on_event)
    order = {
        "hud": [studio.produce_portraits, studio.produce_bar_fill, studio.produce_frame],
        "textures": [studio.produce_textures],
        "audio": [studio.produce_audio],
        "tripo": [studio.produce_tripo],
        "voxelize": [studio.produce_voxelize],
        "all": [
            studio.produce_portraits,
            studio.produce_bar_fill,
            studio.produce_frame,
            studio.produce_textures,
            studio.produce_audio,
            studio.produce_tripo,
            studio.produce_voxelize,
        ],
    }
    if group not in order:
        raise ValueError(f"unknown group {group!r}; expected {sorted(order)}")
    for step in order[group]:
        step()
    report = studio.summary()
    report["report_path"] = str(studio.write_report())
    return report

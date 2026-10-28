#!/usr/bin/env python3
"""NOCTERRA のプレースホルダ用テクスチャ生成（ローカル生成・外部API不使用）。

本番アセットは tools/asset_pipeline で生成し、承認後に差し替える。
使い方: python tools/dev/gen_placeholder_textures.py
"""
from __future__ import annotations

import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "nocterra", "src", "main", "resources", "assets", "nocterra", "textures")


def _rng(seed: int) -> random.Random:
    return random.Random(seed)


def noisy(name: str, base: tuple[int, int, int], variance: int, seed: int,
          veins: tuple[int, int, int] | None = None, vein_chance: float = 0.0) -> Image.Image:
    rng = _rng(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = rng.randint(-variance, variance)
            c = tuple(max(0, min(255, base[i] + d)) for i in range(3))
            px[x, y] = (*c, 255)
    if veins is not None and vein_chance > 0:
        for y in range(16):
            for x in range(16):
                if rng.random() < vein_chance:
                    px[x, y] = (*veins, 255)
    return img


def brick_pattern(base: tuple[int, int, int], mortar: tuple[int, int, int], seed: int) -> Image.Image:
    rng = _rng(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            row = y // 4
            offset = 4 if row % 2 else 0
            is_mortar = (y % 4 == 3) or ((x + offset) % 8 == 7)
            if is_mortar:
                c = mortar
            else:
                d = rng.randint(-8, 8)
                c = tuple(max(0, min(255, base[i] + d)) for i in range(3))
            px[x, y] = (*c, 255)
    return img


def portal_swirl(seed: int) -> Image.Image:
    rng = _rng(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            v = math.sin(r * 1.6 - a * 2.0) * 0.5 + 0.5
            base = (18, 10, 30)
            glow = (150, 90, 220)
            jitter = rng.randint(-6, 6)
            c = tuple(
                max(0, min(255, int(base[i] + (glow[i] - base[i]) * v) + jitter)) for i in range(3)
            )
            alpha = 200 if v > 0.25 else 150
            px[x, y] = (*c, alpha)
    return img


def ember_shard(seed: int) -> Image.Image:
    rng = _rng(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            cx, cy = abs(x - 7.5), abs(y - 7.5)
            if cx / 4.2 + cy / 6.5 <= 1.0:
                t = 1.0 - (cx / 4.2 + cy / 6.5)
                r = int(120 + 135 * t)
                g = int(40 + 120 * t)
                b = int(20 + 40 * t)
                jitter = rng.randint(-12, 12)
                px[x, y] = (
                    max(0, min(255, r + jitter)),
                    max(0, min(255, g + jitter)),
                    max(0, min(255, b + jitter)),
                    255,
                )
    return img


def save(img: Image.Image, rel: str) -> None:
    path = os.path.join(ASSETS, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG")
    print("wrote", os.path.relpath(path, ROOT))


def main() -> None:
    save(brick_pattern((62, 58, 70), (38, 34, 44), 11), "block/gate_frame.png")
    save(portal_swirl(21), "block/ashen_portal.png")
    save(noisy("ashstone", (84, 80, 88), 10, 31), "block/ashstone.png")
    save(noisy("ash_coal_ore", (84, 80, 88), 8, 41, veins=(22, 20, 26), vein_chance=0.16), "block/ash_coal_ore.png")

    log_side = noisy("ashwood_log", (74, 66, 60), 9, 51, veins=(46, 40, 36), vein_chance=0.12)
    save(log_side, "block/ashwood_log.png")
    save(noisy("ashwood_log_top", (92, 82, 72), 8, 52), "block/ashwood_log_top.png")
    save(noisy("ashwood_planks", (100, 90, 78), 7, 61), "block/ashwood_planks.png")
    save(ember_shard(71), "item/ember_shard.png")


if __name__ == "__main__":
    main()

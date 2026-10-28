#!/usr/bin/env python3
"""ボスHPバー用ポートレートのプレースホルダ生成（22x22 / 透過）。

本番版は tools/asset_pipeline で生成し、同じファイル名で差し替える。
"""
from __future__ import annotations

import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(
    ROOT, "nocterra", "src", "main", "resources", "assets", "nocterra",
    "textures", "gui", "sprites", "boss_portrait",
)
SIZE = 22

# boss_id -> (ベース色, アクセント色, モチーフ)
BOSSES = {
    "rotwood_king": ((58, 74, 48), (150, 200, 90), "crown"),
    "sorrow_saint": ((74, 86, 104), (150, 190, 240), "veil"),
    "condemned_knight_lord": ((70, 70, 80), (200, 180, 120), "helm"),
    "sandglass_twin_kings": ((150, 106, 52), (240, 210, 130), "hourglass"),
    "abyssal_sea_emperor": ((32, 64, 92), (90, 200, 220), "trident"),
    "astel_fallen_star_dragon": ((44, 40, 82), (190, 160, 255), "dragon"),
    "silver_executioner": ((86, 94, 102), (220, 235, 245), "axe"),
    "molten_steel_colossus": ((92, 48, 30), (255, 150, 60), "anvil"),
    "black_crowned_king": ((30, 26, 38), (170, 120, 255), "blackcrown"),
    "ember_incarnation": ((120, 60, 20), (255, 200, 90), "flame"),
    "forgotten_knight": ((50, 52, 58), (140, 150, 170), "broken"),
    "nameless_observer": ((26, 30, 34), (120, 220, 210), "eye"),
}


def draw_motif(px, motif: str, accent: tuple[int, int, int]) -> None:
    w = SIZE
    a = (*accent, 255)

    def put(x: int, y: int) -> None:
        if 0 <= x < w and 0 <= y < w:
            px[x, y] = a

    if motif in ("crown", "blackcrown"):
        for x in range(6, 16):
            put(x, 8)
        for x in (6, 9, 12, 15):
            for y in (5, 6, 7):
                put(x, y)
    elif motif == "veil":
        for y in range(6, 17):
            put(7, y)
            put(14, y)
        for x in range(7, 15):
            put(x, 6)
    elif motif == "helm":
        for x in range(7, 15):
            put(x, 8)
        put(10, 10); put(11, 10); put(10, 11); put(11, 11)
    elif motif == "hourglass":
        for x in range(8, 14):
            put(x, 6)
            put(x, 15)
        for d in range(4):
            put(8 + d, 7 + d); put(13 - d, 7 + d)
            put(8 + d, 14 - d); put(13 - d, 14 - d)
    elif motif == "trident":
        for y in range(6, 17):
            put(10, y)
        for dy in (0, 2, 4):
            put(8, 6 + dy); put(12, 6 + dy)
    elif motif == "dragon":
        for x in range(6, 16):
            put(x, 12)
        for d in range(4):
            put(6 + d, 12 - d); put(15 - d, 12 - d)
    elif motif == "axe":
        for y in range(6, 17):
            put(11, y)
        for x in range(6, 10):
            put(x, 8); put(x, 9); put(x, 10)
    elif motif == "anvil":
        for x in range(6, 16):
            put(x, 9)
        for x in range(8, 14):
            put(x, 11); put(x, 12)
    elif motif == "flame":
        for y in range(6, 16):
            span = max(1, 5 - abs(y - 11) // 2)
            for x in range(11 - span, 11 + span):
                put(x, y)
    elif motif == "broken":
        for y in range(6, 16):
            put(10, y)
        for y in range(6, 10):
            put(12, y)
    elif motif == "eye":
        for x in range(6, 16):
            put(x, 11)
        put(10, 10); put(11, 10); put(10, 11); put(11, 11); put(10, 12); put(11, 12)
    else:
        for x in range(8, 14):
            put(x, 10)


def make_portrait(boss_id: str, base: tuple[int, int, int], accent: tuple[int, int, int], motif: str) -> Image.Image:
    rng = random.Random(hash(boss_id) & 0xFFFF)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    cx, cy = (SIZE - 1) / 2, SIZE / 2
    # 円形のシルエット + 肩
    for y in range(SIZE):
        for x in range(SIZE):
            dx = (x - cx) / 9.0
            dy = (y - cy) / 9.5
            inside = dx * dx + dy * dy <= 1.0
            shoulders = y >= 15 and abs(x - cx) <= 8.0
            if not (inside or shoulders):
                continue
            shade = 1.0 - 0.45 * (y / SIZE)
            d = rng.randint(-10, 10)
            c = tuple(max(0, min(255, int(base[i] * shade) + d)) for i in range(3))
            px[x, y] = (*c, 255)
    # 縁取り
    for y in range(SIZE):
        for x in range(SIZE):
            if px[x, y][3] == 0:
                continue
            edge = False
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + ox, y + oy
                if not (0 <= nx < SIZE and 0 <= ny < SIZE) or px[nx, ny][3] == 0:
                    edge = True
                    break
            if edge:
                px[x, y] = (accent[0] // 2, accent[1] // 2, accent[2] // 2, 255)
    draw_motif(px, motif, accent)
    return img


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for boss_id, (base, accent, motif) in BOSSES.items():
        img = make_portrait(boss_id, base, accent, motif)
        path = os.path.join(OUT, f"{boss_id}.png")
        img.save(path, "PNG")
        print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()

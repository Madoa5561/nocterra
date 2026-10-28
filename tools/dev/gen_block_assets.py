#!/usr/bin/env python3
"""NOCTERRA のブロック/アイテム JSON（blockstates, models）と lang を生成する。

テクスチャは tools/dev/gen_placeholder_textures.py で生成。
"""
from __future__ import annotations

import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "nocterra", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nocterra")

BLOCKS_CUBE_ALL = ["gate_frame", "ashstone", "ash_coal_ore", "ashwood_planks"]
BLOCKS_COLUMN = ["ashwood_log"]


def write_json(rel: str, obj: dict) -> None:
    path = os.path.join(ASSETS, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


def main() -> None:
    for name in BLOCKS_CUBE_ALL:
        write_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"nocterra:block/{name}"}}})
        write_json(
            f"models/block/{name}.json",
            {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"nocterra:block/{name}"},
            },
        )
        write_json(f"models/item/{name}.json", {"parent": f"nocterra:block/{name}"})

    for name in BLOCKS_COLUMN:
        write_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"nocterra:block/{name}"}}})
        write_json(
            f"models/block/{name}.json",
            {
                "parent": "minecraft:block/cube_column",
                "textures": {"end": f"nocterra:block/{name}_top", "side": f"nocterra:block/{name}"},
            },
        )
        write_json(f"models/item/{name}.json", {"parent": f"nocterra:block/{name}"})

    # 通過面（2 軸ぶんのバリアント）
    write_json(
        "blockstates/ashen_portal.json",
        {
            "variants": {
                "axis=x": {"model": "nocterra:block/ashen_portal"},
                "axis=z": {"model": "nocterra:block/ashen_portal"},
            }
        },
    )
    write_json(
        "models/block/ashen_portal.json",
        {
            "parent": "minecraft:block/cube_all",
            "render_type": "minecraft:translucent",
            "textures": {"all": "nocterra:block/ashen_portal"},
        },
    )

    write_json(
        "models/item/ember_shard.json",
        {"parent": "minecraft:item/generated", "textures": {"layer0": "nocterra:item/ember_shard"}},
    )

    lang_dir = os.path.join(ASSETS, "lang")

    def write_lang(locale: str, entries: dict[str, str]) -> None:
        os.makedirs(lang_dir, exist_ok=True)
        path = os.path.join(lang_dir, f"{locale}.json")
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(entries, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        print("wrote", os.path.relpath(path, ROOT))

    en = {
        "itemGroup.nocterra": "NOCTERRA",
        "block.nocterra.gate_frame": "Ancient Gate Frame",
        "block.nocterra.ashen_portal": "Ashen Gate",
        "block.nocterra.ashstone": "Ashstone",
        "block.nocterra.ash_coal_ore": "Ashen Coal Ore",
        "block.nocterra.ashwood_log": "Ashwood Log",
        "block.nocterra.ashwood_planks": "Ashwood Planks",
        "item.nocterra.ember_shard": "Ember Shard",
        "biome.nocterra.ashen_forest": "Ashen Forest",
        "biome.nocterra.mourning_marsh": "Mourning Marsh",
    }
    ja = {
        "itemGroup.nocterra": "NOCTERRA",
        "block.nocterra.gate_frame": "古代の門の骨組み",
        "block.nocterra.ashen_portal": "灰冠の門",
        "block.nocterra.ashstone": "灰石",
        "block.nocterra.ash_coal_ore": "灰炭鉱石",
        "block.nocterra.ashwood_log": "灰木の原木",
        "block.nocterra.ashwood_planks": "灰木の板材",
        "item.nocterra.ember_shard": "エンバーシャード",
        "biome.nocterra.ashen_forest": "灰樹の森",
        "biome.nocterra.mourning_marsh": "嘆きの湿原",
    }
    write_lang("en_us", en)
    write_lang("ja_jp", ja)


if __name__ == "__main__":
    main()

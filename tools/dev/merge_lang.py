#!/usr/bin/env python3
"""lang 断片のマージ。

各担当は `nocterra/lang/fragments/<name>.json` にキーを書く。書式は 2 通り:

1. フラット辞書（en_us と ja_jp の両方に同じ値を入れる）
   `{"entity.nocterra.foo": "Foo"}`
2. ロケール別ファイル（ファイル名の末尾で判定）
   `combat.en_us.json` / `combat.ja_jp.json` → それぞれのロケールのみにマージ
3. ネスト辞書
   `{"en_us": {...}, "ja_jp": {...}}`

使い方: python tools/dev/merge_lang.py
"""
from __future__ import annotations

import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FRAGMENTS = os.path.join(ROOT, "nocterra", "lang", "fragments")
LANG = os.path.join(ROOT, "nocterra", "src", "main", "resources", "assets", "nocterra", "lang")
LOCALES = ("en_us", "ja_jp")


def load(path: str) -> dict:
    if not os.path.exists(path):
        return {}
    with open(path, encoding="utf-8-sig") as fh:
        try:
            return json.load(fh)
        except json.JSONDecodeError as exc:
            raise SystemExit(f"invalid JSON: {path}: {exc}") from exc


def locale_of(filename: str) -> str | None:
    for locale in LOCALES:
        if filename.endswith(f".{locale}.json"):
            return locale
    return None


def main() -> None:
    merged: dict[str, dict[str, str]] = {locale: {} for locale in LOCALES}
    for locale in LOCALES:
        merged[locale].update(load(os.path.join(LANG, f"{locale}.json")))

    if os.path.isdir(FRAGMENTS):
        for entry in sorted(os.listdir(FRAGMENTS)):
            if not entry.endswith(".json"):
                continue
            path = os.path.join(FRAGMENTS, entry)
            data = load(path)
            if not isinstance(data, dict):
                raise SystemExit(f"fragment must be an object: {path}")

            if any(isinstance(data.get(locale), dict) for locale in LOCALES):
                targets = {locale: data[locale] for locale in LOCALES if isinstance(data.get(locale), dict)}
            else:
                # ロケール指定の無いフラット断片は en_us のみに入れる
                # （英語文字列を ja_jp に入れると日本語訳を上書きしてしまうため）
                forced = locale_of(entry)
                targets = {forced: data} if forced else {"en_us": data}

            for locale, section in targets.items():
                for key, value in section.items():
                    previous = merged[locale].get(key)
                    if previous is not None and previous != value:
                        print(f"[override] {locale} {key}: {previous!r} -> {value!r}")
                    merged[locale][key] = value
            print(f"merged fragment: {entry} -> {sorted(targets)}")

    for locale, entries in merged.items():
        os.makedirs(LANG, exist_ok=True)
        path = os.path.join(LANG, f"{locale}.json")
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(dict(sorted(entries.items())), fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        print(f"wrote {os.path.relpath(path, ROOT)} ({len(entries)} keys)")

    missing = [k for k in merged["en_us"] if k not in merged["ja_jp"]]
    if missing:
        print(f"[warn] ja_jp に無いキーが {len(missing)} 件: {missing[:10]}")


if __name__ == "__main__":
    main()

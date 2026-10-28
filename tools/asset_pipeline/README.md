# tools/asset_pipeline

NOCTERRA の AI アセット生成パイプライン（Python 3.12 / 標準ライブラリ主 + Pillow + numpy）。

**最初に読むもの: [`docs/ASSET_PIPELINE.md`](../../docs/ASSET_PIPELINE.md)**
（安全設計・確認済み事実 / 未確認の区別・検証ログ・承認待ち事項をすべて記載）

```powershell
cd C:\Users\2674469\Desktop\mcmod\tools
$py = "C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py -m asset_pipeline status
& $py -m asset_pipeline dry-run texture_stone_tile   # HTTP を送らない
& $py -m asset_pipeline.selftest                     # オフライン自己検証
```

- 既定は **DRY RUN**（`ASSET_DRY_RUN=true` / `ASSET_AUTO_SPEND=false`）。実課金は二重ゲート + `--confirm-spend` + 予算台帳の 4 段。
- API キーは `../../.env` からのみ読み、出力は常にマスクされる。
- 実行時状態は `state/`（内蔵 `.gitignore` で非コミット）。

# ASSETS.md — アセットの来歴と生成方法

> 本プロジェクトのアセットはすべて **本プロジェクト内で生成**したもの。
> 有料 API（OpenAI / Tripo / Fish Audio）は **実課金 0 件**（`ASSET_DRY_RUN=true` / `ASSET_AUTO_SPEND=false` のまま）。
> 現状のテクスチャ・モデルは **プレースホルダ**であり、本番品質への差し替えを前提としている。

## 1. 生成方法の区分

| 種別 | 生成方法 | 生成スクリプト | 出力先 |
|---|---|---|---|
| ブロック/アイテムのテクスチャ（16×16） | Python + Pillow による**手続き生成**（ノイズ・レンガ・渦・ルーン） | `tools/dev/gen_placeholder_textures.py`、`tools/dev/gen_block_assets.py` | `assets/nocterra/textures/{block,item}/` |
| 3D モデル（GeckoLib 形式） | **手書き JSON**（Blockbench GeckoLib 4/5 形式） | なし（手書き） | `assets/nocterra/geckolib/models/entity/` |
| アニメーション | **手書き JSON**（keyframe） | なし（手書き） | `assets/nocterra/geckolib/animations/entity/` |
| エンティティテクスチャ | Python + Pillow による手続き生成 | entity 担当 | `assets/nocterra/textures/entity/` |
| スポーンエッグ | Python + Pillow（卵シルエット＋斑点） | Lead / entity 担当 | `assets/nocterra/textures/item/*_spawn_egg.png` |
| 音声 | **未生成**（有料 API 未承認） | — | — |

## 2. モデル・アニメーションの検証

entity 担当が GeckoLib 5.5.7 の実 jar を用いて、**`Geometry.GSON` → `Geometry.bake()`** と
**`ActorAnimations.GSON` → bake** を通して検証済み（5 エンティティ）。
JSON 構文だけでなく、GeckoLib の実ローダで解釈できることを確認している。

| モデル | bones | cubes | アニメ数 |
|---|---|---|---|
| ashen_wraith | 12 | 12 | 5（idle/walk/attack/hurt/death） |
| rotwood_king | 14 | 14 | 8 |
| ash_crawler / mourning_wisp / bog_ghoul | （entity 担当検証済み） | — | 各 5 |

## 3. AI 生成パイプライン（Phase 2 実装・未使用）

`tools/asset_pipeline/` に生成パイプラインを実装済み。**実課金 0 件・DRY RUN のみ**。

- 実装: 環境設定の読込（`.env`）、予算台帳（SQLite・送信前 reserve）、ジョブキュー（指数バックオフ・
  リトライ上限・重複排除・中断復旧・レート制限）、OpenAI 画像 / Tripo / Fish のクライアント、
  テクスチャ加工（16/32/64 最近傍リサイズ・背景除去・パレット量子化・タイル性スコア）、
  GLB/PNG/音声/JSON の実体検証、来歴マニフェスト。
- 検証: `python -m asset_pipeline.selftest` → **92 passed / 0 failed**。
- **未検証**: 各 API の実レスポンス（課金テスト未実施）。料金体系も未確認のため、
  Tripo / Fish は単価未設定のまま＝**実送信は予算ガードが拒否する設計**。
- Tripo は現行 **v3**（`POST /v3/generation/text-to-model` など）であることを SDK 記載から確認。
  v2 の `/user/balance` は実測で生存（残高 600 credits）。

### 有料 API を使う場合の手順（未実施）

1. `.env` の `ASSET_DRY_RUN=false` / `ASSET_AUTO_SPEND=true` / `ASSET_MAX_COST_USD` を**ユーザー承認のうえ**変更
2. `ASSET_PRICE_JSON` 等で実単価を設定
3. `python -m asset_pipeline.run <job> --confirm-spend`
4. 生成物は `generated_assets/`（raw は git 管理外）。来歴は `generated_assets/manifests/` に記録
5. 加工後に `assets/nocterra/...` へ配置

## 4. ライセンス・利用条件

| 対象 | 条件 |
|---|---|
| GeckoLib（依存） | **MIT**。JAR には同梱せず、プレイヤーが別途導入する前提 |
| 本プロジェクトの生成テクスチャ | 本プロジェクト内生成（第三者の権利を含まない） |
| 本プロジェクトのモデル/アニメ JSON | 本プロジェクト内で作成 |
| 将来 AI 生成するアセット | **未確認（UNVERIFIED）**。商用利用条件は各サービスの規約確認が必要 |
| 音声 | 未生成。第三者の声のクローンは禁止（仕様書 10.3） |

## 5. 未実施・TODO

- [ ] 本番品質テクスチャ（現在は手続き生成のプレースホルダ）
- [ ] 音声（NPC 会話・ボス咆哮）の生成と OGG 変換
- [ ] Tripo による 3D モデル生成 → GeckoLib 形式への変換検証（**スキニング互換性は未検証**）
- [ ] 生成物のライセンス確認と manifest への記録

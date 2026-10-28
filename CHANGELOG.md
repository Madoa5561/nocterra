# CHANGELOG

## [0.1.0-alpha] — 2026-10-28

開発初期の垂直スライス。設計書の全項目ではなく、「ノクテラに入り、神殿を探索し、ボスと戦い、
新しい武器を入手できる」ところまでを実装。

### Added — 基盤
- NeoForge 26.3.0.99 / Minecraft 26.3 / Java 25 / ModDevGradle 2.0.148 / Gradle 9.8.2 のビルド環境
- GeckoLib 5.5.7（`com.geckolib:geckolib-neoforge-26.3`）導入
- 開発用 run 構成: `runServer` / `runClient` / `runClientJoin`（127.0.0.1:25565 へ自動接続）/ `runData`

### Added — ワールド
- 独自ディメンション `nocterra:nocterra`（26.3 の `attributes` 形式の DimensionType）
- バイオーム `nocterra:ashen_forest` / `nocterra:mourning_marsh`
- カスタム地形フィーチャー: 古代神殿（15×15・全面屋根・入口 2×3・祭壇）/ 枯れ木 / 湿原植物
- ポータル: 古代の門（骨組み＋通過面）、点火、双方向転送、帰還門の自動生成、ポータルクールダウン

### Added — ブロック・アイテム
- ブロック: `gate_frame`, `ashen_portal`, `ashstone`, `ash_coal_ore`, `ashwood_log`, `ashwood_planks`, `rotwood_altar`
- アイテム: `ember_shard`（門の点火）、`ashwood_blade` / `grave_greatsword` / `mourning_scythe`
- スポーンエッグ 5 種、クリエイティブタブ NOCTERRA
- クラフトレシピ 6 種、ルートテーブル 11 種、言語ファイル（en_us / ja_jp）

### Added — エンティティ・戦闘
- `ashen_wraith`（近接＋ブリンク）、`ash_crawler`（高速近接）、`mourning_wisp`（魂吸い）、
  `bog_ghoul`（範囲攻撃）、`rotwood_king`（ボス・2フェーズ・予兆攻撃・召喚・専用HPバー）
- 戦闘基盤: スタミナ／魔力（Attachment）、回避・パリィ・強攻撃・コンボ、武器戦技、HUD、キーバインド
- ネットワーク: C2S 4 種 / S2C 1 種の payload

### Added — 開発基盤
- `/nocterra info|gate|probe|entities` と `/nocterra dev ...`（ボスAI・各モブ能力の検証用）
- `tools/asset_pipeline`（AI アセット生成パイプライン、DRY RUN のみ）
- `tools/dev`（テクスチャ生成・ブロック JSON 生成・lang マージ）
- `docs/api/`（GeckoLib 5.5.7 / NeoForge 26.3 の実 API 検証メモ）

### Verified
- 専用サーバー起動、クライアント起動、GeckoLib のモデル読込（2 モデル/2 アニメのロードをログで確認）
- ディメンション生成、バイオーム配置（`locate biome`）
- **実プレイヤーでのポータル往復**（オーバーワールド→ノクテラ、ノクテラ→オーバーワールド）
- 古代神殿の**自然生成 37 チャンク**（セーブの region ファイル走査で確認）
- ボス・モブの召喚、モデル描画、ボスHPバー、HUD、アイテムアイコンの描画（スクリーンショットで確認）
- ボスのフェーズ強制（`phase=2`, `phaseLocked=true`）とミニオン召喚の成功

### Known Issues / Not Implemented
- 攻撃の実ダメージ・被弾演出・フェーズ自動移行の実挙動は未検証（自動マウス操作不可のため）
- MB 単位の性能計測は未実施（docs/PERFORMANCE.md）
- 設計書の大部分（8 地域 / 12 ボス / 60 武器 / NPC / クエスト / エンディング等）は未実装
- テクスチャ・モデルは開発用プレースホルダ

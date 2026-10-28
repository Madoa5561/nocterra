# PROGRESS.md — NOCTERRA 開発進捗

最終更新: 2026-10-28（Phase 1 完了時点）

> 表記: ✅実施して確認済み / ⚠️一部のみ / ❌未実施（成功扱いしない）

## 環境（確定事実）

| 項目 | 値 | 確認方法 |
|---|---|---|
| Java | 25.0.5.0.2（実行に使用）/ 21.0.12.1（NFRT ツール用に導入・登録） | `java -version` |
| Gradle | 9.8.2（Wrapper） | `gradlew --version` |
| NeoForge | **26.3.0.99** | maven-metadata `release` |
| ModDevGradle | **2.0.148** | maven-metadata |
| GeckoLib | **5.5.7** `com.geckolib:geckolib-neoforge-26.3` | Modrinth API + cloudsmith |
| Minecraft | **26.3**（world_version 5023 / protocol 777 / pack resource 97.1, data 121.0 / java_version 25） | server jar の `version.json` |
| GPU/CPU | Snapdragon X Plus 8C / RAM 15.6GB / x64 JVM（ARM64 ホスト上のエミュレーション） | `java -XshowSettings` |

## Phase 0: 環境構築 — ✅ 完了

| 作業 | 状態 | 証拠 |
|---|---|---|
| Java 25 確認 | ✅ | `java -version` |
| Gradle Wrapper 生成 | ✅ | sha256 一致 |
| 最小 MOD ビルド | ✅ | `BUILD SUCCESSFUL in 5m 45s`（初回） |
| 専用サーバー起動 | ✅ | `Done (3.947s)!` / Mod List に NOCTERRA + GeckoLib 5.5.7 |
| runClient 起動 | ❌ 未実施 | ヘッドレス環境のため。**未検証**（ARM64 ホストのため要確認） |
| GeckoLib 導入 | ✅ | サーバー起動ログで geckolib 5.5.7 がロード |
| Git 初期化 | ✅ | コミット 3 件 |
| ドキュメント | ✅ | SPEC / ARCHITECTURE / PROGRESS / AGENTS / DEV_NOTES / api 2 本 |

※ `downloadAssets` が Java 21 ツールチェーンを自動DLしようとして停止する問題を発見。
  → Temurin 21 をローカル導入し `org.gradle.java.installations.paths` に登録して解決（再現手順は本ファイルに記載の通り）。

## Phase 1: ディメンション基盤 — ✅ 完了（実機検証済み）

| 作業 | 状態 | 証拠 |
|---|---|---|
| 独自ディメンション `nocterra:nocterra` | ✅ | `/nocterra info` → `dimension=nocterra:nocterra`。`dimensions/nocterra/` が保存された |
| 独自 DimensionType | ✅ | `dimension_type/nocterra.json`（26.3 の `attributes` 形式、cloud_color は 8桁ARGB） |
| バイオーム 灰樹の森 | ✅ | `locate biome nocterra:ashen_forest` → 0,70,0 |
| バイオーム 嘆きの湿原 | ✅ | `locate biome nocterra:mourning_marsh` → 256,70,-416 |
| 独自ブロック（骨組み/通過面/灰石/灰炭鉱石/灰木） | ✅ | 設置・`probe` で確認、テクスチャ/モデル/lang 生成済み |
| ポータル点火（エンバーシャード） | ⚠️ 形状判定は実装・検証済み／**実プレイヤーのitem use は未検証**（クライアント無し） | `/nocterra gate` が同一の形状判定経路を通る |
| ポータル転送（往路） | ✅ | 牛をOWの門に入れ → `nocterra:nocterra` に出現（`nocterra entities cow`） |
| 帰還門の自動生成 | ✅ | 到着地点に `gate_frame` + `ashen_portal` が生成（`probe` で確認） |
| ポータル転送（復路） | ✅ | 牛をノクテラの門に入れ → オーバーワールドに出現（カウント 1→2） |
| ワールド保存・再起動 | ✅ | `save-all` → `stop` → 再起動後も dimensions/nocterra が存在し生成継続 |
| クラフト/ドロップ | ⚠️ JSON 作成済み・**サーバーでのパース検証は未実施** | recipe 3 / loot_table 5 |

### Phase 1 で実測した 26.3 の落とし穴（解決済み）
1. `DimensionType` は record+`attributes`。必須キーは `DimensionType.createDirectCodec` の通り。`cloud_color` は **8桁ARGB**（6桁だとパース失敗）。
2. バイオーム JSON の `minecraft:audio/background_music` に存在しない sound id（`music.overworld.dark_forest`）を書くと**レジストリ全体のロードが失敗**する。
3. 26.3 に `data/minecraft/dimension/*.json` は無いが、`data/<ns>/dimension/<name>.json` は `RegistryDataLoader.DIMENSION_REGISTRIES` 経由で**読み込まれる**（実証済み）。
4. RCON の `execute in <dim> run <cmd>` はコマンドのレベル文脈を変えるが、**`@e` セレクタはディメンションで絞られない**。ディメンション検証は `/nocterra entities`（`level.getAllEntities()`）を使うこと。

## Phase 2: AIアセットパイプライン — ⚠️ 実装完了・実課金0

`tools/asset_pipeline/` に CLI 付き実装（DRY RUN / 予算台帳 / キュー / 再開 / 検証 / 来歴）。
- `python -m asset_pipeline.selftest` → 92 passed / 0 failed（実課金なし）
- tripo / openai / fish の実リクエストは**未送信**（予算ガードで停止する設計）
- 未確認: 各APIの実レスポンス形式・料金。Tripo は v3 が現行（SDK の記載）

## Phase 3: 初期戦闘・ボス — 進行中（担当: entity-dev / combat-dev / worldgen-dev）

| 項目 | 状態 |
|---|---|
| モブ・ボス（GeckoLib モデル/アニメ/AI） | entity-dev 実装中 |
| 武器・スタミナ・回避・パリィ | combat-dev 実装中 |
| ダンジョン（古代神殿）・祭壇 | worldgen-dev 実装中 |

## 既知の問題・リスク

- **runClient 未検証**: ヘッドレス環境。ARM64 ホストで LWJGL が動くかは未確認。クライアント側（HUD・レンダラ・キーバインド）は
  「ビルドが通る」以上の保証が現状ない。
- クライアント専用コードの実行時検証は、GUI 環境が用意できない限り **未実施**と記録する。
- 生成テクスチャは**プレースホルダ**（ローカル生成）。asset_pipeline による本番アセットは未生成（課金未承認）。
- 26.3 のポータルは `Portal` 実装 + `TeleportTransition`。バニラの `NetherPortalBlock` の遷移時間制御は
  `getPortalTransitionTime` で行われるが、こちらは既定 0（即時）。プレイヤー体験は未調整。

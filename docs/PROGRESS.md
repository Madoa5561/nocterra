# PROGRESS.md — NOCTERRA 開発進捗

最終更新: 2026-10-28（第3ラウンド終了時点 / コミット `c4d9439`）

> 表記: ✅実施して確認済み / ⚠️一部のみ / ⬜未実施（成功扱いしない）

## 第3ラウンドの追加（ユーザー要望: ボスHPバーの豪華化 / 本番アセット / 残りボス / 構造物の堅牢性）

| 要望 | 結果 |
|---|---|
| ボスHPバーを豪華に | ✅ 独自描画（金属枠・金の角飾り・脈動オーラ・グラデーション・流れる光沢・ノッチ・**ボス別ポートレート**・フェーズ表示）。実機で赤/青バーの積み重ねと個別ポートレートを確認 |
| 本番アセット生成 | ✅ 実課金 **$2.67612**（OpenAI $1.47 / Tripo $1.20 / Fish $0.006）。ボス12体のポートレート・ブロック/アイテムテクスチャ・音声24本を生成し MOD へ統合。⚠️ 音声は**再生未解決**（下記） |
| 残りのボス | ✅ **10 体追加で計 12 体**。全員ビルド通過・GeckoLib bake・ゲーム内 `/summon` と AI tick を確認 |
| 要塞・ダンジョンの生成堅牢性 | ✅ **チャンク境界で構造物が消える重大バグを特定・修正**。新シード監査で構造破壊 **20件 → 0件**。要塞（24×24）/ 地下墓所（15×15 地下）を新規実装し `/place` の実ブロックを検証 |

### 12 ボス一覧（すべて実装済み）

| # | registry ID | 表示名 | バー色 | フェーズ | 実機確認 |
|---|---|---|---|---|---|
| 1 | rotwood_king | 腐朽の森王 | PURPLE | 2 | ✅ 描画/HPバー/攻撃ダメージ/フェーズ移行 |
| 2 | sorrow_saint | 悲嘆の聖女 | BLUE | 2 | ✅ 描画/HPバー |
| 3 | condemned_knight_lord | 断罪の騎士長 | RED | 2 | ✅ 描画/HPバー/summon |
| 4 | sandglass_twin_kings | 砂時計の双王 | YELLOW | 2 | ✅ summon（2体同時）/AI tick |
| 5 | abyssal_sea_emperor | 深淵の海帝 | BLUE | 2 | ✅ 描画/HPバー/summon |
| 6 | astel_fallen_star_dragon | 墜星竜アステル | PURPLE | 2 | ✅ summon/AI tick |
| 7 | silver_executioner | 白銀の処刑者 | WHITE | 2 | ✅ summon/AI tick |
| 8 | molten_steel_colossus | 灼鋼の巨神 | RED | 2 | ✅ summon/AI tick |
| 9 | black_crowned_king | 黒冠の王 | PURPLE | 3 | ✅ 描画/HPバー/summon |
| 10 | ember_incarnation | 灯火の化身 | YELLOW | 3 | ✅ summon/AI tick |
| 11 | forgotten_knight | 忘却された騎士 | WHITE | 2 | ✅ summon/AI tick |
| 12 | nameless_observer | 名もなき観測者 | PINK | 2 | ✅ summon/AI tick |

⚠️ 「描画/HPバー」列が無いボスは **モデル描画を目視確認していない**（`/summon` と `debugStatus` による AI tick のみ確認）。
画面キャプチャが他アプリの前面化と競合して安定しなかったため。**描画は未検証**として扱うこと。

### 構造物の生成堅牢性（実測）

| 検証項目 | 結果 |
|---|---|
| 原因特定 | 26.3 では FULL になった隣チャンクが `ImposterProtoChunk(allowWrites=false)` になり、**境界を越えた書き込みが例外も出ず捨てられる**。神殿がチャンク境界で切断されていた |
| 修正 | 書き込みを中心チャンク内に完全固定（中心を chunkStart+8 に固定、±7 のみ書く） |
| 神殿の再監査（新シード 123456789・自然生成 61 個） | **roof_holes 0 / wall_broken 0 / floor_missing 0 / entrance_blocked 0 / nearby_temple(重複) 0**（修正前は 46 個中 20 個が破損） |
| 残存する `buried`(24) | すべて**葉**が屋根の上に被さるだけで構造は無傷（バニラのチャンク境界越えの装飾挙動） |
| 要塞 `fallen_fortress`(24×24) | ✅ `/place` で origin (816,816)/baseY 67 に生成。外壁連続・四隅の塔・北門 3×4 開口・中庭・階段→地下牢（密閉・グロウストーン5）をブロック単位で照合、ブロック数も期待値と一致 |
| 地下墓所 `mourning_catacomb`(15×15 地下) | ✅ origin (832,832) に生成。地表 3×3 開口・12 段階段・防水シェル・石棺・明かり5 を確認。水没地でも入口を海面より上に上げる方式へ緩和（**緩和後の水没地生成は未検証**） |
| 自然生成 | ⬜ seed2（`-987654321`）を生成済み。**監査は worldgen 担当が実施中（結果未着）** |

### 本番アセット（実課金・ユーザー承認済み）

- 合計 **$2.67612**（上限 $5/サービス）: OpenAI 30 枚 $1.47 / Tripo 2 タスク $1.20（**残高 600→540 で実測**）/ Fish 24 本 $0.00612
- ボス 12 体のポートレート（128×128）は**実際に MOD のボスバーに反映**（クライアント起動ログにスプライト欠落なし）
- 詳細は [ASSETS.md](ASSETS.md)
- ⚠️ **音声は未解決**: 24 本の OGG と `sounds.json` を同梱したが、クライアントが
  `Missing sound for event: nocterra:boss_<id>_voice<n>` を 24 件出し、**再生されない**。原因未特定（26.3 のサウンド解決仕様を再調査する必要）。
- ⚠️ Tripo の GLB→GeckoLib 変換は**成功したが実用外**（静的 1 ボーン・約1,850 cubes）。ゲーム内では未使用。



## 実機（クライアント接続）で確認したこと

| 項目 | 状態 | 証拠 |
|---|---|---|
| `runClient` / `runClientJoin` の起動 | ✅ | OpenGL 4.6 / LWJGL 3.4.3、タイトル画面とワールド内描画 |
| ポータル往復（オーバーワールド⇄ノクテラ） | ✅ | 実プレイヤーで双方向。`nocterra entities player` でディメンション移動を確認 |
| ノクテラの空・霧（DimensionType attributes） | ✅ | 紫の空・暗い霧を実写確認 |
| ボス `rotwood_king` のモデル描画 | ✅ | 実写確認 |
| ボス HP バー（紫・「腐朽の森王」） | ✅ | 実写確認 |
| スタミナ（緑）/ 魔力（青）HUD | ✅ | 実写確認 |
| 全アイテムのアイコン描画 | ✅ | 実写確認（26.3 の `assets/<ns>/items/*.json` 追加後） |
| 古代神殿の壁面・屋根の描画 | ✅ | 実写確認 |
| ボス自然 AI の攻撃がプレイヤーに命中 | ✅ | HP 20 → 8 → 0（約4秒で死亡） |
| **フェーズ自動移行（HP50%以下）** | ✅ | `/damage` 160 で HP 300→140 → 1秒以内に `phase=2 phaseLocked=false` |
| 腐敗の新星（ボス AoE）のダメージ | ✅ | 抵抗Lv4 状態で HP 20 → 17.93 |
| 嘆きの亡霊の魂吸い / 沼の屍鬼の墓場の拳 | ✅ | 予兆 `windup=20` / `16` 開始後、プレイヤー HP が 20 → 15.2 に減少 |
| 亡霊のブリンク | ⚠️ | 距離 17 ブロックでは `false`。近距離での再検証が必要 |

### 計測・再現の注意（次回のために）

- `resistance` の amplifier 4（Lv5）は **100% 軽減＝完全無効**（`/damage` が "invulnerable" を返す）。Lv4 以下で計測する。
- `regeneration` を付けると数秒で回復して差分が消える。計測時は付与しない。
- クリエイティブでは `Mob#asValidTarget` が対象を弾くため、戦闘検証は必ず**サバイバル**で行う。
- RCON からの `/nocterra dev ...` は `execute in <dim> positioned <x> <y> <z> run ...` を付ける。
  ソース位置がワールドスポーン（0,?,0）のままだと探索半径 64 を外れて「nearby が無い」になる。
- 直したバグ: `debugSetAttackWindup(0)` が予兆 0 のため攻撃が走らなかった → **0 を渡すと次 tick 発動（最低 1）**に丸めるよう修正。

### マルチプレイ検証（第2ラウンド・実測）

`runClientJoin`（Dev）と `runClientJoin2`（Dev2、`--username Dev2`）で**同時 2 クライアント**を接続した。

| 確認項目 | 結果 | 証拠 |
|---|---|---|
| 2 クライアント同時接続 | ✅ | `There are 2 of a max of 8 players online: Dev, Dev2` |
| 同ディメンションへの同時入場 | ✅ | 両者を `nocterra:nocterra` へ tp して `nocterra entities player` で確認 |
| サーバーが落ちない / 例外なし | ✅ | 2 クライアント接続中のサーバーログで `Exception|ERROR` **0 件** |
| プレイヤーごとの状態の独立 | ✅ | 片方だけがダメージを受ける（Dev2: HP 20→15.7、他方は変化なし） |
| 自然スポーンがバイオーム設定どおり動く | ✅ | 走査中に `ash_crawler` / `ashen_wraith` が自然出現（126〜235 体） |
| 完全な「2人同時の戦闘同期」の HUD 目視 | ⚠️ | 検証中に片方のプレイヤーが死亡状態（HP 0 のまま死亡画面）になり、
攻撃対象の選別（クリエイティブ除外）を完全には分離できなかった。**HUD の目視同期は未検証**。

> 補足: プレイヤーの死亡状態はサーバーのプレイヤーデータに残り、`/gamemode` でも復活しない。
> 復活させるには「死亡画面でRespawnを押す」か「クライアント再接続」が必要。
> 自動マウス操作ができないため、この検証では再接続で対応した（片方は復活できず）。

## サーバー単体で確認したこと

- ✅ ビルド成功 / 専用サーバー起動（`Done (2.9s)!`、NOCTERRA + GeckoLib 5.5.7 ロード）
- ✅ ディメンション・バイオームの生成（`locate biome`）
- ✅ 古代神殿の**自然生成 37 チャンク**（セーブの region ファイルを走査。手動 `/place` 分を除く）
- ✅ フェーズ強制（`phase=2`, `phaseLocked=true`）/ ミニオン召喚 / `debugStatus` が AI の tick を反映
- ✅ ボス死亡時のドロップ生成、ルートテーブルのパース
- ✅ 6 エンティティすべて `/summon` 成功・登録例外 0

### 配布 JAR の本番サーバー配備テスト（第2ラウンド・実測）

開発クラスパスではなく、**純正 NeoForge 26.3.0.99 専用サーバーを `--installServer` で新規導入**し、
`mods/` に `nocterra-0.1.0-alpha.jar` と `geckolib-neoforge-26.3-5.5.7.jar` を置いて起動した。

| 確認項目 | 結果 | 証拠（ログ） |
|---|---|---|
| 本番モードで起動 | ✅ | `Failed to find common Minecraft classes... Assuming we're launching production.` |
| 両 MOD が jar から認識 | ✅ | Mod List: `GeckoLib 5 5.5.7 (geckolib)` / `NOCTERRA 0.1.0-alpha (nocterra)` |
| MOD 初期化 | ✅ | `NOCTERRA combat bootstrap: attachments, data components, weapons and combat handlers registered` / `bootstrap complete ... side=DEDICATED_SERVER` |
| サーバー完成 | ✅ | `Done (4.038s)!` |
| 独自コマンド | ✅ | `/nocterra info` が応答 |
| 独自ディメンション | ✅ | `nocterra:nocterra` が生成・保存され `forceload` 可能 |
| カスタムフィーチャー | ✅ | `Placed "nocterra:ancient_temple" at 0, 74, 0` |
| エンティティ登録 | ✅ | `Summoned new Rotwood King` / `Summoned new entity.nocterra.sorrow_saint` |
| 例外・ERROR | ✅ 0 件 | ログ内 `Exception|ERROR` の件数 0 |
| 正常停止 | ✅ | `Stopping the server` → 全ディメンションのチャンク保存 |

検証環境は `dist-test/`（`--installServer` で生成、約 154MB）。**`.gitignore` 済み**で配布物には含めない。


## 実装カタログ

| 分類 | 実装済み |
|---|---|
| ディメンション | `nocterra:nocterra`（26.3 の attributes 形式）、バイオーム 2（灰樹の森 / 嘆きの湿原） |
| 地形 | 古代神殿（Feature・自然生成確認）、枯れ木、湿原植物 |
| ポータル | 骨組み 2×3+ 判定 / 点火 / 双方向転送 / 帰還門自動生成 / クールダウン |
| ブロック | gate_frame, ashen_portal, ashstone, ash_coal_ore, ashwood_log, ashwood_planks, rotwood_altar |
| アイテム | ember_shard, ashwood_blade, grave_greatsword, mourning_scythe, スポーンエッグ 6 |
| エンティティ | ashen_wraith, ash_crawler, mourning_wisp, bog_ghoul, rotwood_king(ボス), sorrow_saint(ボス) |
| 戦闘 | スタミナ/魔力(Attachment), 回避, パリィ, 強攻撃, コンボ, 武器戦技, HUD, キーバインド, payload 5 種 |
| データ | レシピ 9, ルートテーブル 11, 言語 en/ja 43 キー |
| 開発基盤 | `/nocterra` コマンド群, `/nocterra dev ...`（AI 検証）, RCON 検証スクリプト, asset_pipeline(DRY RUN), lang マージ, テクスチャ生成 |

## 未達（設計書との差分・正直な記載）

8 地域のうち **6 地域未実装** / ボス 12 体中 **10 体未実装** / 武器 60 種中 **3 種** /
防具 0 / 遺物 0 / 魔法 24 種中 0 / ダンジョン 30 種中 1 / NPC 0 / クエスト 0 /
探索手帳・拠点・侵食・黒月イベント・ストーリー・エンディング・パーティー **全て未実装**。
マルチプレイは「専用サーバー起動＋クライアント1名接続」まで確認。**2名同時接続・再接続は未実施**。

## 未検証（環境制約）

- 武器での攻撃・ドッジ・パリィの**操作入力**（自動マウス操作が不可。GLFW が `mouse_event` を受け取らない）
- クラフトレシピの実クラフト（GUI 操作が必要）
- 亡霊のブリンク（近距離での再検証）
- 性能計測（MSPT / メモリ / パケット量）— [PERFORMANCE.md](PERFORMANCE.md)

## 環境（確定事実）

| 項目 | 値 |
|---|---|
| Java | 25.0.5.0.2（実行）/ 21.0.12.1（NFRT ツール用・`org.gradle.java.installations.paths` に登録） |
| Gradle | 9.8.2（Wrapper） |
| NeoForge | 26.3.0.99 |
| ModDevGradle | 2.0.148 |
| GeckoLib | 5.5.7（`com.geckolib:geckolib-neoforge-26.3`） |
| Minecraft | 26.3（world_version 5023 / protocol 777 / pack resource 97.1, data 121.0 / java_version 25） |
| 実機 | Snapdragon X Plus 8C / RAM 15.6GB / x64 JVM |

### 環境構築で踏んだ罠

1. `downloadAssets` が **Java 21 ツールチェーンを自動 DL しようとして停止**する → Temurin 21 を導入し
   `org.gradle.java.installations.paths` に登録 + `auto-download=false` で解決。
2. MDK の `main` は空（アーカイブ化）→ 公式 **ModDevGradle** の DSL で直接構築。
3. 26.3 の破壊的変更（詳細は [DEV_NOTES.md](DEV_NOTES.md) / [api/](api/)）:
   `ResourceLocation`→`Identifier` / `DimensionTransition`→`TeleportTransition` /
   `noCollission`→`noCollision` / バイオームの `spawners`→`attributes` /
   `pack_format`→`min/max_format` / **アイテムモデルは `assets/<ns>/items/*.json` が必須** /
   `configured_feature/`→`worldgen/feature/`。
4. RCON の `execute in <dim>` はコマンド文脈を変えるが **`@e` はディメンションで絞られない** →
   ディメンション検証は `/nocterra entities`（`getAllEntities`）を使う。
5. PowerShell の `Set-Content` 経由で日本語ファイルを書き換えると**文字化けする**ことがある
   （本ファイルで実際に発生→書き直した）。日本語を含むファイルはエディタ系ツールで書くこと。

## 既知の問題

- クリエイティブタブのページ数・並びは未整理
- ボスの攻撃は「プレイヤーがシム距離内にいる」前提でないと tick しない（遠隔 `/damage` ではフェーズ移行しない）
- ルーン付きの祭壇テクスチャは手続き生成のプレースホルダ
- `sorrow_saint` の浮遊移動は実装済みだが**実機での挙動確認は未実施**

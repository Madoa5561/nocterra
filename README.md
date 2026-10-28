# NOCTERRA — 灰冠の残響

Minecraft **Java Edition 26.3** / **NeoForge 26.3.0.99** / **Java 25** 向けのダークファンタジーRPG MOD（開発中）。

> 本リポジトリは **開発中のビルド** です。README の「実装済み / 未実装」を必ず確認してください。

## 動作環境

| 項目 | 値 |
|---|---|
| Minecraft | 26.3 |
| MOD Loader | NeoForge 26.3.0.99 |
| Java | 25 |
| 必須依存 | **GeckoLib 5.5.7**（`com.geckolib:geckolib-neoforge-26.3:5.5.7`） |
| サーバー | 専用サーバー対応（サーバー権威の戦闘・ポータル判定） |

## ビルド

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.2"
cd nocterra
.\gradlew.bat build
# 生成物: nocterra/build/libs/nocterra-0.1.0-alpha.jar
```

### 配布 JAR の実配備テスト（実施済み）

純正の **NeoForge 26.3.0.99 専用サーバー**（`java -jar neoforge-installer.jar --installServer` で導入）に
`mods/nocterra-0.1.0-alpha.jar` と `mods/geckolib-neoforge-26.3-5.5.7.jar` を置いて起動し、以下を確認済み:

- `Assuming we're launching production.`（開発環境ではなく本番モード）
- Mod List に `NOCTERRA 0.1.0-alpha` と `GeckoLib 5 5.5.7` が表示され、`Done (4.038s)!` で起動
- `/nocterra info` 応答、`nocterra:nocterra` ディメンション生成、`/place feature nocterra:ancient_temple` 成功、
  2 ボスの `/summon` 成功、ログ内の例外 **0 件**
- 停止も正常（`Stopping the server` → チャンク保存）

開発用起動:

```powershell
.\gradlew.bat runServer      # 専用サーバー（run/ にテストワールド）
.\gradlew.bat runClient      # クライアント
.\gradlew.bat runClientJoin  # 127.0.0.1:25565 のサーバーへ自動接続
```

初回は `nocterra/run/eula.txt` に `eula=true` が必要（このリポジトリの dev 環境では作成済み・`.gitignore` 対象）。

## インストール（プレイヤー向け）

1. NeoForge 26.3.0.99 を導入した Minecraft 26.3 を用意
2. `mods/` に **GeckoLib 5.5.7 (26.3 版)** と `nocterra-*.jar` を入れる
3. 起動

## 遊び方（実装済みの範囲）

1. **エンバーシャード**を作る: 石炭 + 火打石 + 金インゴット（作業台・シャップレス）
2. **古代の門の骨組み**を作る: 石レンガ 8 + エンバーシャード 1 → 8 個
3. 骨組みで **内側 2×3 以上の枠**を作り、**エンバーシャードを右クリック**して点火 → 「灰冠の門」が開く
4. 門に入ると **ノクテラ**（独自ディメンション）へ。到着地点には帰還用の門が自動生成される
5. ノクテラの **灰樹の森 / 嘆きの湿原** を探索。灰樹の森には **古代神殿**（15×15 の深層岩レンガ造り、内部に祭壇）が自然生成される
6. 神殿中央の **腐朽の祭壇**を右クリック → **ボス「腐朽の森王」**が目覚める（HP300 / 2フェーズ）
7. 武器（灰木の刃 / 墓所の大剣 / 嘆きの大鎌）で戦う。**R 回避 / V 強攻撃 / F パリィ / G 武器戦技 / C 魔法**

### キーバインド

| キー | 動作 |
|---|---|
| R | 回避（スタミナ 25） |
| V | 強攻撃（武器種別 15〜35） |
| F | パリィ |
| G | 武器戦技 |
| C | 魔法 |

設定画面で変更可能。

### 開発・検証用コマンド（権限レベル 2）

```
/nocterra info                    # 現在のディメンション情報
/nocterra gate <x> <y> <z>        # その位置に門を生成＋点火（形状判定も同一経路）
/nocterra probe <x> <y> <z>       # ブロック状態の確認
/nocterra entities [filter]        # そのディメンションのエンティティ一覧
/nocterra dev boss status|phase <1|2>|unlock|summon|attack <0-3>|windup <ticks>
/nocterra dev wraith blink|status
/nocterra dev wisp drain|windup <ticks>|status
/nocterra dev ghoul pound|windup <ticks>|status
```

## 実装状況

**実装済み（開発サーバー/クライアントで実動作を確認済み）**
- 独自ディメンション `nocterra:nocterra`、DimensionType（26.3 の `attributes` 形式）
- バイオーム 2 種（灰樹の森 / 嘆きの湿原）＋カスタム地形フィーチャー（古代神殿・枯れ木・湿原植物）
- **構造物**: 古代神殿（自然生成を 2 シードで検証）/ 亡国の城塞（24×24）/ 嘆きの湿原の地下墓所（15×15 地下）
  - チャンク境界で構造物が消えるバグを修正し、**新シード監査で構造破壊 0 件**を確認
- ポータル（点火・双方向転送・帰還門の自動生成・ポータルクールダウン）
- ブロック 7 種 / アイテム（エンバーシャード・武器 3 種・スポーンエッグ 16 種）
- **エンティティ 16 種（一般モブ 4 + ボス 12）**
- **全 12 ボス**: 2〜3 フェーズ・予兆付き攻撃・召喚・AoE・**専用の豪華 HP バー（枠・ポートレート・フェーズ表示）**
- 戦闘基盤: スタミナ・魔力・回避・パリィ・強攻撃・コンボ・武器戦技・HUD・キーバインド
- クラフトレシピ / ルートテーブル / 日本語・英語の言語ファイル
- **本番アセット**: AI 生成のボス 12 体ポートレート・ブロック/アイテムテクスチャ（OpenAI）、ボイス 24 本（Fish Audio）
  ※実課金 $2.67612（各サービス上限 $5）。音声は**同梱済みだが再生は未解決**

**未実装（仕様書に対して未達）**
- 8 地域のうち 6 地域、ボス 12 体中 11 体
- 武器 60 種、防具 12 セット、魔法・戦技 24 種、遺物 30 種
- ダンジョン 30 種、NPC、クエスト、探索手帳、拠点、侵食、黒月イベント、ストーリー、エンディング
- マルチプレイのパーティー機能

**検証済み（実測）**
- 専用サーバー・クライアント起動、ポータル往復、ディメンション生成、神殿の自然生成（37チャンク）
- ボス AI の攻撃ダメージ、HP50% でのフェーズ自動移行、AoE・召喚、モブの予兆攻撃
- 配布 JAR を純正 NeoForge 専用サーバー（`--installServer`）へ配備して起動
- クライアント 2 台同時接続（例外 0、プレイヤーごとのダメージ状態は独立）

**未検証（環境制約）**
- マウス操作が必要な項目: 武器での攻撃・回避・パリィ、アイテム使用、作業台でのクラフト
- `sorrow_saint` の浮遊挙動、亡霊ブリンクの近距離挙動
- 長時間プレイのメモリ・MSPT 計測（[PERFORMANCE.md](docs/PERFORMANCE.md) 参照）

## ドキュメント

| ファイル | 内容 |
|---|---|
| [docs/SPEC.md](docs/SPEC.md) | 仕様（実装対象の確定版） |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | アーキテクチャ・26.3 の仕様差分 |
| [docs/PROGRESS.md](docs/PROGRESS.md) | フェーズ進捗とテスト結果（実施/未実施の区別） |
| [docs/DEV_NOTES.md](docs/DEV_NOTES.md) | 実装前に必ず読む API ノート（実ソース検証済み） |
| [docs/ASSETS.md](docs/ASSETS.md) | アセット来歴・生成方法 |
| [docs/PERFORMANCE.md](docs/PERFORMANCE.md) | 性能計測（未実施項目を明記） |
| [docs/api/](docs/api/) | GeckoLib 5.5.7 / NeoForge 26.3 の実 API 検証メモ |

## ライセンス

コード: リポジトリ内の記載に従う。**GeckoLib は MIT**（依存として別途導入）。生成テクスチャは本プロジェクト内で生成したプレースホルダ。

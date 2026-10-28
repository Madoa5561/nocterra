# STORY.md — NOCTERRA 物語設計と実装状況

> 表記: ✅実装＋実機確認 / ⚠️実装済みだが実機未検証 / ⬜未実装

## 1. 世界観

かつて世界を照らしていた**始原の灯火**（Ancient Flame）が失われ、空に**灰冠**が昇って以来、
世界は「薄暮（トワイライト）」に沈んだ。滅びかけたこの世界が **ノクテラ（Nocterra）＝滅星界**。
時間は緩やかに止まり、死者は眠らず、八つの地域に**八人の冠の者**が君臨している。

プレイヤーは古代の門（`nocterra:gate_frame` ＋ エンバーシャード）をくぐり、ノクテラへ踏み込む。
目的は「帰ること」。しかし進むほどに、**この世界が滅びたのは誰かの意思ではなく、約束が果たされなかったから**
だという真実に近づいていく。

## 2. 物語の骨格

| 段階 | 内容 | 実装 |
|---|---|---|
| 序章 | 古代の門をくぐる。**進捗「滅星界ノクテラ」**が解除される | ✅ |
| 第一幕 | 灰樹の森・嘆きの湿原を探索し、神殿と遺構を見つける | ✅ |
| 第二幕 | **八人の冠の者**を討ち、**王の刻印**を集める | ✅ |
| 第三幕 | 刻印 **5 個**で通常エンディング、**8 個**で真エンディング | ✅ |
| 終幕 | 黒冠の玉座で結末を見る。世界は残り、探索は続けられる | ✅ |

### 八人の冠の者と刻印

| 地域 | 冠の者 | 刻印 | 状態 |
|---|---|---|---|
| 灰樹の森 | 腐朽の森王 | 朽葉の刻印 | ✅ |
| 嘆きの湿原 | 悲嘆の聖女 | 悲嘆の刻印 | ✅ |
| 亡国の城塞 | 断罪の騎士長 | 断罪の刻印 | ✅ |
| 紅砂の荒野 | 砂時計の双王 | 双砂の刻印 | ✅ |
| 深淵の海 | 深淵の海帝 | 深淵の刻印 | ✅ |
| 星骸の浮島 | 墜星竜アステル | 墜星の刻印 | ✅ |
| 永劫の氷原 | 白銀の処刑者 | 白銀の刻印 | ✅ |
| 黒鉄の火山 | 灼鋼の巨神 | 灼鋼の刻印 | ✅ |

> 各地域のバイオーム／地形は未実装のものがある（現在は灰樹の森・嘆きの湿原・黒冠遺跡の 3 種）。
> ボスと刻印は全 8 種が存在し、`/summon` で召喚して入手できる。**地域側の実装は今後の課題**。

## 3. エンディング

黒冠の玉座（`nocterra:black_crown_throne`、黒冠城の最深部）で刻印数を判定する。

### 通常エンディング「薄暮は終わる」（刻印 5 個以上）
> 冠が緩む。永遠の薄暮が、その端から薄れていく。
> 滅びた王国が、ひと息だけ、その名を思い出す。
> あなたは家へ帰れる。だがノクテラは、ここに残る。

### 真エンディング「灯火の化身」（刻印 8 個）
> 八つの刻印が掌で炭火のように燃える。黒冠に罅が走る。
> 冠の下にあったのは王ではなく、世界が忘れた約束だった。
> 始原の灯火は失われてはいなかった。薄暮の中に隠されていたのだ。
> あなたはそれを運び出す。ノクテラは変わり、再び花咲くだろう。あなたも。

- エンディング後もワールドは継続（到達不能にならない）。再訪時は「この結末は既に見届けている」と表示。
- 演出: タイトル／サブタイトル／ナレーション（チャット）／専用効果音（END_PORTAL_SPAWN、真EDは WITHER_DEATH も）／パーティクル（END_ROD・SOUL_FIRE_FLAME）

## 4. 進行データ（サーバー権威）

`ProgressionData`（AttachmentType `nocterra:progression`、`copyOnDeath`）:

| 項目 | 内容 |
|---|---|
| `sigilMask` | 刻印ビットマスク（8 bit） |
| `defeatedBosses` | 撃破したボスの ID 集合 |
| `discoveredNpcs` | 出会った NPC の ID 集合（NPC 実装と連携予定） |
| `discoveredPois` | 発見した地点（未使用） |
| `completedQuests` | 完了クエスト（クエスト実装と連携予定） |
| `normalEndingSeen` / `trueEndingSeen` | エンディング閲覧フラグ |

刻印の入手経路:
1. **ボス撃破時**に自動付与（とどめのプレイヤー、いなければ 64 ブロック内のプレイヤー全員）
2. **刻印アイテムを右クリック**で「同調」（ドロップ品・`/give` で入手した場合）
3. 開発用: `/nocterra story sigils`

刻印アイテムは各ボスのルートテーブルにも直接追加してある（`data/nocterra/loot_table/entities/<boss>.json`）。

## 5. 探索手帳

`nocterra:explorer_journal`（右クリック）で、刻印の収集状況・撃破数・出会った魂・完了した依頼・
玉座の解放状況をチャットに整形表示する。

> 26.3 の `Screen` API は描画方式が刷新されており（`Screen#render(GuiGraphics)` が存在しない）、
> カスタム GUI の実装リスクが高いため、**確実に動くチャット形式＋バニラ進捗画面**で代替している。
> 進捗（Advancements）は `data/nocterra/advancement/` に 10 件（root・刻印8種・全刻印）。

## 6. NPC とクエスト（担当: story-dev・進行中）

- 6 人の NPC（灰の番人／湿原の未亡人／盲目の書記／門番／灰の商人／迷い子）
- データ駆動ダイアログ（`data/nocterra/dialogue/*.json`）とクエスト（`data/nocterra/quest/*.json`）10 本
- 会話・クエスト進行はサーバー権威、`ProgressionData.discoveredNpcs` / `completedQuests` に記録予定

**現状**: 実装中（コンパイルエラー修正中）。**未実機検証**。

## 7. 実装ファイル

| 種別 | パス |
|---|---|
| 刻印定義 | `progression/SigilType.java` |
| 進行データ | `progression/ProgressionData.java`, `ProgressionAttachments.java` |
| ボス撃破フック | `progression/ProgressionEvents.java` |
| エンディング | `progression/EndingHandler.java` |
| 玉座ブロック | `world/structure/BlackCrownThrone.java` |
| 刻印アイテム | `item/SigilItem.java` |
| 手帳アイテム | `item/ExplorerJournalItem.java` |
| 登録 | `registry/NocterraStoryItems.java`, `NocterraBlocks.BLACK_CROWN_THRONE` |
| 開発コマンド | `core/NocterraStoryCommands.java`（`/nocterra story ...`） |
| 進捗 | `data/nocterra/advancement/root.json` ほか 10 件 |
| 言語 | `lang/fragments/story.json` / `story.ja_jp.json` → merge 済み |

## 8. 検証コマンド

```text
/nocterra story status     # 刻印数・撃破数・エンディング閲覧状況
/nocterra story sigils     # 全刻印を付与（開発用）
/nocterra story clear      # 進行データをリセット
/nocterra story ending normal
/nocterra story ending true
```

## 9. 未実装・未検証（正直な記載）

- ⬜ 6 地域分のバイオーム／地形（ボスと刻印は存在するが、地域そのものは未実装）
- ⚠️ NPC・会話・クエスト（story-dev 実装中、未ビルド・未検証）
- ⚠️ 玉座の右クリック操作の実機確認（エンディング処理自体はコマンドで検証予定）
- ⬜ 黒冠城の自然生成（構造物は実装済み・`/place` 検証待ち）
- ⬜ 物語の分岐（選択による結末の変化）、NPC の個別イベント、隠し要素の詳細

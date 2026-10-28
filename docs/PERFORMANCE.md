# PERFORMANCE.md — 性能計測と最適化方針

> 現状: **計測は未実施**（サーバー起動時のログ確認のみ）。以下は実装上の方針と、実施予定の計測手順。
> 数値を捏造しない。計測したら日時・環境・コマンドとともに追記する。

## 計測環境

- CPU: Snapdragon X Plus 8-core / RAM 15.6GB / Windows 11
- JVM: OpenJDK 25.0.5.0.2（ModDevGradle の runServer）
- ワールド: `run/nocterra_test`（シード `nocterra_dev_001`、専用テストワールド）

## 実装上の方針（コードで担保していること）

| 項目 | 方針 | 実装箇所 |
|---|---|---|
| ポータル転送 | 到着地点の探索は 1 回 64 ブロック分のみ。転送時のみ実行 | `AshenPortalShape.findOrCreateLanding` |
| ポータル形状判定 | 起動時＝点火時に 1 回だけ。毎 tick 走査しない | `AshenPortalShape.findAnyShape` |
| ポータルの自己修復 | `updateShape`（隣接ブロック変化時）のみ。tick 走査なし | `AshenPortalBlock.updateShape` |
| ブロック更新フラグ | 通過面生成は `flag 18`（近傍更新を抑制） | `AshenPortalShape.createPortalBlocks` |

## 実施予定の計測（未実施）

1. **チャンク生成時間**: 新規ワールドで `forceload add` を段階的に広げ、`/tick` 関連コマンドとログの `Done` 時刻を比較
2. **MSPT**: サーバーでプレイヤー相当の負荷（mob 30 体 + ボス 1 体）を置き、`/debug` ではなくログの tick 時間で評価
3. **メモリ**: 起動直後 / 10 分後 / 30 分後のヒープ使用量（`jcmd <pid> GC.heap_info`）
4. **ネットワーク**: payload の送信回数（毎 tick 送っていないことの確認）
5. **GeckoLib モデル**: ボスのモデルのキューブ数と同時描画数

## 既知の性能リスク（未計測）

- `AshenPortalBlock.randomTicks()` を付けているが `randomTick` 実装が無い（無駄なランダムティック登録）。
  → 削除候補。
- ディメンション地形は `minecraft:overworld` の noise settings を流用しているため、生成コストはバニラ同等。
  8 バイオーム展開時に `multi_noise` のパラメータ数が増えると RTree 検索コストが上がる。

# ASSETS.md — アセットの来歴と生成方法

> 本プロジェクトのアセットは、**手続き生成**と**有料 API による本番生成**の 2 種類がある。
> 有料 API は**ユーザー承認のうえ実行済み**（各サービス上限 $5/サービス、実績 **$2.67612**）。
> ライセンス条件は各サービスとも `UNVERIFIED`（manifest に記録）。

## 1. 本番生成（実課金・2026-10-28）

| サービス | 件数 | 実額（ledger） | 内容 |
|---|---|---|---|
| OpenAI Images（`gpt-image-2.5-sunburst`） | 30 | **$1.4700** | ボス 12 体のポートレート、ボスバー枠、ブロック/アイテムテクスチャ |
| Tripo3D（v3 text/image-to-model） | 2 | **$1.2000** | ボス 2 体の GLB（**残高 600→540 credits で実測**＝30 credits/タスク） |
| Fish Audio（TTS） | 24 | **$0.00612** | ボス 12 体 × 2 セリフ（日本語・オリジナル声） |
| **合計** | **56** | **$2.67612**（上限 $5） | 失敗 0 / 二重課金 0 |

※ OpenAI / Fish の実額は**運営者単価表による推定**で、請求明細との突合は未実施。Tripo は残高で実測。

### 生成物と配置

| 種別 | 生成物 | 配置先（MOD リソース） |
|---|---|---|
| ボスポートレート | `generated_assets/processed/hud/boss_portrait/*.png`（128×128 RGBA） | `assets/nocterra/textures/gui/sprites/boss_portrait/` |
| ボスバー枠 | `.../hud/boss_frame.png`（512×128, 9-slice 可） | **未使用**（描画はプリミティブ実装のため。差し替え候補） |
| ボスバー塗り | `.../hud/boss_bar_fill.png`（256×32, ローカル生成 $0） | **未使用**（同上） |
| ブロックテクスチャ | `.../textures/block/*.png`（8 種, 16×16, ashen_portal のみ 32×32） | `assets/nocterra/textures/block/` |
| アイテムテクスチャ | `.../textures/item/*.png`（4 種, 16×16 透過） | `assets/nocterra/textures/item/` |
| ボス音声 | `.../audio/boss/<boss_id>_<1|2>.ogg`（Ogg Vorbis, 3.2〜5.8s） | `assets/nocterra/sounds/boss/` + `assets/nocterra/sounds.json` |
| Tripo GLB | `.../geckolib/*.glb`（11,931 / 11,934 tri） | **未使用**（参照用に保存） |
| Tripo ボクセル | `.../geckolib/*.geo.json`（32 解像度, 1,847/1,859 cubes） | **未使用**（静的 1 ボーン・アニメなし） |

### Tripo → GeckoLib 変換の結論

- GLB 取得と**ボクセル化 → `.geo.json` 生成自体は成功**（構造検証 PASS）。
- ただし**スキニング/アニメーションは変換できない**（静的 1 ボーン）。
  32 解像度で約 1,850 cubes は Minecraft のエンティティ描画には重すぎる。
- → **ゲーム内では使用しない**（手書きの 13〜16 キューブモデルを採用）。成功と偽らず「制約あり」と記録。

## 2. 音声の実装状況（解決済み）

- 24 本の OGG は **jar に同梱**、`sounds.json` は 24 イベント分、`SoundEvent` も登録済み。
- **解決済みのバグ**: `SoundEvent.createVariableRangeEvent(...)` に登録キーと**別の ID** を渡していたため、
  `SoundEngine.reload()` の `soundManager.getSoundEvent(sound.location())` が解決できず、
  クライアント起動時に `Missing sound for event` が 24 件出ていた。
  → **SoundEvent の location を登録キー（`nocterra:boss_<id>_voice<v>`）と一致**させて解決。
  再起動後のクライアントログで **`Missing sound` 0 件**を確認済み。
- ボス出現時に `voice1` を再生するコード（`NocterraBossVoice`）が動作する。
- **音が実際に聞こえるか（音量・長さ・内容）は未検証**（自動化では聴取できない）。形式・解決・送信経路までは確認済み。

## 3. 手続き生成（$0・ローカル）

| 種別 | スクリプト | 内容 |
|---|---|---|
| ブロック/アイテム（初期版） | `tools/dev/gen_placeholder_textures.py` | ノイズ・レンガ・渦・ルーン |
| ブロック JSON（blockstates/models） | `tools/dev/gen_block_assets.py` | 26.3 の `items/<name>.json` 含む |
| ボスポートレート（初期版） | `tools/dev/gen_boss_portraits.py` | シルエット＋モチーフ（本番版に置換済み） |
| エンティティテクスチャ | entity 担当が手続き生成 | 128×128 ×16 |
| 言語ファイル | `tools/dev/merge_lang.py` | fragments → en_us/ja_jp |

## 4. ライセンス・利用条件

| 対象 | 条件 |
|---|---|
| GeckoLib（依存） | **MIT**。JAR には同梱せず別途導入 |
| 手続き生成アセット | 本プロジェクト内生成（第三者の権利を含まない） |
| **AI 生成アセット（OpenAI / Tripo / Fish）** | **UNVERIFIED**。各サービスの商用利用条件の確認が未実施。配布前に要確認 |
| ボイス | 第三者の声のクローンは行っていない（Fish の既定声のみ） |

## 5. 再現手順

```powershell
# 予算設定（.env）: ASSET_DRY_RUN=false / ASSET_AUTO_SPEND=true / 各 $5 上限
$py="C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
cd tools
& $py -m asset_pipeline status        # 設定確認（LIVE-SPEND-ENABLED 表示）
& $py -m asset_pipeline cost          # 実額（ledger が正）
& $py -m asset_pipeline.selftest      # 92 passed（強制ゲート閉で安全に実行）
```

成果の集計は `generated_assets/manifests/PRODUCTION_SUMMARY.json`、来歴は `assets.json`（83 アセット）。

## 6. 未実施・TODO

- [ ] 音声のゲーム内再生（サウンドイベント解決の原因調査）
- [ ] 生成 AI アセットのライセンス確認
- [ ] OpenAI / Fish の請求明細と ledger 推定の突合
- [ ] ボスバー枠テクスチャ（`boss_frame.png`）の実装への適用（現在はプリミティブ描画）
- [ ] Tripo モデルの実用化（リトポ＋リギング＋GeckoLib 変換の自動化は未解決）

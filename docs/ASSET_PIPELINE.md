# NOCTERRA AI アセット生成パイプライン

対象: `tools/asset_pipeline/`（Python 3.12 / 標準ライブラリ + Pillow + numpy）
最終更新: 2026-10-28 / 担当: asset-pipeline
状態: **DRY RUN のみ。課金対象リクエストは 1 件も送信していない（累計 $0.0000）。**

---

## 0. 結論サマリ

| 項目 | 状態 | 根拠 |
|---|---|---|
| `python -m asset_pipeline status` | 動作 | 実行ログ（§8-1） |
| DRY RUN でテクスチャジョブ 1 件 → 送信予定 JSON + 見積コスト出力 | 動作・HTTP 0 件 | transport を例外に差し替えても成功、監査ログ無し（§8-2） |
| 実 API 課金テスト | **未実施（禁止）** | Lead 承認待ち |
| texture.py（リサイズ/タイル検査/背景除去/量子化/比較画像） | 動作 | ダミー PNG で実測（§8-3） |
| GLB 実体検証（magic/version/JSON chunk/mesh/accessor/三角形/UV） | 動作 | 自作 valid GLB + 破損 GLB 5 種（§8-4） |
| WAV → Ogg Vorbis 変換 | 動作 | ffmpeg 9.0.2 で実変換・検証（§8-5） |
| 予算上限の送信前拒否 | 動作 | $3.00 予約後に $2.50 → 拒否（§8-6） |
| Tripo 残高 | 600 credits（frozen 0） | v3 / v2 両方 200（§6-2） |
| ffmpeg | あり（9.0.2） | `ffmpeg -version`（§6-4） |

---

## 1. 安全設計（プロジェクト絶対ルールの実装）

API キーは `C:\Users\2674469\Desktop\mcmod\.env` からのみ読み、コード・出力・コミットに含めない。
`.env` は UTF-8 BOM 付きなので `utf-8-sig` で読む（BOM 未処理だと先頭キーが `\ufeffTRIPO` になる。実測済み）。
すべての表示は `env.mask()` 経由で `tsk*** (len=47)` の形にマスクする。

| ガード | 既定値 | 挙動 |
|---|---|---|
| `ASSET_DRY_RUN` | `true` | true の間、課金対象リクエストを **HTTP 送信前に** 停止し、送信予定 JSON のみ返す |
| `ASSET_AUTO_SPEND` | `false` | 独立した第 2 の錠。実送信には `DRY_RUN=false` **かつ** `AUTO_SPEND=true` が必要 |
| `ASSET_MAX_COST_USD` | `0`（未設定時） | 送信前に台帳で **予約（reserve）** し、`committed + reserved + estimate > 上限` なら `BudgetExceeded`。多重実行でも最後の 1 ドルを二重に使えない |
| 価格未定義のモデル | — | 「予算内に収まる証明ができない」ため実送信を拒否 |
| CLI の `--confirm-spend` | 無し | 第 3 の明示確認。無いと `run` は拒否 |
| 課金 POST の自動リトライ | 無効 | 5xx は「生成済み＝課金済み」の可能性があるため再送しない（`ambiguous_billing` を返す）。429 のみ「未処理」として再試行可 |
| 外部 HTTP の監査 | `state/http_audit.jsonl` | 実際に送ったリクエストだけを記録（ヘッダは伏字・ボディ非記録）。DRY RUN ではファイルが作られない＝**送信ゼロの証拠** |

書き込み範囲: 本担当は `tools/`、`docs/ASSET_PIPELINE.md`、`generated_assets/manifests/` のみ。
`nocterra/src/`・ビルドファイル・`.env`・`.gitignore` は触っていない（`.env` は読むだけ）。

---

## 2. 使い方（`tools/` から実行）

```powershell
$py = "C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
cd C:\Users\2674469\Desktop\mcmod\tools

& $py -m asset_pipeline status                 # 設定・ゲート・キュー・台帳・ffmpeg の要約
& $py -m asset_pipeline status --probe --json  # 追加で無料メタデータ取得（残高/モデル一覧）
& $py -m asset_pipeline cost                   # コスト台帳
& $py -m asset_pipeline list                   # ジョブキュー
& $py -m asset_pipeline list --manifest        # アセット台帳（来歴）
& $py -m asset_pipeline dry-run texture_stone_tile      # ★HTTP を送らない送信予定 JSON
& $py -m asset_pipeline dry-run model_forest_king --json
& $py -m asset_pipeline run texture_stone_tile --confirm-spend   # 実送信（ゲート開放時のみ）
& $py -m asset_pipeline validate <path> [...]  # glb/png/ogg/wav/mp3/json 形式検査
& $py -m asset_pipeline texture <png> --out <dir>   # API なしのローカル加工
& $py -m asset_pipeline balance                # Tripo 残高（無料）
& $py -m asset_pipeline voices --limit 5       # Fish 音声モデル一覧（無料）
& $py -m asset_pipeline convert-audio <wav>    # Ogg Vorbis 変換（ffmpeg）
& $py -m asset_pipeline recover                # 中断ジョブを pending に戻す
& $py -m asset_pipeline.selftest               # オフライン自己検証（92 チェック）
```

ジョブは `tools/asset_pipeline/jobs/*.json` に置く（名前 or パス or インライン JSON を指定可）。

---

## 3. 構成

```
tools/asset_pipeline/
├─ __init__.py       バージョン / スキーマ版
├─ __main__.py       CLI（status, cost, dry-run, run, list, validate, texture, balance, voices, ...）
├─ env.py            .env 読み込み（BOM 対応）・マスク
├─ config.py         ゲート / 予算 / 価格表 / レート制限 / パス
├─ ledger.py         SQLite コスト台帳（reserve → commit/release、送信前拒否）
├─ queue.py          SQLite ジョブキュー（state machine / 指数バックオフ / 重複排除 / 再開 / レート制限）
├─ http_client.py    urllib ラッパ（監査ログ・伏字・課金 POST 非リトライ・ダウンロード）
├─ openai_image.py   POST /v1/images/generations
├─ tripo.py          POST /v3/generation/* → task_id 永続化 → polling → GLB 即時 DL
├─ fish.py           POST /v1/tts（model ヘッダ）+ ffmpeg で Ogg Vorbis 化
├─ texture.py        最近傍リサイズ / アルファ / 背景除去 / 量子化 / タイル検査 / 比較画像
├─ audio_tools.py    ffmpeg/ffprobe 検出・変換・長さ取得
├─ validate.py       GLB パーサ / PNG / 音声 / JSON 検証
├─ manifest.py       アセット台帳（来歴 JSON、原子的書き込み）
├─ runner.py         ジョブ実行オーケストレーション（DRY RUN と本実行で同一経路）
├─ jobs.py           ジョブ仕様の検証・読み込み
├─ util.py           JSON 原子的書き込み・DRY RUN プレビュー保存・整形
├─ selftest.py       オフライン自己検証
├─ jobs/             サンプルジョブ 3 種
└─ state/            実行時状態（SQLite・監査ログ・プレビュー。`.gitignore` 内蔵・非コミット）
```

成果物の出力先:

| 種別 | パス | git |
|---|---|---|
| 生画像（OpenAI） | `generated_assets/raw/openai/<sha256>.png` | 除外（`.gitignore` 既存） |
| 生モデル（Tripo） | `generated_assets/raw/tripo/<task_id>.glb` | 除外 |
| 生音声（Fish） | `generated_assets/raw/fish/<sha256>.<ext>` | 除外 |
| 加工済み（テクスチャ/OGG） | `generated_assets/processed/<kind>/` | **現状 git 管理外指定なし → Lead 判断待ち** |
| アセット台帳 | `generated_assets/manifests/assets.json` | コミット対象 |

---

## 4. 設定（`.env`）

### 4-1. 既存（変更不要）

```dotenv
TRIPO=...            # Tripo3D
OPENAI=...           # OpenAI
FISH=...             # Fish Audio
DEEPSEEK=...         # （本パイプラインでは未使用）
ASSET_DRY_RUN=true
ASSET_AUTO_SPEND=false
ASSET_MAX_COST_USD=5
```

### 4-2. 追加を提案する項目（未設定でも安全に動く。設定すると機能が有効化される）

| キー | 既定（未設定時） | 意味 / 提案理由 |
|---|---|---|
| `ASSET_BUDGET_OPENAI_USD` | 未設定＝全体上限のみ | サービス別サブ上限。暴走を 1 サービスに閉じ込める |
| `ASSET_BUDGET_TRIPO_USD` | 同上 | 同上（クレジット課金なので USD 換算が必要） |
| `ASSET_BUDGET_FISH_USD` | 同上 | 同上 |
| `ASSET_TRIPO_CREDIT_USD` | 未設定＝**Tripo 実送信は拒否** | 1 クレジットの USD 単価。未確認のため意図的に未設定 |
| `ASSET_TRIPO_CREDITS_JSON` | 内蔵の暫定値（text_to_model=20 等） | タスク種別ごとのクレジット。公式未確認のため暫定 |
| `ASSET_FISH_USD_PER_1K_CHARS` | 未設定＝**Fish 実送信は拒否** | TTS の 1,000 文字単価。未確認 |
| `ASSET_PRICE_JSON` | 内蔵プロキシ表 | `{"low":0.02,"medium":0.07,...}` 形式で画像単価を上書き |
| `ASSET_RATE_<SERVICE>_MIN_INTERVAL_S` | openai 5.0 / tripo 1.0 / fish 1.0 | サービス別の最小呼び出し間隔（SQLite 保持でプロセス跨ぎ） |
| `ASSET_MAX_ATTEMPTS` | 3 | リトライ上限 |
| `ASSET_BACKOFF_BASE_S` / `ASSET_BACKOFF_CAP_S` | 5 / 900 | 指数バックオフ |
| `ASSET_LEASE_SECONDS` | 900 | 実行リース。（実装は 15 分固定 + recover で復旧） |
| `ASSET_MAX_TRIPO_TRI` | 200000 | ポリゴン上限警告（Minecraft/GeckoLib 前提） |
| `ASSET_STATE_DIR` | `tools/asset_pipeline/state` | SQLite/監査/プレビュー置き場 |
| `ASSET_RAW_DIR` / `ASSET_PROCESSED_DIR` / `ASSET_MANIFESTS_DIR` | `generated_assets/{raw,processed,manifests}` | 出力先 |
| `ASSET_OPENAI_BASE_URL` / `ASSET_TRIPO_BASE_URL` / `ASSET_TRIPO_V2_BASE_URL` / `ASSET_FISH_BASE_URL` | §6 の実測 URL | エンドポイント差し替え（検証用） |

> 予算の推奨初期値（Lead 承認時）: `ASSET_BUDGET_OPENAI_USD=3`、`ASSET_BUDGET_TRIPO_USD=1`、
> `ASSET_BUDGET_FISH_USD=0.5`、`ASSET_MAX_COST_USD=5`。

---

## 5. ジョブ仕様

```jsonc
{
  "id": "stone_wall_tile",          // 省略可（省略時は自動 ID）
  "kind": "texture",                // texture | model | audio（service は自動対応）
  "service": "openai",              // 省略可（kind から決定。不一致はエラー）
  "model": "gpt-image-2.5-flare",   // エイリアス可: basic / precise
  "prompt": "...",
  "params": {                       // OpenAI Images へそのまま渡す（未知キーは拒否）
    "size": "1024x1024", "quality": "medium",
    "background": "transparent", "output_format": "png", "n": 1
  },
  "texture": {                      // 加工設定（kind=texture）
    "sizes": [16, 32, 64], "remove_background": true,
    "bg_method": "flood", "bg_tolerance": 28,
    "alpha_mode": "cutout", "quantize_colors": 16
  },
  "license_note": "..."
}
```

`kind=model` は `mode: "text_to_model" | "image_to_model"`、`params.model_version`、`input_image`（image_to_model 時）を使う。
`kind=audio` は `audio: { tts_model, format, sample_rate, reference_id, prosody }` を使う。

同梱サンプル: `jobs/texture_stone_tile.json`、`jobs/model_forest_king.json`、`jobs/audio_ambient_sting.json`。

重複排除: `kind + service + model + prompt + params...` の SHA-256（先頭 `_` のローカル項目は除外）。
同じ内容を再投入すると既存ジョブが返り、`dedupe_hits` が増える（課金されない）。

---

## 6. 確認済み事実（実測 or 一次資料で確認）

### 6-1. OpenAI Images API — 確認済み

根拠: 公式 OpenAPI ドキュメント `openai/openai-openapi` の `openapi.yaml`
（`CreateImageRequest` / `ImagesResponse` を直接読取）＋ 実 API の**非課金**ルート確認。

| 項目 | 内容 | 根拠 |
|---|---|---|
| エンドポイント | `POST https://api.openai.com/v1/images/generations`（JSON） | 公式 spec |
| ルート実在 | `GET` 同一 URL → **HTTP 405** `Invalid method for URL`（POST 専用として存在） | 実測 2026-10-28 |
| 必須 | `prompt`（最大 32,000 文字）、`model` は明示指定推奨 | 公式 spec |
| `model` 列挙 | `gpt-image-2.5-flare`、`gpt-image-2.5-sunburst`、`gpt-image-2`、`gpt-image-1.5`、`gpt-image-1-mini`、`chatgpt-image-latest`（+ `2026-09-08` スナップショット） | 公式 spec の enum と `GET /v1/models`（136 モデル中 10 が画像系）で一致 |
| `gpt-image-1` | **2026-10-23 に shutdown 済み**（spec の deprecation 注記） | 公式 spec |
| `quality` | `auto / low / medium / high / xhigh / max`（`standard`/`hd` は廃止 DALL·E 用） | 公式 spec |
| `size` | `auto`、`1024x1024`、`1536x1024`、`1024x1536`、任意の `WIDTHxHEIGHT`（両辺 **16 の倍数**、アスペクト 1:3〜3:1、最大 3840x2160、2560x1440 超は実験的） | 公式 spec |
| `output_format` | `png / jpeg / webp`（既定 png）。`output_compression` は jpeg/webp のみ | 公式 spec |
| `background` | `auto / opaque / transparent`（transparent は png/webp 必須） | 公式 spec |
| `response_format` | **非推奨**。GPT image 系は常に base64 を返す（`url` 非対応） | 公式 spec |
| レスポンス | `{created, data:[{b64_json}], background, output_format, size, quality, usage{...}}` | 公式 spec |
| レート制限/課金エラー | 400 / 403 / 410 / 429 / 500 / 503 | 公式 spec |

> アカウント側の `gpt-image-2.5-*` 実価格は **未確認**（§7）。

### 6-2. Tripo3D API — 確認済み

根拠: 公式ドキュメント（JS レンダリングのため Markdown 化された個別ページを取得）＋
公式 Python SDK `VAST-AI-Research/tripo-python-sdk` のソース ＋ 実 API の非課金呼び出し。

| 項目 | 内容 | 根拠 |
|---|---|---|
| v3 ベース | `https://openapi.tripo3d.ai`（グローバル） / `.com`（中国） | SDK |
| v2 ベース | `https://api.tripo3d.ai/v2/openapi`（**読み取り系はまだ生存**） | 実測 200 |
| 残高（v3） | `GET /v3/account/balance` → `{"code":0,"status":"success","data":{"balance":600.00,"frozen":0.00}}` | 実測 2026-10-28 |
| 残高（v2） | `GET /v2/openapi/user/balance` → `{"code":0,"data":{"balance":600,"frozen":0}}` | 実測 2026-10-28 |
| タスク作成（v3） | `POST /v3/generation/text-to-model`、`POST /v3/generation/image-to-model`（GET は 405 `use POST`） | 実測 405 + 公式 docs |
| タスク作成（v2） | `POST /v2/openapi/task`（GET 405 でルート生存確認） | 実測 |
| 参照 | `GET /v3/tasks/{task_id}`、v2 は `GET /v2/openapi/task/{task_id}` | 公式 docs / SDK |
| 状態遷移 | `queued → running → success / failed / banned / expired / cancelled`（`progress` 0-100） | 公式 docs |
| クレジット | 作成時に freeze → 成功で確定、失敗/キャンセルで返却 | 公式 docs |
| ポーリング | 1〜2 秒間隔、最大 5 分推奨。バッチは `POST /v3/tasks/list` | 公式 docs |
| リクエスト本文（v3） | `{"prompt":..., "model":"v2.5-20250123", "face_limit":..., "texture":true, "pbr":true, "export_uv":true, ...}` | SDK（v2 のフィールド名を apiv3 も受理） |
| アップロード | `POST /v3/files`（multipart, field=`file`）→ `file_token`。50MB 超は presign PUT | SDK（**実呼び出しは未実施**） |
| エラー形式 | v3 未知タスク: HTTP 404 `{"code":2001,"status":"error","message":"The task is not found","suggestion":...,"request_id":...}`（`data` 無し） | 実測 |
| v2 エラー形式 | `{"code":9405,"message":"Method Not Allowed",...}`（`status` 無し・v3 と形が違う） | 実測 |

> SDK は「v2 はシャットダウン済み」と明記しているが、実測では v2 の残高照会とルートは 200/405 で生存。
> 新規実装は **v3 を既定**とし、v2 は参照用フォールバックに留める。

### 6-3. Fish Audio API — 確認済み

根拠: 公式ドキュメント `docs.fish.audio` の OpenAPI セクション（Markdown 版）＋ 実 API の非課金呼び出し。

| 項目 | 内容 | 根拠 |
|---|---|---|
| ベース | `https://api.fish.audio` | 公式 docs |
| TTS | `POST /v1/tts`、`Authorization: Bearer <FISH>` | 公式 docs |
| Content-Type | `application/json`（または msgpack）。multipart 不可 | 公式 docs |
| 音声モデル指定 | **リクエストヘッダ `model`**。enum: `s1` / `s2-pro` / `s2.1-pro` / `s2.1-pro-free` / `drama-3`（既定 `s2.1-pro`） | 公式 docs |
| 本文必須 | `text` のみ必須。`reference_id`（音声モデル ID）か `references`（ゼロショット）を推奨 | 公式 docs |
| 出力形式 | `wav / pcm / mp3 / opus`。**Ogg Vorbis は無い**（Minecraft 非対応のため変換必須） | 公式 docs |
| サンプルレート | WAV 8/16/24/32/44.1kHz、MP3 32/44.1kHz、Opus 48kHz | 公式 docs |
| 声モデル一覧 | `GET /model?page_size=N` → `{items:[{_id,title,state,train_mode,tags,languages}], total:...}` | 実測 200 |
| エラー | 401 / 402（残高不足）/ 503 | 公式 docs |

> TTS の実レスポンス（bytes か JSON ラップか）は**未確認**。実装は `content-type` と先頭 magic で
> 音声判定し、JSON の場合は `audio`/`audio_base64`/`b64_json`/`data` を防御的に探索する。

### 6-4. ローカル環境 — 確認済み

| 項目 | 結果 |
|---|---|
| Python | 3.12.14（指定ランタイム）。Pillow 12.3.0 / numpy 2.3.5 |
| ffmpeg | **あり** `...\Gyan.FFmpeg...\ffmpeg-9.0.2-full_build\bin\ffmpeg.exe` |
| ffprobe | あり（WAV/OGG の codec・長さ取得に使用） |
| winget | あり（ffmpeg が無い環境向けの導入案内に記載） |
| `.env` | UTF-8 BOM 付き。`utf-8-sig` 必須 |

---

## 7. 未確認・未検証（実課金なしでは確定できない）

| # | 未確認事項 | 影響 | 確定に必要なこと |
|---|---|---|---|
| 1 | OpenAI `gpt-image-2.5-*` の **実価格**（品質/サイズ別） | 見積は gpt-image-1 の公開価格を**プロキシ**として使用中。`ASSET_PRICE_JSON` で要上書き | Lead 承認後の 1 回の生成 + ダッシュボード請求確認 |
| 2 | `gpt-image-2.5-*` の 200 応答の実データ（`usage` の有無・内容） | 実装は spec 通りに optional 扱い | 同上 |
| 3 | Tripo タスク作成→polling→出力 URL の**実レスポンス形**（`output` のキー名） | 実装は `model_url`/`pbr_model_url`/`model` 等の候補を順に探索。キー名が違えば失敗する | 承認後の 1 タスク（20 credits 前後） |
| 4 | Tripo `model_version` が本アカウントで有効か | 既定 `v2.5-20250123` は SDK 既定値。実際は `v3.0-*` / `v3.5-*` の可能性 | 同上（または docs の Models & Versions を精読） |
| 5 | Tripo の **クレジット単価**（USD 換算） | 未設定のため Tripo の実送信は**予算ガードで拒否**される | 請求情報の確認 |
| 6 | Tripo v2 の画像アップロードフロー | v2 の `image_to_model` は実装していない（v3 のみ） | v2 upload 仕様の確認 or 不要と判断 |
| 7 | Tripo 生成物の**ライセンス/商用利用条件** | manifest に `UNVERIFIED` と記録 | 利用規約の確認（法務判断） |
| 8 | Fish TTS の実レスポンス形式・課金（文字単価） | 未設定のため Fish の実送信は**予算ガードで拒否**。レスポンスは magic 判定で防御 | 承認後の 1 回 + 残高差分確認 |
| 9 | Fish の音声モデル ID が `reference_id` として有効か | 一覧取得は成功。TTS 未実行 | 承認後の 1 回 |
| 10 | OpenAI の利用規約上の生成物権利 | manifest に `UNVERIFIED` と記録 | 規約確認（法務判断） |
| 11 | GeckoLib 用 `.geo.json` への変換工程 | 本パイプラインは GLB 取得・検証まで。変換は別工程 | 別タスク（boss 担当と調整） |
| 12 | 生成テクスチャの「見た目」評価 | 数値（タイル性・パレット）のみ検証。人の目視レビューは未実施 | 実際に生成して目視 |

---

## 8. 実行した検証と結果

### 8-1. `status`（必須要件）

```
& $py -m asset_pipeline status
→ NOCTERRA asset pipeline v0.1.0 [DRY-RUN]
  ASSET_DRY_RUN=True / ASSET_AUTO_SPEND=False / max cost=$5.0000
  キーはすべてマスク表示（DEEPSEEK sk-****/FISH sk-****/OPENAI sk-****/TRIPO tsk****）
  queue total=0 / ledger committed=$0.0000 / manifest assets=0
  ffmpeg: ffmpeg version 9.0.2-full_build
```

### 8-2. DRY RUN の実証（HTTP を送らないこと）

- 方法 1: `dry-run texture_stone_tile` 実行 → 送信予定 JSON・見積 `$0.0700`・ゲート状態を出力。
  監査ログ `state/http_audit.jsonl` が**生成されない**ことを確認。
- 方法 2（より強力）: selftest で `Transport.request` / `Transport.download` / `urllib.request.urlopen`
  を **例外を投げる関数に差し替えた状態**で 3 ジョブを preview → 3 件とも成功（＝HTTP 非依存）。
- 出力例（抜粋）:

```json
{
  "service": "openai", "dry_run": true, "http_sent": false,
  "method": "POST", "endpoint": "https://api.openai.com/v1/images/generations",
  "request": {"model": "gpt-image-2.5-flare", "prompt": "...", "n": 1,
              "quality": "medium", "size": "1024x1024",
              "output_format": "png", "background": "transparent", "moderation": "auto"},
  "estimate": {"usd": 0.07, "priced": true,
               "source": "unverified proxy (gpt-image-1 public list price)"},
  "gate": {"mode": "DRY-RUN", "spend_locked": true, "max_cost_usd": 5.0}
}
```

### 8-3. テクスチャ加工（ダミー PNG。`.agent/tmp/asset_pipeline_selftest/`）

| 検査 | 結果 |
|---|---|
| 周期模様 → タイル性 | `seamless`（worst_ratio 1.0101） |
| 左右グラデーション → タイル性 | `visible-seam`（worst_ratio 175.0） |
| 最近傍リサイズ | 元画像に存在しない色を作らない（補間なし） |
| アルファ cutout | α=100 → 0 / α=200 → 255 |
| パレット量子化 | 8 色指定 → サンプル 8 色以下、α 保持 |
| 単色背景の flood fill | 128×128 中 10,859px（66.28%）を除去、角 α=0 |
| 16/32/64 出力 | すべて正方形・PNG 検証 PASS・タイルプレビュー生成 |
| 比較画像 | `<name>_compare.png` 生成（source / processed 並置） |

### 8-4. GLB 検証（自作 GLB）

| 検査 | 結果 |
|---|---|
| 正常 GLB（2 三角形, UV あり） | PASS / magic=glTF, version=2, chunks=JSON+BIN, meshes=1, accessors=4, tris=2, UV=true |
| magic 破損 | 拒否 |
| version=1 | 拒否 |
| 途中で切れた GLB | 拒否 |
| 非 GLB ゴミ | 拒否 |
| UV 無し | 警告 `no TEXCOORD_0` |
| 三角形上限 2 に対し 4 | 警告（ポリゴン予算超過） |

### 8-5. 音声（ffmpeg 実変換）

```
WAV 0.6s 44.1kHz → ffmpeg libvorbis → .ogg
validate: container=ogg, codec=vorbis, sample_rate=44100, duration=0.6  → PASS
（Minecraft の Ogg Vorbis ローダで再生可能な形式であることを確認）
```

### 8-6. 予算・キュー

| 検査 | 結果 |
|---|---|
| 上限 $5 で $3 予約 → さらに $2.5 予約 | `BudgetExceeded`（$5.50 で上限超過）で拒否。予約合計は不変 |
| 価格未定義（Tripo） | `price unknown` で拒否（予算を証明できないため） |
| ゲート A: DRY_RUN=true + `--confirm-spend` | exit 2 で拒否、HTTP なし |
| ゲート B: DRY_RUN=false / AUTO_SPEND=false | exit 2 で拒否、HTTP なし |
| ゲート C: 両ゲート開放 + 予算 0 + エンドポイントを死んだローカルポートに差し替え | 台帳が**送信前に**拒否。HTTP なし |
| ゲート D: 両ゲート開放 + `--confirm-spend` 無し | exit 2 で拒否 |
| キュー | 重複排除（同一ハッシュで同一ジョブ / `dedupe_hits` 増加） |
| 失敗リトライ | attempts 1→pending（バックオフ）→ attempts 3 で failed |
| 中断復旧 | `running` + lease 失効 → `recover_stale()` で pending に復帰 |
| レート制限 | 最小間隔 0.25s で 2 回目に 0.249s 待機 |

### 8-7. 自己検証スイート

```
& $py -m asset_pipeline.selftest
→ RESULT: 92 passed, 0 failed, 92 checks
```

---

## 9. コスト管理の詳細

- **推定値と実費**: OpenAI は「画像 1 枚 × 品質別単価」の推定のみ（API は明細価格を返さない）。
  Tripo はクレジット推定（単価未確認のため USD は未確定）。Fish は文字数推定（単価未確認）。
- **台帳フロー**: `reserve`（送信前・原子的）→ 成功 `commit` / 失敗 `release`。
  通信断など「送ったか不明」の場合は `ambiguous_billing` を立て、**自動 release せず**人間の確認に回す。
- `cost` コマンドで サービス別・ジョブ別の累計と予約を確認できる。
- 現在の累計: **committed $0.0000 / reserved $0.0000 / 課金リクエスト 0 件**。

---

## 10. 承認が必要なこと / 次のステップ

1. **OpenAI 単価の確認**: `ASSET_PRICE_JSON` に実価格を設定（または 1 枚だけ生成して請求で検証）。
   現状の見積は gpt-image-1 の公開価格プロキシで、`gpt-image-2.5-*` の実価格は**未確認**。
2. **Tripo**: `ASSET_TRIPO_CREDIT_USD` を設定しない限り実送信は拒否される。600 credits の使い道
   （モデル 1 体 ≒ 20 credits 前後と推定）と、テスト 1 回の承認。
3. **Fish**: `ASSET_FISH_USD_PER_1K_CHARS` の設定。音声はゲーム内 BGM/環境音としての利用規約確認。
4. **生成物のライセンス**: 3 サービスすべて `UNVERIFIED` として manifest に記録済み。公開前の法務確認。
5. **`generated_assets/processed/` の扱い**: 現行 `.gitignore` は `raw/` `cache/` `tmp/` のみ除外。
   加工済みテクスチャ/OGG をコミットするのか、mod の `resources` へコピーするのか Lead 判断が必要。
6. **`tools/asset_pipeline/state/`**: 実行時状態（内蔵 `.gitignore` で自己除外）。ルート `.gitignore` に
   追記するかは Lead 判断（本担当はルート `.gitignore` を編集していない）。
7. **GeckoLib `.geo.json` 変換**: GLB→GeckoLib の変換工程は本パイプライン外。boss 担当との分担決め。

---

## 11. 参考リンク（一次資料）

- OpenAI Images API リファレンス: <https://developers.openai.com/api/reference/resources/images/methods/generate>
- OpenAI 公式 OpenAPI（今回の根拠）: <https://github.com/openai/openai-openapi>
- Tripo Task Lifecycle（状態遷移・freeze/deduct）: <https://developers.tripo3d.ai/en/docs/task-lifecycle>
- Tripo 公式 Python SDK（v3 実装の根拠）: <https://github.com/VAST-AI-Research/tripo-python-sdk>
- Fish Audio TTS API: <https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech>

> 注: Tripo のドキュメントは JS レンダリングだが、`developers.tripo3d.ai/en/docs/task-lifecycle` は
> 本文が取得できた（取得日 2026-10-28）。取得できないページは SDK ソースで補完した。

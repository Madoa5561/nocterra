# NOCTERRA AI 繧｢繧ｻ繝・ヨ逕滓・繝代う繝励Λ繧､繝ｳ

蟇ｾ雎｡: `tools/asset_pipeline/`・・ython 3.12 / 讓呎ｺ悶Λ繧､繝悶Λ繝ｪ + Pillow + numpy・・譛邨よ峩譁ｰ: 2026-10-28 / 諡・ｽ・ asset-pipeline
迥ｶ諷・ **譛ｬ逡ｪ逕滓・繧・1 蝗槫ｮ滓命貂医∩・・ead 謇ｿ隱阪・LIVE-SPEND-ENABLED・峨らｴｯ險・$2.67612 / 荳企剞 $5.00縲・隱ｲ驥代Μ繧ｯ繧ｨ繧ｹ繝・56 莉ｶ謌仙粥繝ｻ螟ｱ謨・莠碁㍾隱ｲ驥・0 莉ｶ縲りｩｳ邏ｰ縺ｯ ﾂｧ12縲・*

---

## 0. 邨占ｫ悶し繝槭Μ

| 鬆・岼 | 迥ｶ諷・| 譬ｹ諡 |
|---|---|---|
| `python -m asset_pipeline status` | 蜍穂ｽ・| 螳溯｡後Ο繧ｰ・按ｧ8-1・・|
| DRY RUN 縺ｧ繝・け繧ｹ繝√Ε繧ｸ繝ｧ繝・1 莉ｶ 竊・騾∽ｿ｡莠亥ｮ・JSON + 隕狗ｩ阪さ繧ｹ繝亥・蜉・| 蜍穂ｽ懊・HTTP 0 莉ｶ | transport 繧剃ｾ句､悶↓蟾ｮ縺玲崛縺医※繧よ・蜉溘∫屮譟ｻ繝ｭ繧ｰ辟｡縺暦ｼ按ｧ8-2・・|
| 螳・API 隱ｲ驥代ユ繧ｹ繝・| **螳滓命貂医∩・・ead 謇ｿ隱搾ｼ・* | OpenAI 28 / Fish 24 / Tripo 2 莉ｶ謌仙粥・按ｧ12・・|
| texture.py・医Μ繧ｵ繧､繧ｺ/繧ｿ繧､繝ｫ讀懈渊/閭梧勹髯､蜴ｻ/驥丞ｭ仙喧/豈碑ｼ・判蜒擾ｼ・| 蜍穂ｽ・| 繝繝溘・ PNG 縺ｧ螳滓ｸｬ・按ｧ8-3・会ｼ区悽逡ｪ 12 轤ｹ縺ｧ蜀咲｢ｺ隱搾ｼ按ｧ12・・|
| GLB 螳滉ｽ捺､懆ｨｼ・・agic/version/JSON chunk/mesh/accessor/荳芽ｧ貞ｽ｢/UV・・| 蜍穂ｽ・| 閾ｪ菴・valid GLB + 遐ｴ謳・GLB 5 遞ｮ・按ｧ8-4・会ｼ句ｮ・GLB 2 莉ｶ縺ｧ遒ｺ隱搾ｼ按ｧ12・・|
| WAV 竊・Ogg Vorbis 螟画鋤 | 蜍穂ｽ・| ffmpeg 9.0.2 縺ｧ螳溷､画鋤繝ｻ讀懆ｨｼ・按ｧ8-5・会ｼ区悽逡ｪ 24 譛ｬ・按ｧ12・・|
| 莠育ｮ嶺ｸ企剞縺ｮ騾∽ｿ｡蜑肴拠蜷ｦ | 蜍穂ｽ・| $3.00 莠育ｴ・ｾ後↓ $2.50 竊・諡貞凄・按ｧ8-6・・|
| Tripo 谿矩ｫ・| **540 credits**・磯幕蟋・600縲・0 豸郁ｲｻ・・| v3 / v2 荳｡譁ｹ 200・按ｧ6-2繝ｻﾂｧ12・・|
| GLB 竊・繝懊け繧ｻ繝ｫ 竊・GeckoLib `.geo.json` | **謌仙粥・・ 菴難ｼ・* | 讒矩讀懆ｨｼ PASS・按ｧ12-6・峨ゅ◆縺縺鈴撕逧・・菴主ｿ螳溷ｺｦ縺ｮ蛻ｶ邏・≠繧・|
| ffmpeg | 縺ゅｊ・・.0.2・・| `ffmpeg -version`・按ｧ6-4・・|

> 逕滓・迚ｩ縺ｮ荳隕ｧ縺ｯ `generated_assets/manifests/PRODUCTION_SUMMARY.json`縲∵擂豁ｴ縺ｯ
> `generated_assets/manifests/assets.json` 繧貞盾辣ｧ縲・
---

## 1. 螳牙・險ｭ險茨ｼ医・繝ｭ繧ｸ繧ｧ繧ｯ繝育ｵｶ蟇ｾ繝ｫ繝ｼ繝ｫ縺ｮ螳溯｣・ｼ・
API 繧ｭ繝ｼ縺ｯ `C:\Users\2674469\Desktop\mcmod\.env` 縺九ｉ縺ｮ縺ｿ隱ｭ縺ｿ縲√さ繝ｼ繝峨・蜃ｺ蜉帙・繧ｳ繝溘ャ繝医↓蜷ｫ繧√↑縺・・`.env` 縺ｯ UTF-8 BOM 莉倥″縺ｪ縺ｮ縺ｧ `utf-8-sig` 縺ｧ隱ｭ繧・・OM 譛ｪ蜃ｦ逅・□縺ｨ蜈磯ｭ繧ｭ繝ｼ縺・`\ufeffTRIPO` 縺ｫ縺ｪ繧九ょｮ滓ｸｬ貂医∩・峨・縺吶∋縺ｦ縺ｮ陦ｨ遉ｺ縺ｯ `env.mask()` 邨檎罰縺ｧ `tsk*** (len=47)` 縺ｮ蠖｢縺ｫ繝槭せ繧ｯ縺吶ｋ縲・
| 繧ｬ繝ｼ繝・| 譌｢螳壼､ | 謖吝虚 |
|---|---|---|
| `ASSET_DRY_RUN` | `true` | true 縺ｮ髢薙∬ｪｲ驥大ｯｾ雎｡繝ｪ繧ｯ繧ｨ繧ｹ繝医ｒ **HTTP 騾∽ｿ｡蜑阪↓** 蛛懈ｭ｢縺励・∽ｿ｡莠亥ｮ・JSON 縺ｮ縺ｿ霑斐☆ |
| `ASSET_AUTO_SPEND` | `false` | 迢ｬ遶九＠縺溽ｬｬ 2 縺ｮ骭縲ょｮ滄∽ｿ｡縺ｫ縺ｯ `DRY_RUN=false` **縺九▽** `AUTO_SPEND=true` 縺悟ｿ・ｦ・|
| `ASSET_MAX_COST_USD` | `0`・域悴險ｭ螳壽凾・・| 騾∽ｿ｡蜑阪↓蜿ｰ蟶ｳ縺ｧ **莠育ｴ・ｼ・eserve・・* 縺励～committed + reserved + estimate > 荳企剞` 縺ｪ繧・`BudgetExceeded`縲ょ､夐㍾螳溯｡後〒繧よ怙蠕後・ 1 繝峨Ν繧剃ｺ碁㍾縺ｫ菴ｿ縺医↑縺・|
| 萓｡譬ｼ譛ｪ螳夂ｾｩ縺ｮ繝｢繝・Ν | 窶・| 縲御ｺ育ｮ怜・縺ｫ蜿弱∪繧玖ｨｼ譏弱′縺ｧ縺阪↑縺・阪◆繧∝ｮ滄∽ｿ｡繧呈拠蜷ｦ |
| CLI 縺ｮ `--confirm-spend` | 辟｡縺・| 隨ｬ 3 縺ｮ譏守､ｺ遒ｺ隱阪ら┌縺・→ `run` 縺ｯ諡貞凄 |
| 隱ｲ驥・POST 縺ｮ閾ｪ蜍輔Μ繝医Λ繧､ | 辟｡蜉ｹ | 5xx 縺ｯ縲檎函謌先ｸ医∩・晁ｪｲ驥第ｸ医∩縲阪・蜿ｯ閭ｽ諤ｧ縺後≠繧九◆繧∝・騾√＠縺ｪ縺・ｼ・ambiguous_billing` 繧定ｿ斐☆・峨・29 縺ｮ縺ｿ縲梧悴蜃ｦ逅・阪→縺励※蜀崎ｩｦ陦悟庄 |
| 螟夜Κ HTTP 縺ｮ逶｣譟ｻ | `state/http_audit.jsonl` | 螳滄圀縺ｫ騾√▲縺溘Μ繧ｯ繧ｨ繧ｹ繝医□縺代ｒ險倬鹸・医・繝・ム縺ｯ莨丞ｭ励・繝懊ョ繧｣髱櫁ｨ倬鹸・峨・RY RUN 縺ｧ縺ｯ繝輔ぃ繧､繝ｫ縺御ｽ懊ｉ繧後↑縺・ｼ・*騾∽ｿ｡繧ｼ繝ｭ縺ｮ險ｼ諡** |

譖ｸ縺崎ｾｼ縺ｿ遽・峇: 譛ｬ諡・ｽ薙・ `tools/`縲～docs/ASSET_PIPELINE.md`縲～generated_assets/manifests/` 縺ｮ縺ｿ縲・`nocterra/src/`繝ｻ繝薙Ν繝峨ヵ繧｡繧､繝ｫ繝ｻ`.env`繝ｻ`.gitignore` 縺ｯ隗ｦ縺｣縺ｦ縺・↑縺・ｼ・.env` 縺ｯ隱ｭ繧縺縺托ｼ峨・
---

## 2. 菴ｿ縺・婿・・tools/` 縺九ｉ螳溯｡鯉ｼ・
```powershell
$py = "C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
cd C:\Users\2674469\Desktop\mcmod\tools

& $py -m asset_pipeline status                 # 險ｭ螳壹・繧ｲ繝ｼ繝医・繧ｭ繝･繝ｼ繝ｻ蜿ｰ蟶ｳ繝ｻffmpeg 縺ｮ隕∫ｴ・& $py -m asset_pipeline status --probe --json  # 霑ｽ蜉縺ｧ辟｡譁吶Γ繧ｿ繝・・繧ｿ蜿門ｾ暦ｼ域ｮ矩ｫ・繝｢繝・Ν荳隕ｧ・・& $py -m asset_pipeline cost                   # 繧ｳ繧ｹ繝亥床蟶ｳ
& $py -m asset_pipeline list                   # 繧ｸ繝ｧ繝悶く繝･繝ｼ
& $py -m asset_pipeline list --manifest        # 繧｢繧ｻ繝・ヨ蜿ｰ蟶ｳ・域擂豁ｴ・・& $py -m asset_pipeline dry-run texture_stone_tile      # 笘・TTP 繧帝√ｉ縺ｪ縺・∽ｿ｡莠亥ｮ・JSON
& $py -m asset_pipeline dry-run model_forest_king --json
& $py -m asset_pipeline run texture_stone_tile --confirm-spend   # 螳滄∽ｿ｡・医ご繝ｼ繝磯幕謾ｾ譎ゅ・縺ｿ・・& $py -m asset_pipeline validate <path> [...]  # glb/png/ogg/wav/mp3/json 蠖｢蠑乗､懈渊
& $py -m asset_pipeline texture <png> --out <dir>   # API 縺ｪ縺励・繝ｭ繝ｼ繧ｫ繝ｫ蜉蟾･
& $py -m asset_pipeline balance                # Tripo 谿矩ｫ假ｼ育┌譁呻ｼ・& $py -m asset_pipeline voices --limit 5       # Fish 髻ｳ螢ｰ繝｢繝・Ν荳隕ｧ・育┌譁呻ｼ・& $py -m asset_pipeline convert-audio <wav>    # Ogg Vorbis 螟画鋤・・fmpeg・・& $py -m asset_pipeline recover                # 荳ｭ譁ｭ繧ｸ繝ｧ繝悶ｒ pending 縺ｫ謌ｻ縺・& $py -m asset_pipeline.selftest               # 繧ｪ繝輔Λ繧､繝ｳ閾ｪ蟾ｱ讀懆ｨｼ・・2 繝√ぉ繝・け・・```

繧ｸ繝ｧ繝悶・ `tools/asset_pipeline/jobs/*.json` 縺ｫ鄂ｮ縺擾ｼ亥錐蜑・or 繝代せ or 繧､繝ｳ繝ｩ繧､繝ｳ JSON 繧呈欠螳壼庄・峨・
---

## 3. 讒区・

```
tools/asset_pipeline/
笏懌楳 __init__.py       繝舌・繧ｸ繝ｧ繝ｳ / 繧ｹ繧ｭ繝ｼ繝樒沿
笏懌楳 __main__.py       CLI・・tatus, cost, dry-run, run, list, validate, texture, balance, voices, ...・・笏懌楳 env.py            .env 隱ｭ縺ｿ霎ｼ縺ｿ・・OM 蟇ｾ蠢懶ｼ峨・繝槭せ繧ｯ
笏懌楳 config.py         繧ｲ繝ｼ繝・/ 莠育ｮ・/ 萓｡譬ｼ陦ｨ / 繝ｬ繝ｼ繝亥宛髯・/ 繝代せ
笏懌楳 ledger.py         SQLite 繧ｳ繧ｹ繝亥床蟶ｳ・・eserve 竊・commit/release縲・∽ｿ｡蜑肴拠蜷ｦ・・笏懌楳 queue.py          SQLite 繧ｸ繝ｧ繝悶く繝･繝ｼ・・tate machine / 謖・焚繝舌ャ繧ｯ繧ｪ繝・/ 驥崎､・賜髯､ / 蜀埼幕 / 繝ｬ繝ｼ繝亥宛髯撰ｼ・笏懌楳 http_client.py    urllib 繝ｩ繝・ヱ・育屮譟ｻ繝ｭ繧ｰ繝ｻ莨丞ｭ励・隱ｲ驥・POST 髱槭Μ繝医Λ繧､繝ｻ繝繧ｦ繝ｳ繝ｭ繝ｼ繝会ｼ・笏懌楳 openai_image.py   POST /v1/images/generations
笏懌楳 tripo.py          POST /v3/generation/* 竊・task_id 豌ｸ邯壼喧 竊・polling 竊・GLB 蜊ｳ譎・DL
笏懌楳 fish.py           POST /v1/tts・・odel 繝倥ャ繝・・ ffmpeg 縺ｧ Ogg Vorbis 蛹・笏懌楳 texture.py        譛霑大ｍ繝ｪ繧ｵ繧､繧ｺ / 繧｢繝ｫ繝輔ぃ / 閭梧勹髯､蜴ｻ / 驥丞ｭ仙喧 / 繧ｿ繧､繝ｫ讀懈渊 / 豈碑ｼ・判蜒・笏懌楳 audio_tools.py    ffmpeg/ffprobe 讀懷・繝ｻ螟画鋤繝ｻ髟ｷ縺募叙蠕・笏懌楳 validate.py       GLB 繝代・繧ｵ / PNG / 髻ｳ螢ｰ / JSON 讀懆ｨｼ
笏懌楳 manifest.py       繧｢繧ｻ繝・ヨ蜿ｰ蟶ｳ・域擂豁ｴ JSON縲∝次蟄千噪譖ｸ縺崎ｾｼ縺ｿ・・笏懌楳 runner.py         繧ｸ繝ｧ繝門ｮ溯｡後が繝ｼ繧ｱ繧ｹ繝医Ξ繝ｼ繧ｷ繝ｧ繝ｳ・・RY RUN 縺ｨ譛ｬ螳溯｡後〒蜷御ｸ邨瑚ｷｯ・・笏懌楳 jobs.py           繧ｸ繝ｧ繝紋ｻ墓ｧ倥・讀懆ｨｼ繝ｻ隱ｭ縺ｿ霎ｼ縺ｿ
笏懌楳 util.py           JSON 蜴溷ｭ千噪譖ｸ縺崎ｾｼ縺ｿ繝ｻDRY RUN 繝励Ξ繝薙Η繝ｼ菫晏ｭ倥・謨ｴ蠖｢
笏懌楳 selftest.py       繧ｪ繝輔Λ繧､繝ｳ閾ｪ蟾ｱ讀懆ｨｼ
笏懌楳 jobs/             繧ｵ繝ｳ繝励Ν繧ｸ繝ｧ繝・3 遞ｮ
笏披楳 state/            螳溯｡梧凾迥ｶ諷具ｼ・QLite繝ｻ逶｣譟ｻ繝ｭ繧ｰ繝ｻ繝励Ξ繝薙Η繝ｼ縲Ａ.gitignore` 蜀・鳩繝ｻ髱槭さ繝溘ャ繝茨ｼ・```

謌先棡迚ｩ縺ｮ蜃ｺ蜉帛・:

| 遞ｮ蛻･ | 繝代せ | git |
|---|---|---|
| 逕溽判蜒擾ｼ・penAI・・| `generated_assets/raw/openai/<sha256>.png` | 髯､螟厄ｼ・.gitignore` 譌｢蟄假ｼ・|
| 逕溘Δ繝・Ν・・ripo・・| `generated_assets/raw/tripo/<task_id>.glb` | 髯､螟・|
| 逕滄浹螢ｰ・・ish・・| `generated_assets/raw/fish/<sha256>.<ext>` | 髯､螟・|
| 蜉蟾･貂医∩・医ユ繧ｯ繧ｹ繝√Ε/OGG・・| `generated_assets/processed/<kind>/` | **迴ｾ迥ｶ git 邂｡逅・､匁欠螳壹↑縺・竊・Lead 蛻､譁ｭ蠕・■** |
| 繧｢繧ｻ繝・ヨ蜿ｰ蟶ｳ | `generated_assets/manifests/assets.json` | 繧ｳ繝溘ャ繝亥ｯｾ雎｡ |

---

## 4. 險ｭ螳夲ｼ・.env`・・
### 4-1. 譌｢蟄假ｼ亥､画峩荳崎ｦ・ｼ・
```dotenv
TRIPO=...            # Tripo3D
OPENAI=...           # OpenAI
FISH=...             # Fish Audio
DEEPSEEK=...         # ・域悽繝代う繝励Λ繧､繝ｳ縺ｧ縺ｯ譛ｪ菴ｿ逕ｨ・・ASSET_DRY_RUN=true
ASSET_AUTO_SPEND=false
ASSET_MAX_COST_USD=5
```

### 4-2. 霑ｽ蜉繧呈署譯医☆繧矩・岼・域悴險ｭ螳壹〒繧ょｮ牙・縺ｫ蜍輔￥縲りｨｭ螳壹☆繧九→讖溯・縺梧怏蜉ｹ蛹悶＆繧後ｋ・・
| 繧ｭ繝ｼ | 譌｢螳夲ｼ域悴險ｭ螳壽凾・・| 諢丞袖 / 謠先｡育炊逕ｱ |
|---|---|---|
| `ASSET_BUDGET_OPENAI_USD` | 譛ｪ險ｭ螳夲ｼ晏・菴謎ｸ企剞縺ｮ縺ｿ | 繧ｵ繝ｼ繝薙せ蛻･繧ｵ繝紋ｸ企剞縲よ垓襍ｰ繧・1 繧ｵ繝ｼ繝薙せ縺ｫ髢峨§霎ｼ繧√ｋ |
| `ASSET_BUDGET_TRIPO_USD` | 蜷御ｸ・| 蜷御ｸ奇ｼ医け繝ｬ繧ｸ繝・ヨ隱ｲ驥代↑縺ｮ縺ｧ USD 謠帷ｮ励′蠢・ｦ・ｼ・|
| `ASSET_BUDGET_FISH_USD` | 蜷御ｸ・| 蜷御ｸ・|
| `ASSET_TRIPO_CREDIT_USD` | 譛ｪ險ｭ螳夲ｼ・*Tripo 螳滄∽ｿ｡縺ｯ諡貞凄** | 1 繧ｯ繝ｬ繧ｸ繝・ヨ縺ｮ USD 蜊倅ｾ｡縲よ悴遒ｺ隱阪・縺溘ａ諢丞峙逧・↓譛ｪ險ｭ螳・|
| `ASSET_TRIPO_CREDITS_JSON` | 蜀・鳩縺ｮ證ｫ螳壼､・・ext_to_model=20 遲会ｼ・| 繧ｿ繧ｹ繧ｯ遞ｮ蛻･縺斐→縺ｮ繧ｯ繝ｬ繧ｸ繝・ヨ縲ょ・蠑乗悴遒ｺ隱阪・縺溘ａ證ｫ螳・|
| `ASSET_FISH_USD_PER_1K_CHARS` | 譛ｪ險ｭ螳夲ｼ・*Fish 螳滄∽ｿ｡縺ｯ諡貞凄** | TTS 縺ｮ 1,000 譁・ｭ怜腰萓｡縲よ悴遒ｺ隱・|
| `ASSET_PRICE_JSON` | 蜀・鳩繝励Ο繧ｭ繧ｷ陦ｨ | `{"low":0.02,"medium":0.07,...}` 蠖｢蠑上〒逕ｻ蜒丞腰萓｡繧剃ｸ頑嶌縺・|
| `ASSET_RATE_<SERVICE>_MIN_INTERVAL_S` | openai 5.0 / tripo 1.0 / fish 1.0 | 繧ｵ繝ｼ繝薙せ蛻･縺ｮ譛蟆丞他縺ｳ蜃ｺ縺鈴俣髫費ｼ・QLite 菫晄戟縺ｧ繝励Ο繧ｻ繧ｹ霍ｨ縺趣ｼ・|
| `ASSET_MAX_ATTEMPTS` | 3 | 繝ｪ繝医Λ繧､荳企剞 |
| `ASSET_BACKOFF_BASE_S` / `ASSET_BACKOFF_CAP_S` | 5 / 900 | 謖・焚繝舌ャ繧ｯ繧ｪ繝・|
| `ASSET_LEASE_SECONDS` | 900 | 螳溯｡後Μ繝ｼ繧ｹ縲ゑｼ亥ｮ溯｣・・ 15 蛻・崋螳・+ recover 縺ｧ蠕ｩ譌ｧ・・|
| `ASSET_MAX_TRIPO_TRI` | 200000 | 繝昴Μ繧ｴ繝ｳ荳企剞隴ｦ蜻奇ｼ・inecraft/GeckoLib 蜑肴署・・|
| `ASSET_STATE_DIR` | `tools/asset_pipeline/state` | SQLite/逶｣譟ｻ/繝励Ξ繝薙Η繝ｼ鄂ｮ縺榊ｴ |
| `ASSET_RAW_DIR` / `ASSET_PROCESSED_DIR` / `ASSET_MANIFESTS_DIR` | `generated_assets/{raw,processed,manifests}` | 蜃ｺ蜉帛・ |
| `ASSET_OPENAI_BASE_URL` / `ASSET_TRIPO_BASE_URL` / `ASSET_TRIPO_V2_BASE_URL` / `ASSET_FISH_BASE_URL` | ﾂｧ6 縺ｮ螳滓ｸｬ URL | 繧ｨ繝ｳ繝峨・繧､繝ｳ繝亥ｷｮ縺玲崛縺茨ｼ域､懆ｨｼ逕ｨ・・|

> 莠育ｮ励・謗ｨ螂ｨ蛻晄悄蛟､・・ead 謇ｿ隱肴凾・・ `ASSET_BUDGET_OPENAI_USD=3`縲～ASSET_BUDGET_TRIPO_USD=1`縲・> `ASSET_BUDGET_FISH_USD=0.5`縲～ASSET_MAX_COST_USD=5`縲・
---

## 5. 繧ｸ繝ｧ繝紋ｻ墓ｧ・
```jsonc
{
  "id": "stone_wall_tile",          // 逵∫払蜿ｯ・育怐逡･譎ゅ・閾ｪ蜍・ID・・  "kind": "texture",                // texture | model | audio・・ervice 縺ｯ閾ｪ蜍募ｯｾ蠢懶ｼ・  "service": "openai",              // 逵∫払蜿ｯ・・ind 縺九ｉ豎ｺ螳壹ゆｸ堺ｸ閾ｴ縺ｯ繧ｨ繝ｩ繝ｼ・・  "model": "gpt-image-2.5-flare",   // 繧ｨ繧､繝ｪ繧｢繧ｹ蜿ｯ: basic / precise
  "prompt": "...",
  "params": {                       // OpenAI Images 縺ｸ縺昴・縺ｾ縺ｾ貂｡縺呻ｼ域悴遏･繧ｭ繝ｼ縺ｯ諡貞凄・・    "size": "1024x1024", "quality": "medium",
    "background": "transparent", "output_format": "png", "n": 1
  },
  "texture": {                      // 蜉蟾･險ｭ螳夲ｼ・ind=texture・・    "sizes": [16, 32, 64], "remove_background": true,
    "bg_method": "flood", "bg_tolerance": 28,
    "alpha_mode": "cutout", "quantize_colors": 16
  },
  "license_note": "..."
}
```

`kind=model` 縺ｯ `mode: "text_to_model" | "image_to_model"`縲～params.model_version`縲～input_image`・・mage_to_model 譎ゑｼ峨ｒ菴ｿ縺・・`kind=audio` 縺ｯ `audio: { tts_model, format, sample_rate, reference_id, prosody }` 繧剃ｽｿ縺・・
蜷梧｢ｱ繧ｵ繝ｳ繝励Ν: `jobs/texture_stone_tile.json`縲～jobs/model_forest_king.json`縲～jobs/audio_ambient_sting.json`縲・
驥崎､・賜髯､: `kind + service + model + prompt + params...` 縺ｮ SHA-256・亥・鬆ｭ `_` 縺ｮ繝ｭ繝ｼ繧ｫ繝ｫ鬆・岼縺ｯ髯､螟厄ｼ峨・蜷後§蜀・ｮｹ繧貞・謚募・縺吶ｋ縺ｨ譌｢蟄倥ず繝ｧ繝悶′霑斐ｊ縲～dedupe_hits` 縺悟｢励∴繧具ｼ郁ｪｲ驥代＆繧後↑縺・ｼ峨・
---

## 6. 遒ｺ隱肴ｸ医∩莠句ｮ滂ｼ亥ｮ滓ｸｬ or 荳谺｡雉・侭縺ｧ遒ｺ隱搾ｼ・
### 6-1. OpenAI Images API 窶・遒ｺ隱肴ｸ医∩

譬ｹ諡: 蜈ｬ蠑・OpenAPI 繝峨く繝･繝｡繝ｳ繝・`openai/openai-openapi` 縺ｮ `openapi.yaml`
・・CreateImageRequest` / `ImagesResponse` 繧堤峩謗･隱ｭ蜿厄ｼ会ｼ・螳・API 縺ｮ**髱櫁ｪｲ驥・*繝ｫ繝ｼ繝育｢ｺ隱阪・
| 鬆・岼 | 蜀・ｮｹ | 譬ｹ諡 |
|---|---|---|
| 繧ｨ繝ｳ繝峨・繧､繝ｳ繝・| `POST https://api.openai.com/v1/images/generations`・・SON・・| 蜈ｬ蠑・spec |
| 繝ｫ繝ｼ繝亥ｮ溷惠 | `GET` 蜷御ｸ URL 竊・**HTTP 405** `Invalid method for URL`・・OST 蟆ら畑縺ｨ縺励※蟄伜惠・・| 螳滓ｸｬ 2026-10-28 |
| 蠢・・| `prompt`・域怙螟ｧ 32,000 譁・ｭ暦ｼ峨～model` 縺ｯ譏守､ｺ謖・ｮ壽耳螂ｨ | 蜈ｬ蠑・spec |
| `model` 蛻玲嫌 | `gpt-image-2.5-flare`縲～gpt-image-2.5-sunburst`縲～gpt-image-2`縲～gpt-image-1.5`縲～gpt-image-1-mini`縲～chatgpt-image-latest`・・ `2026-09-08` 繧ｹ繝翫ャ繝励す繝ｧ繝・ヨ・・| 蜈ｬ蠑・spec 縺ｮ enum 縺ｨ `GET /v1/models`・・36 繝｢繝・Ν荳ｭ 10 縺檎判蜒冗ｳｻ・峨〒荳閾ｴ |
| `gpt-image-1` | **2026-10-23 縺ｫ shutdown 貂医∩**・・pec 縺ｮ deprecation 豕ｨ險假ｼ・| 蜈ｬ蠑・spec |
| `quality` | `auto / low / medium / high / xhigh / max`・・standard`/`hd` 縺ｯ蟒・ｭ｢ DALLﾂｷE 逕ｨ・・| 蜈ｬ蠑・spec |
| `size` | `auto`縲～1024x1024`縲～1536x1024`縲～1024x1536`縲∽ｻｻ諢上・ `WIDTHxHEIGHT`・井ｸ｡霎ｺ **16 縺ｮ蛟肴焚**縲√い繧ｹ繝壹け繝・1:3縲・:1縲∵怙螟ｧ 3840x2160縲・560x1440 雜・・螳滄ｨ鍋噪・・| 蜈ｬ蠑・spec |
| `output_format` | `png / jpeg / webp`・域里螳・png・峨Ａoutput_compression` 縺ｯ jpeg/webp 縺ｮ縺ｿ | 蜈ｬ蠑・spec |
| `background` | `auto / opaque / transparent`・・ransparent 縺ｯ png/webp 蠢・茨ｼ・| 蜈ｬ蠑・spec |
| `response_format` | **髱樊耳螂ｨ**縲・PT image 邉ｻ縺ｯ蟶ｸ縺ｫ base64 繧定ｿ斐☆・・url` 髱槫ｯｾ蠢懶ｼ・| 蜈ｬ蠑・spec |
| 繝ｬ繧ｹ繝昴Φ繧ｹ | `{created, data:[{b64_json}], background, output_format, size, quality, usage{...}}` | 蜈ｬ蠑・spec |
| 繝ｬ繝ｼ繝亥宛髯・隱ｲ驥代お繝ｩ繝ｼ | 400 / 403 / 410 / 429 / 500 / 503 | 蜈ｬ蠑・spec |

> 繧｢繧ｫ繧ｦ繝ｳ繝亥・縺ｮ `gpt-image-2.5-*` 螳滉ｾ｡譬ｼ縺ｯ **譛ｪ遒ｺ隱・*・按ｧ7・峨・
### 6-2. Tripo3D API 窶・遒ｺ隱肴ｸ医∩

譬ｹ諡: 蜈ｬ蠑上ラ繧ｭ繝･繝｡繝ｳ繝茨ｼ・S 繝ｬ繝ｳ繝繝ｪ繝ｳ繧ｰ縺ｮ縺溘ａ Markdown 蛹悶＆繧後◆蛟句挨繝壹・繧ｸ繧貞叙蠕暦ｼ会ｼ・蜈ｬ蠑・Python SDK `VAST-AI-Research/tripo-python-sdk` 縺ｮ繧ｽ繝ｼ繧ｹ ・・螳・API 縺ｮ髱櫁ｪｲ驥大他縺ｳ蜃ｺ縺励・
| 鬆・岼 | 蜀・ｮｹ | 譬ｹ諡 |
|---|---|---|
| v3 繝吶・繧ｹ | `https://openapi.tripo3d.ai`・医げ繝ｭ繝ｼ繝舌Ν・・/ `.com`・井ｸｭ蝗ｽ・・| SDK |
| v2 繝吶・繧ｹ | `https://api.tripo3d.ai/v2/openapi`・・*隱ｭ縺ｿ蜿悶ｊ邉ｻ縺ｯ縺ｾ縺逕溷ｭ・*・・| 螳滓ｸｬ 200 |
| 谿矩ｫ假ｼ・3・・| `GET /v3/account/balance` 竊・`{"code":0,"status":"success","data":{"balance":600.00,"frozen":0.00}}` | 螳滓ｸｬ 2026-10-28 |
| 谿矩ｫ假ｼ・2・・| `GET /v2/openapi/user/balance` 竊・`{"code":0,"data":{"balance":600,"frozen":0}}` | 螳滓ｸｬ 2026-10-28 |
| 繧ｿ繧ｹ繧ｯ菴懈・・・3・・| `POST /v3/generation/text-to-model`縲～POST /v3/generation/image-to-model`・・ET 縺ｯ 405 `use POST`・・| 螳滓ｸｬ 405 + 蜈ｬ蠑・docs |
| 繧ｿ繧ｹ繧ｯ菴懈・・・2・・| `POST /v2/openapi/task`・・ET 405 縺ｧ繝ｫ繝ｼ繝育函蟄倡｢ｺ隱搾ｼ・| 螳滓ｸｬ |
| 蜿ら・ | `GET /v3/tasks/{task_id}`縲」2 縺ｯ `GET /v2/openapi/task/{task_id}` | 蜈ｬ蠑・docs / SDK |
| 迥ｶ諷矩・遘ｻ | `queued 竊・running 竊・success / failed / banned / expired / cancelled`・・progress` 0-100・・| 蜈ｬ蠑・docs |
| 繧ｯ繝ｬ繧ｸ繝・ヨ | 菴懈・譎ゅ↓ freeze 竊・謌仙粥縺ｧ遒ｺ螳壹∝､ｱ謨・繧ｭ繝｣繝ｳ繧ｻ繝ｫ縺ｧ霑泌唆 | 蜈ｬ蠑・docs |
| 繝昴・繝ｪ繝ｳ繧ｰ | 1縲・ 遘帝俣髫斐∵怙螟ｧ 5 蛻・耳螂ｨ縲ゅヰ繝・メ縺ｯ `POST /v3/tasks/list` | 蜈ｬ蠑・docs |
| 繝ｪ繧ｯ繧ｨ繧ｹ繝域悽譁・ｼ・3・・| `{"prompt":..., "model":"v2.5-20250123", "face_limit":..., "texture":true, "pbr":true, "export_uv":true, ...}` | SDK・・2 縺ｮ繝輔ぅ繝ｼ繝ｫ繝牙錐繧・apiv3 繧ょ女逅・ｼ・|
| 繧｢繝・・繝ｭ繝ｼ繝・| `POST /v3/files`・・ultipart, field=`file`・俄・ `file_token`縲・0MB 雜・・ presign PUT | SDK・・*螳溷他縺ｳ蜃ｺ縺励・譛ｪ螳滓命**・・|
| 繧ｨ繝ｩ繝ｼ蠖｢蠑・| v3 譛ｪ遏･繧ｿ繧ｹ繧ｯ: HTTP 404 `{"code":2001,"status":"error","message":"The task is not found","suggestion":...,"request_id":...}`・・data` 辟｡縺暦ｼ・| 螳滓ｸｬ |
| v2 繧ｨ繝ｩ繝ｼ蠖｢蠑・| `{"code":9405,"message":"Method Not Allowed",...}`・・status` 辟｡縺励・v3 縺ｨ蠖｢縺碁＆縺・ｼ・| 螳滓ｸｬ |

> SDK 縺ｯ縲計2 縺ｯ繧ｷ繝｣繝・ヨ繝繧ｦ繝ｳ貂医∩縲阪→譏手ｨ倥＠縺ｦ縺・ｋ縺後∝ｮ滓ｸｬ縺ｧ縺ｯ v2 縺ｮ谿矩ｫ倡・莨壹→繝ｫ繝ｼ繝医・ 200/405 縺ｧ逕溷ｭ倥・> 譁ｰ隕丞ｮ溯｣・・ **v3 繧呈里螳・*縺ｨ縺励」2 縺ｯ蜿ら・逕ｨ繝輔か繝ｼ繝ｫ繝舌ャ繧ｯ縺ｫ逡吶ａ繧九・
### 6-3. Fish Audio API 窶・遒ｺ隱肴ｸ医∩

譬ｹ諡: 蜈ｬ蠑上ラ繧ｭ繝･繝｡繝ｳ繝・`docs.fish.audio` 縺ｮ OpenAPI 繧ｻ繧ｯ繧ｷ繝ｧ繝ｳ・・arkdown 迚茨ｼ会ｼ・螳・API 縺ｮ髱櫁ｪｲ驥大他縺ｳ蜃ｺ縺励・
| 鬆・岼 | 蜀・ｮｹ | 譬ｹ諡 |
|---|---|---|
| 繝吶・繧ｹ | `https://api.fish.audio` | 蜈ｬ蠑・docs |
| TTS | `POST /v1/tts`縲～Authorization: Bearer <FISH>` | 蜈ｬ蠑・docs |
| Content-Type | `application/json`・医∪縺溘・ msgpack・峨Ｎultipart 荳榊庄 | 蜈ｬ蠑・docs |
| 髻ｳ螢ｰ繝｢繝・Ν謖・ｮ・| **繝ｪ繧ｯ繧ｨ繧ｹ繝医・繝・ム `model`**縲Ｆnum: `s1` / `s2-pro` / `s2.1-pro` / `s2.1-pro-free` / `drama-3`・域里螳・`s2.1-pro`・・| 蜈ｬ蠑・docs |
| 譛ｬ譁・ｿ・・| `text` 縺ｮ縺ｿ蠢・医Ａreference_id`・磯浹螢ｰ繝｢繝・Ν ID・峨° `references`・医ぞ繝ｭ繧ｷ繝ｧ繝・ヨ・峨ｒ謗ｨ螂ｨ | 蜈ｬ蠑・docs |
| 蜃ｺ蜉帛ｽ｢蠑・| `wav / pcm / mp3 / opus`縲・*Ogg Vorbis 縺ｯ辟｡縺・*・・inecraft 髱槫ｯｾ蠢懊・縺溘ａ螟画鋤蠢・茨ｼ・| 蜈ｬ蠑・docs |
| 繧ｵ繝ｳ繝励Ν繝ｬ繝ｼ繝・| WAV 8/16/24/32/44.1kHz縲｀P3 32/44.1kHz縲＾pus 48kHz | 蜈ｬ蠑・docs |
| 螢ｰ繝｢繝・Ν荳隕ｧ | `GET /model?page_size=N` 竊・`{items:[{_id,title,state,train_mode,tags,languages}], total:...}` | 螳滓ｸｬ 200 |
| 繧ｨ繝ｩ繝ｼ | 401 / 402・域ｮ矩ｫ倅ｸ崎ｶｳ・・ 503 | 蜈ｬ蠑・docs |

> TTS 縺ｮ螳溘Ξ繧ｹ繝昴Φ繧ｹ・・ytes 縺・JSON 繝ｩ繝・・縺具ｼ峨・**譛ｪ遒ｺ隱・*縲ょｮ溯｣・・ `content-type` 縺ｨ蜈磯ｭ magic 縺ｧ
> 髻ｳ螢ｰ蛻､螳壹＠縲゛SON 縺ｮ蝣ｴ蜷医・ `audio`/`audio_base64`/`b64_json`/`data` 繧帝亟蠕｡逧・↓謗｢邏｢縺吶ｋ縲・
### 6-4. 繝ｭ繝ｼ繧ｫ繝ｫ迺ｰ蠅・窶・遒ｺ隱肴ｸ医∩

| 鬆・岼 | 邨先棡 |
|---|---|
| Python | 3.12.14・域欠螳壹Λ繝ｳ繧ｿ繧､繝・峨１illow 12.3.0 / numpy 2.3.5 |
| ffmpeg | **縺ゅｊ** `...\Gyan.FFmpeg...\ffmpeg-9.0.2-full_build\bin\ffmpeg.exe` |
| ffprobe | 縺ゅｊ・・AV/OGG 縺ｮ codec繝ｻ髟ｷ縺募叙蠕励↓菴ｿ逕ｨ・・|
| winget | 縺ゅｊ・・fmpeg 縺檎┌縺・腸蠅・髄縺代・蟆主・譯亥・縺ｫ險倩ｼ会ｼ・|
| `.env` | UTF-8 BOM 莉倥″縲Ａutf-8-sig` 蠢・・|

---

## 7. 譛ｪ遒ｺ隱阪・譛ｪ讀懆ｨｼ

> **2026-10-28 縺ｮ譛ｬ逡ｪ繝ｩ繝ｳ・按ｧ12・峨〒 #1縲・5, #8, #9, #11, #12 縺ｮ螟壹￥縺悟ｮ滓ｸｬ貂医∩縺ｫ縺ｪ縺｣縺溘・*
> 荳玖｡ｨ縺ｯ縲檎樟譎らせ縺ｧ谿九▲縺ｦ縺・ｋ譛ｪ遒ｺ隱阪阪↓譖ｴ譁ｰ貂医∩・亥叙繧頑ｶ医＠邱夲ｼ晁ｧ｣豸茨ｼ峨・
| # | 譛ｪ遒ｺ隱堺ｺ矩・| 迥ｶ諷・| 遒ｺ螳壹↓蠢・ｦ√↑縺薙→ |
|---|---|---|---|
| 1 | OpenAI `gpt-image-2.5-*` 縺ｮ **螳滉ｾ｡譬ｼ** | 笶・譛ｪ遒ｺ隱搾ｼ郁ｫ区ｱよ・邏ｰ縺ｧ隕∫・蜷茨ｼ・| 繝繝・す繝･繝懊・繝峨・隲区ｱる｡阪→ ledger 縺ｮ遯∝粋縲ら樟迥ｶ縺ｯ驕句霧閠・腰萓｡陦ｨ・・0.02/$0.07/$0.19・峨↓繧医ｋ謗ｨ螳・|
| 2 | `gpt-image-2.5-*` 縺ｮ 200 蠢懃ｭ斐・ `usage` | 笨・螳滓ｸｬ・・data[].b64_json` 繧堤｢ｺ隱阪Ａusage` 縺ｯ繝ｬ繧ｹ繝昴Φ繧ｹ縺ｫ蜷ｫ縺ｾ繧後★・晁ｪｲ驥第・邏ｰ縺ｪ縺暦ｼ・| 窶・|
| 3 | Tripo 繧ｿ繧ｹ繧ｯ菴懈・竊恥olling竊貞・蜉・URL 縺ｮ螳溘Ξ繧ｹ繝昴Φ繧ｹ | 笨・螳滓ｸｬ・・output.model_url` / `output.rendered_image_url`縲《tatus `queued竊池unning竊痴uccess`・・| 窶・|
| 4 | Tripo `model_version` 縺ｮ譛牙柑諤ｧ | 笨・螳滓ｸｬ・・v2.5-20250123` 縺ｧ 2 繧ｿ繧ｹ繧ｯ謌仙粥・・| 莉悶・繝｢繝・Ν迚医ｒ菴ｿ縺・ｴ蜷医・蛟句挨遒ｺ隱・|
| 5 | Tripo 繧ｯ繝ｬ繧ｸ繝・ヨ蜊倅ｾ｡・・SD・・| 笶・譛ｪ遒ｺ隱搾ｼ・30 credits/task` 縺ｯ螳滓ｸｬ縲ゞSD 謠帷ｮ励・驕句霧閠・ｨｭ螳・$0.02/credit・・| 隲区ｱよュ蝣ｱ縺ｮ遒ｺ隱・|
| 6 | Tripo v2 縺ｮ逕ｻ蜒上い繝・・繝ｭ繝ｼ繝峨ヵ繝ｭ繝ｼ | 笶・譛ｪ遒ｺ隱搾ｼ・3 `/v3/files` multipart 縺ｯ笨・虚菴懶ｼ・| v2 繧剃ｽｿ縺・ｿ・ｦ√′蜃ｺ縺溷ｴ蜷医・縺ｿ |
| 7 | Tripo 逕滓・迚ｩ縺ｮ繝ｩ繧､繧ｻ繝ｳ繧ｹ/蝠・畑蛻ｩ逕ｨ譚｡莉ｶ | 笶・**譛ｪ遒ｺ隱搾ｼ域ｳ募漁蛻､譁ｭ・・* | 蛻ｩ逕ｨ隕冗ｴ・・遒ｺ隱・|
| 8 | Fish TTS 縺ｮ螳溘Ξ繧ｹ繝昴Φ繧ｹ蠖｢蠑上・隱ｲ驥・| 笨・螳滓ｸｬ・・00繝ｻWAV bytes 霑泌唆竊・ffmpeg 縺ｧ Ogg Vorbis縲・4 譛ｬ謌仙粥・峨よ枚蟄怜腰萓｡縺ｯ驕句霧閠・ｨｭ螳・| 隲区ｱよ・邏ｰ縺ｧ隕∫・蜷・|
| 9 | Fish 縺ｮ髻ｳ螢ｰ繝｢繝・Ν ID・・reference_id`・・| 笞・・譛ｪ菴ｿ逕ｨ・育ｬｬ荳芽・け繝ｭ繝ｼ繝ｳ遖∵ｭ｢縺ｮ縺溘ａ譌｢螳壼｣ｰ縺ｮ縺ｿ菴ｿ逕ｨ・峨よ里螳壼｣ｰ縺ｧ 24 譛ｬ謌仙粥 | 繧ｪ繝ｪ繧ｸ繝翫Ν螢ｰ繧剃ｽｿ縺・ｴ蜷医・蛻･騾・|
| 10 | OpenAI / Fish 縺ｮ逕滓・迚ｩ縺ｮ讓ｩ蛻ｩ | 笶・**譛ｪ遒ｺ隱搾ｼ域ｳ募漁蛻､譁ｭ・・* | 隕冗ｴ・｢ｺ隱・|
| 11 | GeckoLib `.geo.json` 螟画鋤蟾･遞・| 笨・螳溯｣・・讒矩讀懆ｨｼ PASS・磯撕逧・・1 繝懊・繝ｳ繝ｻ繝代Ξ繝・ヨ繝・け繧ｹ繝√Ε・峨・*繧ｲ繝ｼ繝蜀・Ο繝ｼ繝峨・譛ｪ讀懆ｨｼ** | Lead/boss 諡・ｽ薙′螳滓ｩ溘Ο繝ｼ繝臥｢ｺ隱・|
| 12 | 逕滓・繝・け繧ｹ繝√Ε/繝昴・繝医Ξ繝ｼ繝医・隕九◆逶ｮ | 笨・逶ｮ隕也｢ｺ隱肴ｸ医∩・・2 菴薙・12 繝・け繧ｹ繝√Ε繝ｻ譫・・| 螳滓ｩ溘〒縺ｮ譛邨ら｢ｺ隱・|
| 13 | 繝懊う繧ｹ縺ｮ閨ｴ諢滂ｼ亥｣ｰ雉ｪ繝ｻ逋ｺ髻ｳ・・| 笞・・譛ｪ隧ｦ閨ｴ・亥ｽ｢蠑上・髟ｷ縺輔・縺ｿ讀懆ｨｼ・・| 莠ｺ髢薙↓繧医ｋ隧ｦ閨ｴ |

---

## 8. 螳溯｡後＠縺滓､懆ｨｼ縺ｨ邨先棡

### 8-1. `status`・亥ｿ・郁ｦ∽ｻｶ・・
```
& $py -m asset_pipeline status
竊・NOCTERRA asset pipeline v0.1.0 [DRY-RUN]
  ASSET_DRY_RUN=True / ASSET_AUTO_SPEND=False / max cost=$5.0000
  繧ｭ繝ｼ縺ｯ縺吶∋縺ｦ繝槭せ繧ｯ陦ｨ遉ｺ・・EEPSEEK sk-****/FISH sk-****/OPENAI sk-****/TRIPO tsk****・・  queue total=0 / ledger committed=$0.0000 / manifest assets=0
  ffmpeg: ffmpeg version 9.0.2-full_build
```

### 8-2. DRY RUN 縺ｮ螳溯ｨｼ・・TTP 繧帝√ｉ縺ｪ縺・％縺ｨ・・
- 譁ｹ豕・1: `dry-run texture_stone_tile` 螳溯｡・竊・騾∽ｿ｡莠亥ｮ・JSON繝ｻ隕狗ｩ・`$0.0700`繝ｻ繧ｲ繝ｼ繝育憾諷九ｒ蜃ｺ蜉帙・  逶｣譟ｻ繝ｭ繧ｰ `state/http_audit.jsonl` 縺・*逕滓・縺輔ｌ縺ｪ縺・*縺薙→繧堤｢ｺ隱阪・- 譁ｹ豕・2・医ｈ繧雁ｼｷ蜉幢ｼ・ selftest 縺ｧ `Transport.request` / `Transport.download` / `urllib.request.urlopen`
  繧・**萓句､悶ｒ謚輔￡繧矩未謨ｰ縺ｫ蟾ｮ縺玲崛縺医◆迥ｶ諷・*縺ｧ 3 繧ｸ繝ｧ繝悶ｒ preview 竊・3 莉ｶ縺ｨ繧よ・蜉滂ｼ茨ｼ扎TTP 髱樔ｾ晏ｭ假ｼ峨・- 蜃ｺ蜉帑ｾ具ｼ域栢邊具ｼ・

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

### 8-3. 繝・け繧ｹ繝√Ε蜉蟾･・医ム繝溘・ PNG縲Ａ.agent/tmp/asset_pipeline_selftest/`・・
| 讀懈渊 | 邨先棡 |
|---|---|
| 蜻ｨ譛滓ｨ｡讒・竊・繧ｿ繧､繝ｫ諤ｧ | `seamless`・・orst_ratio 1.0101・・|
| 蟾ｦ蜿ｳ繧ｰ繝ｩ繝・・繧ｷ繝ｧ繝ｳ 竊・繧ｿ繧､繝ｫ諤ｧ | `visible-seam`・・orst_ratio 175.0・・|
| 譛霑大ｍ繝ｪ繧ｵ繧､繧ｺ | 蜈・判蜒上↓蟄伜惠縺励↑縺・牡繧剃ｽ懊ｉ縺ｪ縺・ｼ郁｣憺俣縺ｪ縺暦ｼ・|
| 繧｢繝ｫ繝輔ぃ cutout | ﾎｱ=100 竊・0 / ﾎｱ=200 竊・255 |
| 繝代Ξ繝・ヨ驥丞ｭ仙喧 | 8 濶ｲ謖・ｮ・竊・繧ｵ繝ｳ繝励Ν 8 濶ｲ莉･荳九∃ｱ 菫晄戟 |
| 蜊倩牡閭梧勹縺ｮ flood fill | 128ﾃ・28 荳ｭ 10,859px・・6.28%・峨ｒ髯､蜴ｻ縲∬ｧ・ﾎｱ=0 |
| 16/32/64 蜃ｺ蜉・| 縺吶∋縺ｦ豁｣譁ｹ蠖｢繝ｻPNG 讀懆ｨｼ PASS繝ｻ繧ｿ繧､繝ｫ繝励Ξ繝薙Η繝ｼ逕滓・ |
| 豈碑ｼ・判蜒・| `<name>_compare.png` 逕滓・・・ource / processed 荳ｦ鄂ｮ・・|

### 8-4. GLB 讀懆ｨｼ・郁・菴・GLB・・
| 讀懈渊 | 邨先棡 |
|---|---|
| 豁｣蟶ｸ GLB・・ 荳芽ｧ貞ｽ｢, UV 縺ゅｊ・・| PASS / magic=glTF, version=2, chunks=JSON+BIN, meshes=1, accessors=4, tris=2, UV=true |
| magic 遐ｴ謳・| 諡貞凄 |
| version=1 | 諡貞凄 |
| 騾比ｸｭ縺ｧ蛻・ｌ縺・GLB | 諡貞凄 |
| 髱・GLB 繧ｴ繝・| 諡貞凄 |
| UV 辟｡縺・| 隴ｦ蜻・`no TEXCOORD_0` |
| 荳芽ｧ貞ｽ｢荳企剞 2 縺ｫ蟇ｾ縺・4 | 隴ｦ蜻奇ｼ医・繝ｪ繧ｴ繝ｳ莠育ｮ苓ｶ・℃・・|

### 8-5. 髻ｳ螢ｰ・・fmpeg 螳溷､画鋤・・
```
WAV 0.6s 44.1kHz 竊・ffmpeg libvorbis 竊・.ogg
validate: container=ogg, codec=vorbis, sample_rate=44100, duration=0.6  竊・PASS
・・inecraft 縺ｮ Ogg Vorbis 繝ｭ繝ｼ繝縺ｧ蜀咲函蜿ｯ閭ｽ縺ｪ蠖｢蠑上〒縺ゅｋ縺薙→繧堤｢ｺ隱搾ｼ・```

### 8-6. 莠育ｮ励・繧ｭ繝･繝ｼ

| 讀懈渊 | 邨先棡 |
|---|---|
| 荳企剞 $5 縺ｧ $3 莠育ｴ・竊・縺輔ｉ縺ｫ $2.5 莠育ｴ・| `BudgetExceeded`・・5.50 縺ｧ荳企剞雜・℃・峨〒諡貞凄縲ゆｺ育ｴ・粋險医・荳榊､・|
| 萓｡譬ｼ譛ｪ螳夂ｾｩ・・ripo・・| `price unknown` 縺ｧ諡貞凄・井ｺ育ｮ励ｒ險ｼ譏弱〒縺阪↑縺・◆繧・ｼ・|
| 繧ｲ繝ｼ繝・A: DRY_RUN=true + `--confirm-spend` | exit 2 縺ｧ諡貞凄縲？TTP 縺ｪ縺・|
| 繧ｲ繝ｼ繝・B: DRY_RUN=false / AUTO_SPEND=false | exit 2 縺ｧ諡貞凄縲？TTP 縺ｪ縺・|
| 繧ｲ繝ｼ繝・C: 荳｡繧ｲ繝ｼ繝磯幕謾ｾ + 莠育ｮ・0 + 繧ｨ繝ｳ繝峨・繧､繝ｳ繝医ｒ豁ｻ繧薙□繝ｭ繝ｼ繧ｫ繝ｫ繝昴・繝医↓蟾ｮ縺玲崛縺・| 蜿ｰ蟶ｳ縺・*騾∽ｿ｡蜑阪↓**諡貞凄縲・TTP 縺ｪ縺・|
| 繧ｲ繝ｼ繝・D: 荳｡繧ｲ繝ｼ繝磯幕謾ｾ + `--confirm-spend` 辟｡縺・| exit 2 縺ｧ諡貞凄 |
| 繧ｭ繝･繝ｼ | 驥崎､・賜髯､・亥酔荳繝上ャ繧ｷ繝･縺ｧ蜷御ｸ繧ｸ繝ｧ繝・/ `dedupe_hits` 蠅怜刈・・|
| 螟ｱ謨励Μ繝医Λ繧､ | attempts 1竊恥ending・医ヰ繝・け繧ｪ繝包ｼ俄・ attempts 3 縺ｧ failed |
| 荳ｭ譁ｭ蠕ｩ譌ｧ | `running` + lease 螟ｱ蜉ｹ 竊・`recover_stale()` 縺ｧ pending 縺ｫ蠕ｩ蟶ｰ |
| 繝ｬ繝ｼ繝亥宛髯・| 譛蟆城俣髫・0.25s 縺ｧ 2 蝗樒岼縺ｫ 0.249s 蠕・ｩ・|

### 8-7. 閾ｪ蟾ｱ讀懆ｨｼ繧ｹ繧､繝ｼ繝・
```
& $py -m asset_pipeline.selftest
竊・RESULT: 92 passed, 0 failed, 92 checks
```

---

## 9. 繧ｳ繧ｹ繝育ｮ｡逅・・隧ｳ邏ｰ

- **謗ｨ螳壼､縺ｨ螳溯ｲｻ**: OpenAI / Fish 縺ｯ API 縺梧・邏ｰ萓｡譬ｼ繧定ｿ斐＆縺ｪ縺・◆繧・°蝟ｶ閠・腰萓｡陦ｨ縺ｫ繧医ｋ謗ｨ螳壹・  Tripo 縺ｯ **谿矩ｫ伜ｷｮ蛻・°繧牙ｮ溘け繝ｬ繧ｸ繝・ヨ豸郁ｲｻ繧貞ｮ滓ｸｬ**・・0 credits/繧ｿ繧ｹ繧ｯ縲・0.60 @ $0.02/credit・峨・- **蜿ｰ蟶ｳ繝輔Ο繝ｼ**: `reserve`・磯∽ｿ｡蜑阪・蜴溷ｭ千噪・俄・ 謌仙粥 `commit` / 螟ｱ謨・`release`縲・  騾壻ｿ｡譁ｭ縺ｪ縺ｩ縲碁√▲縺溘°荳肴・縲阪・蝣ｴ蜷医・ `ambiguous_billing` 繧堤ｫ九※縲・*閾ｪ蜍・release 縺帙★**莠ｺ髢薙・遒ｺ隱阪↓蝗槭☆縲・- `cost` 繧ｳ繝槭Φ繝峨〒 繧ｵ繝ｼ繝薙せ蛻･繝ｻ繧ｸ繝ｧ繝門挨縺ｮ邏ｯ險医→莠育ｴ・ｒ遒ｺ隱阪〒縺阪ｋ縲・- **迴ｾ蝨ｨ縺ｮ邏ｯ險茨ｼ・edger 縺梧ｭ｣・・ committed $2.67612 / reserved $0.0000 / 谿・$2.32388・井ｸ企剞 $5.00・・*

| 繧ｵ繝ｼ繝薙せ | 隱ｲ驥代Μ繧ｯ繧ｨ繧ｹ繝・| committed | 蛯呵・|
|---|---|---|---|
| OpenAI | 30 莉ｶ | $1.4700 | 逕ｻ蜒冗函謌・30 蝗橸ｼ医≧縺｡ 3 蝗槭・菴手ｩ穂ｾ｡繝・け繧ｹ繝√Ε縺ｮ蜀咲函謌撰ｼ・|
| Tripo | 2 莉ｶ | $1.2000 | 螳滓ｸｬ 60 credits・・0/task・・|
| Fish | 24 莉ｶ | $0.00612 | 譌･譛ｬ隱・24 譛ｬ・域耳螳壽枚蟄怜腰萓｡・・|
| 蜷郁ｨ・| **56 莉ｶ** | **$2.67612** | 螟ｱ謨励・莠碁㍾隱ｲ驥・0 |

---

## 10. 謇ｿ隱阪′蠢・ｦ√↑縺薙→ / 谺｡縺ｮ繧ｹ繝・ャ繝・
1. **隲区ｱよ・邏ｰ縺ｨ縺ｮ遯∝粋**: OpenAI / Fish 縺ｮ螳溯ｫ区ｱる｡阪ｒ繝繝・す繝･繝懊・繝峨〒遒ｺ隱阪＠縲〕edger 縺ｮ謗ｨ螳壼､縺ｨ辣ｧ蜷・   ・・ripo 縺ｯ谿矩ｫ・600竊・40 縺ｧ螳滓ｸｬ貂医∩・峨・2. **`generated_assets/processed/` 縺ｮ謇ｱ縺・*: 迴ｾ陦・`.gitignore` 縺ｯ `raw/` `cache/` `tmp/` 縺ｮ縺ｿ髯､螟悶・   蜉蟾･貂医∩ PNG/OGG/GLB 繧偵さ繝溘ャ繝医☆繧九・縺九［od 縺ｮ `resources` 縺ｸ繧ｳ繝斐・縺吶ｋ縺ｮ縺・Lead 蛻､譁ｭ縺悟ｿ・ｦ√・3. **`tools/asset_pipeline/state/`**: 螳溯｡梧凾迥ｶ諷具ｼ亥・阡ｵ `.gitignore` 縺ｧ閾ｪ蟾ｱ髯､螟厄ｼ峨ゅΝ繝ｼ繝・`.gitignore` 縺ｸ縺ｮ
   霑ｽ險伜庄蜷ｦ・域悽諡・ｽ薙・繝ｫ繝ｼ繝・`.gitignore` 繧堤ｷｨ髮・＠縺ｦ縺・↑縺・ｼ峨・4. **GeckoLib 螳滓ｩ溽｢ｺ隱・*: `.geo.json` 縺ｯ讒矩讀懆ｨｼ貂医∩縺縺・**繧ｲ繝ｼ繝蜀・Ο繝ｼ繝峨・譛ｪ讀懆ｨｼ**縲・   `geometry.nocterra.<boss_id>` / `rotwood_king.png` 縺ｮ繝代せ縺ｧ隱ｭ縺ｿ霎ｼ繧√ｋ縺・boss 諡・ｽ薙→遒ｺ隱阪・5. **繝ｩ繧､繧ｻ繝ｳ繧ｹ**: 3 繧ｵ繝ｼ繝薙せ縺吶∋縺ｦ `UNVERIFIED` 縺ｨ縺励※ manifest 縺ｫ險倬鹸貂医∩縲ょ・髢句燕縺ｮ豕募漁遒ｺ隱阪・6. **髻ｳ螢ｰ縺ｮ隧ｦ閨ｴ**: 24 譛ｬ縺ｯ蠖｢蠑上・髟ｷ縺輔・縺ｿ讀懆ｨｼ縲ょ｣ｰ雉ｪ繝ｻ逋ｺ髻ｳ縺ｮ human review 縺悟ｿ・ｦ√・7. **谿倶ｺ育ｮ・$2.32 縺ｮ菴ｿ縺・％**: 霑ｽ蜉繝懊せ髻ｳ螢ｰ/繝・け繧ｹ繝√Ε縲√∪縺溘・ Tripo 霑ｽ蜉 1 菴難ｼ遺沿$0.67・臥ｭ峨よ価隱肴凾縺ｮ縺ｿ縲・
---

## 11. 蜿り・Μ繝ｳ繧ｯ・井ｸ谺｡雉・侭・・
- OpenAI Images API 繝ｪ繝輔ぃ繝ｬ繝ｳ繧ｹ: <https://developers.openai.com/api/reference/resources/images/methods/generate>
- OpenAI 蜈ｬ蠑・OpenAPI・井ｻ雁屓縺ｮ譬ｹ諡・・ <https://github.com/openai/openai-openapi>
- Tripo Task Lifecycle・育憾諷矩・遘ｻ繝ｻfreeze/deduct・・ <https://developers.tripo3d.ai/en/docs/task-lifecycle>
- Tripo 蜈ｬ蠑・Python SDK・・3 螳溯｣・・譬ｹ諡・・ <https://github.com/VAST-AI-Research/tripo-python-sdk>
- Fish Audio TTS API: <https://docs.fish.audio/api-reference/endpoint/openapi-v1/text-to-speech>

> 豕ｨ: Tripo 縺ｮ繝峨く繝･繝｡繝ｳ繝医・ JS 繝ｬ繝ｳ繝繝ｪ繝ｳ繧ｰ縺縺後～developers.tripo3d.ai/en/docs/task-lifecycle` 縺ｯ
> 譛ｬ譁・′蜿門ｾ励〒縺阪◆・亥叙蠕玲律 2026-10-28・峨ょ叙蠕励〒縺阪↑縺・・繝ｼ繧ｸ縺ｯ SDK 繧ｽ繝ｼ繧ｹ縺ｧ陬懷ｮ後＠縺溘・

---

## 12. 本番生成ランの結果（2026-10-28 / Lead 承認済み LIVE-SPEND-ENABLED）

### 12-1. 実額（ledger が正 / `python -m asset_pipeline cost`）

| サービス | リクエスト数 | committed | 備考 |
|---|---|---|---|
| OpenAI Images | 30 | **$1.4700** | ポートレート12・枠1・テクスチャ12＋再生成3・concept2 |
| Tripo3D | 2 | **$1.2000** | **残高 600→540 で実測 60 credits（30/task）** |
| Fish Audio | 24 | **$0.00612** | 日本語セリフ 24 本（運営者文字単価） |
| **合計** | **56** | **$2.67612** | 残 **$2.32388 / 上限 $5.00**、失敗・二重課金 0 |

- OpenAI/Fish は API が明細価格を返さないため **運営者単価表による推定額**。実際の請求額との突合は未実施。
- Tripo は非課金の残高照会（`GET /v3/account/balance`）をタスク前後で比較し **実消費を確定**（推定 20 → 実際 30 credits）。

### 12-2. 納品物（すべて `validate.py` 通過。実パス）

| 種別 | 件数 | パス |
|---|---|---|
| ボス・ポートレート 128×128 RGBA（上部透過） | 12 | `generated_assets/processed/hud/boss_portrait/<boss_id>.png` |
| ボス HP バー枠 512×128（9-slice: 中央比 0.14） | 1 | `generated_assets/processed/hud/boss_frame.png` |
| ボス HP バー塗り 256×32（ローカル生成 $0） | 1 | `generated_assets/processed/hud/boss_bar_fill.png` |
| ブロックテクスチャ 16×16（`ashen_portal` のみ 32×32） | 8 | `generated_assets/processed/textures/block/<name>.png` |
| アイテムアイコン 16×16（透過・スプライト） | 4 | `generated_assets/processed/textures/item/<name>.png` |
| ボス音声 Ogg Vorbis（各 3.2〜5.8 秒） | 24 | `generated_assets/processed/audio/boss/<boss_id>_<1|2>.ogg` |
| Tripo GLB（11931 / 11934 tri, UV あり） | 2 | `generated_assets/processed/geckolib/<boss_id>.glb` |
| GeckoLib `.geo.json`＋パレット PNG | 2 組 | `generated_assets/processed/geckolib/<boss_id>.geo.json` / `.png` |

- グリッド: ブロック 8 種中 7 種 `seamless`・2 種（`ash_coal_ore`, `ashwood_planks`）`marginal`（warn 記録済み）。
- アイテムは性質上タイル不可（透過スプライト）。`tileability` は `non-tiling (transparent edges)` と正しく判定。
- 枠は「端 16px 装飾＋中央 16px 繰り返し」。生成物の中央比 0.14 のため**強制タイル処理はせず**原画の中央帯を保持（後述 12-5）。
- 全 28 PNG / 24 OGG / 4 GLB / 2 JSON / 2 geo.json が検証 PASS。目視レビュー実施済み（12 体・12 テクスチャ・枠・voxel プレビュー）。

### 12-3. 実 API で確認できた事実（§7 の未確認を解消）

| 項目 | 実測結果 |
|---|---|
| OpenAI Images 200 応答 | `data[0].b64_json` に PNG（1040〜2142 KB）。`output_format=png`・`background=transparent` が反映。`usage` は返らず価格情報なし |
| OpenAI 制約 | `size=1536x512`（3:1）・`1024x1536`・`quality=low/medium/high`・`background=transparent/opaque` すべて受理 |
| Fish TTS 200 応答 | `Content-Type` に依らず **WAV バイト列**（RIFF）を返却。HEAD ヘッダ `model: s2.1-pro` + `format=wav` + `language=ja` が有効 |
| Fish 課金 | 24 本で残高変動なし（文字課金はクレジット式でない/閾値未満の可能性）。実請求は要確認 |
| Tripo タスク | `POST /v3/generation/image-to-model` 201/200 → `task_id`。`GET /v3/tasks/{id}` が `queued→running(2%…87%)→success(100%)`。所要 1〜2 分 |
| Tripo 出力 | `output` キーは **`model_url` と `rendered_image_url`** の 2 つ。GLB は PBR（テクスチャ 3・イメージ 3・UV あり） |
| Tripo 課金 | **30 credits/タスク**（残高差分で実測）。freeze→deduct は docs 通り |
| Tripo アップロード | `POST /v3/files` multipart（field=`file`）＋`file_token` を `input` に渡す方式が**動作** |
| GLB 構造 | magic `glTF` v2、JSON+BIN チャンク、`generator=https://tripo3d.ai`、skins/animations なし、11931/11934 triangles、UV あり |

### 12-4. ボクセル化 → GeckoLib（挑戦枠の結果 = **成功／ただし制約あり**）

`glb_to_geckolib()`（`tools/asset_pipeline/voxelize.py`）で実装。

| 項目 | rotwood_king | black_crowned_king |
|---|---|---|
| 解像度 | 32（高さ 40px = 2.5 ブロック） | 32 |
| 表面ボクセル | 1,970 | 約 2,000 |
| キューブ数（X 方向マージ後） | **1,847** | **1,859** |
| パレット色 / アトラス | 129 色 / 256×136 | 92 色 / 256×96 |
| 構造検証 | PASS（1 ボーン・UV 範囲内・テクスチャ寸法一致） | PASS |

**正直な制約（manifest の `caveats` にも記録）**
- 静的ジオメトリのみ（`body` 1 ボーンのみ。スケルトン／アニメーションなし）。
- PBR/ノーマルマップは破棄され、**単色パレットアトラス**に置換。
- スケール・向きは近似。**ゲーム内ロードは未検証**（GeckoLib 5.5.7 での実機確認が必要）。
- 1 ボクセル未満の細部（指・装飾）は消失。プレビューは `tools/asset_pipeline/state/previews/`。

### 12-5. ランの途中で見つけて直した不具合（正直な記録）

1. **`.env` の UTF-8 BOM**: 先頭キーが `\ufeffTRIPO` になる → `utf-8-sig` で解決（実装済み）。
2. **最初の有料画像で manifest 書き込みが `np.int64` で失敗**（`remove_background` の flood 画素数）。
   画像は生成・課金済みだったため、`util.json_default` を追加し、`int()` 化。以降 56 件すべて成功。
3. **ポートレートの背景判定ミス**: 胸像は下端が胴体で不透明が正常なのに「四隅すべて透過」を要求していた
   → 上部＝背景、下部＝バストとして判定を修正（12 体すべて `bg_ok=True`）。
4. **枠の過剰タイル処理**: 生成原画の中央が既に seamless（比 0.14）だったのに 16px サンプルで置換し
   装飾が壊れた → 計測して必要時のみ強制する方式に変更し、**無課金で再処理**。
5. **セリフ 8 本が 15 字未満**（自作チェックで拒否）→ 台詞を書き直して再生成（成功）。
6. **ボクセライザの UV にじみ**: 1 テクセル／色だと隣の色を拾う → **色ごとの矩形ブロック**を持つ
   アトラスに変更し、各キューブが単色になるよう修正。
7. 構文エラーを `compileall` が課金前に検出（2 体目 Tripo 実行前）。無駄な課金 0。

### 12-6. 再現コマンド（`tools/` から）

```powershell
$py = "C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py -m asset_pipeline cost                       # 実額（ledger）
& $py -m asset_pipeline list --manifest            # 来歴（83 アセット）
& $py -m asset_pipeline produce hud --confirm-spend
& $py -m asset_pipeline produce textures --confirm-spend
& $py -m asset_pipeline produce audio --confirm-spend
& $py -m asset_pipeline produce tripo --only rotwood_king --confirm-spend
& $py -m asset_pipeline produce voxelize --confirm-spend   # ローカル変換（無料）
& $py -m asset_pipeline validate <path> [...]
& $py -m asset_pipeline.selftest                   # 92 checks（LIVE 環境でも安全）
```

- 既存アセットは manifest を見て**再利用**（再課金しない）。再生成は `--force` を明示した場合のみ。
- 生成物の集計は `generated_assets/manifests/PRODUCTION_SUMMARY.json`、来歴は `assets.json`、
  実行レポートは `production_report.json`。

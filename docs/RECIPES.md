# NOCTERRA 全レシピ・入手方法データベース

> このファイルは `nocterra` のデータ（レシピ JSON / ルートテーブル / 言語ファイル）から自動生成しています。

> 生成元: `nocterra/src/main/resources/data/nocterra`（レシピ 50 件）


## 目次

1. [はじめかた（古代の門の作り方）](#1-はじめかた古代の門の作り方)
2. [全レシピ一覧](#2-全レシピ一覧)
3. [敵のドロップ一覧](#3-敵のドロップ一覧)
4. [素材の入手方法まとめ](#4-素材の入手方法まとめ)
5. [武器・防具の入手方法](#5-武器防具の入手方法)
6. [ボスと刻印](#6-ボスと刻印)
7. [構造物](#7-構造物)

---

## 1. はじめかた（古代の門の作り方）

### 手順

1. **エンバーシャード**をクラフトする（作業台・シャップレス）
   - 材料: **石炭 ×1 ＋ 火打石 ×1 ＋ 金インゴット ×1** → **エンバーシャード ×2**
   - ※ 火打石は「砂利 ＋ 鉄インゴット」でクラフト可能。金インゴットは金鉱石の精錬で入手
2. **古代の門の骨組み**（`gate_frame`）をクラフトする（作業台・シャップレス）
   - 材料: **石レンガ ×8 ＋ エンバーシャード ×1** → **古代の門の骨組み ×8**
   - ※ 石レンガは「石 ×4（精錬）→ 石レンガ ×4」
3. 骨組みで **内側が横2×縦3以上** の枠を作る（最大 21×21）
   - **必ず骨組みだけで四角い枠を閉じる**（角も骨組みで埋める）
   - 内側は空気（または火）にしておく
4. **エンバーシャードを骨組みのどれかを右クリック**すると点火し、灰冠の門（`ashen_portal`）が開く
5. 門の中に入ると **ノクテラ** へ転送される。到着地点には帰還用の門が自動生成される

> **うまく点火できないとき**: 内側の空気マスが消えていないか / 枠が閉じているか / 内側が 2×3 以上かを確認してください。

## 2. 全レシピ一覧

| 完成品 | 数量 | 材料 | 形式 |
|---|---|---|---|
| **灰装備の具足** `nocterra:ashen_boots` | 1 | 灰の繊維 ×4 | crafting_shaped |
| **灰装備の鎧** `nocterra:ashen_chestplate` | 1 | 灰の繊維 ×8 ＋ 沼鉄 | crafting_shaped |
| **灰装備の兜** `nocterra:ashen_helmet` | 1 | 灰の繊維 ×5 | crafting_shaped |
| **灰装備の脚甲** `nocterra:ashen_leggings` | 1 | 灰の繊維 ×7 | crafting_shaped |
| **灰繊維のサーベル** `nocterra:ashen_sabre` | 1 | 灰の繊維 ×4 ＋ 灰木の板材 ×2 ＋ 沼鉄 | crafting_shaped |
| **灰繊維のサーベル** `nocterra:ashen_sabre` | 1 | 灰繊維のサーベル ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **灰繊維のサーベル** `nocterra:ashen_sabre` | 1 | 砕けた冠片 ×2 ＋ 灰繊維のサーベル ＋ ベールの絹 | crafting_shapeless |（enhancement=2）
| **灰繊維のサーベル** `nocterra:ashen_sabre` | 1 | 砕けた冠片 ×2 ＋ 灰繊維のサーベル ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）
| **灰木の刃** `nocterra:ashwood_blade` | 1 | 灰木の板材 ×2 ＋ stick | crafting_shaped |
| **灰木の刃** `nocterra:ashwood_blade` | 1 | 灰木の刃 ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **灰木の刃** `nocterra:ashwood_blade` | 1 | 砕けた冠片 ×2 ＋ 灰木の刃 ＋ 朽葉の樹脂 | crafting_shapeless |（enhancement=2）
| **灰木の刃** `nocterra:ashwood_blade` | 1 | 砕けた冠片 ×2 ＋ 灰木の刃 ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）
| **灰木の板材** `nocterra:ashwood_planks` | 4 | 灰木の原木 | crafting_shapeless |
| **沼鉄の大剣** `nocterra:bog_iron_greatsword` | 1 | 沼鉄 ×3 ＋ 灰木の原木 ×2 ＋ エンバーシャード | crafting_shaped |
| **沼鉄の大剣** `nocterra:bog_iron_greatsword` | 1 | 沼鉄の大剣 ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **沼鉄の大剣** `nocterra:bog_iron_greatsword` | 1 | 砕けた冠片 ×2 ＋ 沼鉄の大剣 ＋ 潮の真珠 | crafting_shapeless |（enhancement=2）
| **沼鉄の大剣** `nocterra:bog_iron_greatsword` | 1 | 砕けた冠片 ×2 ＋ 沼鉄の大剣 ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）
| **冠の王笏** `nocterra:crown_scepter` | 1 | 砕けた冠片 ×3 ＋ 星屑の欠片 ＋ 火核 | crafting_shapeless |
| **冠砕き** `nocterra:crownbreaker` | 1 | 砕けた冠片 ×2 ＋ 沼鉄 ×2 ＋ 深淵の鱗 | crafting_shaped |
| **冠砕き** `nocterra:crownbreaker` | 1 | 冠砕き ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **冠砕き** `nocterra:crownbreaker` | 1 | 砕けた冠片 ×2 ＋ 冠砕き ＋ 黄金の砂 | crafting_shapeless |（enhancement=2）
| **冠砕き** `nocterra:crownbreaker` | 1 | 砕けた冠片 ×2 ＋ 冠砕き ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）
| **エンバーシャード** `nocterra:ember_shard` | 2 | coal ＋ flint ＋ gold ingot | crafting_shapeless |
| **霜の具足** `nocterra:frost_boots` | 1 | 霜の結晶 ×2 ＋ 沼鉄 ×2 | crafting_shapeless |
| **霜の鎧** `nocterra:frost_chestplate` | 1 | 霜の結晶 ×5 ＋ 沼鉄 ×3 | crafting_shapeless |
| **霜の兜** `nocterra:frost_helmet` | 1 | 霜の結晶 ×3 ＋ 沼鉄 ×2 | crafting_shapeless |
| **霜の脚甲** `nocterra:frost_leggings` | 1 | 霜の結晶 ×4 ＋ 沼鉄 ×3 | crafting_shapeless |
| **霜の大鎌** `nocterra:frost_scythe` | 1 | 霜の結晶 ×2 ＋ 沼鉄 ×2 ＋ 亡霊の燐粉 ×2 | crafting_shapeless |
| **古代の門の骨組み** `nocterra:gate_frame` | 8 | stone bricks ×8 ＋ エンバーシャード | crafting_shapeless |
| **墓所の大剣** `nocterra:grave_greatsword` | 1 | iron ingot ×4 ＋ 灰石 ＋ stick | crafting_shaped |
| **墓所の大剣** `nocterra:grave_greatsword` | 1 | 墓所の大剣 ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **墓所の大剣** `nocterra:grave_greatsword` | 1 | 砕けた冠片 ×2 ＋ 墓所の大剣 ＋ 墓土の塵 | crafting_shapeless |（enhancement=2）
| **墓所の大剣** `nocterra:grave_greatsword` | 1 | 砕けた冠片 ×2 ＋ 墓所の大剣 ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）
| **墓土の戦槌** `nocterra:grave_hammer` | 1 | 墓土の塵 ×3 ＋ 沼鉄 ×2 ＋ 灰木の原木 | crafting_shapeless |
| **嘆きの大鎌** `nocterra:mourning_scythe` | 1 | iron ingot ×2 ＋ 灰木の板材 ＋ エンバーシャード ＋ stick | crafting_shaped |
| **白刃の短剣** `nocterra:pale_dagger` | 1 | ベールの絹 ×2 ＋ 亡霊の燐粉 ×2 ＋ 灰の繊維 ×2 | crafting_shapeless |
| **樹脂の具足** `nocterra:resin_boots` | 1 | 朽葉の樹脂 ×2 ＋ 灰の繊維 ×2 | crafting_shapeless |
| **樹脂の鎧** `nocterra:resin_chestplate` | 1 | 朽葉の樹脂 ×5 ＋ 灰の繊維 ×3 | crafting_shapeless |
| **樹脂の兜** `nocterra:resin_helmet` | 1 | 朽葉の樹脂 ×3 ＋ 灰の繊維 ×2 | crafting_shapeless |
| **樹脂の脚甲** `nocterra:resin_leggings` | 1 | 朽葉の樹脂 ×4 ＋ 灰の繊維 ×3 | crafting_shapeless |
| **樹脂の長槍** `nocterra:resin_spear` | 1 | 朽葉の樹脂 ×2 ＋ 灰の繊維 ×2 ＋ 灰木の原木 | crafting_shapeless |
| **砂刃** `nocterra:sandglass_blade` | 1 | 黄金の砂 ×2 ＋ 沼鉄 ＋ エンバーシャード | crafting_shapeless |
| **星屑の薙刀** `nocterra:starfall_glaive` | 1 | 星屑の欠片 ×2 ＋ 砕けた冠片 ×2 ＋ 沼鉄 ×2 | crafting_shapeless |
| **潮の三叉槍** `nocterra:tidal_trident` | 1 | 潮の真珠 ×2 ＋ 深淵の鱗 ×2 ＋ 沼鉄 | crafting_shapeless |
| **ベールの杖** `nocterra:veil_wand` | 1 | 亡霊の燐粉 ×3 ＋ ベールの絹 ×2 ＋ エンバーシャード | crafting_shapeless |
| **火山の大槌** `nocterra:volcanic_maul` | 1 | 火山の鱗 ×2 ＋ 火核 ×2 ＋ 沼鉄 ×2 | crafting_shapeless |
| **燐粉の短剣** `nocterra:wispglass_dagger` | 1 | 亡霊の燐粉 ×3 ＋ 灰の繊維 ×2 ＋ エンバーシャード | crafting_shaped |
| **燐粉の短剣** `nocterra:wispglass_dagger` | 1 | 燐粉の短剣 ＋ 砕けた冠片 | crafting_shapeless |（enhancement=1）
| **燐粉の短剣** `nocterra:wispglass_dagger` | 1 | 砕けた冠片 ×2 ＋ 燐粉の短剣 ＋ 霜の結晶 | crafting_shapeless |（enhancement=2）
| **燐粉の短剣** `nocterra:wispglass_dagger` | 1 | 砕けた冠片 ×2 ＋ 燐粉の短剣 ＋ 星屑の欠片 | crafting_shapeless |（enhancement=3）

### 強化レシピ（+1〜+3）

武器は **砕けた冠片** と地域素材で段階強化できます。1段階ごとに攻撃力 +2。

| 完成品 | 材料 | 備考 |
|---|---|---|

## 3. 敵のドロップ一覧

### 深淵の海帝 `nocterra:abyssal_sea_emperor`

| アイテム | 確率 / 個数 |
|---|---|
| prismarine shard | 100% / 4〜8個 |
| heart of the sea | 100% |
| nautilus shell | 100% / 1〜2個 |
| 深淵の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 深淵の鱗 | 100% / 1〜1個 |
| 潮の真珠 | 100% / 1〜2個 |

### 灰樹の這い虫 `nocterra:ash_crawler`

| アイテム | 確率 / 個数 |
|---|---|
| bone | 100% / 0〜1個 |
| coal | 100% / 1〜2個 |
| rotten flesh | 100% |
| 灰の繊維 | 45% |
| 墓土の塵 | 20% |

### 灰樹の亡霊 `nocterra:ashen_wraith`

| アイテム | 確率 / 個数 |
|---|---|
| bone | 100% / 0〜2個 |
| coal | 100% |
| rotten flesh | 100% / 1〜2個 |
| 灰の繊維 | 45% |
| 朽葉の樹脂 | 30% |

### 墜星竜アステル `nocterra:astel_fallen_star_dragon`

| アイテム | 確率 / 個数 |
|---|---|
| ender pearl | 100% / 2〜4個 |
| amethyst shard | 100% / 3〜6個 |
| diamond | 100% / 1〜2個 |
| 墜星の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 星屑の欠片 | 100% / 1〜1個 |

### 黒冠の王 `nocterra:black_crowned_king`

| アイテム | 確率 / 個数 |
|---|---|
| obsidian | 100% / 4〜8個 |
| netherite scrap | 100% / 2〜4個 |
| gold block | 100% |
| 砕けた冠片 | 100% / 1〜3個 |

### 沼の屍鬼 `nocterra:bog_ghoul`

| アイテム | 確率 / 個数 |
|---|---|
| rotten flesh | 100% / 0〜2個 |
| bone | 100% / 1〜3個 |
| iron nugget | 100% / 1〜4個 |
| 沼鉄 | 35% |
| 墓土の塵 | 30% |

### 断罪の騎士長 `nocterra:condemned_knight_lord`

| アイテム | 確率 / 個数 |
|---|---|
| iron ingot | 100% / 4〜8個 |
| gold ingot | 100% / 1〜3個 |
| iron block | 100% |
| 断罪の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 墓土の塵 | 100% / 1〜2個 |

### 灯火の化身 `nocterra:ember_incarnation`

| アイテム | 確率 / 個数 |
|---|---|
| glowstone | 100% / 4〜8個 |
| gold ingot | 100% / 4〜8個 |
| blaze rod | 100% / 1〜3個 |
| 砕けた冠片 | 100% / 1〜3個 |
| 火核 | 100% / 1〜1個 |

### 忘却された騎士 `nocterra:forgotten_knight`

| アイテム | 確率 / 個数 |
|---|---|
| iron ingot | 100% / 3〜6個 |
| gold block | 100% |
| diamond | 100% / 1〜2個 |
| 砕けた冠片 | 100% / 1〜3個 |

### 灼鋼の巨神 `nocterra:molten_steel_colossus`

| アイテム | 確率 / 個数 |
|---|---|
| netherite scrap | 100% / 2〜4個 |
| magma block | 100% / 2〜5個 |
| blaze powder | 100% / 2〜4個 |
| 灼鋼の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 火核 | 100% / 1〜1個 |
| 火山の鱗 | 100% / 1〜2個 |

### 嘆きの亡霊 `nocterra:mourning_wisp`

| アイテム | 確率 / 個数 |
|---|---|
| glowstone dust | 100% / 0〜2個 |
| phantom membrane | 100% / 0〜1個 |
| glow ink sac | 100% |
| 亡霊の燐粉 | 40% |
| ベールの絹 | 30% |

### 名もなき観測者 `nocterra:nameless_observer`

| アイテム | 確率 / 個数 |
|---|---|
| ender pearl | 100% / 3〜6個 |
| amethyst shard | 100% / 4〜8個 |
| ender eye | 100% |
| 砕けた冠片 | 100% / 1〜3個 |

### 腐朽の森王 `nocterra:rotwood_king`

| アイテム | 確率 / 個数 |
|---|---|
| netherite scrap | 100% / 2〜4個 |
| gold block | 100% |
| bone | 100% / 4〜8個 |
| 朽葉の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 朽葉の樹脂 | 100% / 1〜2個 |

### 砂時計の双王 `nocterra:sandglass_twin_kings`

| アイテム | 確率 / 個数 |
|---|---|
| gold ingot | 100% / 3〜6個 |
| clock | 100% |
| gold block | 100% |
| 双砂の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 黄金の砂 | 100% / 1〜2個 |

### 白銀の処刑者 `nocterra:silver_executioner`

| アイテム | 確率 / 個数 |
|---|---|
| iron ingot | 100% / 3〜6個 |
| packed ice | 100% / 4〜8個 |
| diamond | 100% |
| 白銀の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 霜の結晶 | 100% / 1〜2個 |

### 悲嘆の聖女 `nocterra:sorrow_saint`

| アイテム | 確率 / 個数 |
|---|---|
| prismarine shard | 100% / 4〜8個 |
| heart of the sea | 100% |
| prismarine crystals | 100% / 1〜3個 |
| 悲嘆の刻印 | 100% |
| 砕けた冠片 | 100% / 1〜3個 |
| 深淵の鱗 | 100% / 1〜1個 |
| ベールの絹 | 100% / 1〜2個 |

## 4. 素材の入手方法まとめ

| 素材 | 入手方法 |
|---|---|
| amethyst shard | 墜星竜アステル（100% / 3〜6個） / 名もなき観測者（100% / 4〜8個） |
| blaze powder | 灼鋼の巨神（100% / 2〜4個） |
| blaze rod | 灯火の化身（100% / 1〜3個） |
| bone | 灰樹の這い虫（100% / 0〜1個） / 灰樹の亡霊（100% / 0〜2個） / 沼の屍鬼（100% / 1〜3個） / 腐朽の森王（100% / 4〜8個） |
| clock | 砂時計の双王（100%） |
| coal | 灰樹の這い虫（100% / 1〜2個） / 灰樹の亡霊（100%） |
| diamond | 墜星竜アステル（100% / 1〜2個） / 忘却された騎士（100% / 1〜2個） / 白銀の処刑者（100%） |
| ender eye | 名もなき観測者（100%） |
| ender pearl | 墜星竜アステル（100% / 2〜4個） / 名もなき観測者（100% / 3〜6個） |
| glow ink sac | 嘆きの亡霊（100%） |
| glowstone | 灯火の化身（100% / 4〜8個） |
| glowstone dust | 嘆きの亡霊（100% / 0〜2個） |
| gold block | 黒冠の王（100%） / 忘却された騎士（100%） / 腐朽の森王（100%） / 砂時計の双王（100%） |
| gold ingot | 断罪の騎士長（100% / 1〜3個） / 灯火の化身（100% / 4〜8個） / 砂時計の双王（100% / 3〜6個） |
| heart of the sea | 深淵の海帝（100%） / 悲嘆の聖女（100%） |
| iron block | 断罪の騎士長（100%） |
| iron ingot | 断罪の騎士長（100% / 4〜8個） / 忘却された騎士（100% / 3〜6個） / 白銀の処刑者（100% / 3〜6個） |
| iron nugget | 沼の屍鬼（100% / 1〜4個） |
| magma block | 灼鋼の巨神（100% / 2〜5個） |
| nautilus shell | 深淵の海帝（100% / 1〜2個） |
| netherite scrap | 黒冠の王（100% / 2〜4個） / 灼鋼の巨神（100% / 2〜4個） / 腐朽の森王（100% / 2〜4個） |
| obsidian | 黒冠の王（100% / 4〜8個） |
| packed ice | 白銀の処刑者（100% / 4〜8個） |
| phantom membrane | 嘆きの亡霊（100% / 0〜1個） |
| prismarine crystals | 悲嘆の聖女（100% / 1〜3個） |
| prismarine shard | 深淵の海帝（100% / 4〜8個） / 悲嘆の聖女（100% / 4〜8個） |
| rotten flesh | 灰樹の這い虫（100%） / 灰樹の亡霊（100% / 1〜2個） / 沼の屍鬼（100% / 0〜2個） |
| ベールの絹 | 嘆きの亡霊（30%） / 悲嘆の聖女（100% / 1〜2個） |
| 亡霊の燐粉 | 嘆きの亡霊（40%） |
| 双砂の刻印 | 砂時計の双王（100%） |
| 墓土の塵 | 灰樹の這い虫（20%） / 沼の屍鬼（30%） / 断罪の騎士長（100% / 1〜2個） |
| 墜星の刻印 | 墜星竜アステル（100%） |
| 悲嘆の刻印 | 悲嘆の聖女（100%） |
| 断罪の刻印 | 断罪の騎士長（100%） |
| 星屑の欠片 | 墜星竜アステル（100% / 1〜1個） |
| 朽葉の刻印 | 腐朽の森王（100%） |
| 朽葉の樹脂 | 灰樹の亡霊（30%） / 腐朽の森王（100% / 1〜2個） |
| 沼鉄 | 沼の屍鬼（35%） |
| 深淵の刻印 | 深淵の海帝（100%） |
| 深淵の鱗 | 深淵の海帝（100% / 1〜1個） / 悲嘆の聖女（100% / 1〜1個） |
| 潮の真珠 | 深淵の海帝（100% / 1〜2個） |
| 火山の鱗 | 灼鋼の巨神（100% / 1〜2個） |
| 火核 | 灯火の化身（100% / 1〜1個） / 灼鋼の巨神（100% / 1〜1個） |
| 灰の繊維 | 灰樹の這い虫（45%） / 灰樹の亡霊（45%） |
| 灼鋼の刻印 | 灼鋼の巨神（100%） |
| 白銀の刻印 | 白銀の処刑者（100%） |
| 砕けた冠片 | 深淵の海帝（100% / 1〜3個） / 墜星竜アステル（100% / 1〜3個） / 黒冠の王（100% / 1〜3個） / 断罪の騎士長（100% / 1〜3個） / 灯火の化身（100% / 1〜3個） / 忘却された騎士（100% / 1〜3個） / 灼鋼の巨神（100% / 1〜3個） / 名もなき観測者（100% / 1〜3個） / 腐朽の森王（100% / 1〜3個） / 砂時計の双王（100% / 1〜3個） / 白銀の処刑者（100% / 1〜3個） / 悲嘆の聖女（100% / 1〜3個） |
| 霜の結晶 | 白銀の処刑者（100% / 1〜2個） |
| 黄金の砂 | 砂時計の双王（100% / 1〜2個） |

## 5. 武器・防具の入手方法

- **灰装備の具足** `nocterra:ashen_boots` → 灰の繊維×4
- **灰装備の鎧** `nocterra:ashen_chestplate` → 灰の繊維×8 ＋ 沼鉄×1
- **灰装備の兜** `nocterra:ashen_helmet` → 灰の繊維×5
- **灰装備の脚甲** `nocterra:ashen_leggings` → 灰の繊維×7
- **灰繊維のサーベル** `nocterra:ashen_sabre` → 灰の繊維×4 ＋ 灰木の板材×2 ＋ 沼鉄×1 ／ 灰繊維のサーベル×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 灰繊維のサーベル×1 ＋ ベールの絹×1 ／ 砕けた冠片×2 ＋ 灰繊維のサーベル×1 ＋ 星屑の欠片×1
- **灰木の刃** `nocterra:ashwood_blade` → 灰木の板材×2 ＋ stick×1 ／ 灰木の刃×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 灰木の刃×1 ＋ 朽葉の樹脂×1 ／ 砕けた冠片×2 ＋ 灰木の刃×1 ＋ 星屑の欠片×1
- **灰木の板材** `nocterra:ashwood_planks` → 灰木の原木×1
- **沼鉄の大剣** `nocterra:bog_iron_greatsword` → 沼鉄×3 ＋ 灰木の原木×2 ＋ エンバーシャード×1 ／ 沼鉄の大剣×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 沼鉄の大剣×1 ＋ 潮の真珠×1 ／ 砕けた冠片×2 ＋ 沼鉄の大剣×1 ＋ 星屑の欠片×1
- **冠の王笏** `nocterra:crown_scepter` → 砕けた冠片×3 ＋ 星屑の欠片×1 ＋ 火核×1
- **冠砕き** `nocterra:crownbreaker` → 砕けた冠片×2 ＋ 沼鉄×2 ＋ 深淵の鱗×1 ／ 冠砕き×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 冠砕き×1 ＋ 黄金の砂×1 ／ 砕けた冠片×2 ＋ 冠砕き×1 ＋ 星屑の欠片×1
- **エンバーシャード** `nocterra:ember_shard` → coal×1 ＋ flint×1 ＋ gold ingot×1
- **霜の具足** `nocterra:frost_boots` → 霜の結晶×2 ＋ 沼鉄×2
- **霜の鎧** `nocterra:frost_chestplate` → 霜の結晶×5 ＋ 沼鉄×3
- **霜の兜** `nocterra:frost_helmet` → 霜の結晶×3 ＋ 沼鉄×2
- **霜の脚甲** `nocterra:frost_leggings` → 霜の結晶×4 ＋ 沼鉄×3
- **霜の大鎌** `nocterra:frost_scythe` → 霜の結晶×2 ＋ 沼鉄×2 ＋ 亡霊の燐粉×2
- **古代の門の骨組み** `nocterra:gate_frame` → stone bricks×8 ＋ エンバーシャード×1
- **墓所の大剣** `nocterra:grave_greatsword` → iron ingot×4 ＋ 灰石×1 ＋ stick×1 ／ 墓所の大剣×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 墓所の大剣×1 ＋ 墓土の塵×1 ／ 砕けた冠片×2 ＋ 墓所の大剣×1 ＋ 星屑の欠片×1
- **墓土の戦槌** `nocterra:grave_hammer` → 墓土の塵×3 ＋ 沼鉄×2 ＋ 灰木の原木×1
- **嘆きの大鎌** `nocterra:mourning_scythe` → iron ingot×2 ＋ 灰木の板材×1 ＋ エンバーシャード×1 ＋ stick×1
- **白刃の短剣** `nocterra:pale_dagger` → ベールの絹×2 ＋ 亡霊の燐粉×2 ＋ 灰の繊維×2
- **樹脂の具足** `nocterra:resin_boots` → 朽葉の樹脂×2 ＋ 灰の繊維×2
- **樹脂の鎧** `nocterra:resin_chestplate` → 朽葉の樹脂×5 ＋ 灰の繊維×3
- **樹脂の兜** `nocterra:resin_helmet` → 朽葉の樹脂×3 ＋ 灰の繊維×2
- **樹脂の脚甲** `nocterra:resin_leggings` → 朽葉の樹脂×4 ＋ 灰の繊維×3
- **樹脂の長槍** `nocterra:resin_spear` → 朽葉の樹脂×2 ＋ 灰の繊維×2 ＋ 灰木の原木×1
- **砂刃** `nocterra:sandglass_blade` → 黄金の砂×2 ＋ 沼鉄×1 ＋ エンバーシャード×1
- **星屑の薙刀** `nocterra:starfall_glaive` → 星屑の欠片×2 ＋ 砕けた冠片×2 ＋ 沼鉄×2
- **潮の三叉槍** `nocterra:tidal_trident` → 潮の真珠×2 ＋ 深淵の鱗×2 ＋ 沼鉄×1
- **ベールの杖** `nocterra:veil_wand` → 亡霊の燐粉×3 ＋ ベールの絹×2 ＋ エンバーシャード×1
- **火山の大槌** `nocterra:volcanic_maul` → 火山の鱗×2 ＋ 火核×2 ＋ 沼鉄×2
- **燐粉の短剣** `nocterra:wispglass_dagger` → 亡霊の燐粉×3 ＋ 灰の繊維×2 ＋ エンバーシャード×1 ／ 燐粉の短剣×1 ＋ 砕けた冠片×1 ／ 砕けた冠片×2 ＋ 燐粉の短剣×1 ＋ 霜の結晶×1 ／ 砕けた冠片×2 ＋ 燐粉の短剣×1 ＋ 星屑の欠片×1

## 6. ボスと刻印

| ボス | 刻印 |
|---|---|
| 腐朽の森王 `nocterra:rotwood_king` | 朽葉の刻印 |
| 悲嘆の聖女 `nocterra:sorrow_saint` | 悲嘆の刻印 |
| 断罪の騎士長 `nocterra:condemned_knight_lord` | 断罪の刻印 |
| 砂時計の双王 `nocterra:sandglass_twin_kings` | 双砂の刻印 |
| 深淵の海帝 `nocterra:abyssal_sea_emperor` | 深淵の刻印 |
| 墜星竜アステル `nocterra:astel_fallen_star_dragon` | 墜星の刻印 |
| 白銀の処刑者 `nocterra:silver_executioner` | 白銀の刻印 |
| 灼鋼の巨神 `nocterra:molten_steel_colossus` | 灼鋼の刻印 |
| 黒冠の王 `nocterra:black_crowned_king` | （冠の王・刻印なし） |
| 灯火の化身 `nocterra:ember_incarnation` | （灯火・刻印なし） |
| 忘却された騎士 `nocterra:forgotten_knight` | （忘却・刻印なし） |
| 名もなき観測者 `nocterra:nameless_observer` | （観測者・刻印なし） |

## 7. 構造物

| 構造物 | 場所 |
|---|---|
| 古代神殿 | 灰樹の森（自然生成） |
| 亡国の城塞 | 各地（自然生成） |
| 地下墓所 | 各地の地下 |
| 灰の聖域 | 灰樹の森 |
| 地下要塞 | ノクテラの洞窟（y -20〜-40） |
| 黒冠城 | 黒冠遺跡帯 |
| ネザー要塞 | ネザー |
| エンド城塞 | エンド |
| 地域の祭壇 | 全9地域 |

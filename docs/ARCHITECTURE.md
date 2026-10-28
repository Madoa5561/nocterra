# ARCHITECTURE.md — NOCTERRA 実装アーキテクチャ

## 0. 確定した事実（実物から検証）

Minecraft **26.3**（`version.json`: world_version 5023 / protocol 777 / java_version **25** / pack_version resource 97.1, data 121.0）
NeoForge **26.3.0.99** / ModDevGradle **2.0.148** / Gradle **9.8.2** / GeckoLib **5.5.7**

### 26.3 で変わっている点（バニラ実JSONから確認）

1. **dimension_type JSON に `attributes` マップが追加**された。従来の `ultrawarm` / `natural` / `fixed_time` / `piglin_safe` 等のフラットなフィールドは無く、
   `minecraft:visual/*`（sky_color, fog_color, cloud_color, cloud_height, ambient_light_color）、
   `minecraft:audio/*`（ambient_sounds, background_music）、`minecraft:gameplay/*`（bed_rule, respawn_anchor_works, nether_portal_spawns_piglin, straw_bed_rule）
   として名前空間付き属性で表現される。加えて `default_clock` と `timelines`（`#minecraft:in_overworld` 等のタグ参照）を持つ。
   → **自前 dimension_type は overworld の JSON を複製して値を差し替える方式で作る**（必須フィールドの欠落を避ける）。
2. **pack.mcmeta は `min_format` / `max_format`** 形式（旧 `pack_format` 単体ではない）。data は 121。
3. **biome JSON も `attributes` を持つ**（`minecraft:gameplay/natural_mob_spawns`, `minecraft:audio/background_music`, `minecraft:visual/sky_color`）。
   従来の `spawners` / `spawn_costs` は `attributes.minecraft:gameplay/natural_mob_spawns.argument` 配下へ移動。
   `carvers` / `features` / `effects` / `temperature` / `downfall` / `has_precipitation` は従来通り（`features` は 11 段）。
4. ディメンション本体（`data/<ns>/dimension/<name>.json`）はバニラ jar に同梱されていない（ワールドプリセット `world_preset/*.json` 側で定義）。
   自前ディメンションは `data/nocterra/dimension/nocterra.json` の単体 JSON として登録する。

## 1. モジュール構成

```
com.nocterra
├─ Nocterra                  … @Mod エントリ、DeferredRegister の集約、イベントバス購読
├─ registry/
│  ├─ NocterraBlocks / NocterraItems / NocterraEntities / NocterraBlockEntities
│  ├─ NocterraFeatures（worldgen Feature 登録）
│  ├─ NocterraCreativeTabs
│  └─ NocterraAttachments（データアタッチメント型）
├─ networking/NocterraNetwork … CustomPacketPayload の登録と送受信
├─ world/
│  ├─ NocterraDimensions     … ResourceKey<Level> 定義、ポータル座標計算
│  ├─ portal/                … AncientGateBlock, AshenPortalBlock, PortalShape
│  ├─ NocterraBiomes         … （JSON 主体。コードはタグ/参照のみ）
│  └─ feature/               … カスタム地形フィーチャー
├─ entity/                   … 通常モンスター（GeoEntity 派生）
├─ boss/                     … ボス基底 BossEntity + 個別ボス AI（サーバー権威の状態機械）
├─ combat/                   … スタミナ・魔力・回避・パリィ・コンボ・状態異常
├─ magic/                    … 魔法・戦技定義
├─ item/                     … 武器カテゴリ別 Item クラス、武器強化
├─ progression/              … 刻印・侵食値・探索手帳
├─ quest/                    … クエスト・会話（データ駆動）
├─ npc/                      … NPC
└─ client/                   … レンダラ・HUD・スクリーン（クライアント専用。専用サーバーでロードされない）
```

### レジストリ集約ルール

各サブシステムは `public static void register(IEventBus modBus)` を持ち、`Nocterra` から順に呼ぶ。
**DeferredRegister の生成は各サブシステム内**、`Nocterra` は呼び出しのみ（コンフリクト回避）。

## 2. サーバー権威の原則

| 対象 | 判定主体 | クライアントの役割 |
|---|---|---|
| スタミナ/魔力の消費 | サーバー（Attachment） | 表示のみ |
| 回避の無敵フレーム | サーバー | 演出・先行入力 |
| ボスHP・フェーズ | サーバー（EntityDataAccessor 同期） | 演出・HPバー表示 |
| 攻撃ヒット | サーバー（バニラの攻撃判定を利用） | なし（誤差の許容範囲） |
| クエスト・刻印・侵食 | サーバー（Attachment） | 同期表示 |

クライアント専用クラスは `client/` に置き `Dist.CLIENT` 付きイベントでのみ参照する。

## 3. パフォーマンス方針

- ボス AI は 5 tick 間隔の意思決定（状態機械）+ 毎 tick の軽量移動のみ
- 近傍探索は `AABB.inflate` + `Level.getEntitiesOfClass` を状態遷移時のみ実行し、毎 tick 総当たりしない
- GeckoLib のモデル/アニメはエンティティ型ごとにシングルトンで再利用
- ネットワークは変化時のみ送信（毎 tick 送らない）

## 4. アセットパイプライン連携

`tools/asset_pipeline/` が生成 → `generated_assets/manifests/*.json` に来歴記録 → 承認後に
`nocterra/src/main/resources/assets/nocterra/...` へ配置。テクスチャは 16/32/64 の最近傍リサイズとタイル検査を通す。

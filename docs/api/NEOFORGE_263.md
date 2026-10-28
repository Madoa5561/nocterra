# Minecraft 26.3 / NeoForge 26.3.0.99 実 API 検証レポート

対象: **Minecraft 26.3 / NeoForge 26.3.0.99 / Java 25 / Gradle 9.8.2 + ModDevGradle 2.0.148**
検証担当: api-scout
検証日: 2026-10-28

> 方針: 根拠は **Gradle が展開した実ソース** のみ。`[確認]` / `[導出]` / `[未確認]` を明示する。
> **推測で API を書かない。**

---

## 0. 検証に使った実ソース（出所）

`nocterra` のビルドが **成功**（`BUILD SUCCESSFUL in 5m 45s`）し、その過程で
ModDevGradle が MC 26.3 を decompile した。展開物:

| 内容 | パス |
|---|---|
| **バニラ MC 26.3 decompile 済みソース**（NeoForge パッチ適用後・7301 `.java`） | `~/.gradle/caches/neoformruntime/intermediate_results/transformSources_50fcaf34c0df07d79e612014ec5dd107c424915a_output.zip` |
| 同上を展開したもの（本レポートの引用元） | `.agent/refs/mc-src/` |
| バニラの `data/` `assets/`（同 zip 内） | `.agent/refs/mc-src/data/`, `.agent/refs/mc-src/assets/` |
| **NeoForge 26.3.0.99 のソース**（1137 `.java`） | `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.3.0.99/545387d…/neoforge-26.3.0.99-sources.jar` |
| 同上を展開したもの（引用元） | `.agent/refs/neoforge-src/` |
| 生のバニラ jar（難読化） | `~/.gradle/caches/neoformruntime/artifacts/minecraft_26.3_{client,server}.jar` |

以下 `mc-src/...` は `.agent/refs/mc-src/...`、`nf-src/...` は `.agent/refs/neoforge-src/...` の略。

---

## 1. 26.3 の大前提（最初に知るべき差分）

`[確認]` **`ResourceLocation` は存在しない。`Identifier` にリネームされている。**
```
mc-src/net/minecraft/resources/Identifier.java            … 存在する
mc-src/net/minecraft/resources/ResourceLocation.java      … 存在しない
```
`[確認]` `Identifier` の主なファクトリ（`Identifier.java`）:
```java
public static Identifier fromNamespaceAndPath(String namespace, String path)
public static Identifier parse(String identifier)
public static Identifier withDefaultNamespace(String path)   // → namespace は "minecraft" 固定
public Identifier withPath(String newPath)
public Identifier withPath(UnaryOperator<String> modifier)
public Identifier withPrefix(String prefix)                  // = withPath(prefix + this.path)
public Identifier withSuffix(String suffix)
```
`[確認]` `new Identifier(ns, path)` の**public コンストラクタは無い**（`fromNamespaceAndPath` を使う）。
`[確認]` `Identifier.js` の `assertValidPath` を通るので、path に使える文字は制限あり。

`[確認]` `nocterra` の実ビルド設定（`gradle.properties`）:
```
minecraft_version=26.3
neo_version=26.3.0.99
geckolib_version=5.5.7
```
`[確認]` `build.gradle` の依存:
```groovy
implementation "com.geckolib:geckolib-neoforge-${project.minecraft_version}:${project.geckolib_version}"
```
→ `geckolib-neoforge-26.3:5.5.7`。リポジトリは
`https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`。

---

## 2. ディメンション登録

### 2.1 レジストリのディレクトリ名（`Registries.java`）

`[確認]` `mc-src/net/minecraft/core/registries/Registries.java`:
```java
public static final ResourceKey<Registry<DimensionType>> DIMENSION_TYPE = createRegistryKey("dimension_type");
public static final ResourceKey<Registry<LevelStem>>     LEVEL_STEM     = createRegistryKey("dimension");
public static final ResourceKey<Registry<Biome>>         BIOME          = createRegistryKey("worldgen/biome");
public static final ResourceKey<Registry<Attribute>>     ATTRIBUTE      = createRegistryKey("attribute");
public static final ResourceKey<Registry<MobEffect>>     MOB_EFFECT     = createRegistryKey("mob_effect");
public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_TYPE = createRegistryKey("data_component_type");
```

| レジストリ | ディレクトリ | 配置するファイル |
|---|---|---|
| `DimensionType` | `dimension_type` | `data/<ns>/dimension_type/<name>.json` |
| `LevelStem`（＝ディメンション本体） | `dimension` | `data/<ns>/dimension/<name>.json` |
| `Biome` | `worldgen/biome` | `data/<ns>/worldgen/biome/<name>.json` |

`[確認]` どちらのレイヤーに属するか（`mc-src/net/minecraft/resources/RegistryDataLoader.java`）:
```java
public static final List<RegistryData<?>> WORLD_REGISTRIES = List.of(
    new RegistryData<>(Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC),
    new RegistryData<>(Registries.BIOME, Biome.DIRECT_CODEC),
    ...
);
public static final List<RegistryData<?>> DIMENSION_REGISTRIES = List.of(
    new RegistryData<>(Registries.LEVEL_STEM, LevelStem.CODEC)
);
```
`[確認]` `DIMENSION_TYPE` と `BIOME` は **WORLD_REGISTRIES**、
`LEVEL_STEM`（`dimension/`）は **DIMENSION_REGISTRIES**（別レイヤー）。
`[導出]` DataGen では両方必要 → §6.3 の `forWorldLayer` を使う。

### 2.2 `dimension_type` の実スキーマ（**コードが正**）

`[確認]` `mc-src/net/minecraft/world/level/dimension/DimensionType.java` の
`createDirectCodec(...)` がバニラ JSON スキーマそのもの:

```java
public record DimensionType(
    boolean hasFixedTime,
    boolean hasSkyLight,
    boolean hasCeiling,
    boolean hasEnderDragonFight,
    double coordinateScale,
    int minY,
    int height,
    int logicalHeight,
    HolderSet<Block> infiniburn,
    float ambientLight,
    DimensionType.MonsterSettings monsterSettings,
    DimensionType.Skybox skybox,
    CardinalLighting.Type cardinalLightType,
    EnvironmentAttributeMap attributes,
    HolderSet<Timeline> timelines,
    Optional<Holder<WorldClock>> defaultClock
) {
    public static final Codec<DimensionType> DIRECT_CODEC = createDirectCodec(EnvironmentAttributeMap.CODEC);
```

`[確認]` フィールド定義（key名 / 必須か / 既定値 / 制約）:

| JSON key | 型 | 必須 | 既定値 | 制約 |
|---|---|---|---|---|
| `has_fixed_time` | bool | 任意 | `false` | |
| `has_skylight` | bool | **必須** | — | |
| `has_ceiling` | bool | **必須** | — | |
| `has_ender_dragon_fight` | bool | **必須** | — | |
| `coordinate_scale` | double | **必須** | — | `doubleRange(1.0E-5F, 3.0E7)` |
| `min_y` | int | **必須** | — | `intRange(MIN_Y, MAX_Y)` |
| `height` | int | **必須** | — | `intRange(16, Y_SIZE)` |
| `logical_height` | int | **必須** | — | `intRange(0, Y_SIZE)` |
| `infiniburn` | block の HolderSet（`"#tag"` 可） | **必須** | — | |
| `ambient_light` | float | **必須** | — | |
| `monster_spawn_light_level` | IntProvider | **必須** | — | `IntProviders.codec(0, 15)` |
| `monster_spawn_block_light_limit` | int | **必須** | — | `intRange(0, 15)` |
| `skybox` | enum `none`/`overworld`/`end` | 任意 | `overworld` | |
| `cardinal_light` | enum | 任意 | `DEFAULT` | |
| `attributes` | EnvironmentAttributeMap | 任意 | `{}` | |
| `timelines` | Timeline の HolderSet | 任意 | 空 | |
| `default_clock` | WorldClock | 任意 | 無し | |

`[確認]` コンストラクタで検証される制約（`DimensionType` の compact ctor）:
```java
height >= 16
min_y + height <= MAX_Y + 1
logical_height <= height
height % 16 == 0
min_y % 16 == 0
```
`[確認]` `Skybox` enum: `NONE("none")`, `OVERWORLD("overworld")`, `END("end")`。
`[確認]` `MonsterSettings.CODEC`:
```
monster_spawn_light_level     : IntProviders.codec(0, 15)
monster_spawn_block_light_limit: int 0..15
```

### 2.3 `dimension_type` のバニラ実例（全文）

`[確認]` `mc-src/data/minecraft/dimension_type/overworld.json` の全文:

```json
{
  "ambient_light": 0.0,
  "attributes": {
    "minecraft:audio/ambient_sounds": {
      "mood": {
        "block_search_extent": 8,
        "offset": 2.0,
        "sound": "minecraft:ambient.cave",
        "tick_delay": 6000
      }
    },
    "minecraft:audio/background_music": {
      "creative": { "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.creative" },
      "default":  { "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.game" }
    },
    "minecraft:gameplay/bed_rule": {
      "can_set_spawn": "always",
      "can_sleep": "when_dark",
      "error_message": { "translate": "block.minecraft.bed.no_sleep" }
    },
    "minecraft:gameplay/nether_portal_spawns_piglin": true,
    "minecraft:gameplay/respawn_anchor_works": false,
    "minecraft:gameplay/straw_bed_rule": {
      "can_set_spawn": "never",
      "can_sleep": "when_dark",
      "destroy_on_leave": true,
      "error_message": { "translate": "block.minecraft.bed.no_sleep" }
    },
    "minecraft:visual/ambient_light_color": "#0a0a0a",
    "minecraft:visual/cloud_color": "#ccffffff",
    "minecraft:visual/cloud_height": 192.33,
    "minecraft:visual/fog_color": "#c0d8ff",
    "minecraft:visual/sky_color": "#78a7ff"
  },
  "coordinate_scale": 1.0,
  "default_clock": "minecraft:overworld",
  "has_ceiling": false,
  "has_ender_dragon_fight": false,
  "has_skylight": true,
  "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "logical_height": 384,
  "min_y": -64,
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": {
    "type": "minecraft:uniform",
    "max_inclusive": 7,
    "min_inclusive": 0
  },
  "timelines": "#minecraft:in_overworld"
}
```

`[確認]` バニラの `dimension_type` は **4 件だけ**:
`overworld.json`, `overworld_caves.json`, `the_end.json`, `the_nether.json`。

`[確認]` 26.3 で `attributes`（`EnvironmentAttributeMap`）が導入され、
`visual/sky_color` 等は**ここに入る**（1.21 の `effects` フィールドは `dimension_type` には無い）。
`[確認]` `Registries.ENVIRONMENT_ATTRIBUTE = createRegistryKey("environment_attribute")` も新設。
→ キー `minecraft:visual/sky_color` 等はこのレジストリの要素。

### 2.4 `dimension/<name>.json`（LevelStem）の実スキーマ

`[確認]` `mc-src/net/minecraft/world/level/dimension/LevelStem.java` 全文:
```java
public record LevelStem(Holder<DimensionType> type, ChunkGenerator generator,
                        java.util.OptionalLong seedOverride) {
    public static final Codec<LevelStem> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            DimensionType.CODEC.fieldOf("type").forGetter(LevelStem::type),
            ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator))
            // NeoForge: patch the neoforge:seed_override field into the codec
            .and(net.minecraft.util.ExtraCodecs.asOptionalLong(
                Codec.LONG.optionalFieldOf("neoforge:seed_override")).forGetter(LevelStem::seedOverride))
            .apply(i, i.stable(LevelStem::new))
    );
    public LevelStem(Holder<DimensionType> type, ChunkGenerator generator) { ... }
    public static final ResourceKey<LevelStem> OVERWORLD = ... "overworld";
    public static final ResourceKey<LevelStem> NETHER   = ... "the_nether";
    public static final ResourceKey<LevelStem> END      = ... "the_end";
}
```

→ `data/<ns>/dimension/<name>.json` の構造:
| key | 必須 | 内容 |
|---|---|---|
| `type` | **必須** | `dimension_type` の ID（例 `"nocterra:nocterra"`） |
| `generator` | **必須** | `ChunkGenerator`。`type` で種類を選ぶ |
| `neoforge:seed_override` | 任意 | long（NeoForge 拡張） |

`[確認]` ジェネレータの種類（`mc-src/net/minecraft/world/level/chunk/ChunkGenerators.java`）:
```java
Registry.register(registry, "noise", NoiseBasedChunkGenerator.CODEC);
Registry.register(registry, "flat",  FlatLevelSource.CODEC);
Registry.register(registry, "debug", DebugLevelSource.CODEC);
```
`[確認]` レジストリキーは `Registries.CHUNK_GENERATOR = createRegistryKey("worldgen/chunk_generator")`。

`[確認]` `NoiseBasedChunkGenerator.CODEC`（`mc-src/.../levelgen/NoiseBasedChunkGenerator.java`）:
```java
i -> i.group(
    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings)
)
```

### 2.5 `dimension` のバニラ実例（**重要**）

`[確認]` **バニラ 26.3 には `data/minecraft/dimension/` ディレクトリが存在しない。**
```
mc-src/data/minecraft/dimension/           … 存在しない（0 件）
mc-src/data/minecraft/dimension_type/      … 4 件
mc-src/data/minecraft/worldgen/biome/      … 68 件
```
`[確認]` `data/minecraft/` 直下のディレクトリ一覧にも `dimension` は無い
（`dimension_type` はある）。→ **ディメンション本体はコード側（`RegistrySetBuilder`）で定義**されている。

`[確認]` バニラの dimension 定義が **JSON で読める形**で存在するのは
`data/minecraft/worldgen/world_preset/normal.json`（ワールドプリセット）。
`LevelStem` の値の形そのままなので、**これが最良の実例**:

`data/minecraft/worldgen/world_preset/normal.json`（全文）:
```json
{
  "dimensions": {
    "minecraft:overworld": {
      "type": "minecraft:overworld",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": {
          "type": "minecraft:multi_noise",
          "preset": "minecraft:overworld"
        },
        "settings": "minecraft:overworld"
      }
    },
    "minecraft:the_end": {
      "type": "minecraft:the_end",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:the_end" },
        "settings": "minecraft:end"
      }
    },
    "minecraft:the_nether": {
      "type": "minecraft:the_nether",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": {
          "type": "minecraft:multi_noise",
          "preset": "minecraft:nether"
        },
        "settings": "minecraft:nether"
      }
    }
  }
}
```

`[導出]` 自前ディメンションを作る場合、`data/nocterra/dimension/nocterra.json` は
上の `minecraft:overworld` の値部分と同じ形になる:

```json
{
  "type": "nocterra:nocterra",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": { "type": "minecraft:multi_noise", "preset": "nocterra:nocterra" },
    "settings": "nocterra:nocterra"
  }
}
```
（`preset` / `settings` の ID は自前で用意する。`biome_source` の他の型は
`mc-src/net/minecraft/world/level/biome/*Source.java` を参照。`[未確認]` 全型は未列挙。）

---

## 3. バイオーム JSON

### 3.1 実スキーマ

`[確認]` `mc-src/net/minecraft/world/level/biome/Biome.java`:
```java
public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create(
    i -> i.group(
        Biome.ClimateSettings.CODEC.forGetter(...),                                    // → 展開される
        EnvironmentAttributeMap.CODEC_ONLY_POSITIONAL
            .optionalFieldOf("attributes", EnvironmentAttributeMap.EMPTY)...,          // "attributes"（任意）
        BiomeSpecialEffects.CODEC.fieldOf("effects")...,                               // "effects"（必須）
        BiomeGenerationSettings.CODEC.forGetter(...)                                   // → 展開される
    )
    ...
);
public static final Codec<Holder<Biome>> CODEC = RegistryCodecs.holder(Registries.BIOME, DIRECT_CODEC);
```

`[確認]` `Biome.ClimateSettings.CODEC`:
| key | 型 | 必須 | 既定 |
|---|---|---|---|
| `has_precipitation` | bool | **必須** | — |
| `temperature` | float | **必須** | — |
| `temperature_modifier` | enum | 任意 | `none` |
| `downfall` | float | **必須** | — |

`[確認]` `BiomeGenerationSettings.CODEC`（`mc-src/.../biome/BiomeGenerationSettings.java`）:
```java
i -> i.group(
    WorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(b -> b.carvers),
    PlacedFeature.LIST_OF_LISTS_CODEC.fieldOf("features").forGetter(b -> b.features)
)
```
→ **`carvers` と `features` のみ。`spawners` / `spawn_costs` はここには無い。**

### 3.2 26.3 の大きな変更: スポーン設定が `attributes` へ移動 `[確認]`

`[確認]` バニラ `plains.json` には `spawners` も `spawn_costs` も**トップレベルに存在しない**。
代わりに `attributes` の中に `minecraft:gameplay/natural_mob_spawns` がある:

```json
"attributes": {
  "minecraft:gameplay/natural_mob_spawns": {
    "argument": {
      "spawn_costs": {},
      "spawns_by_category": {
        "ambient":  [ { "type": "minecraft:bat", "count": 8, "weight": 10 } ],
        "creature": [ { "type": "minecraft:sheep", "count": 4, "weight": 12 }, ... ],
        "monster":  [ { "type": "minecraft:spider", "count": 4, "weight": 100 }, ... ],
        "underground_water_creature": [ { "type": "minecraft:glow_squid", "count": {...}, "weight": 10 } ]
      }
    },
    "modifier": "overlay"
  },
  "minecraft:visual/sky_color": "#78a7ff"
}
```

`[確認]` 1.21 系にあった `"spawners": {"monster": [...], ...}` / `"spawn_costs": {...}` は
**26.3 のバニラ JSON には無い**。`[導出]` 旧形式のバイオーム JSON はそのままでは通らない可能性が高い。
（パースエラーになるかは実行検証が必要 → `[未確認]`）

`[確認]` `count` は int 直接でも IntProvider でもよい（`plains.json` に両方の実例あり）。

### 3.3 バニラ実例 `plains.json`（`features` は省略）

`[確認]` `mc-src/data/minecraft/worldgen/biome/plains.json`:
```json
{
  "attributes": {
    "minecraft:gameplay/natural_mob_spawns": {
      "argument": {
        "spawn_costs": {},
        "spawns_by_category": {
          "ambient":  [ { "type": "minecraft:bat", "count": 8, "weight": 10 } ],
          "creature": [
            { "type": "minecraft:sheep",   "count": 4, "weight": 12 },
            { "type": "minecraft:pig",     "count": 4, "weight": 10 },
            { "type": "minecraft:chicken", "count": 4, "weight": 10 },
            { "type": "minecraft:cow",     "count": 4, "weight": 8 },
            { "type": "minecraft:horse",   "count": { "type": "minecraft:uniform",
                                                      "max_inclusive": 6, "min_inclusive": 2 },
                                           "weight": 5 },
            { "type": "minecraft:donkey",  "count": { "type": "minecraft:uniform",
                                                      "max_inclusive": 3, "min_inclusive": 1 },
                                           "weight": 1 }
          ],
          "monster": [
            { "type": "minecraft:spider",         "count": 4, "weight": 100 },
            { "type": "minecraft:zombie",         "count": 4, "weight": 90  },
            { "type": "minecraft:zombie_villager","count": 1, "weight": 5   },
            { "type": "minecraft:zombie_horse",   "count": 1, "weight": 5   },
            { "type": "minecraft:skeleton",       "count": 4, "weight": 100 },
            { "type": "minecraft:creeper",        "count": 4, "weight": 100 },
            { "type": "minecraft:slime",          "count": 4, "weight": 100 },
            { "type": "minecraft:enderman",       "count": { "type": "minecraft:uniform",
                                                             "max_inclusive": 4, "min_inclusive": 1 },
                                                  "weight": 10 },
            { "type": "minecraft:witch",          "count": 1, "weight": 5   }
          ],
          "underground_water_creature": [
            { "type": "minecraft:glow_squid", "count": { "type": "minecraft:uniform",
                                                         "max_inclusive": 6, "min_inclusive": 4 },
                                              "weight": 10 }
          ]
        }
      },
      "modifier": "overlay"
    },
    "minecraft:visual/sky_color": "#78a7ff"
  },
  "carvers": [
    "minecraft:cave",
    "minecraft:cave_extra_underground",
    "minecraft:canyon"
  ],
  "downfall": 0.4,
  "effects": { "water_color": "#3f76e4" },
  "features": [
    [], [ "minecraft:lake_lava_underground", "minecraft:lake_lava_surface" ],
    [ "minecraft:amethyst_geode" ],
    [ "minecraft:monster_room", "minecraft:monster_room_deep" ],
    [], [],
    [ "minecraft:ore_dirt", "minecraft:ore_gravel", ... ],
    [],
    [ "minecraft:spring_water", "minecraft:spring_lava" ],
    [ "minecraft:glow_lichen", "minecraft:trees_plains", "minecraft:flower_plains", ... ],
    [ "minecraft:freeze_top_layer" ]
  ],
  "has_precipitation": true,
  "temperature": 0.8
}
```
（`features` は JSON では 11 個の配列のリスト。上の `...` は省略。実際の全文は
`mc-src/data/minecraft/worldgen/biome/plains.json` を参照）

`[確認]` `effects` は **必須**（`fieldOf("effects")`）。`plains.json` では `{"water_color": "#3f76e4"}` のみ。

---

## 4. ネットワーク（CustomPacketPayload / StreamCodec / PayloadRegistrar）

### 4.1 バニラ `CustomPacketPayload`

`[確認]` `mc-src/net/minecraft/network/protocol/common/custom/CustomPacketPayload.java` 全文の要点:
```java
public interface CustomPacketPayload {
    CustomPacketPayload.Type<? extends CustomPacketPayload> type();

    static <B extends ByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(
        StreamMemberEncoder<B, T> writer, StreamDecoder<B, T> reader);

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String id);  // → minecraft: namespace

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(
        FallbackProvider<B> fallback, List<TypeAndCodec<? super B, ?>> types,
        ConnectionProtocol protocol, PacketFlow packetFlow);

    default ClientboundCustomPayloadPacket toVanillaClientbound();
    default ServerboundCustomPayloadPacket toVanillaServerbound();

    record Type<T extends CustomPacketPayload>(Identifier id) {}
    record TypeAndCodec<B extends FriendlyByteBuf, T extends CustomPacketPayload>(
        CustomPacketPayload.Type<T> type, StreamCodec<B, T> codec) {}
}
```
`[確認]` `Type` は **`record Type<T>(Identifier id)`**。
`[確認]` `createType(String id)` は `Identifier.withDefaultNamespace(id)` を使うので
**namespace が `minecraft` になる**。
→ 自前 namespace にしたい場合は `new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("nocterra", "xxx"))`
を使う（`[導出]`: record のコンストラクタがそのまま使える）。

`[確認]` ワイヤ上は `type().id()` を `output.writeIdentifier(...)` / `input.readIdentifier()` で書く。
**payload の codec 内で ID を書いてはいけない**（`RegisterPayloadHandlersEvent` の javadoc に明記）。

### 4.2 バニラ `StreamCodec`

`[確認]` `mc-src/net/minecraft/network/codec/StreamCodec.java`:
```java
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder);
    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder);
    static <B, V> StreamCodec<B, V> unit(V instance);
    default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> operation);
    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from);
    default <O extends ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> operation);
}
```
`[確認]` `V decode(B input)` / `void encode(B output, V value)` が中核。

### 4.3 NeoForge `RegisterPayloadHandlersEvent` / `PayloadRegistrar`

`[確認]` `nf-src/net/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent.java`:
```java
public class RegisterPayloadHandlersEvent extends Event implements IModBusEvent {
    public PayloadRegistrar registrar(String version);
}
```

`[確認]` `nf-src/.../network/registration/PayloadRegistrar.java` の**全登録メソッド**
（すべて `CustomPacketPayload.Type<T>` + `StreamCodec` + `IPayloadHandler<T>` を取る）:

```java
public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
    CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler);
public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
    CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);   // handler 無し版

public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
    Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler);

public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(
    Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
    IPayloadHandler<T> serverHandler, @Nullable IPayloadHandler<T> clientHandler);

public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(
    Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler);
public <T extends CustomPacketPayload> PayloadRegistrar configurationToServer(...);
public <T extends CustomPacketPayload> PayloadRegistrar configurationBidirectional(...);
public <T extends CustomPacketPayload> PayloadRegistrar commonToClient(...);
public <T extends CustomPacketPayload> PayloadRegistrar commonToServer(...);
public <T extends CustomPacketPayload> PayloadRegistrar commonBidirectional(...);

public PayloadRegistrar executesOn(HandlerThread thread);
public PayloadRegistrar versioned(String version);
public PayloadRegistrar optional();
```
`[確認]` **`play` 系だけ codec の型が `RegistryFriendlyByteBuf`**、`configuration` 系と `common` 系は
`FriendlyByteBuf`（`common` は PLAY+CONFIGURATION 両方なので `FriendlyByteBuf`）。
`[確認]` 既定の `HandlerThread` は `MAIN`（= `MainThreadPayloadHandler` でラップされメインスレッド実行）。

`[確認]` `nf-src/.../network/handling/IPayloadHandler.java`:
```java
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, IPayloadContext context);
}
```

`[確認]` `IPayloadContext`（`@ApiStatus.NonExtendable`）の主なメソッド:
```java
ICommonPacketListener listener();
default Connection connection();
Player player();                                   // PLAY 相のみ
default void reply(CustomPacketPayload payload);
default void disconnect(Component reason);
CompletableFuture<Void> enqueueWork(Runnable task);
<T> CompletableFuture<T> enqueueWork(Supplier<T> task);
PacketFlow flow();
default ConnectionProtocol protocol();
default void handle(Packet<?> packet);
void handle(CustomPacketPayload payload);
void finishCurrentTask(ConfigurationTask.Type type);
default ChannelHandlerContext channelHandlerContext();
```
`[確認]` `HandlerThread` の値は `MAIN` と（`executesOn` で切替可能な）別値が存在する。
`[未確認]` 全 enum 定数の列挙は未取得。

`[確認]` 送信は `CustomPacketPayload.toVanillaClientbound()` / `toVanillaServerbound()` で
バニラパケット化できる。NeoForge の `ICommonPacketListener.send(...)` も併用可。

---

## 5. 登録 API（Attributes / MobEffect / DataComponents）

### 5.1 Attributes

`[確認]` `Registries.ATTRIBUTE = createRegistryKey("attribute")`
→ `data/<ns>/attribute/*.json` ではなく **`DeferredRegister` で登録**（バニラもコード登録）。

`[確認]` `mc-src/.../ai/attributes/Attribute.java`:
```java
public class Attribute implements IAttributeExtension {
    public static final Codec<Holder<Attribute>> CODEC = BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC
        = ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE);

    protected Attribute(String descriptionId, double defaultValue);   // protected!
    public double getDefaultValue();
    public boolean isClientSyncable();
    public Attribute setSyncable(boolean syncable);
    public Attribute setSentiment(Attribute.Sentiment sentiment);
    public double sanitizeValue(double value);
    public String getDescriptionId();
    public ChatFormatting getStyle(boolean valueIncrease);
}
```
`[確認]` `Attribute` のコンストラクタは **`protected`**。直接 `new Attribute(...)` は**できない**。
`[確認]` 範囲つきは `RangedAttribute`（`mc-src/.../ai/attributes/RangedAttribute.java`）:
```java
public class RangedAttribute extends Attribute {
    public RangedAttribute(String descriptionId, double defaultValue, double minValue, double maxValue);
    public double getMinValue();
    public double getMaxValue();
    public double sanitizeValue(double value);   // NaN → minValue, それ以外は clamp
}
```
`[確認]` バニラの登録例（`mc-src/.../ai/attributes/Attributes.java`）:
```java
public static final Holder<Attribute> ARMOR = register("armor",
    new RangedAttribute("attribute.name.armor", 0.0, 0.0, 30.0).setSyncable(true));
public static final Holder<Attribute> ATTACK_DAMAGE = register("attack_damage",
    new RangedAttribute("attribute.name.attack_damage", 2.0, 0.0, 2048.0));
```
`[導出]` 自前 MOD では:
```java
public static final DeferredRegister<Attribute> ATTRIBUTES =
    DeferredRegister.create(Registries.ATTRIBUTE, "nocterra");
public static final DeferredHolder<Attribute, Attribute> DARK_RESIST =
    ATTRIBUTES.register("dark_resistance",
        () -> new RangedAttribute("attribute.nocterra.dark_resistance", 0.0, 0.0, 100.0).setSyncable(true));
```
（`DeferredHolder` の型名は 26.3 の `DeferredRegister` 実装に合わせる。`[未確認]`: `register(...)` の戻り型の完全修飾名は本検証で未確認）

### 5.2 MobEffect

`[確認]` `Registries.MOB_EFFECT = createRegistryKey("mob_effect")`（コード登録）。

`[確認]` `mc-src/net/minecraft/world/effect/MobEffect.java`:
```java
public class MobEffect {
    public static final Codec<Holder<MobEffect>> CODEC = BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> STREAM_CODEC
        = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);

    protected MobEffect(MobEffectCategory category, int color);
    protected MobEffect(MobEffectCategory category, int color, ParticleOptions particleOptions);
    // Neo: 追加コンストラクタ（MobEffectInstance 依存の ParticleOptions を取る）
}
```
`[確認]` `MobEffectCategory` の全定数（`mc-src/net/minecraft/world/effect/MobEffectCategory.java` 全文）:
```java
public enum MobEffectCategory {
    BENEFICIAL(ChatFormatting.BLUE),
    HARMFUL(ChatFormatting.RED),
    NEUTRAL(ChatFormatting.BLUE);

    public ChatFormatting getTooltipFormatting();
}
```
→ 定数は **`BENEFICIAL` / `HARMFUL` / `NEUTRAL` の 3 つだけ**。

`[確認]` バニラの登録例（`mc-src/net/minecraft/world/effect/MobEffects.java`）:
```java
public static final Holder<MobEffect> SPEED = register("speed",
    new MobEffect(MobEffectCategory.BENEFICIAL, 3402751)
        .addAttributeModifier(Attributes.MOVEMENT_SPEED,
            Identifier.withDefaultNamespace("effect.speed"), 0.2F,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
```
`[未確認]` `addAttributeModifier(...)` の完全なシグネチャ（引数名・戻り型）は本検証では未取得。
使用例から `(Holder<Attribute>, Identifier, float|double, AttributeModifier.Operation)` と読めるが断定しない。

### 5.3 DataComponents

`[確認]` `Registries.DATA_COMPONENT_TYPE = createRegistryKey("data_component_type")`。

`[確認]` `mc-src/net/minecraft/core/component/DataComponentType.java`:
```java
interface DataComponentType<T> {
    static <T> DataComponentType.Builder<T> builder();     // package-private static
    @Nullable Codec<T> codec();
    ...
    class Builder<T> {
        public DataComponentType.Builder<T> persistent(Codec<T> codec);
        public DataComponentType.Builder<T> networkSynchronized(
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec);
        public DataComponentType.Builder<T> cacheEncoding();
        public DataComponentType.Builder<T> ignoreSwapAnimation();
    }
}
```
`[確認]` `builder()` は **package-private**（`net.minecraft.core.component` 内からのみ）。
`[導出]` 自前 MOD からは `DataComponentType.Builder` を直接 `builder()` では作れない可能性が高い
→ NeoForge が公開 API を用意しているか要確認。**`[未確認]`**
（`DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, modid)` は
`nf-src/.../registries/DeferredRegister.java` に `[確認]` 実在。ただし要素の作り方は要確認）

`[確認]` NeoForge `DeferredRegister`:
```java
public static DataComponents createDataComponents(
    ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid);
public static class DataComponents extends DeferredRegister<DataComponentType<?>> { ... }
```

---

## 6. データ生成（DataGen）

### 6.1 `GatherDataEvent`（26.3 で API が変わっている）

`[確認]` `nf-src/net/neoforged/neoforge/data/event/GatherDataEvent.java` 全文の要点:
```java
public abstract sealed class GatherDataEvent extends Event implements IModBusEvent {
    public ModContainer getModContainer();
    public ResourceManager getResourceManager(PackType packType);
    public Collection<Path> getInputs();
    public DataGenerator getGenerator();
    public PackGenerator getDefaultPackGenerator();
    public PackGenerator getPackGenerator(PackOutput output);
    public CompletableFuture<HolderLookup.Provider> getWorldLookupProvider();
    public CompletableFuture<HolderLookup.Provider> getReloadableLookupProvider();
    public boolean includeDev();
    public boolean includeReports();
    public boolean validate();

    public <T extends DataProvider> T addProvider(T provider);
    public <T extends DataProvider> T createProvider(PackGenerator.DataProviderFromOutput<T> builder);
    public <T extends DataProvider> T createProvider(PackGenerator.DataProviderFromOutputLookup<T> builder);
    public void createBlockAndItemTags(
        PackGenerator.DataProviderFromOutputLookup<TagsProvider<Block>> blockTagsProvider,
        PackGenerator.ItemTagsProvider itemTagsProvider);

    public static final class Server extends GatherDataEvent { ... }
    public static final class Client extends GatherDataEvent { ... }
}
```
`[確認]` `GatherDataEvent` は **`sealed`** で、`Server` / `Client` の 2 実装のみ。
`[確認]` javadoc: 「`--mod` で追加された mod に対してのみ発火する」。
`[確認]` `getWorldLookupProvider()` / `getReloadableLookupProvider()` の 2 系統がある。

`[未確認]` `PackGenerator` の完全なメソッド一覧は未取得。
`[導出]` 従来の `event.getGenerator().addProvider(...)` ではなく
`event.addProvider(...)` / `event.createProvider(...)` を使う形になっている。

### 6.2 `DatapackBuiltinEntriesProvider`

`[確認]` `nf-src/net/neoforged/neoforge/common/data/DatapackBuiltinEntriesProvider.java` 全文の要点:
```java
public class DatapackBuiltinEntriesProvider extends RegistriesDatapackGenerator {
    public DatapackBuiltinEntriesProvider(
        PackOutput output, String name,
        Collection<RegistryDataLoader.RegistryData<?>> registryData,
        CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries,
        @Nullable Set<String> modIds);

    public static DatapackBuiltinEntriesProvider forWorldLayer(
        PackOutput output, String name,
        CompletableFuture<HolderLookup.Provider> worldRegistries,
        RegistrySetBuilder entriesBuilder, @Nullable Set<String> modIds);

    public static DatapackBuiltinEntriesProvider forReloadableLayer(
        PackOutput output, String name,
        CompletableFuture<HolderLookup.Provider> worldRegistries,
        CompletableFuture<HolderLookup.Provider> reloadableRegistries,
        RegistrySetBuilder entriesBuilder, @Nullable Set<String> modIds);

    public CompletableFuture<HolderLookup.Provider> getRegistryProvider();
}
```
`[確認]` **26.3 では `forWorldLayer` / `forReloadableLayer` の静的ファクトリを使うのが正**。
旧 API（`new DatapackBuiltinEntriesProvider(output, lookupProvider, builder, modIds)`）は**無い**。

`[確認]` `forWorldLayer` は内部で
`DataPackRegistriesHooks.getWorldRegistriesWithDimensions()` を使う。
`nf-src/.../registries/DataPackRegistriesHooks.java`:
```java
/// {@return a stream of the "world" and dimension datapack registries}
/// This method is used in datagen (RegistryPatchGenerator, RegistriesDatapackGenerator,
/// DatapackBuiltinEntriesProvider) in place of RegistryDataLoader#WORLD_REGISTRIES
/// to ensure mods can datagen [dimensions][RegistryDataLoader#DIMENSION_REGISTRIES].
public static Stream<RegistryDataLoader.RegistryData<?>> getWorldRegistriesWithDimensions() {
    return Stream.concat(WORLD_REGISTRIES.registryData.stream(),
                         RegistryDataLoader.DIMENSION_REGISTRIES.stream());
}
```
→ **`forWorldLayer` を使えばディメンション（`LEVEL_STEM`）も DataGen できる。** `[確認]`

`[確認]` `RegistrySetBuilder.add`（`mc-src/net/minecraft/core/RegistrySetBuilder.java`）:
```java
public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> key,
                                  SingleRegistryBootstrap<T> bootstrap);
public RegistrySetBuilder add(MultiRegistryBootstrap bootstrap);
```

`[導出]` ディメンションを DataGen する骨格:
```java
@SubscribeEvent
static void gatherData(GatherDataEvent.Server event) {
    RegistrySetBuilder builder = new RegistrySetBuilder();
    builder.add(Registries.LEVEL_STEM, ctx -> {
        // ctx.register(ResourceKey, value) で登録  ※ register のシグネチャは要確認（[未確認]）
    });
    event.addProvider(DatapackBuiltinEntriesProvider.forWorldLayer(
        event.getGenerator().getPackOutput(), "nocterra_worldgen",
        event.getWorldLookupProvider(), builder, Set.of("nocterra")));
}
```
`[未確認]` `SingleRegistryBootstrap` / `BootstrapContext` の `register(...)` 完全シグネチャは未取得。
実装時に `mc-src/net/minecraft/core/RegistrySetBuilder.java` を読むこと。

### 6.3 実行

`[確認]` `nocterra/build.gradle` の `data` run:
```groovy
data {
    data()
    programArguments.addAll '--mod', project.mod_id, '--all',
        '--output', file('src/generated/resources/').getAbsolutePath(),
        '--existing', file('src/main/resources/').getAbsolutePath()
}
```
`[確認]` `sourceSets.main.resources { srcDir 'src/generated/resources' }` で生成物が取り込まれる。
→ `.\gradlew.bat runData` で `nocterra/src/generated/resources/` に出力。

---

## 7. ポータル実装に使える API

### 7.1 `DimensionTransition` は**存在しない** → `TeleportTransition`

`[確認]` `DimensionTransition` というクラスは 26.3 に**存在しない**。
`mc-src/net/minecraft/world/level/portal/TeleportTransition.java` が実体。

`[確認]` `mc-src/net/minecraft/world/level/portal/TeleportTransition.java` 全文の要点:
```java
public record TeleportTransition(
    ServerLevel newLevel,
    Vec3 position,
    Vec3 deltaMovement,
    float yRot,
    float xRot,
    boolean missingRespawnBlock,
    boolean asPassenger,
    Set<Relative> relatives,
    TeleportTransition.PostTeleportTransition postTeleportTransition
) {
    public static final PostTeleportTransition DO_NOTHING;
    public static final PostTeleportTransition PLAY_PORTAL_SOUND;
    public static final PostTeleportTransition PLACE_PORTAL_TICKET;

    public TeleportTransition(ServerLevel newLevel, Vec3 pos, Vec3 speed,
                              float yRot, float xRot, PostTeleportTransition post);
    public TeleportTransition(ServerLevel newLevel, Vec3 pos, Vec3 speed,
                              float yRot, float xRot, Set<Relative> relatives,
                              PostTeleportTransition post);

    public static TeleportTransition createDefault(ServerPlayer player, PostTeleportTransition post);
    public static TeleportTransition missingRespawnBlock(ServerPlayer player, PostTeleportTransition post);

    public TeleportTransition withRotation(float yRot, float xRot);
    public TeleportTransition withPosition(Vec3 position);
    public TeleportTransition transitionAsPassenger();

    @FunctionalInterface
    public interface PostTeleportTransition {
        void onTransition(Entity entity);
        default PostTeleportTransition then(PostTeleportTransition other);
    }
}
```

### 7.2 `Portal` インタフェース（自前ポータルの入口）

`[確認]` `mc-src/net/minecraft/world/level/block/Portal.java` 全文:
```java
public interface Portal {
    default int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0;
    }

    @Nullable TeleportTransition getPortalDestination(
        final ServerLevel currentLevel, final Entity entity, final BlockPos portalEntryPos);

    default Portal.Transition getLocalTransition() {
        return Portal.Transition.NONE;
    }

    enum Transition { CONFUSION, NONE; }
}
```

`[確認]` バニラの実装例（`implements Portal` しているブロック）:
| クラス | 場所 |
|---|---|
| `NetherPortalBlock` | `mc-src/net/minecraft/world/level/block/NetherPortalBlock.java:46` |
| `EndPortalBlock` | `mc-src/net/minecraft/world/level/block/EndPortalBlock.java:31` |
| `EndGatewayBlock` | `mc-src/net/minecraft/world/level/block/EndGatewayBlock.java:27` |

`[確認]` ポータル進入の通知（`mc-src/net/minecraft/world/entity/Entity.java:2706`）:
```java
public void setAsInsidePortal(Portal portal, BlockPos pos)
```
`[確認]` 呼び出し例 `NetherPortalBlock.java:107`: `entity.setAsInsidePortal(this, pos);`

`[確認]` `PortalProcessor`（`mc-src/net/minecraft/world/entity/PortalProcessor.java`）が
進入時間をカウントし、`Entity` の tick で `getPortalDestination(...)` を呼ぶ:
```java
// Entity.java:2727-2729
TeleportTransition teleportTransition = this.portalProcess.getPortalDestination(level, this);
if (teleportTransition != null) {
    this.teleportToPortalDestination(level, teleportTransition);
}
```
`[確認]` 実際の転送（`Entity.java:3177`）:
```java
public @Nullable Entity teleport(TeleportTransition transition)
```
`[確認]` `teleportToPortalDestination(ServerLevel level, TeleportTransition transition)`（`Entity.java:2740`）は
`level.isAllowedToEnterPortal(newLevel)` と `canTeleport(...)` をチェックしてから `teleport(...)` を呼ぶ。

`[確認]` ポータル探索補助: `mc-src/net/minecraft/world/level/portal/PortalForcer.java`,
`mc-src/net/minecraft/world/level/portal/PortalShape.java`（バニラのネザーポータル形状判定）。

`[導出]` 自前ポータルの実装方針:
1. `Block` に `implements Portal` を付ける（`NetherPortalBlock` と同型）。
2. `onInside` 相当で `entity.setAsInsidePortal(this, pos)` を呼ぶ。
3. `getPortalDestination(ServerLevel, Entity, BlockPos)` で `TeleportTransition` を組み立てて返す。
4. 遷移時間を変えたければ `getPortalTransitionTime(ServerLevel, Entity)` を override。
5. 画面演出は `getLocalTransition()`（`CONFUSION` / `NONE`）。

`[未確認]` `isAllowedToEnterPortal` / `canTeleport` を自前ディメンションで通すための条件
（`dimension_type` のどのフィールドが効くか）は未検証。

---

## 8. レンダラ登録（GeckoLib 連携に必要）

`[確認]` `nf-src/net/neoforged/neoforge/client/event/EntityRenderersEvent.java`:
```java
public abstract class EntityRenderersEvent extends Event implements IModBusEvent {
    public static class RegisterLayerDefinitions extends EntityRenderersEvent {
        public void registerLayerDefinition(ModelLayerLocation, Supplier<LayerDefinition>);
    }
    public static class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(
            EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider);
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
            BlockEntityType<? extends T> blockEntityType,
            BlockEntityRendererProvider<T, S> blockEntityRendererProvider);
    }
    public static class AddLayers extends EntityRenderersEvent {
        public Set<PlayerModelType> getSkins();
        public <T extends Entity, R extends EntityRenderer<T, ?>> R getRenderer(EntityType<? extends T>);
        public EntityModelSet getEntityModels();
        public EntityRendererProvider.Context getContext();
    }
    public static class CreateSkullModels extends EntityRenderersEvent { ... }
}
```
`[確認]` **26.3 でも `EntityRenderersEvent.RegisterRenderers` が存在**し、
エンティティとブロックエンティティの両方をここで登録する（別イベントは無い）。
`[確認]` 26.3 では `registerBlockEntityRenderer` の型引数が `(T, S extends BlockEntityRenderState)` の 2 つ。
`[確認]` カスタムアイテムモデル用:
```java
// nf-src/net/neoforged/neoforge/client/event/RegisterSpecialModelRendererEvent.java
public class RegisterSpecialModelRendererEvent extends Event implements IModBusEvent {
    public void register(Identifier location,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> source);
}
```
`[確認]` これは `SpecialModelRenderer.Unbaked` の **codec** を登録するイベントであり、
個々のアイテムにレンダラを紐付けるイベントではない。

`[導出]` GeckoLib 連携:
```java
// エンティティ
@SubscribeEvent
static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(NocterraEntities.FOREST_KING.get(),
        ctx -> new GeoEntityRenderer<>(ctx, new ForestKingModel()));
}
// ブロックエンティティ
event.registerBlockEntityRenderer(NocterraBlockEntities.ALTAR.get(),
    ctx -> new GeoBlockRenderer<>(ctx, new AltarModel()));
```
（`GeoEntityRenderer` / `GeoBlockRenderer` のコンストラクタは `GECKOLIB5.md` §4.2 / §5.3 で `[確認]`）

`[未確認]` GeckoLib のアイテム/防具レンダラを利用者が明示登録する必要があるか。
GeckoLib 側は `GeoRenderProvider` + `createGeoRenderer(Consumer<GeoRenderProvider>)` を
override させる設計（`GECKOLIB5.md` §5.1）だが、26.3 で
`RegisterSpecialModelRendererEvent` への追加登録が必要かは未確定。要実機検証。

---

## 9. 26.3 で「1.21 系と変わった」と実物から言える点

`[確認]` 以下はすべて実ソースに基づく:

| 項目 | 26.3 の実物 |
|---|---|
| `ResourceLocation` | **無い。`net.minecraft.resources.Identifier`** |
| `Identifier` の生成 | `new Identifier(...)` 不可。`Identifier.fromNamespaceAndPath(ns, path)` |
| `DimensionTransition` | **無い。`net.minecraft.world.level.portal.TeleportTransition`** |
| バニラの dimension JSON | `data/minecraft/dimension/` は**存在しない**（dimension はコード定義） |
| `dimension_type` | `has_fixed_time` / `has_ender_dragon_fight` / `skybox` / `cardinal_light` / `attributes` / `timelines` / `default_clock` が追加。`effects` は無い |
| バイオームのスポーン設定 | `spawners` / `spawn_costs` は廃止。`attributes."minecraft:gameplay/natural_mob_spawns"` に移動 |
| バイオームの `effects` | **必須**（`fieldOf`） |
| `Attribute` | コンストラクタが `protected`。範囲は `RangedAttribute`。`setSyncable` / `setSentiment` はビルダー風 |
| `GatherDataEvent` | `sealed`（`Server`/`Client`）。`addProvider` / `createProvider` / `getDefaultPackGenerator()` |
| `DatapackBuiltinEntriesProvider` | `forWorldLayer(...)` / `forReloadableLayer(...)` の静的ファクトリ |
| DataGen の dimension | `DataPackRegistriesHooks.getWorldRegistriesWithDimensions()` 経由で可能 |
| レンダラ登録 | `EntityRenderersEvent.RegisterRenderers`（entity + blockentity を 1 か所で） |
| `RenderType` のパッケージ | `net.minecraft.client.renderer.rendertype.RenderType` |

---

## 10. 未確認事項（推測で埋めていないもの）

1. `[未確認]` `DataComponentType.Builder` を**外部パッケージから**生成する公開手段
   （`builder()` は package-private。NeoForge の公開 API 有無）。
2. `[未確認]` `RegistrySetBuilder` の `BootstrapContext.register(...)` / `SingleRegistryBootstrap` の完全シグネチャ。
3. `[未確認]` `PackGenerator` の全メソッド（`DataProviderFromOutput` 等の関数型シグネチャ）。
4. `[未確認]` `BiomeSource` の全 `type`（`multi_noise` / `the_end` 以外）。
5. `[未確認]` `MobEffect.addAttributeModifier(...)` の完全シグネチャ。
6. ~~`MobEffectCategory` の全定数~~ → **解決済み**（§5.2、3 定数）。
7. `[未確認]` `HandlerThread` の全定数。
8. `[未確認]` 旧形式バイオーム JSON（`spawners` トップレベル）が 26.3 で
   パースエラーになるか、無視されるか（実行検証が必要）。
9. `[未確認]` 自前ディメンションへポータルで入るための `dimension_type` 要件。
10. `[未確認]` GeckoLib アイテム/防具レンダラの登録に `RegisterSpecialModelRendererEvent` への
    追加登録が必要か。

---

## 付録: 検証手順

```powershell
# 1. ビルド（ModDevGradle が MC 26.3 を decompile する）
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.2"
cd C:\Users\2674469\Desktop\mcmod\nocterra
.\gradlew.bat build          # → BUILD SUCCESSFUL in 5m 45s

# 2. 展開済みソースの場所
#   ~/.gradle/caches/neoformruntime/intermediate_results/transformSources_*_output.zip

# 3. NeoForge ソース
#   ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.3.0.99/*/neoforge-26.3.0.99-sources.jar
```

本レポートで使った展開先:
- `.agent/refs/mc-src/` … バニラ MC 26.3（NeoForge パッチ適用後）7301 ファイル
- `.agent/refs/neoforge-src/` … NeoForge 26.3.0.99 ソース 1137 ファイル

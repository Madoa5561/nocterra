# DEV_NOTES.md — 実装前に必ず読む（26.3 実API・検証済み）

根拠: 実際の decompile 済みソース（`.agent/refs/mc-src`, `.agent/refs/neoforge-src`）。
さらに詳しくは `docs/api/NEOFORGE_263.md` と `docs/api/GECKOLIB5.md` を読むこと。

## 絶対に間違えやすい点

| 旧（1.20/1.21 の知識） | 26.3 の正解 |
|---|---|
| `new ResourceLocation(ns, path)` | **`Identifier.fromNamespaceAndPath(ns, path)`**（`ResourceLocation` は存在しない。`new` も private で不可） |
| `ResourceKey.location()` | **`ResourceKey.identifier()`** |
| `DimensionTransition` | **`TeleportTransition`**（`net.minecraft.world.level.portal`） |
| `BlockBehaviour.Properties.noCollission()` | **`noCollision()`** |
| `Commands.hasPermission(int)` / `source.hasPermission(2)` | **`Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`** |
| biome JSON の `spawners` / `spawn_costs` | **`attributes."minecraft:gameplay/natural_mob_spawns"`**（`{argument, modifier}` 形式） |
| dimension_type JSON の `effects`, `ultrawarm` 等 | **`attributes` マップ**（`minecraft:visual/sky_color` など）。`cloud_color` は **8桁 ARGB** |
| `pack.mcmeta` の `pack_format` | **`min_format` / `max_format`** |

## 登録 API

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nocterra.MOD_ID);
public static final DeferredRegister.Items ITEMS   = DeferredRegister.createItems(Nocterra.MOD_ID);
public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Nocterra.MOD_ID);

// ブロック（Properties は Supplier / UnaryOperator で渡す）
BLOCKS.registerSimpleBlock("name", () -> BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.STONE));
BLOCKS.registerBlock("name", MyBlock::new, () -> BlockBehaviour.Properties.of());

// アイテム
ITEMS.registerItem("name", MyItem::new, props -> props.stacksTo(1));
ITEMS.registerSimpleItem("name", props -> props);
ITEMS.registerSimpleBlockItem(BLOCKS.SOME_BLOCK);   // DeferredBlock を渡す

// クリエイティブタブ
DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nocterra.MOD_ID)
  .register("nocterra", () -> CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.nocterra"))
      .icon(() -> new ItemStack(SOME_ITEM.get()))
      .displayItems((params, out) -> out.accept(...))
      .build());
```

- エンティティ: `DeferredRegister.Entities.registerEntityType(name, factory, MobCategory.X, properties)`
- 属性: `new RangedAttribute("attribute.nocterra.x", 20.0, 0.0, 1024.0).setSyncable(true)`
  （`Attribute` のコンストラクタは protected → `RangedAttribute` を使う）

## ネットワーク

```java
modBus.addListener((RegisterPayloadHandlersEvent event) -> {
    PayloadRegistrar reg = event.registrar("1");
    CustomPacketPayload.Type<MyPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("nocterra", "my_payload"));
    reg.playToClient(TYPE, MyPayload.STREAM_CODEC, MyPayload::handle);   // codec は RegistryFriendlyByteBuf
    reg.playToServer(TYPE, MyPayload.STREAM_CODEC, MyPayload::handle);
});
```
登録は `com.nocterra.networking.NocterraNetwork`（Lead 所有）に追記する。直接編集せず Lead に依頼。

## レンダラ登録（クライアント）

```java
modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
    event.registerEntityRenderer(NocterraEntities.FOO.get(), FooRenderer::new));
```
`com.nocterra.client.NocterraClientRenderers`（entity 担当が所有）に実装 → Lead が `NocterraClient` から呼ぶ。

## ビルド・検証

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.0.2"
cd C:\Users\2674469\Desktop\mcmod\nocterra
.\gradlew.bat build --console=plain
```

サーバー起動と RCON 検証（テストワールド `run/nocterra_test` のみ使用）:

```powershell
.\gradlew.bat runServer --console=plain *> ..\.agent\server.log    # バックグラウンド
$py="C:\Users\2674469\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py .agent\rcon.py "nocterra info"
& $py .agent\rcon.py "nocterra probe 0 70 0"
& $py .agent\rcon.py "nocterra entities"
& $py .agent\rcon.py "execute in nocterra:nocterra run nocterra entities"
& $py .agent\rcon.py "stop"        # 終了時は必ずこれ
```

**注意**: `execute in <dim> run <cmd>` はコマンドのレベル文脈を変える（`nocterra info` で確認済み）が、
`@e` セレクタはディメンションで絞られない（RCON 実行時）。エンティティのディメンション確認は
`nocterra entities`（`level.getAllEntities()`）を使うこと。

## 所有権（編集してよい範囲）

| 担当 | 編集可 |
|---|---|
| Lead | `build.gradle` `Nocterra.java` `registry/` `networking/` `world/portal/` `core/` `client/NocterraClient.java` |
| combat | `combat/` `item/`（`EmberShardItem.java` は除く）`magic/` `client/NocterraKeybinds.java` |
| entity | `entity/` `boss/` `client/render/` `client/NocterraClientRenderers.java` `assets/nocterra/geckolib/` `assets/nocterra/textures/entity/` |
| worldgen | `world/feature/` `world/structure/` `data/nocterra/worldgen/`（dimension と dimension_type は Lead 所有） |

`lang/en_us.json` / `lang/ja_jp.json` は **Lead がマージ**する。各担当は
`nocterra/lang/fragments/<担当名>.json` にキーを書くこと（読み込まれない断片ファイル）。

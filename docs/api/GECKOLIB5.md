# GeckoLib 5.5.7 (MC 26.3 / NeoForge) 実 API 検証レポート

対象: **GeckoLib 5.5.7 / neoforge / MC 26.3**
検証担当: api-scout
検証日: 2026-10-28

> 本ドキュメントの方針
> - 根拠は **実物の jar（ダウンロードした配布物）** と **`javap` の出力** のみ。
> - 各記述に `[確認]` / `[導出]` / `[未確認]` を付ける。
>   - `[確認]` … jar の中身・javap 出力に直接書かれていた事実
>   - `[導出]` … 確認済みの事実から機械的に導ける結論（推論であることを明示）
>   - `[未確認]` … この jar だけでは決められないもの（推測で埋めない）

---

## 0. 検証対象の同一性（取得元・ハッシュ）

| 項目 | 値 |
|---|---|
| 取得 API | Modrinth v2 `/project/geckolib/version?game_versions=["26.3"]&loaders=["neoforge"]` |
| version id | `2s1O42FX` |
| version_number | `5.5.7` |
| ファイル名 | `geckolib-neoforge-26.3-5.5.7.jar` |
| ダウンロード URL | `https://cdn.modrinth.com/data/8BmcQJ2H/versions/2s1O42FX/geckolib-neoforge-26.3-5.5.7.jar` |
| サイズ | 1,159,026 bytes（Modrinth メタデータの `size` と一致） |
| SHA1（実測） | `24D1AA6C02AFA779558165D0529B1F21A03E1CF7` |
| ローカル保存先 | `.agent/refs/geckolib-neoforge-26.3-5.5.7.jar` |
| jar エントリ数 | 485（うち `.class` 410） |
| class file major version | **69 = Java 25**（`javap -verbose` で確認） |

`[確認]` class file が major 69（Java 25）なので、**Java 25 ツールチェーンでビルドされた jar**。
プロジェクト（AGENTS.md: Java 25）と一致する。

検証に使ったツール:
```
javap: C:\Program Files\Java\jdk-25.0.5.0.2\bin\javap.exe   (version 25.0.5.0.2)
jar  : C:\Program Files\Java\jdk-25.0.5.0.2\bin\jar.exe
```
※ PATH 上の `java.exe` は `C:\Program Files\Common Files\Oracle\Java\javapath` の
シムで、そこには `jar.exe` / `javap.exe` が無い。実体は `C:\Program Files\Java\jdk-25.0.5.0.2\bin`。

---

## 1. パッケージ構成 / 4.x との違い

`[確認]` 5.5.7 のルートパッケージは **`com.geckolib.*`**。
jar 内のトップレベルは `com/` のみ（+ `META-INF/`, `geckolib.mixins.json`, icon/banner の png, `LICENSE.txt`）。

`[確認]` **jar に `assets/` が存在しない。** GeckoLib 自身はモデル・アニメ・テクスチャを同梱していない
（`geckolib_banner.png` / `geckolib_icon.png` は jar 直下、mod アイコン用）。
→ GeckoLib 用リソースはすべて**利用側 MOD の `assets/<ns>/` に置く**。

`[確認]` 主要パッケージ（`.class` の実在で確認）:

```
com.geckolib                       GeckoLib / GeckoLibClient / GeckoLibConstants /
                                   GeckoLibServices / GeckoLibClientServices
com.geckolib.animatable            GeoAnimatable / GeoEntity / GeoBlockEntity / GeoItem /
                                   GeoReplacedEntity / SingletonGeoAnimatable
com.geckolib.animatable.client     GeoRenderProvider
com.geckolib.animatable.instance   AnimatableInstanceCache / SingletonAnimatableInstanceCache /
                                   InstancedAnimatableInstanceCache
com.geckolib.animatable.manager    AnimatableManager / ContextAwareAnimatableManager
                                   （内部クラス ControllerRegistrar）
com.geckolib.animatable.stateless  StatelessAnimatable / StatelessGeoEntity /
                                   StatelessGeoBlockEntity / StatelessAnimationController ほか
com.geckolib.animation             AnimationController / AnimationProcessor / RawAnimation
com.geckolib.animation.object      PlayState / LoopType / EasingType
com.geckolib.animation.state       AnimationTest / KeyFrameEvent / AnimationPoint /
                                   AnimationTimeline / ControllerState / BoneSnapshot / EasingState
com.geckolib.cache                 BakedAnimationCache / BakedModelCache / GeckoLibResources /
                                   AnimatableIdCache / SyncedSingletonAnimatableCache
com.geckolib.cache.animation       Animation / BakedAnimations / BoneAnimation / Keyframe /
                                   KeyframeStack
com.geckolib.cache.animation.keyframeevent
                                   SoundKeyframeData / ParticleKeyframeData /
                                   CustomInstructionKeyframeData / KeyFrameData
com.geckolib.cache.model           BakedGeoModel / GeoBone / GeoQuad / GeoVertex /
                                   GeoLocator / ModelProperties（cuboid: CuboidGeoBone, GeoCube）
com.geckolib.constant              DataTickets / DefaultAnimations
com.geckolib.constant.dataticket   DataTicket / OverridingDataTicket
com.geckolib.event                 GeoRenderEvent / GeckoLibEventsNeoForge
com.geckolib.event.{entity,block,item,armor,object,replacedentity}
                                   Compile*RenderLayersEvent / Compile*RenderStateEvent /
                                   Geo*PreRenderEvent
com.geckolib.loading               GeckoLibGsonLoader ほか（Molang パーサ・ジオメトリ定義）
com.geckolib.model                 GeoModel / DefaultedGeoModel / DefaultedEntityGeoModel /
                                   DefaultedBlockGeoModel / DefaultedItemGeoModel
com.geckolib.network               GeckoLibNetworkingNeoForge
com.geckolib.network.packet.*      MultiloaderPacket ほか（entity / blockentity / singleton）
com.geckolib.renderer              GeoEntityRenderer / GeoBlockRenderer / GeoItemRenderer /
                                   GeoArmorRenderer / GeoObjectRenderer / GeoReplacedEntityRenderer
com.geckolib.renderer.base         GeoRenderer / GeoRendererInternals / GeoRenderState /
                                   RenderPassInfo / BoneSnapshots / PerBoneRender
com.geckolib.renderer.layer        GeoRenderLayer / GeoRenderLayersContainer
com.geckolib.renderer.layer.builtin
                                   AutoGlowingGeoLayer / BlockAndItemGeoLayer /
                                   CustomBoneTextureGeoLayer / ItemArmorGeoLayer /
                                   ItemInHandGeoLayer / TextureLayerGeoLayer
com.geckolib.renderer.specialty    DirectionalProjectileRenderer / DyeableGeoArmorRenderer
com.geckolib.renderer.texture      GeckoLibAnimatedTexture
com.geckolib.util                  GeckoLibUtil / ClientUtil / RenderUtil / RenderStateUtil /
                                   JsonUtil / MiscUtil
```

`[確認]` **`com.geckolib.renderer.base.GeoRenderer`** と **`com.geckolib.renderer.GeoEntityRenderer`**
はパッケージが違う。`GeoRenderer` は `...renderer.base` 側。

`[未確認]` 4.x のパッケージ名（`software.bernie.geckolib.*` だったか）は、この jar には
4.x の情報が入っていないため**本検証では確認していない**。断定しない。
5.5.7 が `com.geckolib.*` であることだけが `[確認]`。

---

## 2. 依存関係 / 初期化（`neoforge.mods.toml` 実物）

`[確認]` jar 内 `META-INF/neoforge.mods.toml` の全文:

```toml
modLoader="javafml"
loaderVersion="[5,)"
issueTrackerURL="https://github.com/bernie-g/geckolib/issues"
license="MIT"

[[mods]]
    modId="geckolib"
    version="5.5.7"
    displayName="GeckoLib 5"
    displayURL="http://geckolib.com/"
    iconFile="geckolib_icon.png"
    bannerFile="geckolib_banner.png"
    authors="Tslat"
    credits="Zigy, Witixin, Gecko"
    description='''
GeckoLib is an animation engine for Minecraft Mods, ...
'''

[[mixins]]
    config = "geckolib.mixins.json"

[[accessTransformers]]
    file = "META-INF/accesstransformer.cfg"

[[dependencies.geckolib]]
    modId="neoforge"
    type="required"
    versionRange="[26.3.0.7-beta,)"
    ordering="NONE"
    side="BOTH"
[[dependencies.geckolib]]
    modId="minecraft"
    type="required"
    versionRange="[26.3,)"
    ordering="NONE"
    side="BOTH"
```

`[確認]` `META-INF/MANIFEST.MF`:
```
Manifest-Version: 1.0
Specification-Title: GeckoLib 5
Specification-Version: 5.5.7
Implementation-Version: 5.5.7
Built-On-Minecraft: {strictly [26.3,); prefer 26.3}
MixinConfigs: geckolib.mixins.json
```

### 2.1 自 MOD の `neoforge.mods.toml` に書く依存

`[確認]` 上のとおり **modId は `geckolib`**、要求 versionRange は
`neoforge [26.3.0.7-beta,)` / `minecraft [26.3,)`。
自 MOD 側は以下で足りる（`type="required"` で GeckoLib を必須にする）:

```toml
[[dependencies.<自分のmodid>]]
    modId = "geckolib"
    type = "required"
    versionRange = "[5.5.7,)"
    ordering = "AFTER"
    side = "BOTH"
```
※ このテンプレート部分は**書き方の例**であり、値（`[5.5.7,)` など）は自 MOD の運用判断。
GeckoLib 側が要求する値（上記 `neoforge` / `minecraft` の range）は `[確認]`。

### 2.2 `GeckoLib.initialize()` は存在しない

`[確認]` `javap com.geckolib.GeckoLib` の出力は以下で**すべて**:

```
public final class com.geckolib.GeckoLib {
  public static final net.neoforged.neoforge.registries.DeferredRegister$DataComponents DATA_COMPONENTS_REGISTER;
  public com.geckolib.GeckoLib(net.neoforged.bus.api.IEventBus);
  static {};
}
```

→ **`initialize()` のような static 初期化メソッドは無い。**

`[確認]` コンストラクタの実処理（`javap -c`）:
```
0: aload_0
1: invokespecial  Object."<init>":()V
4: aload_1
5: invokestatic   com/geckolib/network/GeckoLibNetworkingNeoForge.init:(Lnet/neoforged/bus/api/IEventBus;)V
8: getstatic      com/geckolib/GeckoLib.DATA_COMPONENTS_REGISTER
11: aload_1
12: invokevirtual DeferredRegister$DataComponents.register:(Lnet/neoforged/bus/api/IEventBus;)V
15: invokestatic  com/geckolib/GeckoLibConstants.init:()V
18: return
```
`[確認]` static 初期化で
`DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "geckolib")` を作っている。

`[導出]` 初期化は **NeoForge が modId `geckolib` の mod クラスとして
`com.geckolib.GeckoLib` を生成する**ことで完結する。
利用側が `GeckoLib.initialize()` 等を呼ぶ必要は**ない**（そもそもメソッドが無い）。
必要なのは「依存を宣言して GeckoLib を読み込ませる」ことだけ。

`[確認]` クライアント側は `com.geckolib.GeckoLibClient`:
```
public final class com.geckolib.GeckoLibClient {
  public com.geckolib.GeckoLibClient();
  public static void registerReloadListeners(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent);
}
```

`[確認]` NeoForge のサービスローダ（`META-INF/services/`）で実装が差し込まれる:
```
com.geckolib.service.GeckoLibClient     -> com.geckolib.platform.GeckoLibClientNeoForge
com.geckolib.service.GeckoLibEvents     -> com.geckolib.event.GeckoLibEventsNeoForge
com.geckolib.service.GeckoLibNetworking -> com.geckolib.network.GeckoLibNetworkingNeoForge
com.geckolib.service.GeckoLibPlatform   -> com.geckolib.platform.GeckoLibNeoForge
```

---

## 3. エンティティ用 API

### 3.1 `com.geckolib.animatable.GeoAnimatable`（全アニメ対象の基底）

`[確認]` `javap` 出力そのまま:
```java
public interface com.geckolib.animatable.GeoAnimatable {
  public abstract void registerControllers(com.geckolib.animatable.manager.AnimatableManager$ControllerRegistrar);
  public abstract com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache();
  public default com.geckolib.animatable.instance.AnimatableInstanceCache animatableCacheOverride();
}
```

### 3.2 `com.geckolib.animatable.GeoEntity`

`[確認]`
```java
public interface com.geckolib.animatable.GeoEntity extends com.geckolib.animatable.GeoAnimatable {
  public default void triggerAnim(java.lang.String, java.lang.String);
  public default void stopTriggeredAnim(java.lang.String, java.lang.String);
}
```
引数は 2 つとも `String`（`javap` は引数名を出さないが、GeckoLib の他クラス
`AnimatableManager.tryTriggerAnimation(String,String)` と同型）。
`[導出]` 1 つ目がコントローラ名、2 つ目がアニメーション名。

`[確認]` **`GeoEntity` はインタフェースであり、クラスではない。**
実装は利用側の Entity クラスに `implements GeoEntity` させる。

### 3.3 `com.geckolib.animatable.instance.AnimatableInstanceCache`

`[確認]`
```java
public abstract class com.geckolib.animatable.instance.AnimatableInstanceCache {
  protected final com.geckolib.animatable.GeoAnimatable animatable;
  protected final java.util.function.Supplier<com.geckolib.animatable.client.GeoRenderProvider> renderProvider;
  public AnimatableInstanceCache(com.geckolib.animatable.GeoAnimatable);
  public abstract <T extends GeoAnimatable> com.geckolib.animatable.manager.AnimatableManager<T> getManagerForId(long);
  public <D> void addDataPoint(long, com.geckolib.constant.dataticket.DataTicket<D>, D);
  public <D> D getDataPoint(long, com.geckolib.constant.dataticket.DataTicket<D>);
  public java.lang.Object getRenderProvider();
}
```

`[確認]` 実装クラスは 2 つ:
```java
// エンティティなど「インスタンスごとに 1 manager」
public class InstancedAnimatableInstanceCache extends AnimatableInstanceCache {
  public InstancedAnimatableInstanceCache(GeoAnimatable);
  public AnimatableManager<?> getManagerForId(long);
}

// アイテム・防具など「ID ごとに manager」
public class SingletonAnimatableInstanceCache extends AnimatableInstanceCache {
  public SingletonAnimatableInstanceCache(GeoAnimatable);
  public <T extends GeoAnimatable> AnimatableManager<T> getManagerForId(long);
  protected <T extends GeoAnimatable> AnimatableManager<T> createManagerForId(long);
}
```

`[確認]` 生成は `GeckoLibUtil` 経由:
```java
public static com.geckolib.animatable.instance.AnimatableInstanceCache createInstanceCache(GeoAnimatable);
public static com.geckolib.animatable.instance.AnimatableInstanceCache createInstanceCache(GeoAnimatable, boolean);
```
（`boolean` の意味は `javap` のシグネチャからは決められない → `[未確認]`）

### 3.4 `com.geckolib.model.GeoModel`

`[確認]`
```java
public abstract class com.geckolib.model.GeoModel<T extends GeoAnimatable> {
  public GeoModel();
  public abstract net.minecraft.resources.Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState);
  public abstract net.minecraft.resources.Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState);
  public abstract net.minecraft.resources.Identifier getAnimationResource(T);
  public net.minecraft.resources.Identifier[] getAnimationResourceFallbacks(T);
  public void addAdditionalStateData(T, java.lang.Object, com.geckolib.renderer.base.GeoRenderState);
  public com.geckolib.cache.model.BakedGeoModel getBakedModel(net.minecraft.resources.Identifier);
  public com.geckolib.cache.animation.Animation getBakedAnimation(T, java.lang.String) throws java.lang.RuntimeException;
}
```

> **重要（5.x の破壊的変更）** `[確認]`
> `getModelResource` / `getTextureResource` の引数は **`GeoRenderState`** であって
> アニメ対象オブジェクト（`T`）ではない。`getAnimationResource` だけが `T` を取る。
> → 4.x の「`getModelResource(T animatable)`」を書くとコンパイルできない。

`[確認]` `net.minecraft.resources.Identifier` が使われている。
（**`ResourceLocation` ではない**。26.3 の実クラス名が `Identifier` であることは
GeckoLib の全シグネチャで一貫して `net/minecraft/resources/Identifier` が出ることで `[確認]`。）

`[確認]` `GeoRenderState`（`com.geckolib.renderer.base.GeoRenderState`）の実体:
```java
public interface com.geckolib.renderer.base.GeoRenderState {
  public default <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>, D);
  public default boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?>);
  public default <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>);
  public default <D> D getOrDefaultGeckolibData(DataTicket<D>, D);
  public default <D> D getOrDefaultGeckolibData(DataTicket<D>, java.util.function.Supplier<D>);
  public default int getPackedLight();
  public default float getPartialTick();
  public default double getAnimatableAge();
  public default java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, java.lang.Object> getDataMap();
}
```

`[確認]` この interface は **NeoForge の interface injection でバニラの render state に注入**される。
jar 内 `META-INF/interface_injections.json` の全文:
```json
{
  "net/minecraft/client/renderer/entity/state/EntityRenderState": [
    "com/geckolib/renderer/base/GeoRenderState"
  ],
  "net/minecraft/client/renderer/blockentity/state/BlockEntityRenderState": [
    "com/geckolib/renderer/base/GeoRenderState"
  ]
}
```
`[導出]` よって `EntityRenderState` / `BlockEntityRenderState` を
`GeoRenderState` にキャストして `getModelResource` に渡せる。

### 3.5 `DefaultedGeoModel` 系（パス自動生成つきモデル）

`[確認]`
```java
public class DefaultedEntityGeoModel<T extends GeoAnimatable> extends DefaultedGeoModel<T> {
  public <E extends Entity & GeoAnimatable> DefaultedEntityGeoModel(net.minecraft.world.entity.EntityType<E>);
  public DefaultedEntityGeoModel(net.minecraft.resources.Identifier);
  protected java.lang.String subtype();
  public DefaultedEntityGeoModel<T> withAltModel(Identifier);
  public DefaultedEntityGeoModel<T> withAltAnimations(Identifier);
  public DefaultedEntityGeoModel<T> withAltTexture(Identifier);
}
```
`DefaultedBlockGeoModel` / `DefaultedItemGeoModel` も同型（`subtype()` だけ違う）。

`[確認]` `subtype()` の戻り値（`javap -c` の `ldc` で確認）:
| クラス | `subtype()` |
|---|---|
| `DefaultedEntityGeoModel` | `"entity"` |
| `DefaultedBlockGeoModel` | `"block"` |
| `DefaultedItemGeoModel` | `"item"` |

---

## 4. レンダラ

### 4.1 `com.geckolib.renderer.base.GeoRenderer`

`[確認]` 26.3 は **render state + submit node 方式**。`javap` 出力:
```java
public interface com.geckolib.renderer.base.GeoRenderer<T extends GeoAnimatable, O, R extends GeoRenderState>
        extends com.geckolib.renderer.base.GeoRendererInternals<T, O, R> {
  public default int getRenderColor(T, O, float);
  public default int getPackedOverlay(T, O, float, float);
  public default float getMotionAnimThreshold(T);
  public default net.minecraft.client.renderer.rendertype.RenderType getRenderType(R, net.minecraft.resources.Identifier);
  public default void addRenderData(T, O, R, float);
  public default void setMolangQueryValues(T, O, R, float);
  public default void performRenderPass(R, com.mojang.blaze3d.vertex.PoseStack,
                                        net.minecraft.client.renderer.SubmitNodeCollector,
                                        net.minecraft.client.renderer.state.level.CameraRenderState);
  public default void performRenderPass(R, PoseStack, SubmitNodeCollector, CameraRenderState,
                                        java.util.List<RenderPassInfo$BoneUpdater<R>>);
  public default void preRenderPass(RenderPassInfo<R>, SubmitNodeCollector);
  public default void scaleModelForRender(RenderPassInfo<R>, float, float);
  public default void adjustRenderPose(RenderPassInfo<R>);
  public default void adjustModelBonesForRender(RenderPassInfo<R>, BoneSnapshots);
  public default void submitRenderTasks(RenderPassInfo<R>,
                                        net.minecraft.client.renderer.OrderedSubmitNodeCollector,
                                        net.minecraft.client.renderer.rendertype.RenderType);
  public default void postRenderPass(RenderPassInfo<R>, SubmitNodeCollector);
}
```

`[確認]` 26.3 側で登場するクラス（GeckoLib のシグネチャに現れる実在型）:
`net.minecraft.client.renderer.SubmitNodeCollector`,
`net.minecraft.client.renderer.OrderedSubmitNodeCollector`,
`net.minecraft.client.renderer.state.level.CameraRenderState`,
`net.minecraft.client.renderer.rendertype.RenderType`.

> `[確認]` **旧来の `render(GeoModel, T, float, PoseStack, MultiBufferSource, VertexConsumer, int, int)`
> 形式のメソッドは `GeoRenderer` に存在しない。** 4.x のチュートリアル通りに
> `render()` を override すると**存在しないメソッド**になる。

### 4.2 `com.geckolib.renderer.GeoEntityRenderer`

`[確認]`
```java
public class com.geckolib.renderer.GeoEntityRenderer<
        T extends net.minecraft.world.entity.Entity & GeoAnimatable,
        R extends net.minecraft.client.renderer.entity.state.EntityRenderState>
      extends net.minecraft.client.renderer.entity.EntityRenderer<T, R>
      implements com.geckolib.renderer.base.GeoRenderer<T, java.lang.Void, R> {

  protected final GeoRenderLayersContainer<T, java.lang.Void, R> renderLayers;
  protected final com.geckolib.model.GeoModel<T> model;
  protected final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;
  protected float scaleWidth;
  protected float scaleHeight;

  public GeoEntityRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider$Context,
                           net.minecraft.world.entity.EntityType<? extends T>);
  public GeoEntityRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider$Context,
                           com.geckolib.model.GeoModel<T>);

  public double getNameRenderCutoffDistance(T);
  protected float getDeathMaxRotation(GeoRenderState);
  protected <S extends LivingEntityRenderState> S convertRenderStateToLiving(R);
  public com.geckolib.model.GeoModel<T> getGeoModel();
  public java.util.List<GeoRenderLayer<T, java.lang.Void, R>> getRenderLayers();
  public GeoEntityRenderer<T, R> withRenderLayer(java.util.function.Function<? super GeoEntityRenderer<T,R>, GeoRenderLayer<T,java.lang.Void,R>>);
  public GeoEntityRenderer<T, R> withRenderLayer(GeoRenderLayer<T, java.lang.Void, R>);
  public GeoEntityRenderer<T, R> withScale(float);
  public GeoEntityRenderer<T, R> withScale(float, float);
  public long getInstanceId(T, java.lang.Void);
  public int getRenderColor(T, java.lang.Void, float);
  public int getPackedOverlay(T, java.lang.Void, float, float);
  public boolean shouldShowName(T, double);
  protected float calculateYRot(T, float, float);
  public RenderType getRenderType(R, net.minecraft.resources.Identifier);
  public void captureDefaultRenderState(T, java.lang.Void, R, float);
  public void scaleModelForRender(RenderPassInfo<R>, float, float);
  public void adjustRenderPose(RenderPassInfo<R>);
  public void submit(R, PoseStack, SubmitNodeCollector, CameraRenderState);
  public void postRenderPass(RenderPassInfo<R>, SubmitNodeCollector);
  protected void applyRotations(RenderPassInfo<R>, PoseStack, float);
  public R createRenderState(T, java.lang.Void);
  public void extractRenderState(T, R, float);
  protected void extractLivingEntityRenderState(LivingEntity, LivingEntityRenderState, float, ItemModelResolver);
  public void fireCompileRenderLayersEvent();
  public void fireCompileRenderStateEvent(T, java.lang.Void, R, float);
  public boolean firePreRenderEvent(RenderPassInfo<R>, SubmitNodeCollector);
}
```
※ 末尾に `GeoAnimatable`/`Object` を取る**ブリッジメソッド**（コンパイラ生成）が並ぶが省略。

`[確認]` `R` の実体は `EntityRenderer` の `createRenderState()` が返す型。
`GeoEntityRenderer` は `net.minecraft.client.renderer.entity.EntityRenderer<T,R>` を継承している。

### 4.3 `GeoRenderLayer`

`[確認]` クラス実在: `com.geckolib.renderer.layer.GeoRenderLayer` /
`com.geckolib.renderer.layer.GeoRenderLayersContainer`。
追加は `withRenderLayer(...)`（上記シグネチャ）。
組込レイヤー: `AutoGlowingGeoLayer`, `BlockAndItemGeoLayer`, `CustomBoneTextureGeoLayer`,
`ItemArmorGeoLayer`, `ItemInHandGeoLayer`, `TextureLayerGeoLayer`。

`[確認]` `RenderPassInfo`（`render` 系コールバックの引数）:
```java
public class RenderPassInfo<R extends GeoRenderState> {
  public GeoRenderer<?,?,R> renderer();
  public R renderState();
  public com.mojang.blaze3d.vertex.PoseStack poseStack();
  public BakedGeoModel model();
  public CameraRenderState cameraState();
  public int packedLight();
  public int packedOverlay();
  public int renderColor();
  public boolean willRender();
  public <D> D getGeckolibData(DataTicket<D>);
  public <D> D getOrDefaultGeckolibData(DataTicket<D>, D);
  public com.mojang.blaze3d.vertex.PoseStack$Pose getPreRenderMatrixPose();
  public org.joml.Matrix4f getPreRenderMatrixState();
  public com.mojang.blaze3d.vertex.PoseStack$Pose getModelRenderMatrixPose();
  public org.joml.Matrix4f getModelRenderMatrixState();
  public void addPerBoneRender(GeoBone, PerBoneRender<R>);
  public void addBoneUpdater(RenderPassInfo$BoneUpdater<R>);
  public void addBonePositionListener(java.lang.String, RenderPassInfo$BonePositionListener);
  public void addBonePositionListener(GeoBone, RenderPassInfo$BonePositionListener);
  public void addLocatorPositionListener(java.lang.String, RenderPassInfo$BonePositionListener);
  public void renderPosed(java.lang.Runnable);
  public java.util.Map<GeoBone, java.util.List<PerBoneRender<R>>> getBoneRenderTasks();
  public void captureModelRenderPose();
  public static <R extends GeoRenderState> RenderPassInfo<R> create(
      GeoRenderer<?,?,R>, R, PoseStack, CameraRenderState, boolean);
}
```

---

## 5. アイテム / 防具 / ブロックエンティティ

### 5.1 `com.geckolib.animatable.GeoItem`

`[確認]`
```java
public interface com.geckolib.animatable.GeoItem extends com.geckolib.animatable.SingletonGeoAnimatable {
  public static void registerSyncedAnimatable(com.geckolib.animatable.SingletonGeoAnimatable);
  public static long getId(net.minecraft.world.item.ItemStack);
  public static long getOrAssignId(net.minecraft.world.item.ItemStack, net.minecraft.server.level.ServerLevel);
  public default boolean isPerspectiveAware();
  public default com.geckolib.animatable.instance.AnimatableInstanceCache animatableCacheOverride();
}
```

`[確認]` `SingletonGeoAnimatable`（`GeoItem` の親）:
```java
public interface com.geckolib.animatable.SingletonGeoAnimatable extends GeoAnimatable {
  public static void registerSyncedAnimatable(SingletonGeoAnimatable);
  public default void triggerAnim(net.minecraft.world.entity.Entity, long, java.lang.String, java.lang.String);
  public default void stopTriggeredAnim(Entity, long, String, String);
  public default void triggerArmorAnim(Entity, long, String, String);
  public default void stopTriggeredArmorAnim(Entity, long, String, String);
  public default AnimatableInstanceCache animatableCacheOverride();
  public default void createGeoRenderer(java.util.function.Consumer<com.geckolib.animatable.client.GeoRenderProvider>);
  public default java.lang.Object getRenderProvider();
}
```
`[確認]` `createGeoRenderer` の default 実装は **空（`return` のみ）**。
→ アイテム/防具でレンダラを渡したいクラスが override する。

`[確認]` `GeoRenderProvider`:
```java
public interface com.geckolib.animatable.client.GeoRenderProvider {
  public static final GeoRenderProvider DEFAULT;
  public static GeoRenderProvider of(net.minecraft.world.item.ItemStack);
  public static GeoRenderProvider of(net.minecraft.world.item.Item);
  public default com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer();
  public default com.geckolib.renderer.GeoArmorRenderer<?, ?> getGeoArmorRenderer(
      net.minecraft.world.item.ItemStack, net.minecraft.world.entity.EquipmentSlot);
}
```

### 5.2 `com.geckolib.animatable.GeoBlockEntity`

`[確認]`
```java
public interface com.geckolib.animatable.GeoBlockEntity extends GeoAnimatable {
  public default void triggerAnim(java.lang.String, java.lang.String);
  public default void stopTriggeredAnim(java.lang.String, java.lang.String);
}
```
`[確認]` 実装は `BlockEntity` に `implements GeoBlockEntity`。
`com.geckolib.renderer.GeoBlockRenderer` の型制約が
`T extends net.minecraft.world.level.block.entity.BlockEntity & GeoAnimatable` なので
`BlockEntity` 側で implements する必要がある。

### 5.3 `GeoBlockRenderer`

`[確認]`
```java
public class com.geckolib.renderer.GeoBlockRenderer<
        T extends net.minecraft.world.level.block.entity.BlockEntity & GeoAnimatable,
        R extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      implements com.geckolib.renderer.base.GeoRenderer<T, java.lang.Void, R>,
                 net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T, R> {
  public static final DataTicket<net.minecraft.core.Direction> DIRECTION_FACING;
  public GeoBlockRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context,
                          net.minecraft.world.level.block.entity.BlockEntityType<? extends T>);
  public GeoBlockRenderer(BlockEntityRendererProvider$Context, GeoModel<T>);
  protected net.minecraft.core.Direction getBlockStateDirection(T);
  protected void tryRotateByBlockstate(RenderPassInfo<R>, PoseStack);
  // getGeoModel/getRenderLayers/withRenderLayer/withScale/... は GeoEntityRenderer と同型
}
```

### 5.4 `GeoItemRenderer`

`[確認]`
```java
public class com.geckolib.renderer.GeoItemRenderer<T extends net.minecraft.world.item.Item & GeoAnimatable>
      implements GeoRenderer<T, GeoItemRenderer$RenderData, GeoRenderState> {
  public static final DataTicket<net.minecraft.world.item.Item> CURRENT_ITEM;
  public <I extends T> GeoItemRenderer(I);                           // Item インスタンスから
  public GeoItemRenderer(com.geckolib.model.GeoModel<T>);
  public GeoItemRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher,
                         net.minecraft.client.model.geom.EntityModelSet,
                         com.geckolib.model.GeoModel<T>);
  public GeoItemRenderer<T> useAlternateGuiLighting();
  public void setupLightingForGuiRender();
  public GeoItemRenderer<T> withRenderLayer(...);
  public GeoItemRenderer<T> withScale(float); withScale(float,float);
  public void submit(GeoRenderState, PoseStack, SubmitNodeCollector, int);
  // ...
}
```

### 5.5 `GeoArmorRenderer`

`[確認]`
```java
public class com.geckolib.renderer.GeoArmorRenderer<
        T extends net.minecraft.world.item.Item & GeoItem,
        R extends net.minecraft.client.renderer.entity.state.HumanoidRenderState>
      implements GeoRenderer<T, GeoArmorRenderer$RenderData, R> {
  public static final DataTicket<java.lang.Boolean> IS_GECKOLIB_WEARER;
  public static final DataTicket<net.minecraft.client.model.HumanoidModel<? extends HumanoidRenderState>> BASE_MODEL;
  public static final DataTicket<net.minecraft.world.entity.EquipmentSlot> CURRENT_SLOT;
  public <I extends T> GeoArmorRenderer(I);
  public GeoArmorRenderer(com.geckolib.model.GeoModel<T>);
  public java.util.List<GeoArmorRenderer$ArmorSegment> getSegmentsForSlot(R, EquipmentSlot);
  public java.lang.String getBoneNameForSegment(R, GeoArmorRenderer$ArmorSegment);
  public static <R extends HumanoidRenderState, A extends HumanoidModel<R>> boolean tryRenderGeoArmorPiece(
      java.util.function.BiFunction<R, EquipmentSlot, A>, PoseStack, SubmitNodeCollector,
      ItemStack, EquipmentSlot, int, R);
  public static <R extends HumanoidRenderState, A extends HumanoidModel<R>> void captureRenderStates(
      R, LivingEntity, float, java.util.function.BiFunction<R, EquipmentSlot, A>,
      java.util.function.Function<EquipmentSlot, R>);
  // ...
}
```
`[確認]` 防具は `HumanoidRenderState` ベース。`GeoArmorRenderer$ArmorSegment` /
`GeoArmorRenderer$RenderData` / `GeoArmorRenderer$StackForRender` という内部型を持つ。

`[確認]` 関連する組込レイヤー: `ItemArmorGeoLayer`（`ItemArmorGeoLayer$RenderData`）、
専門レンダラ: `com.geckolib.renderer.specialty.DyeableGeoArmorRenderer`。

### 5.6 その他の実在レンダラ

`[確認]` `com.geckolib.renderer.GeoObjectRenderer`（エンティティでない任意オブジェクト用）、
`com.geckolib.renderer.GeoReplacedEntityRenderer`、
`com.geckolib.renderer.specialty.DirectionalProjectileRenderer`、
`com.geckolib.renderer.internal.GeckolibItemSpecialRenderer`
（バニラ `SpecialModelRenderer` 実装。アイテム描画の内部配線）。

`[確認]` `GeckolibItemSpecialRenderer` は
`net.minecraft.client.renderer.special.SpecialModelRenderer<GeckolibItemSpecialRenderer$RenderData<T>>`
を実装。`submit(RenderData<T>, PoseStack, SubmitNodeCollector, int,int,boolean,int)` /
`extractArgument(ItemStack, ItemStackRenderState, ItemDisplayContext, ClientLevel, ItemOwner)`。

`[未確認]` 26.3 で「アイテム用に `SpecialModelRenderer` をどう登録するか」のイベント名・
登録 API は、GeckoLib 内部の mixin（`SpecialModelRenderersMixin`）が握っており、
この jar からは**利用者が追加登録すべきか否かを確定できない**。
→ アイテム/防具の登録手順は 26.3 の実ソース確認後に追記する（`NEOFORGE_263.md` 側）。

---

## 6. アニメーション制御 API

### 6.1 `com.geckolib.animation.AnimationController`

`[確認]` コンストラクタ 3 種:
```java
public class AnimationController<T extends GeoAnimatable> {
  public AnimationController(AnimationController$AnimationStateHandler<T>);
  public AnimationController(java.lang.String, AnimationController$AnimationStateHandler<T>);
  public AnimationController(java.lang.String, int, AnimationController$AnimationStateHandler<T>);
}
```
`[導出]` 2 番目/3 番目の第 1 引数がコントローラ名（`protected final String name` に対応）。
3 番目の `int` は `protected int transitionTicks` に対応すると考えられるが、
`javap` のシグネチャだけでは確定しない → **`[未確認]`**（`setTransitionTicks(int)` が別途ある）。

`[確認]` 主要メソッド（すべて `javap` 出力より）:
```java
public String getName();
public PlayState getPlayState();
public int getTransitionTicks();
public double getAnimationSpeed();
public com.geckolib.animation.state.AnimationTimeline getTimeline();
public double getCurrentTimelineTime();
public double getCurrentAnimationTime();
public com.geckolib.animation.state.AnimationPoint getCurrentAnimationPoint();
public com.geckolib.animation.RawAnimation getCurrentRawAnimation();
public boolean isPlayingTriggeredAnimation();
public boolean hasAnimationFinished();
public boolean isAnimatingBones();
public boolean isTransitioning();

public AnimationController<T> setSoundKeyframeHandler(KeyframeEventHandler<T, SoundKeyframeData>);
public AnimationController<T> setParticleKeyframeHandler(KeyframeEventHandler<T, ParticleKeyframeData>);
public AnimationController<T> setCustomInstructionKeyframeHandler(KeyframeEventHandler<T, CustomInstructionKeyframeData>);
public AnimationController<T> setAnimationSpeed(double);
public AnimationController<T> setTransitionTicks(int);
public AnimationController<T> setOverrideEasingType(com.geckolib.animation.object.EasingType);
public AnimationController<T> additiveAnimations();
public AnimationController<T> receiveTriggeredAnimations();
public AnimationController<T> triggerableAnim(String, RawAnimation);

public void setAnimation(com.geckolib.animation.RawAnimation);
public boolean triggerAnimation(String);
public void setAnimationTime(double);
public void setTimelineTime(double);
public boolean stopTriggeredAnimation();
public void reset();
public java.util.Set<com.geckolib.loading.math.value.Variable> getUsedVariables();
public boolean isTriggeredAnimation(String);
```

> `[確認]` アニメを開始するメソッド名は **`setAnimation(RawAnimation)`**。
> `playAnimation(...)` というメソッドは `javap` 出力に**存在しない**。

`[確認]` 内部ハンドラ型（関数型インタフェース）:
```java
public interface AnimationController$AnimationStateHandler<A extends GeoAnimatable> {
  public abstract com.geckolib.animation.object.PlayState handle(
      com.geckolib.animation.state.AnimationTest<A>);
}

public interface AnimationController$KeyframeEventHandler<
        A extends GeoAnimatable, E extends com.geckolib.cache.animation.keyframeevent.KeyFrameData> {
  public abstract void handle(com.geckolib.animation.state.KeyFrameEvent<A, E>);
}
```

### 6.2 `com.geckolib.animation.object.PlayState`

`[確認]` enum で、定数は **3 つだけ**:
```java
public final class PlayState extends java.lang.Enum<PlayState> {
  public static final PlayState CONTINUE;
  public static final PlayState PAUSE;
  public static final PlayState STOP;
  public static PlayState[] values();
  public static PlayState valueOf(String);
}
```
（`PLAY` という定数は 5.5.7 には**無い**。）

### 6.3 `com.geckolib.animation.state.AnimationTest`

`[確認]` **Record**（`extends java.lang.Record`）。ハンドラの引数として渡ってくる。
```java
public final class com.geckolib.animation.state.AnimationTest<T extends GeoAnimatable>
      extends java.lang.Record {
  public AnimationTest(T, GeoRenderState, AnimatableManager<T>, AnimationController<T>);
  public boolean isMoving();
  public void setAnimation(RawAnimation);
  public PlayState setAndContinue(RawAnimation);
  public boolean isCurrentAnimation(RawAnimation);
  public boolean isCurrentAnimationStage(String);
  public void setControllerSpeed(float);
  public boolean hasData(DataTicket<?>);
  public <D> D getData(DataTicket<D>);
  public <D> D getDataOrDefault(DataTicket<D>, D);
  // Record アクセサ
  public T animatable();
  public GeoRenderState renderState();
  public AnimatableManager<T> manager();
  public AnimationController<T> controller();
  public final String toString(); public final int hashCode(); public final boolean equals(Object);
}
```
`[確認]` `isMoving()` が**引数なし**で存在する（旧 API で `isMoving(AnimationTest)` と
書かれていたものが、テストオブジェクト自身のメソッドになっている）。

### 6.4 `com.geckolib.animation.state.KeyFrameEvent`

`[確認]` Record:
```java
public final class KeyFrameEvent<T extends GeoAnimatable, E extends KeyFrameData> extends java.lang.Record {
  public KeyFrameEvent(T, GeoRenderState, AnimationController<T>, E);
  public float getPartialTick();
  public double getAnimationTick();
  public T animatable();
  public GeoRenderState renderState();
  public AnimationController<T> controller();
  public E keyframeData();
}
```
`[確認]` クラス名は **`KeyFrameEvent`**（`KeyframeEvent` ではない。`F` が大文字）。

### 6.5 `com.geckolib.animation.RawAnimation`

`[確認]`
```java
public final class com.geckolib.animation.RawAnimation {
  public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, RawAnimation> STREAM_CODEC;
  public static RawAnimation begin();
  public RawAnimation thenPlay(String);
  public RawAnimation thenLoop(String);
  public RawAnimation thenWait(int);
  public RawAnimation thenPlayAndHold(String);
  public RawAnimation thenPlayXTimes(String, int);
  public RawAnimation then(String, com.geckolib.animation.object.LoopType);
  public java.util.List<RawAnimation$Stage> getAnimationStages();
  public static RawAnimation copyOf(RawAnimation);
  public int getStageCount();
}
```

### 6.6 `com.geckolib.animation.object.LoopType` / `EasingType`

`[確認]` どちらも **interface + 定数**（enum ではない）:
```java
public interface LoopType {
  public static final java.util.Map<String, LoopType> LOOP_TYPES;
  public static final LoopType DEFAULT;
  public static final LoopType PLAY_ONCE;
  public static final LoopType HOLD_ON_LAST_FRAME;
  public static final LoopType LOOP;
  public abstract boolean shouldKeepPlaying(GeoAnimatable, AnimationPoint,
      AnimationTimeline$Stage, GeoRenderState, AnimationController<? extends GeoAnimatable>);
  public default String getId() throws java.lang.IllegalStateException;
  public static LoopType fromString(String);
  public static LoopType register(String, LoopType);
}
```
`[確認]` `EasingType` も interface で `LINEAR` / `STEP` / `EASE_IN_SINE` … 等の定数を持つ
（`EASING_TYPES` マップ、`CatmullRomEasing` 内部クラスあり）。

`[確認]` 独自イージング/ループの登録 API（`GeckoLibUtil`）:
```java
public static synchronized LoopType addCustomLoopType(String, LoopType);
public static synchronized EasingType addCustomEasingType(String, EasingType);
public static synchronized EasingType addCustomSimpleEasingType(String, it.unimi.dsi.fastutil.doubles.Double2DoubleFunction);
```

### 6.7 `AnimatableManager$ControllerRegistrar`

`[確認]` `registerControllers` の引数。Record:
```java
public final class AnimatableManager$ControllerRegistrar extends java.lang.Record {
  public ControllerRegistrar(java.util.List<AnimationController<? extends GeoAnimatable>>);
  public ControllerRegistrar add(AnimationController<?>...);
  public ControllerRegistrar add(AnimationController<?>);
  public ControllerRegistrar remove(String);
  public java.util.List<AnimationController<? extends GeoAnimatable>> controllers();
}
```

`[確認]` `AnimatableManager`:
```java
public class AnimatableManager<T extends GeoAnimatable> {
  public AnimatableManager(GeoAnimatable);
  public java.util.Map<String, AnimationController<T>> getAnimationControllers();
  public void addController(AnimationController<T>);
  public void removeController(String);
  public <D> void setAnimatableData(DataTicket<D>, D);
  public <D> D getAnimatableData(DataTicket<D>);
  public void tryTriggerAnimation(String);
  public void tryTriggerAnimation(String, String);
  public void stopTriggeredAnimation(String);
  public void stopTriggeredAnimation(String, String);
  public void markRenderedAt(double);
  public double getFirstRenderTick();
}
```

### 6.8 `DefaultAnimations`（組込 RawAnimation 定数）

`[確認]` `com.geckolib.constant.DefaultAnimations` に `RawAnimation` 定数が実在:
`ITEM_ON_USE, IDLE, IDLE_FLYING, LIVING, SPAWN, DIE, INTERACT, DEPLOY, REST, SIT, WALK, SWIM,
RUN, DRIVE, FLY, CRAWL, JUMP, SNEAK, DIVE, ATTACK_CAST, ATTACK_SWING, ATTACK_THROW,
ATTACK_PUNCH, ATTACK_BITE, ATTACK_SLAM, ATTACK_STOMP, ATTACK_STRIKE, ATTACK_FLYING_ATTACK,
ATTACK_SHOOT, ATTACK_BLOCK, ATTACK_CHARGE, ATTACK_CHARGE_END, ATTACK_POWERUP`

`[確認]` 補助ファクトリ:
```java
public static <T extends GeoAnimatable> AnimationController<T> triggerOnlyController();
public static <T extends GeoAnimatable> AnimationController<T> basicPredicateController(
    java.util.function.Function<AnimationTest<T>, RawAnimation>);
public static <T extends GeoAnimatable> AnimationController<T> genericLivingController();
public static <T extends Entity & GeoAnimatable> AnimationController<T> genericDeathController();
```
`[未確認]` 各 `RawAnimation` 定数が参照する実際のアニメ名文字列は、
`<clinit>` を追う必要がある。必要になったら追加調査する（本レポートでは未取得）。

---

## 7. リソースパス（**最重要**）

### 7.1 確認できた生の定数（`javap -c` の `ldc` / `BootstrapMethods` より）

`[確認]` `com.geckolib.cache.GeckoLibResources` の static 初期化:
```
ldc "geckolib_resources"   -> RELOAD_LISTENER_ID = GeckoLibConstants.id("geckolib_resources")
ldc "geckolib/animations"  -> ANIMATIONS_PATH    = GeckoLibConstants.id("geckolib/animations")
ldc "geckolib/models"      -> MODELS_PATH        = GeckoLibConstants.id("geckolib/models")
Pattern.compile("((\\.geo)|((\\.animation)s?))?(\\.json)$")   -> SUFFIX_STRIPPER
Pattern.compile("^(geckolib/)((animations/)|(models/))?")      -> PREFIX_STRIPPER
SUPPORTED_FILE_TYPES = { "json" }
```

`[確認]` ローダが実際に走査するのは `MODELS_PATH.getPath()` / `ANIMATIONS_PATH.getPath()`、
つまり文字列 **`"geckolib/models"`** と **`"geckolib/animations"`**:
```
loadAnimations: ... getstatic ANIMATIONS_PATH; invokevirtual Identifier.getPath();
                invokedynamic; invokestatic bakeAllResources(...)
loadModels:     ... getstatic MODELS_PATH;     invokevirtual Identifier.getPath();
                invokedynamic; invokestatic bakeAllResources(...)
bakeAllResources: ResourceManager.listResources:(Ljava/lang/String;)... を呼ぶ
```
`[確認]` `listResources` の実シグネチャ（定数プールより）:
```
net/minecraft/server/packs/resources/ResourceManager.listResources:
    (Ljava/lang/String;Lnet/minecraft/server/packs/resources/ResourceManager$Selector;)Ljava/util/Map;
```

`[確認]` キャッシュのキーは、走査で得た Identifier に `stripPrefixAndSuffix` を適用したもの
（`lambda$bakeResource$1` が `stripPrefixAndSuffix(Identifier)` を呼んで `Pair.of(key, baked)` を作る）。
`stripPrefixAndSuffix` は `PREFIX_STRIPPER` / `SUFFIX_STRIPPER` を `find()` して削る。

`[確認]` `com.geckolib.cache.BakedModelCache.stripLegacyPath` は同じことを手書きで行う:
```
path.startsWith("geckolib/") -> path.substring(9)
path.startsWith("models/")   -> path.substring(7)
path.endsWith(".json")       -> path.substring(0, len-5)
path.endsWith(".geo")        -> path.substring(0, len-4)
```
`[確認]` 余計な prefix/suffix が付いていると **ERROR ログ**を出す:
```
"Superfluous prefix or suffix found in model resource path: '{}'. Should be '{}'"
```
（アニメ側も同様: `"Superfluous prefix or suffix found in animation resource path: '{}'. Should be '{}'"`）

`[確認]` `BakedAnimationCache` 側の定数: `"geckolib/"`, `"animations/"`, `".json"`, `".animations"`。

`[確認]` `DefaultedGeoModel` のパス生成（BootstrapMethods の concat レシピ）:
```
BootstrapMethod #0:  recipe = "\u0001/"                    （args: subtype）
BootstrapMethod #1:  recipe = "textures/\u0001/\u0001.png"  （args: subtype, id.getPath()）
```
```
buildFormattedModelPath(id)     = id.withPrefix( subtype() + "/" )
buildFormattedAnimationPath(id) = id.withPrefix( subtype() + "/" )
buildFormattedTexturePath(id)   = id.withPath( "textures/" + subtype() + "/" + id.getPath() + ".png" )
```
`subtype()` = `"entity"` / `"block"` / `"item"`（`Defaulted*GeoModel` ごと、§3.5 参照）。

`[確認]` `GeckoLibResources` のログ文字列: `"Loaded {} models and {} animations from resources"`、
`"Found duplicate GeckoLib resource in {}: {}"`、`"Error loading resource: {}"`。

### 7.2 ファイル配置 `[確認]`（26.3 実ソースで裏取り済み）

`[確認]` 26.3 の実ソースで `ResourceManager.listResources` の実装を確認した。
根拠（`.agent/refs/mc-src/` の decompile 済みソース）:

```java
// net/minecraft/server/packs/PackType.java
public enum PackType implements StringRepresentable {
    CLIENT_RESOURCES("assets"),
    SERVER_DATA("data");

// net/minecraft/server/packs/PathPackResources.java
private Path topPackDir(PackType type) { return this.root.resolve(type.getDirectory()); }

public static void listResources(Path topPath, String namespace, String directory,
                                 PackResources.ResourceOutput output) {
    FileUtil.decomposePath(directory).ifSuccess(decomposedPath -> {
        Path namespaceDir = topPath.resolve(namespace);          // assets/<namespace>
        listPath(namespace, namespaceDir, (List<String>)decomposedPath, output);
    }).ifError(...);
}

public static void listPath(String namespace, Path topDir, List<String> decomposedPrefixPath,
                            PackResources.ResourceOutput output) {
    Path targetPath = FileUtil.resolvePath(topDir, decomposedPrefixPath);
    try (Stream<Path> files = Files.find(targetPath, Integer.MAX_VALUE, PathPackResources::isRegularFile)) {
        files.forEach(file -> {
            String resourcePath = PATH_JOINER.join(topDir.relativize(file));  // "geckolib/models/entity/xxx.geo.json"
            Identifier identifier = Identifier.tryBuild(namespace, resourcePath);
            ...
            output.accept(identifier, IoSupplier.create(file));
        });
    }
}

// net/minecraft/resources/Identifier.java
public Identifier withPath(String newPath) { return new Identifier(this.namespace, assertValidPath(...)); }
public Identifier withPrefix(String prefix) { return this.withPath(prefix + this.path); }
```

→ 走査で返る Identifier は **`<ns>:geckolib/models/<以降のパス>`**。
GeckoLib はこれに `stripPrefixAndSuffix` を当てて `entity/xxx` をキーにする。
一方 `DefaultedEntityGeoModel` は `id.withPrefix("entity/")` で `<ns>:entity/xxx` を作る。
両者が一致する → **ファイルは `assets/<ns>/geckolib/models/` 配下**。`[確認]`

`new Identifier("nocterra", "forest_king")` を
`DefaultedEntityGeoModel` に渡した場合:　**`[確認]`**

| 種別 | `GeoModel` が返す Identifier | 配置するファイル（実パス） |
|---|---|---|
| モデル | `nocterra:entity/forest_king` | `assets/nocterra/geckolib/models/entity/forest_king.geo.json` |
| アニメ | `nocterra:entity/forest_king` | `assets/nocterra/geckolib/animations/entity/forest_king.animation.json` |
| テクスチャ | `nocterra:textures/entity/forest_king.png` | `assets/nocterra/textures/entity/forest_king.png` |

理由:
- 走査パスが `geckolib/models` / `geckolib/animations`（§7.1）
- 走査結果のキーは prefix/suffix を削ったもの → `entity/forest_king`
- モデル/アニメの既定 Identifier は `id.withPrefix("entity/")` → `nocterra:entity/forest_king`
- `withPrefix` は `withPath(prefix + path)` で **path の先頭に付くだけ**（namespace は不変）
- テクスチャだけは `withPath("textures/entity/<path>.png")` で**別系統**
  （`geckolib/` 配下ではなく、バニラの `assets/<ns>/<path>` として解決される）
- `SUFFIX_STRIPPER` が `.geo.json` / `.animation.json` / `.animations.json` / `.json` を吸収

> **注意 `[確認]`**: モデル/アニメの Identifier に `geckolib/` や `models/` を自分で付けると
> **動くが ERROR ログが出る**（"Superfluous prefix or suffix …"）。
> 正しいのは **付けない**形（`<ns>:entity/<name>`）。
> 旧パスも `stripLegacyPath` で解決はされる（4.x 互換）。

`new Identifier("nocterra", "forest_king")` を
`DefaultedEntityGeoModel` に渡した場合:

| 種別 | `GeoModel` が返す Identifier | 配置されるファイル |
|---|---|---|
| モデル | `nocterra:entity/forest_king` | `assets/nocterra/geckolib/models/entity/forest_king.geo.json` |
| アニメ | `nocterra:entity/forest_king` | `assets/nocterra/geckolib/animations/entity/forest_king.animation.json` |
| テクスチャ | `nocterra:textures/entity/forest_king.png` | `assets/nocterra/textures/entity/forest_king.png` |

理由（すべて `[確認]` 事実の組み合わせ）:
- 走査パスが `geckolib/models` / `geckolib/animations` である（§7.1）
- 走査結果のキーは prefix/suffix を削ったもの → `entity/forest_king`
- モデル/アニメの既定 Identifier は `id.withPrefix("entity/")` → `nocterra:entity/forest_king`
- テクスチャだけは `withPath("textures/entity/<path>.png")` で**別系統**
  （`geckolib/` 配下ではない。バニラのテクスチャ解決に乗る）
- `SUFFIX_STRIPPER` が `.geo.json` / `.animation.json` / `.animations.json` / `.json` を吸収
  → 拡張子は `.geo.json`（`.animation.json`）が素直

> **注意 `[導出]`**: モデル/アニメの Identifier に `geckolib/` や `models/` を自分で付けると
> **動くが ERROR ログが出る**（"Superfluous prefix or suffix …"）。
> 正しいのは **付けない**形（`<ns>:entity/<name>`）。
> 4.x 互換のため旧パスも解決はされる（`stripLegacyPath`）。

`[未確認]` **`assets/<ns>/` を前置する部分**（= バニラ `ResourceManager.listResources` が
`assets/<namespace>/<startingPath>` を走査する仕様）は、GeckoLib の jar からは確定できない
（バニラ側の実装）。→ 26.3 の実ソースで確認して確定させる。**現時点では `[導出]` 扱い。**
（プロジェクトの AGENTS.md も `assets/nocterra/geckolib/` を配置先としており、整合する。）

### 7.3 テクスチャの扱い

`[確認]` テクスチャは GeckoLib の `GeckoLibResources` ローダの対象**ではない**
（`MODELS_PATH` / `ANIMATIONS_PATH` の 2 系統のみを走査）。
`[導出]` ので通常の `assets/<ns>/<path>` としてバニラのテクスチャ機構で読まれる。

`[確認]` アニメーションテクスチャ（`.mcmeta` 相当）用クラス
`com.geckolib.renderer.texture.GeckoLibAnimatedTexture` が実在
（`AnimationInfo`, `FrameInfo`, `InterpolationData` 内部型）。
mixin `client.TextureManagerMixin` が絡む。

`[確認]` グロー用レイヤー `AutoGlowingGeoLayer` は内部に
`EmissiveRenderType` / `EmissiveRenderType$Entry` を持つ。

---

## 8. 登録方法

### 8.1 確認できたこと

`[確認]` GeckoLib 側の登録は `com.geckolib.GeckoLib` のコンストラクタで完結（§2.2）。
`[確認]` `GeckoLib.DATA_COMPONENTS_REGISTER` は
`DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "geckolib")`
で作られ、`geckolib:animatable_id` 相当の `DataComponentType<Long>` を登録している
（`GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT` が
`Supplier<DataComponentType<Long>>`）。

`[確認]` `com.geckolib.GeckoLibConstants`:
```java
public static final org.apache.logging.log4j.Logger LOGGER;
public static final java.lang.String MODID;
public static final java.util.function.Supplier<net.minecraft.core.component.DataComponentType<java.lang.Long>>
    STACK_ANIMATABLE_ID_COMPONENT;
public static void init();
public static net.minecraft.resources.Identifier id(java.lang.String);
public static java.lang.RuntimeException exception(net.minecraft.resources.Identifier, java.lang.String);
public static java.lang.RuntimeException exception(Identifier, String, java.lang.Throwable);
```

### 8.2 利用者が `DeferredRegister` に入れるもの `[導出]`

`[導出]` GeckoLib のクラスはエンティティ・アイテム・ブロックエンティティの**型を登録しない**。
GeckoLib は「振る舞い（`GeoAnimatable` 実装）」と「描画（renderer/model）」を提供するだけ。
したがって:

- **エンティティ本体**（`EntityType`）は自前の `DeferredRegister<EntityType<?>>` に入れる。
  Entity クラスに `implements GeoEntity` を付ける。
- **アイテム**（`Item`）は自前の `DeferredRegister<Item>`。`implements GeoItem`。
  アニメ ID は `GeoItem.getOrAssignId(ItemStack, ServerLevel)` で割り当てる。
- **ブロックエンティティ**は自前の `DeferredRegister<BlockEntityType<?>>`。
  `implements GeoBlockEntity`。
- **レンダラ**はクライアント側イベントで登録する。→ **26.3 実ソースで確認済み**:

`[確認]` `neoforge-26.3.0.99-sources.jar` の
`net/neoforged/neoforge/client/event/EntityRenderersEvent.java`:
```java
public abstract class EntityRenderersEvent extends Event implements IModBusEvent {
    public static class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(
            EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider);
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
            BlockEntityType<? extends T> blockEntityType,
            BlockEntityRendererProvider<T, S> blockEntityRendererProvider);
    }
    public static class RegisterLayerDefinitions extends EntityRenderersEvent {
        public void registerLayerDefinition(ModelLayerLocation, Supplier<LayerDefinition>);
    }
    public static class AddLayers extends EntityRenderersEvent { ... }
}
```
`[確認]` **26.3 ではエンティティ用もブロックエンティティ用も
`EntityRenderersEvent.RegisterRenderers` の 1 か所**に集約されている
（`BlockEntityRenderersEvent` という別クラスは NeoForge 26.3.0.99 には存在しない）。

`[導出]` 使い方（GeckoLib のレンダラを渡すだけ）:
```java
@SubscribeEvent
static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(NocterraEntities.FOREST_KING.get(),
        ctx -> new GeoEntityRenderer<>(ctx, new ForestKingModel()));
    event.registerBlockEntityRenderer(NocterraBlockEntities.ALTAR.get(),
        ctx -> new GeoBlockRenderer<>(ctx, new AltarModel()));
}
```

`[確認]` アイテム用の特殊モデルレンダラ登録イベントも実在:
`net/neoforged/neoforge/client/event/RegisterSpecialModelRendererEvent.java`
```java
public class RegisterSpecialModelRendererEvent extends Event implements IModBusEvent {
    public void register(Identifier location,
        MapCodec<? extends SpecialModelRenderer.Unbaked<?>> source);
}
```
`[未確認]` ただしこれは `SpecialModelRenderer.Unbaked` の **codec** を登録するイベントであり、
GeckoLib のアイテム/防具レンダラのために利用者が追加登録する必要があるかは未確定
（GeckoLib 側は mixin + `GeoRenderProvider` で配線している）。

---

## 9. jar 内の補助設定ファイル（実物）

`[確認]` `geckolib.mixins.json` の全文:
```json
{
  "required": true,
  "package": "com.geckolib.mixin",
  "compatibilityLevel": "JAVA_17",
  "minVersion": "0.8",
  "injectors": { "defaultRequire": 1 },
  "mixins": [
    "common.AbstractContainerMenuMixin", "common.HashedStackMixin", "common.ItemStackMixin",
    "common.LivingEntityMixin", "common.SlotMixin", "common.SynchronizedRemoteSlotMixin"
  ],
  "client": [
    "client.BlockEntityRenderStateMixin", "client.EntityRendererMixin",
    "client.EntityRenderStateMixin", "client.HumanoidArmorLayerMixin", "client.HumanoidModelMixin",
    "client.ModelFeatureRendererMixin", "client.ModelMixin", "client.PlayerModelMixin",
    "client.SpecialModelRenderersMixin", "client.SpecialModelWrapperMixin",
    "client.TextureManagerMixin"
  ]
}
```
※ `compatibilityLevel` は `JAVA_17` のまま（class file 自体は major 69 = Java 25）。

`[確認]` `META-INF/accesstransformer.cfg` から、26.3 の実クラス名が読み取れる:
```
net.minecraft.client.renderer.LevelRenderer            (levelRenderState, renderBuffers)
net.minecraft.client.renderer.RenderPipelines           (register, MATRICES_FOG_LIGHT_DIR_SNIPPET)
net.minecraft.client.renderer.rendertype.RenderType     (create)
com.mojang.blaze3d.vertex.PoseStack$Pose                (set)
net.minecraft.client.renderer.entity.EntityRenderDispatcher (renderers)
net.minecraft.client.renderer.entity.EntityRenderer     (createRenderState)
net.minecraft.client.renderer.entity.LivingEntityRenderer (solveBodyRot, layers, ...)
net.minecraft.client.renderer.entity.HumanoidMobRenderer (getEquipmentIfRenderable)
net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher (renderers)
net.minecraft.client.model.Model                        (renderToBuffer)
net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer (shouldRender, getArmorModel, usesInnerModel)
net.minecraft.client.renderer.entity.layers.CustomHeadLayer    (ITEM_SCALE, SKULL_SCALE)
net.minecraft.core.component.PatchedDataComponentMap    (copyOnWrite)
```
`[確認]` 26.3 では `net.minecraft.client.renderer.rendertype.RenderType` という
パッケージに `RenderType` が居る（旧 `net.minecraft.client.renderer.RenderType` ではない）。

`[確認]` 依存ライブラリとして `com.mojang.renderpearl.api.*`
（`pipeline.RenderPipeline`, `textures.GpuTexture`, `textures.FilterMode`, `textures.AddressMode`）
を参照している（`GeckoLibAnimatedTexture` / `RenderUtil`）。

---

## 10. 4.x → 5.x で「実物から確認できた」差分まとめ

以下は 5.5.7 側の事実として `[確認]`。4.x 側の値は本検証では未確認なので併記しない。

| 項目 | 5.5.7 の実物 |
|---|---|
| ルートパッケージ | `com.geckolib` |
| `GeoRenderer` の場所 | `com.geckolib.renderer.base.GeoRenderer`（`renderer` 直下ではない） |
| 描画方式 | render state + submit node（`SubmitNodeCollector`, `CameraRenderState`） |
| `GeoModel.getModelResource` 引数 | `GeoRenderState`（`T` ではない） |
| `GeoModel.getTextureResource` 引数 | `GeoRenderState`（`T` ではない） |
| `GeoModel.getAnimationResource` 引数 | `T`（従来どおり） |
| `PlayState` 定数 | `CONTINUE` / `PAUSE` / `STOP` の 3 つのみ |
| アニメ開始 | `AnimationController.setAnimation(RawAnimation)` |
| キーフレームイベント型名 | `KeyFrameEvent`（`KeyframeEvent` ではない） |
| `AnimationTest` | Record。`isMoving()` は引数なし |
| リソース配置 | `assets/<ns>/geckolib/models/...` と `assets/<ns>/geckolib/animations/...` `[導出]` |
| 旧パス互換 | あり。ただし ERROR ログ（"Superfluous prefix or suffix …"） |
| 初期化 | `GeckoLib.initialize()` は**存在しない**。NeoForge が mod として読む |
| `LoopType` / `EasingType` | enum ではなく interface + 定数。`register` で拡張可 |
| 新規: stateless 系 | `com.geckolib.animatable.stateless.*`（`StatelessGeoEntity` ほか） |
| 新規: 組込レイヤー | `ItemInHandGeoLayer`, `CustomBoneTextureGeoLayer`, `TextureLayerGeoLayer` |
| 新規: 専門レンダラ | `DirectionalProjectileRenderer`, `DyeableGeoArmorRenderer` |
| 新規: render provider | `GeoRenderProvider` を `AnimatableInstanceCache` が保持 |

`[未確認]` 4.x の API 名（`GeoEntityRenderer` の `render`、`GeoModel.getModelResource(T)`、
`software.bernie.geckolib.*` 等）は**この jar には含まれない**ため未確認。
「4.x ではこうだった」という断定はしない。

---

## 11. 未確認事項（推測で埋めていないもの）

1. ~~バニラ `ResourceManager.listResources` の走査仕様~~ → **解決済み**（§7.2 で `[確認]`）。
   26.3 実ソース（`PathPackResources`, `PackType`, `Identifier`）で裏取りした。
2. `[未確認]` `GeckoLibUtil.createInstanceCache(GeoAnimatable, boolean)` の第 2 引数の意味。
3. `[未確認]` `DefaultAnimations.*` 各定数が指すアニメ名の文字列。
4. ~~NeoForge 26.3.0.99 のレンダラ登録イベント名・シグネチャ~~ → **解決済み**（§8.2 で `[確認]`）。
   `EntityRenderersEvent.RegisterRenderers` の 1 か所。
5. `[未確認]` アイテム/防具で利用者が `SpecialModelRenderer` を追加登録する必要があるか
   （`RegisterSpecialModelRendererEvent` は実在するが、GeckoLib が内部で配線している可能性が高い）。
6. ~~`Identifier.withPrefix(String)` の意味~~ → **解決済み** `[確認]`:
   `withPath(prefix + this.path)`（26.3 `Identifier.java`）。path の先頭に付くだけ。

---

## 付録: 検証に使ったコマンド

```powershell
$JDK = "C:\Program Files\Java\jdk-25.0.5.0.2"
$jar = "C:\Users\2674469\Desktop\mcmod\.agent\refs\geckolib-neoforge-26.3-5.5.7.jar"

# 一覧
& "$JDK\bin\jar.exe" tf $jar

# シグネチャ
& "$JDK\bin\javap.exe" -classpath $jar com.geckolib.animatable.GeoAnimatable
& "$JDK\bin\javap.exe" -classpath $jar com.geckolib.model.GeoModel
& "$JDK\bin\javap.exe" -classpath $jar com.geckolib.renderer.GeoEntityRenderer

# 文字列定数 / concat レシピ
& "$JDK\bin\javap.exe" -p -c    -classpath $jar com.geckolib.cache.BakedModelCache
& "$JDK\bin\javap.exe" -p -v    -classpath $jar com.geckolib.model.DefaultedGeoModel
& "$JDK\bin\javap.exe" -p -c    -classpath $jar com.geckolib.cache.GeckoLibResources
```

中間生成物: `.agent/refs/`（jar、展開物、javap ダンプ）。`jar`/`javap` の生ログは
`.agent/refs/javap-*.txt` に保存。

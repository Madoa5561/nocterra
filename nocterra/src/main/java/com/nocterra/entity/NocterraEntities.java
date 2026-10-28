package com.nocterra.entity;

import com.nocterra.Nocterra;
import com.nocterra.boss.AbyssalSeaEmperorEntity;
import com.nocterra.boss.AstelFallenStarDragonEntity;
import com.nocterra.boss.BlackCrownedKingEntity;
import com.nocterra.boss.CondemnedKnightLordEntity;
import com.nocterra.boss.EmberIncarnationEntity;
import com.nocterra.boss.ForgottenKnightEntity;
import com.nocterra.boss.MoltenSteelColossusEntity;
import com.nocterra.boss.NamelessObserverEntity;
import com.nocterra.boss.RotwoodKingEntity;
import com.nocterra.boss.SandglassTwinKingsEntity;
import com.nocterra.boss.SilverExecutionerEntity;
import com.nocterra.boss.SorrowSaintEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NOCTERRA のエンティティ型登録。所有: entity/boss 担当。
 *
 * <p>ここで以下もまとめて配線する:
 * <ul>
 *   <li>{@link EntityAttributeCreationEvent} — モブ属性（26.3 の正規ルート）</li>
 *   <li>{@link RegisterSpawnPlacementsEvent} — 自然スポーン判定（26.3 のイベント名はこれ）</li>
 *   <li>{@link NocterraEntityItems} — スポーンエッグ（検証・クリエイティブタブ用）</li>
 * </ul>
 *
 * <p><b>未配線:</b> バイオームの {@code minecraft:gameplay/natural_mob_spawns} への追記は
 * {@code data/nocterra/worldgen/biome/*.json}（Lead 所有）側の作業。
 * ashen_forest に {@code ashen_wraith} / {@code ash_crawler}、
 * mourning_marsh に {@code mourning_wisp} / {@code bog_ghoul} を追加する。</p>
 */
public final class NocterraEntities {
    private NocterraEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Nocterra.MOD_ID);

    /** 灰樹の森の亡霊。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AshenWraithEntity>> ASHEN_WRAITH =
            ENTITIES.registerEntityType("ashen_wraith", AshenWraithEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.7F, 2.1F)
                            .clientTrackingRange(10)
                            .updateInterval(2));

    /** 灰樹の森の這い虫。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AshCrawlerEntity>> ASH_CRAWLER =
            ENTITIES.registerEntityType("ash_crawler", AshCrawlerEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.0F, 0.8F)
                            .clientTrackingRange(8)
                            .updateInterval(3));

    /** 嘆きの湿原の亡霊。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MourningWispEntity>> MOURNING_WISP =
            ENTITIES.registerEntityType("mourning_wisp", MourningWispEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.8F, 1.6F)
                            .clientTrackingRange(10)
                            .updateInterval(3));

    /** 嘆きの湿原の屍鬼。 */
    public static final DeferredHolder<EntityType<?>, EntityType<BogGhoulEntity>> BOG_GHOUL =
            ENTITIES.registerEntityType("bog_ghoul", BogGhoulEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(0.95F, 2.3F)
                            .clientTrackingRange(10)
                            .updateInterval(3));

    /** ボス「腐朽の森王」。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RotwoodKingEntity>> ROTWOOD_KING =
            ENTITIES.registerEntityType("rotwood_king", RotwoodKingEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.2F, 3.2F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune());

    /** ボス「悲嘆の聖女」（嘆きの湿原）。浮遊型。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SorrowSaintEntity>> SORROW_SAINT =
            ENTITIES.registerEntityType("sorrow_saint", SorrowSaintEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.1F, 3.3F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「断罪の騎士長」（亡国の城塞）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<CondemnedKnightLordEntity>> CONDEMNED_KNIGHT_LORD =
            ENTITIES.registerEntityType("condemned_knight_lord", CondemnedKnightLordEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.3F, 3.3F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「深淵の海帝」（深淵の海）。水陸両用。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalSeaEmperorEntity>> ABYSSAL_SEA_EMPEROR =
            ENTITIES.registerEntityType("abyssal_sea_emperor", AbyssalSeaEmperorEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.6F, 3.4F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「砂時計の双王」（紅砂の荒野）。2 体 1 組。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SandglassTwinKingsEntity>> SANDGLASS_TWIN_KINGS =
            ENTITIES.registerEntityType("sandglass_twin_kings", SandglassTwinKingsEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.2F, 3.0F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「墜星竜アステル」（星骸の浮島）。飛行型。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AstelFallenStarDragonEntity>> ASTEL_FALLEN_STAR_DRAGON =
            ENTITIES.registerEntityType("astel_fallen_star_dragon", AstelFallenStarDragonEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(2.4F, 3.3F)
                            .clientTrackingRange(14)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「白銀の処刑者」（永劫の氷原）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SilverExecutionerEntity>> SILVER_EXECUTIONER =
            ENTITIES.registerEntityType("silver_executioner", SilverExecutionerEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.2F, 3.1F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ボス「灼鋼の巨神」（黒鉄の火山）。炎無効・KB耐性1.0。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MoltenSteelColossusEntity>> MOLTEN_STEEL_COLOSSUS =
            ENTITIES.registerEntityType("molten_steel_colossus", MoltenSteelColossusEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.8F, 3.0F)
                            .clientTrackingRange(14)
                            .updateInterval(2)
                            .fireImmune()
                            .notInPeaceful());

    /** ストーリーボス「黒冠の王」。3 フェーズ。 */
    public static final DeferredHolder<EntityType<?>, EntityType<BlackCrownedKingEntity>> BLACK_CROWNED_KING =
            ENTITIES.registerEntityType("black_crowned_king", BlackCrownedKingEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.2F, 3.1F)
                            .clientTrackingRange(14)
                            .updateInterval(2)
                            .notInPeaceful());

    /** ストーリーボス「灯火の化身」。3 フェーズ・炎無効。 */
    public static final DeferredHolder<EntityType<?>, EntityType<EmberIncarnationEntity>> EMBER_INCARNATION =
            ENTITIES.registerEntityType("ember_incarnation", EmberIncarnationEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.1F, 3.1F)
                            .clientTrackingRange(14)
                            .updateInterval(2)
                            .fireImmune()
                            .notInPeaceful());

    /** 隠しボス「忘却された騎士」。高速・パリィ依存。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ForgottenKnightEntity>> FORGOTTEN_KNIGHT =
            ENTITIES.registerEntityType("forgotten_knight", ForgottenKnightEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.0F, 3.0F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    /** 隠しボス「名もなき観測者」。テレポート・幻影。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NamelessObserverEntity>> NAMELESS_OBSERVER =
            ENTITIES.registerEntityType("nameless_observer", NamelessObserverEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.0F, 2.6F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .notInPeaceful());

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NocterraEntityItems.register(modBus);
        modBus.addListener(NocterraEntities::onEntityAttributes);
        modBus.addListener(NocterraEntities::onRegisterSpawnPlacements);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ASHEN_WRAITH.get(), AshenWraithEntity.createAttributes().build());
        event.put(ASH_CRAWLER.get(), AshCrawlerEntity.createAttributes().build());
        event.put(MOURNING_WISP.get(), MourningWispEntity.createAttributes().build());
        event.put(BOG_GHOUL.get(), BogGhoulEntity.createAttributes().build());
        event.put(ROTWOOD_KING.get(), RotwoodKingEntity.createAttributes().build());
        event.put(SORROW_SAINT.get(), SorrowSaintEntity.createAttributes().build());
        event.put(CONDEMNED_KNIGHT_LORD.get(), CondemnedKnightLordEntity.createAttributes().build());
        event.put(ABYSSAL_SEA_EMPEROR.get(), AbyssalSeaEmperorEntity.createAttributes().build());
        event.put(SANDGLASS_TWIN_KINGS.get(), SandglassTwinKingsEntity.createAttributes().build());
        event.put(ASTEL_FALLEN_STAR_DRAGON.get(), AstelFallenStarDragonEntity.createAttributes().build());
        event.put(SILVER_EXECUTIONER.get(), SilverExecutionerEntity.createAttributes().build());
        event.put(MOLTEN_STEEL_COLOSSUS.get(), MoltenSteelColossusEntity.createAttributes().build());
        event.put(BLACK_CROWNED_KING.get(), BlackCrownedKingEntity.createAttributes().build());
        event.put(EMBER_INCARNATION.get(), EmberIncarnationEntity.createAttributes().build());
        event.put(FORGOTTEN_KNIGHT.get(), ForgottenKnightEntity.createAttributes().build());
        event.put(NAMELESS_OBSERVER.get(), NamelessObserverEntity.createAttributes().build());
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerGroundMonster(event, ASHEN_WRAITH.get());
        registerGroundMonster(event, ASH_CRAWLER.get());
        registerGroundMonster(event, MOURNING_WISP.get());
        registerGroundMonster(event, BOG_GHOUL.get());
    }

    private static <T extends Monster> void registerGroundMonster(
            RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (spawnType, level, spawnReason, pos, random) ->
                        Monster.checkMonsterSpawnRules(spawnType, level, spawnReason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

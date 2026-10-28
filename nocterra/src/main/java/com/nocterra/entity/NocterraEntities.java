package com.nocterra.entity;

import com.nocterra.Nocterra;
import com.nocterra.boss.RotwoodKingEntity;
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
                            .sized(1.0F, 2.8F)
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

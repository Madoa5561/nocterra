package com.nocterra.entity;

import com.nocterra.Nocterra;
import com.nocterra.boss.RotwoodKingEntity;
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
 *   <li>{@link NocterraEntityItems} — スポーンエッグ（検証用）</li>
 * </ul>
 *
 * <p><b>未配線:</b> バイオームの {@code minecraft:gameplay/natural_mob_spawns} への
 * {@code nocterra:ashen_wraith} 追加は {@code data/nocterra/worldgen/biome/ashen_forest.json}
 * （Lead 所有）側の作業。詳細は Lead への報告に記載。</p>
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

    /** ボス「腐朽の森王」。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RotwoodKingEntity>> ROTWOOD_KING =
            ENTITIES.registerEntityType("rotwood_king", RotwoodKingEntity::new, MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.2F, 3.2F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune());

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NocterraEntityItems.register(modBus);
        modBus.addListener(NocterraEntities::onEntityAttributes);
        modBus.addListener(NocterraEntities::onRegisterSpawnPlacements);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ASHEN_WRAITH.get(), AshenWraithEntity.createAttributes().build());
        event.put(ROTWOOD_KING.get(), RotwoodKingEntity.createAttributes().build());
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ASHEN_WRAITH.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnReason, pos, random) ->
                        Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

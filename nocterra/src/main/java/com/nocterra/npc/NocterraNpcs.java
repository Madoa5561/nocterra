package com.nocterra.npc;

import com.nocterra.Nocterra;
import com.nocterra.npc.network.NpcPayloads;
import com.nocterra.quest.NocterraQuests;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NPC エンティティの登録とストーリーサブシステムの起動。所有: story 担当。
 *
 * <p><b>Lead への依頼（1 行）:</b> {@code com.nocterra.Nocterra} のコンストラクタに</p>
 * <pre>{@code com.nocterra.npc.NocterraNpcs.register(modBus);}</pre>
 * <p>を追加する。ここから NPC エンティティ・属性・payload・クエスト/会話ローダ・
 * 検証コマンドがまとめて登録される。</p>
 *
 * <p>召喚: {@code /summon nocterra:warden_of_ash}（他は {@code /nocterra npc list}）。
 * 自然スポーンはしない（{@link MobCategory#MISC}）。</p>
 */
public final class NocterraNpcs {
    private NocterraNpcs() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Nocterra.MOD_ID);

    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<NocterraNpc>>> ALL =
            new LinkedHashMap<>();

    /** 灰樹の森の案内人。最初のクエストを付与する。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> WARDEN_OF_ASH =
            register("warden_of_ash", builder -> builder
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    /** 嘆きの湿原の未亡人。悲嘆の聖女の母。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> MARSH_WIDOW =
            register("marsh_widow", builder -> builder
                    .sized(0.6F, 1.85F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    /** 古代神殿の盲目の書記。刻印と黒冠の伝承。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> BLIND_SCRIBE =
            register("blind_scribe", builder -> builder
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    /** 黒冠城への道を知る門番。刻印 5 個で城門を開く。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> GATE_KEEPER =
            register("gate_keeper", builder -> builder
                    .sized(0.7F, 2.05F)
                    .clientTrackingRange(12)
                    .updateInterval(3));

    /** 灰の商人。右クリックで交易。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> ASHEN_MERCHANT =
            register("ashen_merchant", builder -> builder
                    .sized(0.65F, 1.85F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    /** 隠し要素: 迷い子。連れ帰ると報酬。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> LOST_CHILD =
            register("lost_child", builder -> builder
                    .sized(0.45F, 1.2F)
                    .clientTrackingRange(10)
                    .updateInterval(3));

    /** 登録済み NPC ID の一覧。 */
    public static List<String> ids() {
        return List.copyOf(ALL.keySet());
    }

    public static boolean isKnown(String npcId) {
        return ALL.containsKey(npcId);
    }

    /** デバッグ用の召喚。 */
    public static NocterraNpc spawn(ServerLevel level, String npcId, BlockPos pos) {
        DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> holder = ALL.get(npcId);
        if (holder == null) {
            return null;
        }
        return holder.get().spawn(level, pos, EntitySpawnReason.COMMAND);
    }

    private static DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> register(
            String name, UnaryOperator<EntityType.Builder<NocterraNpc>> properties) {
        DeferredHolder<EntityType<?>, EntityType<NocterraNpc>> holder =
                ENTITIES.registerEntityType(name, NocterraNpc::new, MobCategory.MISC, properties);
        ALL.put(name, holder);
        return holder;
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(NocterraNpcs::onEntityAttributes);
        modBus.addListener(NpcPayloads::registerPayloads);
        NocterraQuests.register(modBus);
        Nocterra.LOG.info("[NOCTERRA] story bootstrap: {} npc types registered", ALL.size());
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(WARDEN_OF_ASH.get(), NocterraNpc.createAttributes().build());
        event.put(MARSH_WIDOW.get(), NocterraNpc.createAttributes().build());
        event.put(BLIND_SCRIBE.get(), NocterraNpc.createAttributes().build());
        event.put(GATE_KEEPER.get(), NocterraNpc.createAttributes().build());
        event.put(ASHEN_MERCHANT.get(), NocterraNpc.createAttributes().build());
        event.put(LOST_CHILD.get(), NocterraNpc.createAttributes().build());
    }
}

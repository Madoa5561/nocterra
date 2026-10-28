package com.nocterra.world.feature;

import com.nocterra.Nocterra;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * カスタム {@code Feature} の「型」を {@link Registries#FEATURE_TYPE} に登録する。
 *
 * <p>26.3 では {@code net.minecraft.world.level.levelgen.feature.Feature} が
 * インターフェースになり、{@code ConfiguredFeature} は廃止された。datapack の
 * {@code data/<ns>/worldgen/feature/<name>.json} は {@code Feature} そのものを
 * 表し、JSON の {@code "type"} がこの {@code FEATURE_TYPE} を引く。
 * したがってここでは「JSON の type 名 → MapCodec」の対応だけをコード登録する。</p>
 *
 * <p>{@link EventBusSubscriber} はイベント型が {@code IModBusEvent} かどうかで
 * mod バス / ゲームバスを自動判別する（FML loader 12.0.12 の
 * {@code AutomaticEventSubscriber} 実装で確認済み）。{@link RegisterEvent} は
 * {@code IModBusEvent} なので mod バスに登録される。この方式なら
 * {@code Nocterra.java}（Lead 所有）を編集せずに自己登録できる。</p>
 */
@EventBusSubscriber(modid = Nocterra.MOD_ID)
public final class NocterraFeatureTypes {
    private NocterraFeatureTypes() {}

    public static final Identifier ANCIENT_TEMPLE = id("ancient_temple");
    public static final Identifier ASHWOOD_DEAD_TREE = id("ashwood_dead_tree");
    public static final Identifier MOURNING_REED = id("mourning_reed");

    /** 二重登録防止。{@link #onRegister} はレジストリごとに何度も呼ばれるので、FEATURE_TYPE のときだけ 1 回実行する。 */
    private static final AtomicBoolean TYPES_REGISTERED = new AtomicBoolean(false);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, path);
    }

    /**
     * 明示登録用のフォールバック。{@code @EventBusSubscriber} の自動購読が効かない環境では
     * {@code Nocterra.java} からこのメソッドを呼べばよい。{@link #onRegister} は冪等なので
     * 自動購読と併用しても二重登録にはならない。
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(NocterraFeatureTypes::onRegister);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistry(Registries.FEATURE_TYPE) == null) {
            return; // FEATURE_TYPE 以外のレジストリのイベント
        }
        if (!TYPES_REGISTERED.compareAndSet(false, true)) {
            return; // すでに登録済み（自動購読と明示登録の二重呼び出し対策）
        }
        event.register(Registries.FEATURE_TYPE, ANCIENT_TEMPLE, () -> AncientTempleFeature.CODEC);
        event.register(Registries.FEATURE_TYPE, ASHWOOD_DEAD_TREE, () -> AshwoodDeadTreeFeature.CODEC);
        event.register(Registries.FEATURE_TYPE, MOURNING_REED, () -> MourningReedFeature.CODEC);
    }
}

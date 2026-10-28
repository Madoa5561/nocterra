package com.nocterra.combat;

import com.nocterra.Nocterra;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * プレイヤー戦闘データの {@link AttachmentType} 登録。
 *
 * <p>26.3 の AttachmentType レジストリは {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES}。
 * バニラ {@code Registries} には存在しないので注意（{@code Registries.ATTACHMENT_TYPE} は無い）。
 * 登録は {@link #register(IEventBus)} を {@link NocterraCombat#register(IEventBus)} から呼ぶ。</p>
 */
public final class CombatAttachments {
    private CombatAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nocterra.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<?>> COMBAT_DATA =
            ATTACHMENT_TYPES.register("combat_data", CombatAttachments::createCombatData);

    private static AttachmentType<?> createCombatData() {
        return AttachmentType.builder(CombatData::new)
                .serialize(CombatData.CODEC)
                .copyOnDeath()
                .build();
    }

    /**
     * 型付きの {@link AttachmentType}。登録は遅延されるため保持は {@link DeferredHolder} で行い、
     * 取得時にだけ unchecked cast する（型引数はこのクラス内で閉じている）。
     */
    @SuppressWarnings("unchecked")
    public static AttachmentType<CombatData> combatData() {
        return (AttachmentType<CombatData>) COMBAT_DATA.get();
    }

    /** プレイヤーの戦闘データ（未作成なら既定値が作成される）。 */
    public static CombatData get(Player player) {
        return player.getData(combatData());
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}

package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * プレイヤーの進行データ {@link ProgressionData} の AttachmentType 登録。所有: Lead。
 *
 * <p>26.3 の AttachmentType レジストリは {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES}。</p>
 */
public final class ProgressionAttachments {
    private ProgressionAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nocterra.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<?>> PROGRESSION =
            ATTACHMENT_TYPES.register("progression", ProgressionAttachments::createProgression);

    private static AttachmentType<?> createProgression() {
        return AttachmentType.builder(ProgressionData::new)
                .serialize(ProgressionData.MAP_CODEC)
                .copyOnDeath()
                .build();
    }

    @SuppressWarnings("unchecked")
    public static AttachmentType<ProgressionData> progressionType() {
        return (AttachmentType<ProgressionData>) PROGRESSION.get();
    }

    /** プレイヤーの進行データ（未作成なら既定値が作成される）。 */
    public static ProgressionData get(Player player) {
        return player.getData(progressionType());
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}

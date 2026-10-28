package com.nocterra.quest;

import com.nocterra.Nocterra;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * プレイヤーの物語データ {@link StoryData} の AttachmentType 登録。所有: story 担当。
 *
 * <p>登録は {@link NocterraQuests#register(IEventBus)} から行う（Lead が
 * {@code Nocterra} のコンストラクタで {@code com.nocterra.npc.NocterraNpcs.register(modBus)}
 * を 1 行呼べば、そこから連鎖して登録される）。</p>
 *
 * <p>26.3 の AttachmentType レジストリは {@code NeoForgeRegistries.Keys.ATTACHMENT_TYPES}。
 * Lead の {@code ProgressionAttachments} と同じパターン。</p>
 */
public final class StoryAttachments {
    private StoryAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Nocterra.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<?>> STORY =
            ATTACHMENT_TYPES.register("story", StoryAttachments::createStory);

    private static AttachmentType<?> createStory() {
        return AttachmentType.builder(StoryData::new)
                .serialize(StoryData.MAP_CODEC)
                .copyOnDeath()
                .build();
    }

    @SuppressWarnings("unchecked")
    public static AttachmentType<StoryData> storyType() {
        return (AttachmentType<StoryData>) STORY.get();
    }

    /** プレイヤーの物語データ（未作成なら既定値が作成される）。 */
    public static StoryData get(Player player) {
        return player.getData(storyType());
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}

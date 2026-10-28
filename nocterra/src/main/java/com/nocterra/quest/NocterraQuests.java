package com.nocterra.quest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * ストーリー（NPC / 会話 / クエスト）サブシステムの登録入口。所有: story 担当。
 *
 * <p>Lead は {@code Nocterra} のコンストラクタに
 * {@code com.nocterra.npc.NocterraNpcs.register(modBus);} を 1 行足すだけでよい
 * （{@code NocterraNpcs} が本クラスを呼ぶ）。</p>
 *
 * <p>ここで登録するもの:</p>
 * <ul>
 *   <li>{@link StoryAttachments} — プレイヤーの物語データ（AttachmentType）</li>
 *   <li>{@link StoryEvents} — データパックリロード / 死亡 / tick / ログアウト</li>
 *   <li>{@link StoryCommands} — {@code /nocterra quest ...} / {@code /nocterra npc ...}</li>
 * </ul>
 */
public final class NocterraQuests {
    private NocterraQuests() {}

    public static void register(IEventBus modBus) {
        StoryAttachments.register(modBus);

        NeoForge.EVENT_BUS.addListener(StoryEvents::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(StoryEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(StoryEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(StoryEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                StoryCommands.register(event.getDispatcher()));
    }
}

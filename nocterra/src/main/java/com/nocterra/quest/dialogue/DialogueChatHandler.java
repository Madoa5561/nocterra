package com.nocterra.quest.dialogue;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Locale;

/**
 * 会話中のチャット入力を選択肢の回答として扱う。所有: Lead。
 *
 * <p>GUI のキー入力が環境によって届かない場合でも回答できるよう、
 * 会話中にチャットで数字（1〜9）を送るとその選択肢を選び、0 で会話を閉じる。
 * 数字以外はそのまま通常のチャットとして扱う。</p>
 */
public final class DialogueChatHandler {
    private DialogueChatHandler() {}

    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        DialogueSession session = DialogueService.currentSession(player);
        if (session == null) {
            return;
        }
        String text = event.getRawText().trim();
        if (text.isEmpty() || text.length() > 2) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if ("close".equals(normalized) || "bye".equals(normalized)) {
            event.setCanceled(true);
            DialogueService.close(player);
            return;
        }
        int value;
        try {
            value = Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return;
        }
        if (value < 0 || value > 9) {
            return;
        }
        event.setCanceled(true);
        if (value == 0) {
            DialogueService.close(player);
            return;
        }
        int index = value - 1;
        if (index >= session.choices().size()) {
            player.sendSystemMessage(Component.translatable("dialogue.nocterra.hint"));
            return;
        }
        DialogueService.select(player, session.npcEntityId, index);
        Nocterra.LOG.debug("[NOCTERRA] dialogue choice {} via chat (npc={})", index, session.npcId);
    }
}

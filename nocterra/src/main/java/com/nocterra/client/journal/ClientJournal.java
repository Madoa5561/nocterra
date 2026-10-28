package com.nocterra.client.journal;

import com.nocterra.networking.journal.JournalDataPayload;
import net.minecraft.client.Minecraft;

/**
 * サーバーから届いた探索手帳データで GUI を開く（クライアント専用）。所有: Lead。
 */
public final class ClientJournal {
    private ClientJournal() {}

    public static void open(JournalDataPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new JournalScreen(payload));
        com.nocterra.Nocterra.LOG.info(
                "[NOCTERRA] journal screen opened: sigils={} bosses={} npcs={} quests={}",
                Integer.bitCount(payload.sigilMask()), payload.bossCount(), payload.npcCount(),
                payload.questCount());
    }
}

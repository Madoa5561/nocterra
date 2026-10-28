package com.nocterra.quest;

import net.minecraft.server.level.ServerPlayer;

/**
 * 会話・クエスト条件の評価コンテキスト。所有: story 担当。
 *
 * <p>{@code npcId} は会話中の NPC（クエスト受注条件では {@code null}）。
 * {@code npcWasDiscovered} は「この会話を開く前から発見済みだったか」で、
 * {@code first_meet} 条件がこのスナップショットを見る。</p>
 */
public record StoryContext(ServerPlayer player, StoryData story, String npcId, boolean npcWasDiscovered) {

    public static StoryContext of(ServerPlayer player) {
        return new StoryContext(player, StoryAttachments.get(player), null, true);
    }

    public StoryContext withNpc(String npcId, boolean wasDiscovered) {
        return new StoryContext(this.player, this.story, npcId, wasDiscovered);
    }
}

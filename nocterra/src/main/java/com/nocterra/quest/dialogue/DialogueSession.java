package com.nocterra.quest.dialogue;

import java.util.List;
import java.util.UUID;

/**
 * 会話中のサーバー側セッション（プレイヤーごとに 1 つ）。所有: story 担当。
 *
 * <p>「どの NPC と」「どのノードにいて」「直前にどの選択肢を送ったか」を保持する。
 * クライアントからは選択 index しか受け取らないため、この index の範囲チェックと
 * 条件の再評価をここで行う。</p>
 */
public final class DialogueSession {
    public final UUID playerId;
    public final int npcEntityId;
    public final String npcId;
    /** 会話を開いた時点で既に発見済みだったか（{@code first_meet} 条件用のスナップショット）。 */
    public final boolean wasDiscovered;
    /**
     * 会話を開いた時点で既にエンディングを見ていたか。
     * 会話中にエンディングが新たに発火したらセッションを閉じるために使う。
     */
    public final boolean endingSeenAtOpen;

    String nodeId;
    List<DialogueDefinition.ChoiceSpec> choices = List.of();
    boolean closed;

    public DialogueSession(UUID playerId, int npcEntityId, String npcId,
                           boolean wasDiscovered, boolean endingSeenAtOpen) {
        this.playerId = playerId;
        this.npcEntityId = npcEntityId;
        this.npcId = npcId;
        this.wasDiscovered = wasDiscovered;
        this.endingSeenAtOpen = endingSeenAtOpen;
    }

    public String nodeId() {
        return this.nodeId;
    }

    public List<DialogueDefinition.ChoiceSpec> choices() {
        return this.choices;
    }

    public boolean isClosed() {
        return this.closed;
    }
}

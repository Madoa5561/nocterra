package com.nocterra.client.dialogue;

import com.nocterra.npc.network.DialogueOpenPayload;
import net.minecraft.client.Minecraft;

/**
 * 会話 UI のクライアント側エントリ。所有: story 担当。
 *
 * <p>{@link com.nocterra.npc.network.DialogueOpenPayload#handle} から呼ばれる
 * （playToClient ハンドラはクライアントでのみ実行される）。</p>
 *
 * <p>26.3 の画面は {@code Minecraft#setScreen} が無くなり、
 * {@code Minecraft.gui.setScreen(Screen)} で開き、閉じるのは
 * {@code Screen#onClose()}（内部で {@code Gui#popScreenLayer()}）という構成。
 * ここではバニラの GUI と同じ経路だけを使う。</p>
 */
public final class DialogueClient {
    private DialogueClient() {}

    private static DialogueScreen active;

    /** サーバーから届いたノードを表示する（既に開いていれば内容だけ更新）。 */
    public static void open(DialogueOpenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active != null && active.npcEntityId() == payload.npcEntityId()
                && minecraft.gui.screen() == active) {
            active.update(payload);
            return;
        }
        active = new DialogueScreen(payload);
        minecraft.gui.setScreen(active);
    }

    /** サーバー主導のクローズ（距離超過・NPC 消滅など）。 */
    public static void close(int npcEntityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || active.npcEntityId() != npcEntityId) {
            return;
        }
        if (minecraft.gui.screen() == active) {
            active.closeFromServer();
        }
        active = null;
    }

    /** 画面が自前で閉じられた時に呼ぶ。 */
    static void onScreenRemoved(DialogueScreen screen) {
        if (active == screen) {
            active = null;
        }
    }
}

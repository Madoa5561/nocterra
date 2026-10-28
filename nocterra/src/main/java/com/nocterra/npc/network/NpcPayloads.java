package com.nocterra.npc.network;

import com.nocterra.networking.NocterraNetwork;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NPC 会話 payload の登録。所有: story 担当。
 *
 * <p>Lead 所有の {@code NocterraNetwork#registerPayloads} を触らずに済むよう、
 * 自分で {@link RegisterPayloadHandlersEvent} のリスナを登録する
 * （{@link NocterraNetwork#register} と同様に {@code event.registrar(version)} を
 * 新規に呼ぶ。NeoForge は複数 registrar を許容する）。
 * 呼び出し元は {@code com.nocterra.npc.NocterraNpcs#register}。</p>
 */
public final class NpcPayloads {
    private NpcPayloads() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NocterraNetwork.PROTOCOL_VERSION);

        // サーバー → クライアント: 会話ノード表示 / 強制クローズ
        registrar.playToClient(DialogueOpenPayload.TYPE, DialogueOpenPayload.STREAM_CODEC,
                DialogueOpenPayload::handle);
        registrar.playToClient(DialogueClosePayload.TYPE, DialogueClosePayload.STREAM_CODEC,
                DialogueClosePayload::handle);

        // クライアント → サーバー: 選択
        registrar.playToServer(DialogueSelectPayload.TYPE, DialogueSelectPayload.STREAM_CODEC,
                DialogueSelectPayload::handle);
    }
}

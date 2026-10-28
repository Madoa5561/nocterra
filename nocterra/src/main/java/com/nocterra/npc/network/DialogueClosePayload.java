package com.nocterra.npc.network;

import com.nocterra.Nocterra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント: 会話を閉じる。所有: story 担当。
 *
 * <p>サーバー側が不正・距離超過・NPC 消滅などを検出した場合にも送る。
 * クライアントは対応する NPC の会話画面が開いていれば閉じる。</p>
 */
public record DialogueClosePayload(int npcEntityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DialogueClosePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "dialogue_close"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueClosePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.npcEntityId()),
                    buf -> new DialogueClosePayload(buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueClosePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.nocterra.client.dialogue.DialogueClient.close(payload.npcEntityId()));
    }
}

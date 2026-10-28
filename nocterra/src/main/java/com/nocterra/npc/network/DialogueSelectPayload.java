package com.nocterra.npc.network;

import com.nocterra.Nocterra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー: 選択肢の選択（{@code choiceIndex = -1} は閉じる要求）。
 * 所有: story 担当。
 *
 * <p>サーバーは「セッションが存在するか」「NPC が同一で近傍にいるか」
 * 「その index の選択肢を実際に送ったか」を検証してから実行する。
 * クライアントからノード ID やアクションを直接受け取ることはない。</p>
 */
public record DialogueSelectPayload(int npcEntityId, int choiceIndex) implements CustomPacketPayload {

    public static final int CLOSE = -1;

    public static final CustomPacketPayload.Type<DialogueSelectPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "dialogue_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueSelectPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.npcEntityId());
                        buf.writeVarInt(payload.choiceIndex());
                    },
                    buf -> new DialogueSelectPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.nocterra.quest.dialogue.DialogueService.select(
                        player, payload.npcEntityId(), payload.choiceIndex());
            }
        });
    }
}

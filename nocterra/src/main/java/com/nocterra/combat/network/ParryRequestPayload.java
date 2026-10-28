package com.nocterra.combat.network;

import com.nocterra.Nocterra;
import com.nocterra.combat.CombatActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー: パリィ要求。サーバーが 0.5 秒の受付ウィンドウを開く。
 */
public record ParryRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ParryRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "parry_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParryRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new ParryRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ParryRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatActions.parry(player);
            }
        });
    }
}

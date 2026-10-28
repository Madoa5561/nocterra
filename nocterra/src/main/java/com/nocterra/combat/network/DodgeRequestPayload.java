package com.nocterra.combat.network;

import com.nocterra.Nocterra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nocterra.combat.CombatActions;

/**
 * クライアント → サーバー: 回避要求。
 *
 * <p>登録は Lead 所有の {@code NocterraNetwork#registerCombatPayloads} から
 * {@link CombatPayloads#register} を呼ぶ形で行う（このクラスは登録しない）。</p>
 */
public record DodgeRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DodgeRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "dodge_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DodgeRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new DodgeRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DodgeRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatActions.dodge(player);
            }
        });
    }
}

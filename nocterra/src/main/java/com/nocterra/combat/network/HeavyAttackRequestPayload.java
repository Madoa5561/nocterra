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
 * クライアント → サーバー: 強攻撃要求。コストは武器カテゴリ（{@code WeaponType}）で決まる。
 */
public record HeavyAttackRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HeavyAttackRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "heavy_attack_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeavyAttackRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new HeavyAttackRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HeavyAttackRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatActions.heavyAttack(player);
            }
        });
    }
}

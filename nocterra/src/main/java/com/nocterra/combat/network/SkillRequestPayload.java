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
 * クライアント → サーバー: 戦技要求（既定キー G）。
 * 右クリック戦技と同じサーバー処理（{@link CombatActions#useWeaponSkill}）を呼ぶ。
 */
public record SkillRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SkillRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "skill_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new SkillRequestPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CombatActions.useWeaponSkill(player);
            }
        });
    }
}

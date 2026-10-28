package com.nocterra.combat.network;

import com.nocterra.Nocterra;
import com.nocterra.combat.CombatClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント: スタミナ・魔力・コンボ・クールダウンの同期。
 *
 * <p>サーバー権威の値をそのまま送る。クライアントは表示にのみ使う
 * （{@link CombatClientCache} を更新し、HUD が参照する）。</p>
 */
public record CombatSyncPayload(
        float stamina,
        float mana,
        float maxStamina,
        float maxMana,
        int comboHits,
        float comboMultiplier,
        boolean parryActive,
        int dodgeCooldown,
        int heavyCooldown) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CombatSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "combat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatSyncPayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, CombatSyncPayload payload) -> {
                buf.writeFloat(payload.stamina());
                buf.writeFloat(payload.mana());
                buf.writeFloat(payload.maxStamina());
                buf.writeFloat(payload.maxMana());
                buf.writeVarInt(payload.comboHits());
                buf.writeFloat(payload.comboMultiplier());
                buf.writeBoolean(payload.parryActive());
                buf.writeVarInt(payload.dodgeCooldown());
                buf.writeVarInt(payload.heavyCooldown());
            },
            (RegistryFriendlyByteBuf buf) -> new CombatSyncPayload(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CombatClientCache.apply(payload));
    }
}

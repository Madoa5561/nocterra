package com.nocterra.networking.journal;

import com.nocterra.Nocterra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 探索手帳を開くためのサーバー→クライアント送信データ。所有: Lead。
 *
 * <p>進行データはサーバー権威（AttachmentType）なので、クライアントに必要な値だけを送る。</p>
 */
public record JournalDataPayload(
        int sigilMask,
        int bossCount,
        int npcCount,
        int questCount,
        boolean normalEnding,
        boolean trueEnding
) implements CustomPacketPayload {
    public static final Type<JournalDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "journal_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, JournalDataPayload::sigilMask,
                    ByteBufCodecs.VAR_INT, JournalDataPayload::bossCount,
                    ByteBufCodecs.VAR_INT, JournalDataPayload::npcCount,
                    ByteBufCodecs.VAR_INT, JournalDataPayload::questCount,
                    ByteBufCodecs.BOOL, JournalDataPayload::normalEnding,
                    ByteBufCodecs.BOOL, JournalDataPayload::trueEnding,
                    JournalDataPayload::new);

    @Override
    public Type<JournalDataPayload> type() {
        return TYPE;
    }
}

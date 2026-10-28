package com.nocterra.npc.network;

import com.nocterra.Nocterra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント: 会話ノードの表示内容。所有: story 担当。
 *
 * <p>クライアントは「表示」だけを行う。選択肢の可否・次ノード・アクションは
 * サーバーが {@code DialogueService} で決定し、この payload はその結果を運ぶだけ。</p>
 *
 * <p>{@code textKey} / {@code Choice#textKey} は翻訳キー。クライアントが
 * {@code Component.translatable} で解決する（言語切替に追従できる）。</p>
 */
public record DialogueOpenPayload(int npcEntityId,
                                  String npcId,
                                  String textKey,
                                  List<Choice> choices) implements CustomPacketPayload {

    /** 選択肢 1 つ（表示用）。 */
    public record Choice(String textKey, boolean enabled) {}

    public static final CustomPacketPayload.Type<DialogueOpenPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "dialogue_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueOpenPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.npcEntityId());
                        buf.writeUtf(payload.npcId());
                        buf.writeUtf(payload.textKey());
                        buf.writeVarInt(payload.choices().size());
                        for (Choice choice : payload.choices()) {
                            buf.writeUtf(choice.textKey());
                            buf.writeBoolean(choice.enabled());
                        }
                    },
                    buf -> {
                        int entityId = buf.readVarInt();
                        String npcId = buf.readUtf();
                        String textKey = buf.readUtf();
                        int size = buf.readVarInt();
                        List<Choice> choices = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            choices.add(new Choice(buf.readUtf(), buf.readBoolean()));
                        }
                        return new DialogueOpenPayload(entityId, npcId, textKey, List.copyOf(choices));
                    });

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueOpenPayload payload, IPayloadContext context) {
        // このハンドラはクライアントでのみ実行される（playToClient）。
        context.enqueueWork(() -> com.nocterra.client.dialogue.DialogueClient.open(payload));
    }
}

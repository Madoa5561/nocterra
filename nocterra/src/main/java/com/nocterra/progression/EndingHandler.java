package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * エンディング演出。所有: Lead（サーバー権威）。
 *
 * <p>通常エンディング（刻印 5 個以上）と真エンディング（8 個）を分岐させ、
 * タイトル・ナレーション・効果音・チャットで演出する。到達後もワールドは継続可能。</p>
 */
public final class EndingHandler {
    private EndingHandler() {}

    public static void playNormalEnding(ServerPlayer player) {
        ProgressionData data = ProgressionAttachments.get(player);
        if (data.normalEndingSeen()) {
            player.sendSystemMessage(Component.translatable("message.nocterra.ending.already")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        data.markNormalEnding();
        showTitle(player, "title.nocterra.ending.normal", "title.nocterra.ending.normal.subtitle");
        narrate(player, "message.nocterra.ending.normal.1", "message.nocterra.ending.normal.2",
                "message.nocterra.ending.normal.3");
        player.sendSystemMessage(Component.translatable("message.nocterra.ending.continue")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        celebrate(player, false);
        Nocterra.LOG.info("[NOCTERRA] normal ending reached by {}", player.getName().getString());
    }

    public static void playTrueEnding(ServerPlayer player) {
        ProgressionData data = ProgressionAttachments.get(player);
        if (data.trueEndingSeen()) {
            player.sendSystemMessage(Component.translatable("message.nocterra.ending.already")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        data.markTrueEnding();
        showTitle(player, "title.nocterra.ending.true", "title.nocterra.ending.true.subtitle");
        narrate(player, "message.nocterra.ending.true.1", "message.nocterra.ending.true.2",
                "message.nocterra.ending.true.3", "message.nocterra.ending.true.4");
        player.sendSystemMessage(Component.translatable("message.nocterra.ending.true.reward")
                .withStyle(ChatFormatting.GOLD));
        celebrate(player, true);
        Nocterra.LOG.info("[NOCTERRA] TRUE ending reached by {}", player.getName().getString());
    }

    private static void showTitle(ServerPlayer player, String titleKey, String subtitleKey) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 120, 40));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable(titleKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable(subtitleKey).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static void narrate(ServerPlayer player, String... keys) {
        for (String key : keys) {
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static void celebrate(ServerPlayer player, boolean grand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                grand ? SoundEvents.END_PORTAL_SPAWN : SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, grand ? 1.0F : 0.8F);
        if (grand) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.4F, 0.6F);
        }
    }
}

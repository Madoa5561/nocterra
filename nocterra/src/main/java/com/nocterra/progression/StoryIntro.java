package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * ノクテラ初入場時の導入演出。所有: Lead。
 *
 * <p>ポータルでノクテラに入ると、タイトル・ナレーション・効果音を出し、
 * 探索手帳を 1 冊渡して進行データに「世界発見」を記録する。</p>
 */
public final class StoryIntro {
    private StoryIntro() {}

    private static final String POI_ENTERED = "nocterra:first_entry";

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getTo().equals(com.nocterra.world.NocterraDimensions.NOCTERRA)) {
            return;
        }
        ProgressionData data = ProgressionAttachments.get(player);
        if (!data.addDiscoveredPoi(POI_ENTERED)) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("title.nocterra.intro").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("title.nocterra.intro.subtitle").withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(Component.translatable("message.nocterra.intro.1")
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("message.nocterra.intro.2")
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("message.nocterra.intro.3")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        // 探索手帳を渡す（既に持っていれば渡さない）
        boolean hasJournal = player.getInventory().contains(stack ->
                stack.is(com.nocterra.registry.NocterraStoryItems.EXPLORER_JOURNAL.get()));
        if (!hasJournal) {
            ItemStack journal = new ItemStack(com.nocterra.registry.NocterraStoryItems.EXPLORER_JOURNAL.get());
            if (!player.getInventory().add(journal)) {
                player.drop(journal, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        // 案内人（灰の番人）を到着地点の近くに配置する（物語の入口を必ず見つけられるように）
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos base = player.blockPosition();
            boolean alreadyNearby = !serverLevel.getEntitiesOfClass(
                    com.nocterra.npc.NocterraNpc.class,
                    new net.minecraft.world.phys.AABB(base).inflate(48.0),
                    npc -> npc.getType().equals(com.nocterra.npc.NocterraNpcs.WARDEN_OF_ASH.get())).isEmpty();
            if (!alreadyNearby) {
                for (int attempt = 0; attempt < 8; attempt++) {
                    int dx = (attempt % 3) - 1;
                    int dz = (attempt / 3) - 1;
                    net.minecraft.core.BlockPos spot = base.offset(dx * 3, 0, dz * 3);
                    if (serverLevel.isEmptyBlock(spot) && serverLevel.isEmptyBlock(spot.above())) {
                        com.nocterra.npc.NocterraNpc npc =
                                com.nocterra.npc.NocterraNpcs.spawn(serverLevel, "warden_of_ash", spot);
                        if (npc != null) {
                            npc.setPersistenceRequired();
                            break;
                        }
                    }
                }
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7F, 0.8F);
        Nocterra.LOG.info("[NOCTERRA] {} entered Nocterra for the first time",
                player.getName().getString());
    }
}

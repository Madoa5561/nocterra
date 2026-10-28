package com.nocterra.npc;

import com.nocterra.quest.StoryAttachments;
import com.nocterra.quest.StoryData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * NPC 固有の小さな演出・動作。所有: story 担当。
 *
 * <p>会話 JSON から {@code {"type": "npc_action", "action": "..."}} で呼ばれる。
 * 汎用アクション（クエスト・アイテム・交易）は {@code DialogueService} 側にあり、
 * ここには「その NPC でしか起きないこと」だけを置く。</p>
 */
public final class NpcBehaviors {
    private NpcBehaviors() {}

    /** 会話を開いた直後のフック（クエスト判定より後に呼ばれる）。 */
    public static void onTalkedTo(ServerPlayer player, NocterraNpc npc) {
        if ("warden_of_ash".equals(npc.npcId())) {
            reuniteLostChild(player);
        }
    }

    /** {@code npc_action} の実行。@return 処理したら true */
    public static boolean onAction(ServerPlayer player, NocterraNpc npc, String action) {
        switch (action) {
            case "follow_player" -> {
                npc.followPlayer(player);
                StoryAttachments.get(player).setFlag("lost_child_following");
                player.sendSystemMessage(Component.translatable("message.nocterra.child.following")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
                return true;
            }
            case "stop_following" -> {
                npc.stopFollowing();
                StoryAttachments.get(player).clearFlag("lost_child_following");
                return true;
            }
            case "open_black_gate" -> {
                boolean opened = StoryAttachments.get(player).setFlag("black_crown_gate_open");
                player.sendSystemMessage(Component.translatable(
                                opened ? "message.nocterra.gate.open" : "message.nocterra.gate.already_open")
                        .withStyle(ChatFormatting.GOLD));
                playSound(player, SoundEvents.BEACON_ACTIVATE, 1.2F, 0.7F);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            npc.getX(), npc.getY() + 1.2, npc.getZ(), 40, 0.6, 0.9, 0.6, 0.03);
                }
                return true;
            }
            default -> {
                com.nocterra.Nocterra.LOG.warn("[NOCTERRA] unknown npc_action '{}' for npc '{}'",
                        action, npc.npcId());
                return false;
            }
        }
    }

    /**
     * 迷い子を連れ帰った演出。{@code lost_child_return} 完了済みで、追従中の迷い子が
     * 近くにいれば、光となって還る（エンティティを消す）。
     */
    private static void reuniteLostChild(ServerPlayer player) {
        StoryData story = StoryAttachments.get(player);
        if (!story.isQuestCompleted("lost_child_return")) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<NocterraNpc> children = level.getEntitiesOfClass(NocterraNpc.class,
                player.getBoundingBox().inflate(48.0),
                npc -> "lost_child".equals(npc.npcId())
                        && player.getUUID().equals(npc.followingPlayer()));
        if (children.isEmpty()) {
            return;
        }
        for (NocterraNpc child : children) {
            level.sendParticles(ParticleTypes.END_ROD,
                    child.getX(), child.getY() + 1.0, child.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
            level.playSound(null, child.getX(), child.getY(), child.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.5F);
            child.discard();
        }
        story.setFlag("lost_child_home");
        story.clearFlag("lost_child_following");
        player.sendSystemMessage(Component.translatable("message.nocterra.child.reunited")
                .withStyle(ChatFormatting.GOLD));
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float pitch) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }
}

package com.nocterra.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 繧ｯ繧ｨ繧ｹ繝医・蜿玲ｳｨ繝ｻ騾ｲ陦後・螳御ｺ・ｼ医し繝ｼ繝舌・讓ｩ螽・ｼ峨よ園譛・ story 諡・ｽ薙・ *
 * <p>騾ｲ陦後う繝吶Φ繝・</p>
 * <ul>
 *   <li>{@link QuestType#KILL_ENTITY} / {@link QuestType#DEFEAT_BOSS}:
 *       {@code LivingDeathEvent} 縺九ｉ {@link #onLivingDeath}</li>
 *   <li>{@link QuestType#COLLECT_ITEM} / {@link QuestType#VISIT_BIOME}:
 *       繝励Ξ繧､繝､繝ｼ tick・・0 tick 髢馴囈・峨°繧・{@link #tickPlayer}</li>
 *   <li>{@link QuestType#TALK_TO_NPC}: 莨夊ｩｱ UI 繧帝幕縺・◆譎ゅ↓
 *       {@link #onTalkToNpc}・・ialogueService 縺九ｉ蜻ｼ縺ｶ・・/li>
 * </ul>
 *
 * <p>螳御ｺ・凾縺ｯ蝣ｱ驟ｬ・・P繝ｻ譌｢蟄倥い繧､繝・Β・峨ｒ莉倅ｸ弱＠縲√メ繝｣繝・ヨ縺ｧ騾夂衍縲・ * Lead 縺ｮ騾ｲ陦後ョ繝ｼ繧ｿ・・@code completedQuests}・峨∈繝溘Λ繝ｼ縺吶ｋ縲・/p>
 */
public final class QuestManager {
    private QuestManager() {}

    // ------------------------------------------------------------------ 蜿玲ｳｨ

    /** 繧ｯ繧ｨ繧ｹ繝医ｒ謠千､ｺ縺吶ｋ縲ょ女豕ｨ譚｡莉ｶ繧呈ｺ縺溘＆縺ｪ縺・ｴ蜷医・繝｡繝・そ繝ｼ繧ｸ縺ｮ縺ｿ縲・*/
    public static boolean offer(ServerPlayer player, String questId) {
        QuestDefinition definition = StoryRepository.quest(questId);
        if (definition == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] quest_offer for unknown quest '{}'", questId);
            return false;
        }
        StoryData story = StoryAttachments.get(player);
        if (story.isQuestCompleted(questId)) {
            player.sendSystemMessage(Component.translatable("message.nocterra.quest.already_completed",
                    title(definition)).withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (story.isActive(questId)) {
            player.sendSystemMessage(Component.translatable("message.nocterra.quest.already_active",
                    title(definition)).withStyle(ChatFormatting.GRAY));
            return false;
        }
        StoryContext context = StoryContext.of(player);
        if (!StoryCondition.testAll(definition.requires(), context)) {
            player.sendSystemMessage(Component.translatable("message.nocterra.quest.locked",
                    title(definition)).withStyle(ChatFormatting.RED));
            return false;
        }
        story.startQuest(questId);
        player.sendSystemMessage(Component.translatable("message.nocterra.quest.accepted",
                title(definition)).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable(definition.descriptionKey())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 1.4F);
        refreshActive(player);
        return true;
    }

    /** 繧ｯ繧ｨ繧ｹ繝医ｒ螳御ｺ・＆縺帙ｋ・亥ｱ驟ｬ莉倅ｸ趣ｼ峨ゆｼ夊ｩｱ縺ｮ {@code quest_complete} 縺九ｉ繧ょ他縺ｶ縲・*/
    public static boolean complete(ServerPlayer player, String questId) {
        QuestDefinition definition = StoryRepository.quest(questId);
        StoryData story = StoryAttachments.get(player);
        if (story.isQuestCompleted(questId)) {
            return false;
        }
        if (definition == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] quest_complete for unknown quest '{}'", questId);
            return false;
        }
        if (!story.isActive(questId)) {
            // 莨夊ｩｱ縺ｫ繧医ｋ邏榊刀縺ｪ縺ｩ縲∵・遉ｺ逧・↓螳御ｺ・＆縺帙ｋ邨瑚ｷｯ縺ｯ蜿玲ｳｨ貂医∩縺ｧ縺ｪ縺上※繧る壹☆
            story.startQuest(questId);
        }
        story.markQuestCompleted(questId);
        StoryProgression.mirrorQuestCompleted(player, questId);

        player.sendSystemMessage(Component.translatable("message.nocterra.quest.completed",
                title(definition)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        giveRewards(player, definition);
        playSound(player, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
        com.nocterra.Nocterra.LOG.info("[NOCTERRA] {} completed quest {}",
                player.getName().getString(), questId);
        return true;
    }

    // ------------------------------------------------------------------ 騾ｲ陦・
    /** 騾ｲ陦悟ｺｦ繧貞刈邂励☆繧具ｼ域ｸ帛ｰ代＠縺ｪ縺・ｼ峨・*/
    public static void addProgress(ServerPlayer player, String questId, int delta) {
        QuestDefinition definition = StoryRepository.quest(questId);
        if (definition == null) {
            return;
        }
        StoryData story = StoryAttachments.get(player);
        if (!story.isActive(questId)) {
            return;
        }
        int next = story.progress(questId) + Math.max(1, delta);
        boolean changed = story.setProgress(questId, next);
        if (changed) {
            notifyProgress(player, definition, story.progress(questId));
        }
        if (story.progress(questId) >= definition.requiredCount()) {
            complete(player, questId);
        }
    }

    /**
     * 繝励Ξ繧､繝､繝ｼ tick・・0 tick 髢馴囈・峨ゅい繧､繝・Β謇謖√→繝舌う繧ｪ繝ｼ繝貊槫惠繧貞・隧穂ｾ｡縺吶ｋ縲・     */
    public static void tickPlayer(ServerPlayer player) {
        refreshActive(player);
    }

    /** 蜿玲ｳｨ荳ｭ縺ｮ繧ｯ繧ｨ繧ｹ繝医・縺・■縲∫憾諷九°繧芽・蜍募愛螳壹〒縺阪ｋ繧ゅ・繧呈峩譁ｰ縺吶ｋ縲・*/
    public static void refreshActive(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        StoryData story = StoryAttachments.get(player);
        List<String> active = new ArrayList<>(story.activeQuests().keySet());
        for (String questId : active) {
            QuestDefinition definition = StoryRepository.quest(questId);
            if (definition == null) {
                continue;
            }
            switch (definition.type()) {
                case COLLECT_ITEM -> {
                    Item item = StoryItems.resolve(definition.target());
                    if (item == null) {
                        continue;
                    }
                    int held = Math.min(StoryItems.count(player, item), definition.requiredCount());
                    if (story.setProgress(questId, held)) {
                        notifyProgress(player, definition, held);
                    }
                    if (held >= definition.requiredCount()) {
                        complete(player, questId);
                    }
                }
                case VISIT_BIOME -> {
                    if (definition.target().isBlank()) {
                        continue;
                    }
                    String biome = StoryCondition.biomeId(level, player.blockPosition());
                    if (StoryCondition.normalize(definition.target()).equals(biome)) {
                        complete(player, questId);
                    }
                }
                default -> {
                    // KILL_ENTITY / DEFEAT_BOSS / TALK_TO_NPC 縺ｯ繧､繝吶Φ繝磯ｧ・虚
                }
            }
        }
    }

    // ------------------------------------------------------------------ 繧､繝吶Φ繝・
    /** 繧ｨ繝ｳ繝・ぅ繝・ぅ豁ｻ莠｡縲よ茶遐ｴ謨ｰ繧ｫ繧ｦ繝ｳ繝医→繝懊せ謦・ｴ繧ｯ繧ｨ繧ｹ繝医ｒ蜃ｦ逅・☆繧九・*/
    public static void onLivingDeath(LivingEntity dead, ServerPlayer credit) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (typeId == null) {
            return;
        }
        String fullId = typeId.toString();
        for (ServerPlayer player : creditRecipients(dead, credit)) {
            StoryData story = StoryAttachments.get(player);
            List<String> active = new ArrayList<>(story.activeQuests().keySet());
            for (String questId : active) {
                QuestDefinition definition = StoryRepository.quest(questId);
                if (definition == null || !targetMatches(definition.target(), fullId)) {
                    continue;
                }
                switch (definition.type()) {
                    case KILL_ENTITY -> addProgress(player, questId, 1);
                    case DEFEAT_BOSS -> complete(player, questId);
                    default -> {
                    }
                }
            }
        }
    }

    /** 莨夊ｩｱ UI 繧帝幕縺・◆縲５ALK_TO_NPC 繧帝＃謌舌＆縺帙ｋ縲・*/
    public static void onTalkToNpc(ServerPlayer player, String npcId) {
        StoryData story = StoryAttachments.get(player);
        List<String> active = new ArrayList<>(story.activeQuests().keySet());
        for (String questId : active) {
            QuestDefinition definition = StoryRepository.quest(questId);
            if (definition == null || definition.type() != QuestType.TALK_TO_NPC) {
                continue;
            }
            String normalized = StoryCondition.normalize(definition.target());
            if (normalized.equals(StoryCondition.normalize(npcId))) {
                complete(player, questId);
            }
        }
    }

    // ------------------------------------------------------------------ 陦ｨ遉ｺ

    /** 繝・ヰ繝・げ逕ｨ: 蜿玲ｳｨ荳ｭ繧ｯ繧ｨ繧ｹ繝医・ 1 陦後し繝槭Μ縲・*/
    public static String debugStatus(ServerPlayer player) {
        StoryData story = StoryAttachments.get(player);
        StringBuilder builder = new StringBuilder();
        builder.append("sigils=").append(StoryProgression.sealCount(player))
                .append(" npcs=").append(story.discoveredNpcs().size())
                .append(" completed=").append(story.completedQuests().size())
                .append(" lore=").append(story.lore().size())
                .append(" active=[");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : story.activeQuests().entrySet()) {
            QuestDefinition definition = StoryRepository.quest(entry.getKey());
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(entry.getKey()).append(' ').append(entry.getValue());
            if (definition != null) {
                builder.append('/').append(definition.requiredCount());
            }
        }
        builder.append(']');
        return builder.toString();
    }

    // ------------------------------------------------------------------ 蜀・Κ

    static Component title(QuestDefinition definition) {
        return Component.translatable(definition.titleKey());
    }

    private static void notifyProgress(ServerPlayer player, QuestDefinition definition, int progress) {
        player.sendSystemMessage(Component.translatable("message.nocterra.quest.progress",
                title(definition).copy().withStyle(ChatFormatting.YELLOW),
                Math.min(progress, definition.requiredCount()),
                definition.requiredCount()).withStyle(ChatFormatting.GRAY));
    }

    private static void giveRewards(ServerPlayer player, QuestDefinition definition) {
        if (definition.xpReward() > 0) {
            player.giveExperiencePoints(definition.xpReward());
            player.sendSystemMessage(Component.translatable("message.nocterra.quest.reward.xp",
                    definition.xpReward()).withStyle(ChatFormatting.AQUA));
        }
        for (QuestDefinition.ItemReward reward : definition.itemRewards()) {
            Item item = StoryItems.resolve(reward.itemId());
            if (item == null) {
                continue;
            }
            StoryItems.give(player, item, reward.count());
            ItemStack stack = new ItemStack(item);
            player.sendSystemMessage(Component.translatable("message.nocterra.quest.reward.item",
                    stack.getHoverName(), reward.count()).withStyle(ChatFormatting.AQUA));
        }
    }

    private static List<ServerPlayer> creditRecipients(LivingEntity dead, ServerPlayer credit) {
        if (credit != null && credit.isAlive()) {
            return List.of(credit);
        }
        if (dead.level() instanceof ServerLevel level) {
            List<ServerPlayer> nearby = level.getEntitiesOfClass(
                    ServerPlayer.class, dead.getBoundingBox().inflate(64.0));
            if (!nearby.isEmpty()) {
                return nearby;
            }
        }
        return List.of();
    }

    private static boolean targetMatches(String target, String fullId) {
        if (target == null || target.isBlank()) {
            return false;
        }
        return StoryCondition.normalize(target).equals(fullId);
    }

    private static void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float pitch) {
        if (sound == null) {
            return;
        }
        ServerLevel level = player.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        if (level == null) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }
}

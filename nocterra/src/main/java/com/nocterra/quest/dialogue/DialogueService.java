package com.nocterra.quest.dialogue;

import com.nocterra.npc.NocterraNpc;
import com.nocterra.npc.NpcBehaviors;
import com.nocterra.npc.network.DialogueClosePayload;
import com.nocterra.npc.network.DialogueOpenPayload;
import com.nocterra.quest.QuestManager;
import com.nocterra.quest.StoryAttachments;
import com.nocterra.quest.StoryCondition;
import com.nocterra.quest.StoryContext;
import com.nocterra.quest.StoryData;
import com.nocterra.quest.StoryItems;
import com.nocterra.quest.StoryProgression;
import com.nocterra.quest.StoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 会話のサーバー権威エンジン。所有: story 担当。
 *
 * <p>流れ:</p>
 * <ol>
 *   <li>NPC 右クリック → {@link #open}（発見記録・TALK_TO_NPC 達成・開始ノード決定）</li>
 *   <li>サーバーが {@link DialogueOpenPayload} で「本文キー + 選択肢キー」を送る</li>
 *   <li>クライアントは index だけ返す → {@link #select} で検証して次ノードへ</li>
 * </ol>
 *
 * <p>距離・同一ディメンション・NPC の生存・セッションの一致・選択肢 index の範囲を
 * 毎回検証する。条件は送信時と選択時の両方で評価する（途中で状態が変わっても破綻しない）。</p>
 */
public final class DialogueService {
    private DialogueService() {}

    /** これ以上離れたら会話を閉じる（8 ブロック）。 */
    private static final double MAX_DISTANCE_SQ = 64.0;

    private static final Map<UUID, DialogueSession> SESSIONS = new HashMap<>();

    // ------------------------------------------------------------------ 開始


    /** 進行中の会話セッション（なければ null）。所有: Lead（デバッグ・フォールバック用）。 */
    public static @Nullable DialogueSession currentSession(ServerPlayer player) {
        return SESSIONS.get(player.getUUID());
    }

    public static void open(ServerPlayer player, NocterraNpc npc) {
        String npcId = npc.npcId();
        StoryData story = StoryAttachments.get(player);
        boolean wasDiscovered = story.isNpcDiscovered(npcId);

        // 1) 発見記録（会話を開いた事実そのもの）
        if (!wasDiscovered) {
            story.markNpcDiscovered(npcId);
            StoryProgression.mirrorNpcDiscovered(player, npcId);
            player.sendSystemMessage(Component.translatable("message.nocterra.npc.met",
                    Component.translatable("entity.nocterra." + npcId))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // 2) TALK_TO_NPC クエストの達成（会話を開いた時点）
        QuestManager.onTalkToNpc(player, npcId);

        // 3) NPC 固有の会話開始フック（例: 迷い子を連れ帰った演出）
        NpcBehaviors.onTalkedTo(player, npc);

        DialogueDefinition definition = StoryRepository.dialogue(npcId);
        if (definition == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] no dialogue definition for npc '{}'", npcId);
            player.sendSystemMessage(Component.translatable("message.nocterra.dialogue.missing")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 4) 既存セッションを閉じる（連打対策）
        close(player);

        StoryContext context = new StoryContext(player, story, npcId, wasDiscovered);
        String startNode = definition.chooseStartNode(context);
        if (startNode == null || definition.node(startNode) == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] dialogue '{}' has no matching start node", npcId);
            return;
        }

        boolean endingSeen = isEndingSeen(player);
        DialogueSession session = new DialogueSession(player.getUUID(), npc.getId(), npcId,
                wasDiscovered, endingSeen);
        SESSIONS.put(player.getUUID(), session);
        npc.triggerTalkAnimation();
        npc.facePlayer(player);
        levelSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.5F);
        showNode(player, session, definition, startNode);
    }

    // ------------------------------------------------------------------ 選択

    public static void select(ServerPlayer player, int npcEntityId, int choiceIndex) {
        com.nocterra.Nocterra.LOG.info("[NOCTERRA] dialogue select received npcEntity={} index={}",
                npcEntityId, choiceIndex);
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.npcEntityId != npcEntityId) {
            return;
        }
        Entity entity = player.level().getEntity(npcEntityId);
        if (!(entity instanceof NocterraNpc npc) || !npc.isAlive()
                || npc.distanceToSqr(player) > MAX_DISTANCE_SQ) {
            com.nocterra.Nocterra.LOG.info("[NOCTERRA] dialogue close reason=npc-invalid entity={} dist2={}",
                    entity, entity == null ? -1 : entity.distanceToSqr(player));
            close(player);
            return;
        }
        if (choiceIndex == com.nocterra.npc.network.DialogueSelectPayload.CLOSE) {
            close(player);
            return;
        }
        if (choiceIndex < 0 || choiceIndex >= session.choices.size()) {
            // 不正 index は無視して現ノードを再送する
            resend(player, session);
            return;
        }

        DialogueDefinition definition = StoryRepository.dialogue(session.npcId);
        if (definition == null) {
            close(player);
            return;
        }

        DialogueDefinition.ChoiceSpec choice = session.choices.get(choiceIndex);
        StoryContext context = new StoryContext(player, StoryAttachments.get(player),
                session.npcId, session.wasDiscovered);
        if (!StoryCondition.testAll(choice.conditions(), context)) {
            // 選択肢の条件が途中で変わった → 現在ノードを再評価
            resend(player, session);
            return;
        }

        if (choice.action() != null) {
            execute(player, session, choice.action());
        }
        if (session.closed) {
            close(player);
            return;
        }
        if (choice.next() != null) {
            DialogueDefinition.Node next = definition.node(choice.next());
            if (next == null) {
                com.nocterra.Nocterra.LOG.warn("[NOCTERRA] dialogue '{}' references missing node '{}'",
                        session.npcId, choice.next());
                close(player);
                return;
            }
            showNode(player, session, definition, choice.next());
        } else {
            close(player);
        }
    }

    /** プレイヤー tick から呼ぶ距離チェックと、会話中のエンディング検出。 */
    public static void validate(ServerPlayer player) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        Entity entity = player.level().getEntity(session.npcEntityId);
        if (!(entity instanceof NocterraNpc npc) || !npc.isAlive()
                || npc.distanceToSqr(player) > MAX_DISTANCE_SQ) {
            com.nocterra.Nocterra.LOG.info("[NOCTERRA] dialogue close reason=npc-invalid entity={} dist2={}",
                    entity, entity == null ? -1 : entity.distanceToSqr(player));
            close(player);
            return;
        }
        // 会話中にエンディング演出が始まったら UI を閉じる（タイトルと重ならないように）
        if (!session.endingSeenAtOpen && isEndingSeen(player)) {
            com.nocterra.Nocterra.LOG.info("[NOCTERRA] dialogue close reason=ending-seen");
            close(player);
        }
    }

    /** 通常／真エンディングのいずれかを既に見たか（Lead の進行データを参照）。 */
    private static boolean isEndingSeen(ServerPlayer player) {
        var data = StoryProgression.of(player);
        return data != null && (data.normalEndingSeen() || data.trueEndingSeen());
    }

    public static void onLogout(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    /** サーバー主導で閉じる（クライアントにも通知）。 */
    public static void close(ServerPlayer player) {
        DialogueSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            PacketDistributor.sendToPlayer(player, new DialogueClosePayload(session.npcEntityId));
        }
    }

    public static boolean isTalking(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------------ 内部: ノード表示

    private static void resend(ServerPlayer player, DialogueSession session) {
        DialogueDefinition definition = StoryRepository.dialogue(session.npcId);
        if (definition == null || session.nodeId == null) {
            close(player);
            return;
        }
        DialogueDefinition.Node node = definition.node(session.nodeId);
        if (node == null) {
            close(player);
            return;
        }
        sendNode(player, session, node);
    }

    private static void showNode(ServerPlayer player, DialogueSession session,
                                 DialogueDefinition definition, String nodeId) {
        DialogueDefinition.Node node = definition.node(nodeId);
        if (node == null) {
            close(player);
            return;
        }
        session.nodeId = nodeId;
        session.closed = false;

        // onEnter アクション（ノードに入った時点で確定で起きること）
        for (StoryAction action : node.onEnter()) {
            execute(player, session, action);
            if (session.closed) {
                close(player);
                return;
            }
        }
        sendNode(player, session, node);
    }

    private static void sendNode(ServerPlayer player, DialogueSession session,
                                 DialogueDefinition.Node node) {
        StoryContext context = new StoryContext(player, StoryAttachments.get(player),
                session.npcId, session.wasDiscovered);
        List<DialogueDefinition.ChoiceSpec> visible = new ArrayList<>();
        for (DialogueDefinition.ChoiceSpec choice : node.choices()) {
            if (StoryCondition.testAll(choice.conditions(), context)) {
                visible.add(choice);
            }
        }
        session.choices = List.copyOf(visible);

        List<DialogueOpenPayload.Choice> payloadChoices = new ArrayList<>(visible.size());
        for (DialogueDefinition.ChoiceSpec choice : visible) {
            payloadChoices.add(new DialogueOpenPayload.Choice(choice.textKey(), true));
        }
        PacketDistributor.sendToPlayer(player, new DialogueOpenPayload(
                session.npcEntityId, session.npcId, node.textKey(), List.copyOf(payloadChoices)));
    }

    // ------------------------------------------------------------------ 内部: アクション実行

    private static void execute(ServerPlayer player, DialogueSession session, StoryAction action) {
        switch (action.type()) {
            case "quest_offer" -> QuestManager.offer(player, action.quest());
            case "quest_complete" -> QuestManager.complete(player, action.quest());
            case "give_item" -> giveItem(player, action);
            case "lore_unlock" -> unlockLore(player, action);
            case "trade" -> trade(player, action);
            case "flag" -> StoryAttachments.get(player).setFlag(action.flag());
            case "npc_action" -> {
                Entity entity = player.level().getEntity(session.npcEntityId);
                if (entity instanceof NocterraNpc npc) {
                    npc.handleStoryAction(player, action.action());
                }
            }
            case "message" -> player.sendSystemMessage(
                    Component.translatable(action.messageKey())
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            case "close" -> session.closed = true;
            default -> com.nocterra.Nocterra.LOG.warn(
                    "[NOCTERRA] unknown dialogue action type '{}'", action.type());
        }
    }

    private static void giveItem(ServerPlayer player, StoryAction action) {
        Item item = StoryItems.resolve(action.item());
        if (item == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] dialogue gives unknown item '{}'", action.item());
            return;
        }
        StoryItems.give(player, item, action.count());
        player.sendSystemMessage(Component.translatable("message.nocterra.item.received",
                new ItemStack(item).getHoverName(), action.count()).withStyle(ChatFormatting.GOLD));
        levelSound(player, SoundEvents.ITEM_PICKUP, 0.7F, 1.2F);
    }

    private static void unlockLore(ServerPlayer player, StoryAction action) {
        StoryData story = StoryAttachments.get(player);
        if (!story.unlockLore(action.lore())) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.nocterra.lore.unlocked")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.translatable("lore.nocterra." + action.lore())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        levelSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.9F);
    }

    private static void trade(ServerPlayer player, StoryAction action) {
        Item cost = StoryItems.resolve(action.costItem());
        Item result = StoryItems.resolve(action.resultItem());
        if (cost == null || result == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] trade with unknown item (cost='{}', result='{}')",
                    action.costItem(), action.resultItem());
            return;
        }
        if (!StoryItems.consume(player, cost, action.costCount())) {
            player.sendSystemMessage(Component.translatable("message.nocterra.trade.insufficient",
                    new ItemStack(cost).getHoverName(), action.costCount()).withStyle(ChatFormatting.RED));
            levelSound(player, SoundEvents.VILLAGER_NO, 0.8F, 1.0F);
            return;
        }
        StoryItems.give(player, result, action.resultCount());
        player.sendSystemMessage(Component.translatable("message.nocterra.trade.done",
                new ItemStack(cost).getHoverName(), action.costCount(),
                new ItemStack(result).getHoverName(), action.resultCount())
                .withStyle(ChatFormatting.GREEN));
        levelSound(player, SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------ ユーティリティ

    private static void levelSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                   float volume, float pitch) {
        if (!(player.level() instanceof ServerLevel level) || sound == null) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.NEUTRAL, volume, pitch);
    }
}

package com.nocterra.quest;

import com.nocterra.Nocterra;
import com.nocterra.quest.dialogue.DialogueService;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * ストーリー（クエスト・会話）のゲームバスリスナ。所有: story 担当。
 *
 * <p>{@link NocterraQuests#register(IEventBus)} が登録する。</p>
 */
public final class StoryEvents {
    private StoryEvents() {}

    /** データパックのリロード時に会話・クエストを読み直す。 */
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "story"),
                new StoryReloadListener());
    }

    /** ボス・モブの死亡 → KILL_ENTITY / DEFEAT_BOSS。 */
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        ServerPlayer credit = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
        QuestManager.onLivingDeath(entity, credit);
    }

    /** 20 tick ごとの自動進行判定（COLLECT_ITEM / VISIT_BIOME）と会話距離チェック。 */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            QuestManager.tickPlayer(player);
        }
        if (player.tickCount % 10 == 0) {
            DialogueService.validate(player);
        }
    }

    /** ログアウト時に会話セッションを破棄する。 */
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DialogueService.onLogout(player);
        }
    }
}

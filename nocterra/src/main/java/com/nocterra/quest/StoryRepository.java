package com.nocterra.quest;

import com.nocterra.quest.dialogue.DialogueDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * データパックからロードした会話・クエスト定義の置き場。所有: story 担当。
 *
 * <p>サーバーのリロード時に丸ごと差し替える（{@code AddServerReloadListenersEvent} →
 * {@link StoryReloadListener}）。キーは path 部分（例: {@code warden_of_ash}, {@code ash_first_light}）。</p>
 */
public final class StoryRepository {
    private StoryRepository() {}

    private static volatile Map<String, QuestDefinition> quests = Map.of();
    private static volatile Map<String, DialogueDefinition> dialogues = Map.of();

    static void replace(Map<String, QuestDefinition> loadedQuests,
                        Map<String, DialogueDefinition> loadedDialogues) {
        quests = Collections.unmodifiableMap(new LinkedHashMap<>(loadedQuests));
        dialogues = Collections.unmodifiableMap(new LinkedHashMap<>(loadedDialogues));
        com.nocterra.Nocterra.LOG.info("[NOCTERRA] story data loaded: {} quests, {} dialogues",
                quests.size(), dialogues.size());
    }

    public static QuestDefinition quest(String questId) {
        return quests.get(questId);
    }

    public static Collection<QuestDefinition> quests() {
        return quests.values();
    }

    public static Map<String, QuestDefinition> questMap() {
        return quests;
    }

    public static DialogueDefinition dialogue(String npcId) {
        return dialogues.get(npcId);
    }

    public static Collection<DialogueDefinition> dialogues() {
        return dialogues.values();
    }
}

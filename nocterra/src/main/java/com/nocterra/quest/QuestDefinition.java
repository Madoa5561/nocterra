package com.nocterra.quest;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * データ駆動クエスト定義（{@code data/nocterra/quest/<id>.json}）。所有: story 担当。
 *
 * <pre>{@code
 * {
 *   "title": "quest.nocterra.ash_first_light.title",
 *   "description": "quest.nocterra.ash_first_light.description",
 *   "type": "COLLECT_ITEM",
 *   "target": "nocterra:ember_shard",
 *   "count": 3,
 *   "giver": "warden_of_ash",
 *   "requires": [ { "type": "quest", "quest": "other_quest", "state": "completed" } ],
 *   "reward": { "xp": 40, "items": [ { "item": "nocterra:ember_shard", "count": 3 } ] }
 * }
 * }</pre>
 *
 * <p>進捗・完了はサーバーが記録する。クライアントはチャット通知だけを受け取る。</p>
 */
public record QuestDefinition(Identifier id,
                              String titleKey,
                              String descriptionKey,
                              QuestType type,
                              String target,
                              int count,
                              String giver,
                              List<StoryCondition> requires,
                              int xpReward,
                              List<ItemReward> itemRewards) {

    public record ItemReward(String itemId, int count) {}

    /** path 部分（データパック上のクエスト ID）。 */
    public String questId() {
        return this.id.getPath();
    }

    public int requiredCount() {
        return Math.max(1, this.count);
    }

    public static QuestDefinition parse(Identifier id, JsonObject json) {
        String path = id.getPath();
        QuestType type = QuestType.parse(StoryJson.string(json, "type", ""));
        if (type == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] quest '{}' has an unknown type; ignored", id);
            return null;
        }
        int count = Math.max(1, StoryJson.integer(json, "count", 1));
        List<StoryCondition> requires = new ArrayList<>();
        for (JsonObject condition : StoryJson.objectList(json, "requires")) {
            requires.add(StoryCondition.parse(condition));
        }
        int xp = 0;
        List<ItemReward> rewards = new ArrayList<>();
        JsonObject reward = StoryJson.object(json, "reward");
        if (reward != null) {
            xp = StoryJson.integer(reward, "xp", 0);
            for (JsonObject item : StoryJson.objectList(reward, "items")) {
                String itemId = StoryJson.string(item, "item", "");
                int itemCount = Math.max(1, StoryJson.integer(item, "count", 1));
                if (!itemId.isBlank()) {
                    if (StoryItems.resolve(itemId) == null) {
                        com.nocterra.Nocterra.LOG.warn(
                                "[NOCTERRA] quest '{}' rewards unknown item '{}'; skipped", id, itemId);
                        continue;
                    }
                    rewards.add(new ItemReward(itemId, itemCount));
                }
            }
        }
        return new QuestDefinition(
                id,
                StoryJson.string(json, "title", "quest.nocterra." + path + ".title"),
                StoryJson.string(json, "description", "quest.nocterra." + path + ".description"),
                type,
                StoryJson.string(json, "target", ""),
                count,
                StoryJson.string(json, "giver", ""),
                List.copyOf(requires),
                Math.max(0, xp),
                List.copyOf(rewards));
    }
}

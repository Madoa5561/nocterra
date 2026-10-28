package com.nocterra.quest.dialogue;

import com.google.gson.JsonObject;
import com.nocterra.quest.StoryCondition;
import com.nocterra.quest.StoryContext;
import com.nocterra.quest.StoryJson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * データ駆動の会話ツリー（{@code data/nocterra/dialogue/<npc>.json}）。所有: story 担当。
 *
 * <pre>{@code
 * {
 *   "npc": "warden_of_ash",
 *   "entries": [
 *     { "node": "after_king", "conditions": [ { "type": "quest", "quest": "rotwood_king_fall", "state": "completed" } ] },
 *     { "node": "first",      "conditions": [ { "type": "first_meet" } ] },
 *     { "node": "root" }
 *   ],
 *   "nodes": {
 *     "root": {
 *       "text": "dialogue.nocterra.warden_of_ash.root",
 *       "choices": [
 *         { "text": "dialogue.nocterra.common.farewell", "action": { "type": "close" } }
 *       ]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>会話開始時、{@code entries} を上から順に見て<b>最初に条件を満たしたノード</b>から始める。
 * 条件の無い entry を最後に置くと既定分岐になる。</p>
 */
public final class DialogueDefinition {
    public String npcId;
    public final List<Entry> entries = new ArrayList<>();
    public final Map<String, Node> nodes = new LinkedHashMap<>();

    public record Entry(String node, List<StoryCondition> conditions) {}

    public record ChoiceSpec(String textKey, String next, List<StoryCondition> conditions, StoryAction action) {}

    public record Node(String textKey, List<StoryAction> onEnter, List<ChoiceSpec> choices) {}

    public Node node(String nodeId) {
        return this.nodes.get(nodeId);
    }

    /** 開始ノードを決める（条件を満たす最初の entry）。 */
    public String chooseStartNode(StoryContext context) {
        for (Entry entry : this.entries) {
            if (StoryCondition.testAll(entry.conditions(), context)) {
                return entry.node();
            }
        }
        return null;
    }

    public static DialogueDefinition parse(String npcId, JsonObject json) {
        DialogueDefinition definition = new DialogueDefinition();
        definition.npcId = StoryJson.string(json, "npc", npcId);

        String start = StoryJson.string(json, "start", "root");
        List<JsonObject> entries = StoryJson.objectList(json, "entries");
        if (entries.isEmpty()) {
            definition.entries.add(new Entry(start, List.of()));
        } else {
            for (JsonObject entryJson : entries) {
                String node = StoryJson.string(entryJson, "node", start);
                List<StoryCondition> conditions = new ArrayList<>();
                for (JsonObject condition : StoryJson.objectList(entryJson, "conditions")) {
                    conditions.add(StoryCondition.parse(condition));
                }
                definition.entries.add(new Entry(node, List.copyOf(conditions)));
            }
        }

        JsonObject nodesJson = StoryJson.object(json, "nodes");
        if (nodesJson == null) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] dialogue '{}' has no nodes", npcId);
            return definition;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry : nodesJson.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            definition.nodes.put(entry.getKey(), parseNode(entry.getValue().getAsJsonObject()));
        }
        return definition;
    }

    private static Node parseNode(JsonObject json) {
        String textKey = StoryJson.string(json, "text", "");
        List<StoryAction> onEnter = new ArrayList<>();
        for (JsonObject action : StoryJson.objectList(json, "onEnter")) {
            StoryAction parsed = StoryAction.parse(action);
            if (parsed != null) {
                onEnter.add(parsed);
            }
        }
        List<ChoiceSpec> choices = new ArrayList<>();
        for (JsonObject choiceJson : StoryJson.objectList(json, "choices")) {
            String text = StoryJson.string(choiceJson, "text", "");
            if (text.isBlank()) {
                continue;
            }
            List<StoryCondition> conditions = new ArrayList<>();
            for (JsonObject condition : StoryJson.objectList(choiceJson, "conditions")) {
                conditions.add(StoryCondition.parse(condition));
            }
            StoryAction action = null;
            JsonObject actionJson = StoryJson.object(choiceJson, "action");
            if (actionJson != null) {
                action = StoryAction.parse(actionJson);
            }
            String next = StoryJson.string(choiceJson, "next", "");
            choices.add(new ChoiceSpec(text, next.isBlank() ? null : next, List.copyOf(conditions), action));
        }
        return new Node(textKey, List.copyOf(onEnter), List.copyOf(choices));
    }
}

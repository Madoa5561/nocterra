package com.nocterra.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nocterra.Nocterra;
import com.nocterra.quest.dialogue.DialogueDefinition;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * データパックから会話・クエストをロードするリロードリスナ。所有: story 担当。
 *
 * <p>対象:</p>
 * <ul>
 *   <li>{@code data/nocterra/dialogue/<npc_id>.json} → {@link DialogueDefinition}</li>
 *   <li>{@code data/nocterra/quest/<quest_id>.json} → {@link QuestDefinition}</li>
 * </ul>
 *
 * <p>登録は {@code AddServerReloadListenersEvent}（{@link StoryEvents#onAddReloadListener}）。
 * {@code /reload} で即時反映される。</p>
 */
public final class StoryReloadListener extends SimplePreparableReloadListener<StoryReloadListener.Prepared> {

    public record Prepared(Map<String, QuestDefinition> quests,
                           Map<String, DialogueDefinition> dialogues) {}

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, QuestDefinition> quests = new LinkedHashMap<>();
        Map<String, DialogueDefinition> dialogues = new LinkedHashMap<>();

        for (Map.Entry<Identifier, Resource> entry
                : manager.listResources("quest", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier fileId = entry.getKey();
            String path = strip(fileId.getPath(), "quest/");
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                QuestDefinition definition = QuestDefinition.parse(
                        Identifier.fromNamespaceAndPath(fileId.getNamespace(), path), json);
                if (definition != null) {
                    quests.put(path, definition);
                }
            } catch (IOException | RuntimeException e) {
                Nocterra.LOG.error("[NOCTERRA] failed to load quest '{}': {}", fileId, e.toString());
            }
        }

        for (Map.Entry<Identifier, Resource> entry
                : manager.listResources("dialogue", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier fileId = entry.getKey();
            String path = strip(fileId.getPath(), "dialogue/");
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                dialogues.put(path, DialogueDefinition.parse(path, json));
            } catch (IOException | RuntimeException e) {
                Nocterra.LOG.error("[NOCTERRA] failed to load dialogue '{}': {}", fileId, e.toString());
            }
        }

        return new Prepared(Map.copyOf(quests), Map.copyOf(dialogues));
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        StoryRepository.replace(prepared.quests(), prepared.dialogues());
    }

    /** {@code "quest/foo.json"} → {@code "foo"}。 */
    private static String strip(String path, String prefix) {
        String value = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
        return value.endsWith(".json") ? value.substring(0, value.length() - 5) : value;
    }
}

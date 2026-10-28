package com.nocterra.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * プレイヤーごとの「物語」データ。所有: story 担当（NPC / 会話 / クエスト）。
 *
 * <p>ここに入るのは <b>クエストの受注状態・進行度・解放済み知識（lore）・物語フラグ</b>。
 * 発見した NPC と完了したクエストは {@link com.nocterra.progression.ProgressionAttachments}
 * （Lead 所有の進行データ）にもミラーする。刻印（sigil）は Lead 側が唯一の持ち主で、
 * 本クラスは一切保持しない。</p>
 *
 * <p>サーバー権威。クライアントには同期しない（会話 UI はサーバーがノード単位で送る）。</p>
 */
public final class StoryData {
    /** questId(path) -> 進行度。 */
    private final Map<String, Integer> activeQuests = new LinkedHashMap<>();
    private final Set<String> discoveredNpcs = new LinkedHashSet<>();
    private final Set<String> completedQuests = new LinkedHashSet<>();
    private final Set<String> lore = new LinkedHashSet<>();
    private final Set<String> flags = new LinkedHashSet<>();

    public StoryData() {}

    // ------------------------------------------------------------------ codec

    public static final com.mojang.serialization.MapCodec<StoryData> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("active", Map.of())
                            .forGetter(data -> Map.copyOf(data.activeQuests)),
                    Codec.STRING.listOf().optionalFieldOf("npcs", List.of())
                            .forGetter(data -> List.copyOf(data.discoveredNpcs)),
                    Codec.STRING.listOf().optionalFieldOf("completed", List.of())
                            .forGetter(data -> List.copyOf(data.completedQuests)),
                    Codec.STRING.listOf().optionalFieldOf("lore", List.of())
                            .forGetter(data -> List.copyOf(data.lore)),
                    Codec.STRING.listOf().optionalFieldOf("flags", List.of())
                            .forGetter(data -> List.copyOf(data.flags))
            ).apply(instance, StoryData::fromCodec));

    /** MapCodec の {@link Codec} 版（必要な箇所向け）。 */
    public static final Codec<StoryData> CODEC = MAP_CODEC.codec();

    private static StoryData fromCodec(Map<String, Integer> active, List<String> npcs,
                                       List<String> completed, List<String> lore, List<String> flags) {
        StoryData data = new StoryData();
        data.activeQuests.putAll(active);
        data.discoveredNpcs.addAll(npcs);
        data.completedQuests.addAll(completed);
        data.lore.addAll(lore);
        data.flags.addAll(flags);
        return data;
    }

    // ------------------------------------------------------------------ クエスト

    public Map<String, Integer> activeQuests() {
        return Collections.unmodifiableMap(this.activeQuests);
    }

    public boolean isActive(String questId) {
        return this.activeQuests.containsKey(questId);
    }

    public int progress(String questId) {
        return this.activeQuests.getOrDefault(questId, 0);
    }

    /** 受注。既に受注済みなら false。 */
    public boolean startQuest(String questId) {
        if (this.activeQuests.containsKey(questId) || this.completedQuests.contains(questId)) {
            return false;
        }
        this.activeQuests.put(questId, 0);
        return true;
    }

    /** 進行度を設定する（減少は許可しない）。@return 変化があったら true */
    public boolean setProgress(String questId, int progress) {
        Integer current = this.activeQuests.get(questId);
        if (current == null || progress <= current) {
            return false;
        }
        this.activeQuests.put(questId, progress);
        return true;
    }

    /** 受注状態から外す（完了記録は {@link #markQuestCompleted} で行う）。 */
    public void removeQuest(String questId) {
        this.activeQuests.remove(questId);
    }

    public void resetQuest(String questId) {
        this.activeQuests.remove(questId);
        this.completedQuests.remove(questId);
    }

    // ------------------------------------------------------------------ NPC 発見

    public Set<String> discoveredNpcs() {
        return Collections.unmodifiableSet(this.discoveredNpcs);
    }

    public boolean isNpcDiscovered(String npcId) {
        return this.discoveredNpcs.contains(npcId);
    }

    /** @return 新規に記録した場合 true */
    public boolean markNpcDiscovered(String npcId) {
        return this.discoveredNpcs.add(npcId);
    }

    // ------------------------------------------------------------------ 完了クエスト

    public Set<String> completedQuests() {
        return Collections.unmodifiableSet(this.completedQuests);
    }

    public boolean isQuestCompleted(String questId) {
        return this.completedQuests.contains(questId);
    }

    public boolean markQuestCompleted(String questId) {
        this.activeQuests.remove(questId);
        return this.completedQuests.add(questId);
    }

    // ------------------------------------------------------------------ lore / フラグ

    public Set<String> lore() {
        return Collections.unmodifiableSet(this.lore);
    }

    public boolean hasLore(String loreId) {
        return this.lore.contains(loreId);
    }

    /** @return 新規に解放した場合 true */
    public boolean unlockLore(String loreId) {
        return this.lore.add(loreId);
    }

    public Set<String> flags() {
        return Collections.unmodifiableSet(this.flags);
    }

    public boolean hasFlag(String flag) {
        return this.flags.contains(flag);
    }

    public boolean setFlag(String flag) {
        return this.flags.add(flag);
    }

    public void clearFlag(String flag) {
        this.flags.remove(flag);
    }
}

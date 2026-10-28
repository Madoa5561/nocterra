package com.nocterra.quest;

import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

/**
 * データ駆動の条件 1 つ。所有: story 担当。
 *
 * <p>会話の選択肢・ノード分岐・クエストの受注条件のすべてがこの 1 形式を使う。
 * JSON では {@code {"type": "...", ...}}。複数指定した場合は AND。</p>
 *
 * <table>
 *   <tr><th>type</th><th>追加フィールド</th><th>意味</th></tr>
 *   <tr><td>quest</td><td>quest, state(active|completed|not_started|not_completed)</td><td>クエスト状態</td></tr>
 *   <tr><td>seals</td><td>min</td><td>王の刻印の所持数（Lead の進行データ）</td></tr>
 *   <tr><td>boss_defeated</td><td>boss</td><td>ボス撃破済み（Lead の進行データ）</td></tr>
 *   <tr><td>first_meet</td><td>-</td><td>この会話で初めて会った</td></tr>
 *   <tr><td>flag / flag_not</td><td>flag</td><td>物語フラグ</td></tr>
 *   <tr><td>lore / lore_not</td><td>lore</td><td>知識の解放</td></tr>
 *   <tr><td>has_item</td><td>item, count</td><td>アイテム所持</td></tr>
 *   <tr><td>npc_met</td><td>npc</td><td>その NPC に会ったことがある</td></tr>
 *   <tr><td>always</td><td>-</td><td>常に真</td></tr>
 * </table>
 */
public record StoryCondition(String type, String quest, String state, String flag, String lore,
                             String boss, String item, String npc, int count) {

    public static StoryCondition parse(JsonObject json) {
        String type = StoryJson.string(json, "type", "always");
        return new StoryCondition(
                type,
                StoryJson.string(json, "quest", ""),
                StoryJson.string(json, "state", "active"),
                StoryJson.string(json, "flag", ""),
                StoryJson.string(json, "lore", ""),
                StoryJson.string(json, "boss", ""),
                StoryJson.string(json, "item", ""),
                StoryJson.string(json, "npc", ""),
                StoryJson.integer(json, "count", StoryJson.integer(json, "min", 1)));
    }

    public boolean test(StoryContext context) {
        StoryData story = context.story();
        return switch (this.type) {
            case "quest" -> switch (this.state) {
                case "completed" -> story.isQuestCompleted(this.quest);
                case "not_completed" -> !story.isQuestCompleted(this.quest);
                case "not_started" -> !story.isActive(this.quest) && !story.isQuestCompleted(this.quest);
                default -> story.isActive(this.quest);
            };
            case "seals" -> StoryProgression.sealCount(context.player()) >= this.count;
            case "boss_defeated" -> StoryProgression.isBossDefeated(context.player(), this.boss);
            case "first_meet" -> context.npcId() != null && !context.npcWasDiscovered();
            case "flag" -> story.hasFlag(this.flag);
            case "flag_not" -> !story.hasFlag(this.flag);
            case "lore" -> story.hasLore(this.lore);
            case "lore_not" -> !story.hasLore(this.lore);
            case "has_item" -> {
                Item resolved = StoryItems.resolve(this.item);
                yield resolved != null && StoryItems.count(context.player(), resolved) >= this.count;
            }
            case "npc_met" -> story.isNpcDiscovered(this.npc);
            case "always" -> true;
            default -> {
                com.nocterra.Nocterra.LOG.warn("[NOCTERRA] unknown story condition type '{}' -> treated as false",
                        this.type);
                yield false;
            }
        };
    }

    public static boolean testAll(List<StoryCondition> conditions, StoryContext context) {
        for (StoryCondition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 便利メソッド（クエスト側から使う）

    /** 現在のバイオーム ID（{@code nocterra:mourning_marsh} 形式）。 */
    public static String biomeId(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
    }

    /** レジストリ ID を正規化する（namespace 省略時は nocterra とみなす）。 */
    public static String normalize(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "";
        }
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            id = Identifier.tryBuild("nocterra", rawId);
        }
        return id == null ? rawId : id.toString();
    }

    /** バイオーム条件用の正規化 ID（{@link Biome} のキーと比較する）。 */
    public static String biomeKey(String rawId) {
        return normalize(rawId);
    }
}

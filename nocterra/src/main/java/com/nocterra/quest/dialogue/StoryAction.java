package com.nocterra.quest.dialogue;

import com.google.gson.JsonObject;
import com.nocterra.quest.StoryJson;

/**
 * 会話で実行できるアクション（{@code data/nocterra/dialogue/*.json} の {@code action} / {@code onEnter}）。
 * 所有: story 担当。
 *
 * <p>すべて <b>サーバー側で検証してから</b>実行する。クライアントは「選択肢を送る」だけで、
 * 何が起きるかを決めない。</p>
 *
 * <table>
 *   <tr><th>type</th><th>フィールド</th><th>効果</th></tr>
 *   <tr><td>quest_offer</td><td>quest</td><td>クエスト受注（受注条件を満たさなければ失敗）</td></tr>
 *   <tr><td>quest_complete</td><td>quest</td><td>クエスト完了（報酬付与）</td></tr>
 *   <tr><td>give_item</td><td>item, count</td><td>既存アイテムを渡す</td></tr>
 *   <tr><td>lore_unlock</td><td>lore</td><td>知識を解放し、チャットで語る</td></tr>
 *   <tr><td>trade</td><td>cost:{item,count}, result:{item,count}</td><td>所持品を消費して交換</td></tr>
 *   <tr><td>flag</td><td>flag</td><td>物語フラグを立てる</td></tr>
 *   <tr><td>npc_action</td><td>action</td><td>NPC 固有の動作（例: follow_player / open_black_gate）</td></tr>
 *   <tr><td>message</td><td>text</td><td>ナレーションをチャットに出す</td></tr>
 *   <tr><td>close</td><td>-</td><td>会話を閉じる</td></tr>
 * </table>
 */
public record StoryAction(String type,
                          String quest,
                          String item,
                          int count,
                          String costItem,
                          int costCount,
                          String resultItem,
                          int resultCount,
                          String flag,
                          String lore,
                          String messageKey,
                          String action) {

    public static StoryAction parse(JsonObject json) {
        String type = StoryJson.string(json, "type", "");
        if (type.isBlank()) {
            com.nocterra.Nocterra.LOG.warn("[NOCTERRA] dialogue action without type; ignored");
            return null;
        }
        String costItem = "";
        int costCount = 0;
        JsonObject cost = StoryJson.object(json, "cost");
        if (cost != null) {
            costItem = StoryJson.string(cost, "item", "");
            costCount = StoryJson.integer(cost, "count", 1);
        }
        String resultItem = "";
        int resultCount = 0;
        JsonObject result = StoryJson.object(json, "result");
        if (result != null) {
            resultItem = StoryJson.string(result, "item", "");
            resultCount = StoryJson.integer(result, "count", 1);
        }
        return new StoryAction(
                type,
                StoryJson.string(json, "quest", ""),
                StoryJson.string(json, "item", ""),
                Math.max(1, StoryJson.integer(json, "count", 1)),
                costItem,
                costCount,
                resultItem,
                resultCount,
                StoryJson.string(json, "flag", ""),
                StoryJson.string(json, "lore", ""),
                StoryJson.string(json, "text", ""),
                StoryJson.string(json, "action", ""));
    }

    public boolean isClose() {
        return "close".equals(this.type);
    }
}

package com.nocterra.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * データパック JSON の読み取りヘルパ。所有: story 担当。
 *
 * <p>会話・クエスト JSON は「人が書くもの」なので、欠けているフィールドで
 * 例外にせず既定値へフォールバックさせる（ロード時に警告を出す）。</p>
 */
public final class StoryJson {
    private StoryJson() {}

    public static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsString();
    }

    public static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        return element.getAsBoolean();
    }

    public static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public static List<JsonObject> objectList(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<JsonObject> out = new ArrayList<>(array.size());
        for (JsonElement child : array) {
            if (child.isJsonObject()) {
                out.add(child.getAsJsonObject());
            }
        }
        return out;
    }
}

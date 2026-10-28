package com.nocterra.quest;

/**
 * クエストの達成条件種別。所有: story 担当。
 */
public enum QuestType {
    /** 指定エンティティを N 体倒す。 */
    KILL_ENTITY,
    /** 指定アイテムを N 個所持する（消費はしない）。 */
    COLLECT_ITEM,
    /** 指定バイオームに足を踏み入れる。 */
    VISIT_BIOME,
    /** 指定 NPC と話す（会話 UI を開いた時点で達成）。 */
    TALK_TO_NPC,
    /** 指定ボスを撃破する。 */
    DEFEAT_BOSS;

    /** 未知の種別は {@code null}（ロード時に警告する）。 */
    public static QuestType parse(String raw) {
        if (raw == null) {
            return null;
        }
        for (QuestType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }
}

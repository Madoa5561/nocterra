package com.nocterra.quest;

import com.nocterra.Nocterra;
import com.nocterra.progression.ProgressionAttachments;
import com.nocterra.progression.ProgressionData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lead 所有の進行データ（{@code com.nocterra.progression}）への橋渡し。所有: story 担当。
 *
 * <p>責務は 3 つだけ:
 * <ul>
 *   <li><b>刻印数の参照</b>（会話条件 {@code seals} / 門番の城門開放条件）。刻印の持ち主は Lead。</li>
 *   <li><b>発見 NPC・完了クエストのミラー</b>。Lead の探索手帳（{@code NocterraStoryCommands} の
 *       {@code status}）に、story 側の NPC / クエストがそのまま数えられるようにする。</li>
 *   <li>ボス撃破済み判定（{@code boss_defeated} 条件）。</li>
 * </ul>
 *
 * <p>統合の安全弁: {@code ProgressionAttachments} がまだ {@code Nocterra} に登録されていない
 * 環境（Lead の実装途中・単体テスト等）でも落ちないよう、初回アクセスで登録済みかを判定し、
 * 未登録なら「刻印 0・未撃破」として扱う（1 度だけ WARN ログ）。</p>
 */
public final class StoryProgression {
    private StoryProgression() {}

    /** null = 未判定 / TRUE = 利用可 / FALSE = 未登録。 */
    private static Boolean available;
    private static boolean warned;

    /** Lead 側の進行データ。未登録環境では {@code null}。 */
    public static ProgressionData of(ServerPlayer player) {
        if (Boolean.FALSE.equals(available)) {
            return null;
        }
        try {
            ProgressionData data = ProgressionAttachments.get(player);
            available = Boolean.TRUE;
            return data;
        } catch (RuntimeException e) {
            available = Boolean.FALSE;
            if (!warned) {
                warned = true;
                Nocterra.LOG.warn("[NOCTERRA] progression attachment is not registered yet; "
                        + "story features fall back to 0 sigils / unknown bosses ({})", e.toString());
            }
            return null;
        }
    }

    /** 刻印の所持数（Lead のデータが無ければ 0）。 */
    public static int sealCount(ServerPlayer player) {
        ProgressionData data = of(player);
        return data == null ? 0 : data.sigilCount();
    }

    public static boolean isBossDefeated(ServerPlayer player, String bossId) {
        ProgressionData data = of(player);
        return data != null && data.defeatedBosses().contains(bossId);
    }

    /** 発見 NPC を Lead の進行データへミラーする（ベストエフォート）。 */
    public static void mirrorNpcDiscovered(ServerPlayer player, String npcId) {
        ProgressionData data = of(player);
        if (data != null) {
            data.addDiscoveredNpc(npcId);
        }
    }

    /** 完了クエストを Lead の進行データへミラーする（ベストエフォート）。 */
    public static void mirrorQuestCompleted(ServerPlayer player, String questId) {
        ProgressionData data = of(player);
        if (data != null) {
            data.addCompletedQuest(questId);
        }
    }
}

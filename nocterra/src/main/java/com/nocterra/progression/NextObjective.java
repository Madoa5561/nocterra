package com.nocterra.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * プレイヤーに「次に何をすべきか」を提示する。所有: Lead。
 *
 * <p>物語が分かりにくいという声に応え、刻印数・クエスト・エンディング閲覧状況から
 * 次の目的を 1 行で出す。探索手帳と入場時のガイドで使う。</p>
 */
public final class NextObjective {
    private NextObjective() {}

    /** サーバー側の進行データから。 */
    public static MutableComponent describe(ProgressionData data) {
        return describe(data.sigilCount(), SigilType.values().length,
                data.trueEndingSeen(), data.normalEndingSeen(),
                data.completedQuests().size(), data.discoveredNpcs().size());
    }

    /** クライアント（手帳GUI）でも使えるプリミティブ版。 */
    public static MutableComponent describe(int sigils, int total, boolean trueEnding,
                                     boolean normalEnding, int quests, int npcs) {
        if (trueEnding) {
            return Component.translatable("guide.nocterra.done");
        }
        if (sigils >= total) {
            return Component.translatable("guide.nocterra.true_ready");
        }
        if (sigils >= 5) {
            return Component.translatable("guide.nocterra.normal_ready", total - sigils);
        }
        if (quests == 0 && npcs == 0) {
            return Component.translatable("guide.nocterra.find_npc", 5 - sigils);
        }
        return Component.translatable("guide.nocterra.collect_sigils", 5 - sigils);
    }
}

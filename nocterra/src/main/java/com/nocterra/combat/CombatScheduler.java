package com.nocterra.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * サーバー tick 上の遅延実行キュー。3 連斬りのような「時間差ヒット」と
 * 大剣の「溜め後の発動」に使う。
 *
 * <p>サーバーメインスレッドからのみ触る。ログアウト時は {@link #clear(ServerPlayer)} で破棄し、
 * 実行時にもプレイヤーの生存を確認するので、切断済みプレイヤーへ作用することはない。</p>
 */
public final class CombatScheduler {
    private CombatScheduler() {}

    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    private static final class ScheduledTask {
        private final ServerPlayer player;
        private final Consumer<ServerPlayer> action;
        private int remainingTicks;

        private ScheduledTask(ServerPlayer player, int delayTicks, Consumer<ServerPlayer> action) {
            this.player = player;
            this.action = action;
            this.remainingTicks = Math.max(0, delayTicks);
        }
    }

    /**
     * @param delayTicks 0 の場合は次のサーバー tick で実行される
     */
    public static void schedule(ServerPlayer player, int delayTicks, Consumer<ServerPlayer> action) {
        TASKS.add(new ScheduledTask(player, delayTicks, action));
    }

    /** {@code ServerTickEvent.Post} から毎 tick 呼ぶ。 */
    public static void tick() {
        if (TASKS.isEmpty()) {
            return;
        }

        List<ScheduledTask> due = new ArrayList<>();
        Iterator<ScheduledTask> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            ServerPlayer player = task.player;
            if (player.hasDisconnected() || player.isRemoved() || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            if (--task.remainingTicks <= 0) {
                iterator.remove();
                due.add(task);
            }
        }

        // 実行中に schedule() されてもリスト走査が壊れないよう、いったん外へ出してから実行する。
        for (ScheduledTask task : due) {
            task.action.accept(task.player);
        }
    }

    /** プレイヤーに紐づく未実行タスクを破棄する。 */
    public static void clear(ServerPlayer player) {
        TASKS.removeIf(task -> task.player == player);
    }

    /** サーバー停止時など。 */
    public static void clearAll() {
        TASKS.clear();
    }

    /** デバッグ用。 */
    public static int pendingTasks() {
        return TASKS.size();
    }
}

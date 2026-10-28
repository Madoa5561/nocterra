package com.nocterra.combat;

import com.nocterra.combat.network.CombatSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * サーバー → クライアント同期の送信ヘルパー。
 *
 * <p>毎 tick 送るとパケット過多になるため、定期同期は
 * {@link CombatBalance#syncIntervalTicks} 間隔、アクション直後は即時送信する。</p>
 */
public final class CombatSync {
    private CombatSync() {}

    public static void sync(ServerPlayer player) {
        CombatData data = CombatAttachments.get(player);
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(
                (float) data.getStamina(),
                (float) data.getMana(),
                (float) CombatBalance.maxStamina,
                (float) CombatBalance.maxMana,
                data.getComboHits(),
                (float) data.getComboMultiplier(),
                data.getParryTicks() > 0,
                data.getDodgeCooldown(),
                data.getHeavyCooldown()));
        data.setSyncTimer(0);
    }

    /** 同期間隔を迎えていたら送る（{@link CombatHandler} の tick から呼ぶ）。 */
    public static void syncIfDue(ServerPlayer player) {
        CombatData data = CombatAttachments.get(player);
        if (data.getSyncTimer() >= CombatBalance.syncIntervalTicks) {
            sync(player);
        } else {
            data.setSyncTimer(data.getSyncTimer() + 1);
        }
    }
}

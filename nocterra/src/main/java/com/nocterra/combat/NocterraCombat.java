package com.nocterra.combat;

import net.neoforged.bus.api.IEventBus;

/**
 * 戦闘システム（スタミナ／魔力／回避／パリィ／コンボ／戦技）の登録点。所有: combat 担当。
 *
 * <p>サーバー権威。クライアントは表示のみ。追加するもの:</p>
 * <ul>
 *   <li>AttachmentType（プレイヤーごとのスタミナ・魔力・侵食値など）</li>
 *   <li>Attribute（最大スタミナ等）</li>
 *   <li>Payload（HUD 同期）→ {@link com.nocterra.networking.NocterraNetwork} に追記</li>
 * </ul>
 */
public final class NocterraCombat {
    private NocterraCombat() {}

    public static void register(IEventBus modBus) {
        // combat 担当がここに登録を追加する
    }
}

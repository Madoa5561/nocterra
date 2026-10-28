package com.nocterra.client;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * キーバインド登録。所有: combat 担当。
 *
 * <p>既定キー: 回避 = R、強攻撃 = V、パリィ = F、戦技 = G、魔法 = C。
 * プレイヤーが設定画面で変更できるようにする（KeyMapping を使う）。</p>
 */
public final class NocterraKeybinds {
    private NocterraKeybinds() {}

    public static void register(RegisterKeyMappingsEvent event) {
        // combat 担当がここに KeyMapping を登録する
    }
}

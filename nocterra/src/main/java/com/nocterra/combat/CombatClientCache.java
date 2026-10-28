package com.nocterra.combat;

import com.nocterra.combat.network.CombatSyncPayload;

/**
 * クライアント側 HUD 表示用キャッシュ。サーバーから届いた
 * {@link CombatSyncPayload} の内容をそのまま保持するだけの入れ物。
 *
 * <p>ここにはクライアント専用クラス（{@code Minecraft} 等）への参照を置かない。
 * そうすることで payload ハンドラ（共通コード）から安全に呼べる。
 * 描画は {@code com.nocterra.client.hud.CombatHud} が担当する。</p>
 */
public final class CombatClientCache {
    private CombatClientCache() {}

    private static float stamina = (float) CombatBalance.maxStamina;
    private static float mana = (float) CombatBalance.maxMana;
    private static float maxStamina = (float) CombatBalance.maxStamina;
    private static float maxMana = (float) CombatBalance.maxMana;
    private static int comboHits;
    private static float comboMultiplier = 1.0F;
    private static boolean parryActive;
    private static int dodgeCooldown;
    private static int heavyCooldown;

    /** サーバー同期の適用。 */
    public static void apply(CombatSyncPayload payload) {
        stamina = payload.stamina();
        mana = payload.mana();
        maxStamina = payload.maxStamina();
        maxMana = payload.maxMana();
        comboHits = payload.comboHits();
        comboMultiplier = payload.comboMultiplier();
        parryActive = payload.parryActive();
        dodgeCooldown = payload.dodgeCooldown();
        heavyCooldown = payload.heavyCooldown();
    }

    /** 切断時などに既定値へ戻す。 */
    public static void reset() {
        stamina = (float) CombatBalance.maxStamina;
        mana = (float) CombatBalance.maxMana;
        maxStamina = (float) CombatBalance.maxStamina;
        maxMana = (float) CombatBalance.maxMana;
        comboHits = 0;
        comboMultiplier = 1.0F;
        parryActive = false;
        dodgeCooldown = 0;
        heavyCooldown = 0;
    }

    public static float getStamina() {
        return stamina;
    }

    public static float getMana() {
        return mana;
    }

    public static float getMaxStamina() {
        return maxStamina;
    }

    public static float getMaxMana() {
        return maxMana;
    }

    public static int getComboHits() {
        return comboHits;
    }

    public static float getComboMultiplier() {
        return comboMultiplier;
    }

    public static boolean isParryActive() {
        return parryActive;
    }

    public static int getDodgeCooldown() {
        return dodgeCooldown;
    }

    public static int getHeavyCooldown() {
        return heavyCooldown;
    }
}

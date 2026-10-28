package com.nocterra.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * プレイヤーごとの戦闘状態。{@link CombatAttachments#COMBAT_DATA} として
 * {@code AttachmentType} に保持され、サーバー権威で更新される。
 *
 * <p>永続化されるのはスタミナ／魔力のみ。回避・パリィ・コンボ等のタイマーは
 * 一時状態として扱い、再ログイン時は 0 から始まる。</p>
 */
public final class CombatData {
    /** 永続化用 codec。スタミナ／魔力のみを保存する。 */
    public static final MapCodec<CombatData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.optionalFieldOf("stamina", 100.0).forGetter(CombatData::getStamina),
                    Codec.DOUBLE.optionalFieldOf("mana", 100.0).forGetter(CombatData::getMana))
            .apply(instance, CombatData::new));

    // ---- 永続化対象 ----
    private double stamina;
    private double mana;

    // ---- 一時状態（保存しない） ----
    private int regenTimer;
    private int dodgeTicks;
    private int dodgeCooldown;
    private int invulnerableTicks;
    private Vec3 dodgeDirection = Vec3.ZERO;
    private int parryTicks;
    private int parryCooldown;
    private int heavyCooldown;
    private int comboHits;
    private int comboWindow;
    private String lastWeapon = "";
    private int syncTimer;

    /** 新規プレイヤー用。 */
    public CombatData() {
        this(CombatBalance.maxStamina, CombatBalance.maxMana);
    }

    private CombatData(double stamina, double mana) {
        this.stamina = clamp(stamina, 0.0, CombatBalance.maxStamina);
        this.mana = clamp(mana, 0.0, CombatBalance.maxMana);
    }

    // ---------------------------------------------------------------- リソース

    public double getStamina() {
        return this.stamina;
    }

    public void setStamina(double value) {
        this.stamina = clamp(value, 0.0, CombatBalance.maxStamina);
    }

    public double getMana() {
        return this.mana;
    }

    public void setMana(double value) {
        this.mana = clamp(value, 0.0, CombatBalance.maxMana);
    }

    public boolean hasStamina(double amount) {
        return this.stamina >= amount;
    }

    /** 消費できたら true。足りなければ何もせず false。 */
    public boolean spendStamina(double amount) {
        if (amount < 0.0 || !hasStamina(amount)) {
            return false;
        }
        this.stamina -= amount;
        return true;
    }

    public boolean spendMana(double amount) {
        if (amount < 0.0 || this.mana < amount) {
            return false;
        }
        this.mana -= amount;
        return true;
    }

    public void addStamina(double amount) {
        this.stamina = clamp(this.stamina + amount, 0.0, CombatBalance.maxStamina);
    }

    public void addMana(double amount) {
        this.mana = clamp(this.mana + amount, 0.0, CombatBalance.maxMana);
    }

    // ---------------------------------------------------------------- タイマー

    public int getRegenTimer() {
        return this.regenTimer;
    }

    public void setRegenTimer(int value) {
        this.regenTimer = value;
    }

    public int getDodgeTicks() {
        return this.dodgeTicks;
    }

    public void setDodgeTicks(int value) {
        this.dodgeTicks = Math.max(0, value);
    }

    public int getDodgeCooldown() {
        return this.dodgeCooldown;
    }

    public void setDodgeCooldown(int value) {
        this.dodgeCooldown = Math.max(0, value);
    }

    public int getInvulnerableTicks() {
        return this.invulnerableTicks;
    }

    public void setInvulnerableTicks(int value) {
        this.invulnerableTicks = Math.max(0, value);
    }

    public Vec3 getDodgeDirection() {
        return this.dodgeDirection;
    }

    public void setDodgeDirection(Vec3 direction) {
        this.dodgeDirection = direction;
    }

    public int getParryTicks() {
        return this.parryTicks;
    }

    public void setParryTicks(int value) {
        this.parryTicks = Math.max(0, value);
    }

    public int getParryCooldown() {
        return this.parryCooldown;
    }

    public void setParryCooldown(int value) {
        this.parryCooldown = Math.max(0, value);
    }

    public int getHeavyCooldown() {
        return this.heavyCooldown;
    }

    public void setHeavyCooldown(int value) {
        this.heavyCooldown = Math.max(0, value);
    }

    public int getSyncTimer() {
        return this.syncTimer;
    }

    public void setSyncTimer(int value) {
        this.syncTimer = value;
    }

    // ---------------------------------------------------------------- コンボ

    public int getComboHits() {
        return this.comboHits;
    }

    public void setComboHits(int value) {
        this.comboHits = Math.max(0, value);
    }

    public int getComboWindow() {
        return this.comboWindow;
    }

    public void setComboWindow(int value) {
        this.comboWindow = Math.max(0, value);
    }

    public String getLastWeapon() {
        return this.lastWeapon;
    }

    public void setLastWeapon(String weaponId) {
        this.lastWeapon = weaponId == null ? "" : weaponId;
    }

    /** 現在のヒット数に対応する倍率（1.0 → 1.1 → 1.25）。 */
    public double getComboMultiplier() {
        double[] multipliers = CombatBalance.comboMultipliers;
        if (multipliers.length == 0) {
            return 1.0;
        }
        return multipliers[Math.min(this.comboHits, multipliers.length - 1)];
    }

    /** コンボを初期化する（武器変更・時間切れ）。 */
    public void resetCombo() {
        this.comboHits = 0;
        this.comboWindow = 0;
        this.lastWeapon = "";
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}

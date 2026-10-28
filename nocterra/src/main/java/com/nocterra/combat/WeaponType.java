package com.nocterra.combat;

/**
 * 武器カテゴリ。強攻撃のスタミナコスト／リーチ／倍率をカテゴリ単位で持つ。
 *
 * <p>26.3 に {@code SwordItem} は存在しないため、武器は {@code Item} 派生 +
 * {@code Item.Properties#sword(ToolMaterial, float, float)} で攻撃力を持たせ、
 * カテゴリ固有の挙動はこの enum と {@link CombatWeapon} で表現する。</p>
 */
public enum WeaponType {
    /** 片手剣。軽快・低コスト。 */
    LIGHT_BLADE("light_blade", 3.0, 15.0, 1.8),
    /** 大剣。高コスト・高倍率。 */
    GREATSWORD("greatsword", 4.0, 35.0, 2.2),
    /** 鎌。中コスト・長リーチ。 */
    SCYTHE("scythe", 3.5, 25.0, 1.9);

    private final String id;
    private final double reach;
    private final double heavyAttackStaminaCost;
    private final double heavyAttackMultiplier;

    WeaponType(String id, double reach, double heavyAttackStaminaCost, double heavyAttackMultiplier) {
        this.id = id;
        this.reach = reach;
        this.heavyAttackStaminaCost = heavyAttackStaminaCost;
        this.heavyAttackMultiplier = heavyAttackMultiplier;
    }

    public String id() {
        return this.id;
    }

    public String translationKey() {
        return "nocterra.weapon_type." + this.id;
    }

    /** 強攻撃・戦技の対象探索リーチ（blocks）。 */
    public double reach() {
        return this.reach;
    }

    /** 強攻撃のスタミナコスト。 */
    public double heavyAttackStaminaCost() {
        return this.heavyAttackStaminaCost;
    }

    /** 強攻撃のダメージ倍率。 */
    public double heavyAttackMultiplier() {
        return this.heavyAttackMultiplier;
    }
}

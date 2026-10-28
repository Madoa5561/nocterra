package com.nocterra.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * NOCTERRA の武器が実装する共通インターフェース。
 *
 * <p>攻撃力そのものは 26.3 の {@code Item.Properties#sword(ToolMaterial, float, float)} が
 * {@code ItemAttributeModifiers}（{@code minecraft:attribute_modifiers}）として持たせる。
 * ここでは「どのカテゴリか」「戦技は何か」「近接ヒット時の追加効果」を表現し、
 * 戦闘計算は {@link CombatActions} / {@link CombatHandler} に集約する（サーバー権威）。</p>
 */
public interface CombatWeapon {
    /** 武器カテゴリ。強攻撃コスト・リーチ・倍率を決める。 */
    WeaponType weaponType();

    /** Data Component 未設定時の既定属性。 */
    WeaponElement defaultElement();

    /** 戦技の再使用クールダウン（tick）。 */
    int skillCooldownTicks();

    /** 戦技のスタミナコスト。魔力消費型の戦技では未使用（0 でよい）。 */
    double skillStaminaCost();

    /**
     * 戦技を発動する（サーバー側でのみ呼ばれる）。
     *
     * <p>スタミナ消費・クールダウン設定は武器側の責任。標準的な戦技は
     * {@link CombatActions#beginSkill(ServerPlayer, ItemStack, CombatWeapon)} を
     * 先頭で呼ぶ。魔力消費型は
     * {@link CombatActions#beginManaSkill(ServerPlayer, ItemStack, double, int)} を使う。
     * 溜め斬りのように解放時コストの戦技は自前で処理してよい。</p>
     *
     * @return 発動したら true。クールダウン中・リソース不足・対象なしなら false
     */
    boolean activateSkill(ServerPlayer player, ItemStack stack);

    /**
     * 近接ヒット時のダメージ倍率（コンボ倍率・強化補正の後に乗算される）。
     * 例: 背後攻撃 +50%、水中 +30%。既定 1.0。
     */
    default double meleeDamageMultiplier(ServerPlayer attacker, LivingEntity target) {
        return 1.0;
    }

    /**
     * 近接ヒット時の追加効果（着火・移動速度低下・ノックバックなど）。
     * ダメージ適用後にサーバー側で呼ばれる。既定は何もしない。
     */
    default void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
    }
}

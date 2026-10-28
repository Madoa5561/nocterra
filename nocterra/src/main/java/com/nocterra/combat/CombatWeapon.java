package com.nocterra.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * NOCTERRA の武器が実装する共通インターフェース。
 *
 * <p>攻撃力そのものは 26.3 の {@code Item.Properties#sword(ToolMaterial, float, float)} が
 * {@code ItemAttributeModifiers}（{@code minecraft:attribute_modifiers}）として持たせる。
 * ここでは「どのカテゴリか」「戦技は何か」だけを表現し、戦闘計算は
 * {@link CombatActions} に集約する（サーバー権威）。</p>
 */
public interface CombatWeapon {
    /** 武器カテゴリ。強攻撃コスト・リーチ・倍率を決める。 */
    WeaponType weaponType();

    /** Data Component 未設定時の既定属性。 */
    WeaponElement defaultElement();

    /** 戦技の再使用クールダウン（tick）。 */
    int skillCooldownTicks();

    /** 戦技のスタミナコスト。 */
    double skillStaminaCost();

    /**
     * 戦技を発動する（サーバー側でのみ呼ばれる）。
     *
     * <p>スタミナ消費・クールダウン設定は武器側の責任。標準的な戦技は
     * {@link CombatActions#beginSkill(ServerPlayer, ItemStack, CombatWeapon)} を
     * 先頭で呼ぶ。溜め斬りのように解放時コストの戦技は自前で処理してよい。</p>
     *
     * @return 発動したら true。クールダウン中・スタミナ不足・対象なしなら false
     */
    boolean activateSkill(ServerPlayer player, ItemStack stack);
}

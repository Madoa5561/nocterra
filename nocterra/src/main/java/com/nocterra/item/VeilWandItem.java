package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.WeaponElement;
import com.nocterra.combat.WeaponType;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * ベールの杖（杖 / 攻撃力 4）。
 *
 * <p>魔力消費 -20%（戦技コスト 20 → 16）。戦技「ベールの波」は周囲 4.5 blocks の
 * 敵すべてに魔法ダメージ（攻撃力 80%）。スタミナではなく<b>魔力</b>を消費する。</p>
 */
public class VeilWandItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 0.8;
    public static final double AREA_RANGE = 4.5;
    /** 魔力消費 -20% 後の実コスト。 */
    public static final double MANA_COST = 16.0;
    /** 表示用の基本魔力コスト（20 × 0.8 = 16）。 */
    public static final double BASE_MANA_COST = 20.0;

    public VeilWandItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.WAND;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 40;
    }

    @Override
    public double skillStaminaCost() {
        // 魔力消費型のためスタミナは使わない
        return 0.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        // 全周（minDot = -1）の敵を対象にする
        List<LivingEntity> targets = CombatActions.targetsInFront(player, AREA_RANGE, -1.0);
        if (targets.isEmpty()) {
            return false;
        }
        if (!CombatActions.beginManaSkill(player, stack, MANA_COST, skillCooldownTicks())) {
            return false;
        }
        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);
        for (LivingEntity target : targets) {
            CombatActions.dealMagicDamage(player, target, damage);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}

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
 * 沼鉄の大剣（大剣 / 攻撃力 12）。沼鉄を主素材とする第 2 段階の武器。
 *
 * <p>戦技「沼鉄の薙ぎ払い」: スタミナ 30 で前方 180 度・4 blocks を攻撃力 140% で薙ぎ払う。
 * 対象が複数いれば全員に当たる。</p>
 */
public class BogIronGreatswordItem extends NocterraWeaponItem {
    /** 戦技の攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 1.4;
    /** 戦技のリーチ。 */
    public static final double SWEEP_RANGE = 4.0;
    /** 戦技の前方判定（0.15 ≒ 反時計回り 81 度）。 */
    public static final double SWEEP_DOT = 0.15;

    public BogIronGreatswordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.GREATSWORD;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.ASHEN;
    }

    @Override
    public int skillCooldownTicks() {
        return 50;
    }

    @Override
    public double skillStaminaCost() {
        return 30.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        List<LivingEntity> targets = CombatActions.targetsInFront(player, SWEEP_RANGE, SWEEP_DOT);
        if (targets.isEmpty()) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }

        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);
        for (LivingEntity target : targets) {
            CombatActions.dealDamage(player, target, damage);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }
}

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
 * 星屑の薙刀（薙刀 / 攻撃力 15）。
 *
 * <p>戦技「星屑の横薙ぎ」: スタミナ 35 で前方 180 度・4.5 blocks の敵すべてに
 * 攻撃力 125%。扇状の広範囲攻撃。</p>
 */
public class StarfallGlaiveItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.25;
    public static final double SWEEP_RANGE = 4.5;
    /** 0 で前方 90 度（片側）、-0.1 でわずかに背後まで。 */
    public static final double SWEEP_DOT = -0.1;

    public StarfallGlaiveItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.GLAIVE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 60;
    }

    @Override
    public double skillStaminaCost() {
        return 35.0;
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
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }
}

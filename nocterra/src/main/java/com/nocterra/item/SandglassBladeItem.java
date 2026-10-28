package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.WeaponElement;
import com.nocterra.combat.WeaponType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 砂刃（曲刀 / 攻撃力 9 / 攻撃速度 +0.4）。
 *
 * <p>攻撃速度は登録時の {@code sword(...)} baseline を -2.0 にして実現（標準 -2.4 より 0.4 速い）。</p>
 *
 * <p>戦技「時の斬撃」: スタミナ 16 で前方の単体へ攻撃力 150%。命中時に相手の移動速度を
 * 0.5 秒だけ低下させる。</p>
 */
public class SandglassBladeItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.5;

    public SandglassBladeItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.LIGHT_BLADE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.EMBER;
    }

    @Override
    public int skillCooldownTicks() {
        return 25;
    }

    @Override
    public double skillStaminaCost() {
        return 16.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        LivingEntity target = CombatActions.findTarget(player, 3.6, 0.4);
        if (target == null) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }
        CombatActions.dealDamage(player, target, CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER));
        CombatActions.applySlowness(target, 10, 1);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }
}

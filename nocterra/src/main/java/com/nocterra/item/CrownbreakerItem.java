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
 * 冠砕き（大剣 / 攻撃力 14）。ボス素材（砕けた冠片・深淵の鱗）を使う最上位武器。
 *
 * <p>戦技「冠砕き」: スタミナ 40 で前方の単体へ攻撃力 250% の一撃。
 * 命中した相手は 2 秒スタン（移動速度低下）する。</p>
 */
public class CrownbreakerItem extends NocterraWeaponItem {
    /** 戦技の攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 2.5;
    /** 戦技のリーチ。 */
    public static final double REACH = 4.5;

    public CrownbreakerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.GREATSWORD;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 70;
    }

    @Override
    public double skillStaminaCost() {
        return 40.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        LivingEntity target = CombatActions.findTarget(player, REACH, 0.45);
        if (target == null) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }

        CombatActions.dealDamage(player, target, CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER));
        CombatActions.stun(target);

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }
}

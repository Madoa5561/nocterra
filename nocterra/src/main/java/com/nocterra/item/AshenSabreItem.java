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
import net.minecraft.world.phys.Vec3;

/**
 * 灰繊維のサーベル（片手剣 / 攻撃力 7）。ドロップ素材から作る第 1 段階の武器。
 *
 * <p>戦技「灰繊維の一閃」: スタミナ 18 で前方の単体へ攻撃力 160% の突き。
 * 発動時に前方へ小さく踏み込む。</p>
 */
public class AshenSabreItem extends NocterraWeaponItem {
    /** 戦技の攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 1.6;
    /** 踏み込みの水平速度（blocks/tick）。 */
    public static final double LUNGE_SPEED = 0.45;

    public AshenSabreItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.LIGHT_BLADE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.ASHEN;
    }

    @Override
    public int skillCooldownTicks() {
        return 30;
    }

    @Override
    public double skillStaminaCost() {
        return 18.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        LivingEntity target = CombatActions.findTarget(player, 3.5, 0.5);
        if (target == null) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }

        CombatActions.dealDamage(player, target, CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER));

        Vec3 lunge = CombatActions.horizontalLook(player).scale(LUNGE_SPEED);
        player.setDeltaMovement(lunge.x, player.getDeltaMovement().y, lunge.z);
        player.syncVelocity = true;
        player.needsSync = true;

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 1.1F);
        return true;
    }
}

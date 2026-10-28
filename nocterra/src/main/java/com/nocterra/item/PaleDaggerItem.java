package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.CombatScheduler;
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
 * 白刃の短剣（短剣 / 攻撃力 6 / 攻撃速度が速い）。
 *
 * <p>背後攻撃 +50%: 相手の視線方向と「相手→自分」の向きが逆（後ろから）なら
 * 近接ダメージを 1.5 倍する（{@link #meleeDamageMultiplier}）。</p>
 *
 * <p>戦技「白刃の急襲」: スタミナ 12 で 0.1 秒間隔の 3 連刺し（各攻撃力 40%）。</p>
 */
public class PaleDaggerItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 0.4;
    public static final int HIT_INTERVAL_TICKS = 2;
    public static final int HIT_COUNT = 3;
    public static final double BACKSTAB_MULTIPLIER = 1.5;
    /** 背後判定: 相手の視線との内積がこの値未満なら背後とみなす。 */
    public static final double BACKSTAB_DOT_THRESHOLD = -0.3;

    public PaleDaggerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.LIGHT_BLADE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.NONE;
    }

    @Override
    public int skillCooldownTicks() {
        return 25;
    }

    @Override
    public double skillStaminaCost() {
        return 12.0;
    }

    @Override
    public double meleeDamageMultiplier(ServerPlayer attacker, LivingEntity target) {
        Vec3 toAttacker = new Vec3(
                attacker.getX() - target.getX(),
                attacker.getY() - target.getY(),
                attacker.getZ() - target.getZ());
        if (toAttacker.lengthSqr() < 1.0E-4) {
            return 1.0;
        }
        double dot = toAttacker.normalize().dot(target.getLookAngle());
        return dot < BACKSTAB_DOT_THRESHOLD ? BACKSTAB_MULTIPLIER : 1.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }
        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);
        for (int hit = 0; hit < HIT_COUNT; hit++) {
            CombatScheduler.schedule(player, hit * HIT_INTERVAL_TICKS, striker -> {
                LivingEntity target = CombatActions.findTarget(striker, 3.0, 0.45);
                if (target == null) {
                    return;
                }
                if (CombatActions.dealDamageBypassingInvulnerability(striker, target, damage)) {
                    striker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5F, 1.7F);
                }
            });
        }
        return true;
    }
}

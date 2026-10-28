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

/**
 * 灰木の刃（片手剣 / 攻撃力 6）。
 *
 * <p>戦技「三連斬り」: スタミナ 20 で、前方の対象へ 0.15 秒間隔で 3 回斬撃。
 * 各ヒットは攻撃力の 45%（合計 135%）。無敵時間をリセットして連続ヒットさせる。</p>
 */
public class AshwoodBladeItem extends NocterraWeaponItem {
    /** 1 ヒットあたりの攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 0.45;
    /** 斬撃の間隔（tick）。 */
    public static final int HIT_INTERVAL_TICKS = 3;
    /** ヒット回数。 */
    public static final int HIT_COUNT = 3;

    public AshwoodBladeItem(Item.Properties properties) {
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
        return 20.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }

        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);

        for (int hit = 0; hit < HIT_COUNT; hit++) {
            CombatScheduler.schedule(player, hit * HIT_INTERVAL_TICKS, striker -> {
                LivingEntity target = CombatActions.findTarget(striker, 3.0, CombatActions.DEFAULT_AIM_DOT);
                if (target == null) {
                    striker.level().playSound(null, striker.getX(), striker.getY(), striker.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 1.3F);
                    return;
                }
                if (CombatActions.dealDamageBypassingInvulnerability(striker, target, damage)) {
                    striker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.2F);
                }
            });
        }
        return true;
    }
}

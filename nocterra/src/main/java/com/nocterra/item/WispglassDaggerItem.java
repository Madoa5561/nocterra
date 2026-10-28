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
 * 燐粉の短剣（片手剣 / 攻撃力 5）。短剣だが攻撃速度が速く、戦技で手数を稼ぐ。
 *
 * <p>戦技「燐粉の連刺」: スタミナ 15 で 0.1 秒間隔の 3 連刺し（各攻撃力 35%）。
 * 無敵時間をリセットして連続ヒットさせる。</p>
 */
public class WispglassDaggerItem extends NocterraWeaponItem {
    /** 1 ヒットあたりの攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 0.35;
    /** ヒット間隔（tick）。 */
    public static final int HIT_INTERVAL_TICKS = 2;
    /** ヒット回数。 */
    public static final int HIT_COUNT = 3;

    public WispglassDaggerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.LIGHT_BLADE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.FROST;
    }

    @Override
    public int skillCooldownTicks() {
        return 18;
    }

    @Override
    public double skillStaminaCost() {
        return 15.0;
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
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5F, 1.6F);
                }
            });
        }
        return true;
    }
}

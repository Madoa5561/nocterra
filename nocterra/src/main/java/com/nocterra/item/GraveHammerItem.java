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
 * 墓土の戦槌（槌 / 攻撃力 13 / 攻撃速度は遅い）。
 *
 * <p>近接ヒット時に相手へ移動速度低下を付与（スタン）。戦技「墓土の震動」は
 * 周囲 3.5 blocks の敵すべてに攻撃力 130% + 2 秒スタン。</p>
 */
public class GraveHammerItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.3;
    public static final double AREA_RANGE = 3.5;

    public GraveHammerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.HAMMER;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 55;
    }

    @Override
    public double skillStaminaCost() {
        return 32.0;
    }

    @Override
    public void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
        // 鈍器: 近接ヒットで短時間の移動速度低下
        CombatActions.applySlowness(target, 20, 2);
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        List<LivingEntity> targets = CombatActions.targetsInFront(player, AREA_RANGE, -1.0);
        if (targets.isEmpty()) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }
        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);
        for (LivingEntity target : targets) {
            CombatActions.dealDamage(player, target, damage);
            CombatActions.stun(target);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }
}

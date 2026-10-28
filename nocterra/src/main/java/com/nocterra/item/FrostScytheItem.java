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
 * 霜の大鎌（大鎌 / 攻撃力 12）。
 *
 * <p>近接ヒット時に相手へ移動速度低下を付与。戦技「霜の旋風」: スタミナ 28 で
 * 周囲 4 blocks の敵すべてに攻撃力 110% + 2 秒の移動速度低下。</p>
 */
public class FrostScytheItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.1;
    public static final double AREA_RANGE = 4.0;

    public FrostScytheItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.SCYTHE;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.FROST;
    }

    @Override
    public int skillCooldownTicks() {
        return 50;
    }

    @Override
    public double skillStaminaCost() {
        return 28.0;
    }

    @Override
    public void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
        CombatActions.applySlowness(target, 30, 1);
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
            CombatActions.applySlowness(target, 40, 1);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }
}

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
 * 潮の三叉槍（三叉槍 / 攻撃力 10）。
 *
 * <p>水中では近接ダメージ +30%。戦技「潮の三叉」: スタミナ 25 で前方 4.5 blocks に
 * 攻撃力 160% の突き（複数命中）。水中ではさらに +30%。</p>
 */
public class TidalTridentItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.6;
    public static final double WATER_BONUS = 1.3;
    public static final double SKILL_RANGE = 4.5;

    public TidalTridentItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.TRIDENT;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.FROST;
    }

    @Override
    public int skillCooldownTicks() {
        return 40;
    }

    @Override
    public double skillStaminaCost() {
        return 25.0;
    }

    @Override
    public double meleeDamageMultiplier(ServerPlayer attacker, LivingEntity target) {
        return attacker.isInWater() ? WATER_BONUS : 1.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        List<LivingEntity> targets = CombatActions.targetsInFront(player, SKILL_RANGE, 0.35);
        if (targets.isEmpty()) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }
        double bonus = player.isInWater() ? WATER_BONUS : 1.0;
        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER * bonus);
        for (LivingEntity target : targets) {
            CombatActions.dealDamage(player, target, damage);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }
}

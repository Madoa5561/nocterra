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
 * 樹脂の長槍（槍 / 攻撃力 8 / リーチ +1.5）。
 *
 * <p>バニラの攻撃間合いは {@code Attributes.ENTITY_INTERACTION_RANGE} +1.5 で延長
 * （登録時の {@code ItemAttributeModifiers} で付与）。</p>
 *
 * <p>戦技「貫通突き」: スタミナ 22 で前方の狭い直線上（5.5 blocks, 約 53 度）の
 * 敵すべてに攻撃力 120%。</p>
 */
public class ResinSpearItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.2;
    public static final double SKILL_RANGE = 5.5;
    /** 狭い直線判定（cos 約 53 度）。 */
    public static final double SKILL_DOT = 0.6;

    public ResinSpearItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.SPEAR;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.ASHEN;
    }

    @Override
    public int skillCooldownTicks() {
        return 35;
    }

    @Override
    public double skillStaminaCost() {
        return 22.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        List<LivingEntity> targets = CombatActions.targetsInFront(player, SKILL_RANGE, SKILL_DOT);
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
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 1.1F);
        return true;
    }
}

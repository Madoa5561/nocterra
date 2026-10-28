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
 * 火山の大槌（大槌 / 攻撃力 16 / 最重量）。
 *
 * <p>近接ヒット時に着火 + ノックバック。戦技「火山の大槌」: スタミナ 40 で
 * 周囲 3.5 blocks の敵すべてに攻撃力 150% + 5 秒着火 + 吹き飛ばし。</p>
 */
public class VolcanicMaulItem extends NocterraWeaponItem {
    public static final double HIT_MULTIPLIER = 1.5;
    public static final double AREA_RANGE = 3.5;
    public static final float HIT_IGNITE_SECONDS = 3.0F;
    public static final float SKILL_IGNITE_SECONDS = 5.0F;
    public static final double HIT_PUSH = 0.3;
    public static final double SKILL_PUSH = 0.6;

    public VolcanicMaulItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.MAUL;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.EMBER;
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
    public void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
        CombatActions.ignite(target, HIT_IGNITE_SECONDS);
        CombatActions.pushAway(attacker, target, HIT_PUSH);
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
            CombatActions.ignite(target, SKILL_IGNITE_SECONDS);
            CombatActions.pushAway(player, target, SKILL_PUSH);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.9F);
        return true;
    }
}

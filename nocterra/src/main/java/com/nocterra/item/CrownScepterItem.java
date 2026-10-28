package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.WeaponElement;
import com.nocterra.combat.WeaponType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 冠の王笏（王笏 / 攻撃力 7 / 支援）。
 *
 * <p>戦技「王笏の号令」: <b>魔力 30</b> を消費し、周囲 8 blocks のプレイヤー全員
 * （自分を含む）へ STRENGTH II と RESISTANCE I を 8 秒付与。自分は 4 回復する。</p>
 */
public class CrownScepterItem extends NocterraWeaponItem {
    public static final double MANA_COST = 30.0;
    public static final double ALLY_RADIUS = 8.0;
    public static final int BUFF_TICKS = 160;
    public static final int STRENGTH_AMPLIFIER = 1;
    public static final int RESISTANCE_AMPLIFIER = 0;
    public static final float SELF_HEAL = 4.0F;

    public CrownScepterItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.SCEPTER;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 120;
    }

    @Override
    public double skillStaminaCost() {
        // 魔力消費型
        return 0.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        if (!CombatActions.beginManaSkill(player, stack, MANA_COST, skillCooldownTicks())) {
            return false;
        }
        for (ServerPlayer ally : CombatActions.alliesNearby(player, ALLY_RADIUS)) {
            ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BUFF_TICKS, STRENGTH_AMPLIFIER, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, BUFF_TICKS, RESISTANCE_AMPLIFIER, false, true));
        }
        player.heal(SELF_HEAL);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.2F);
        return true;
    }
}

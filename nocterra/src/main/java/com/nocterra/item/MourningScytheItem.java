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
import net.minecraft.world.phys.Vec3;

/**
 * 嘆きの大鎌（鎌 / 攻撃力 9）。
 *
 * <p>戦技「引き寄せ」: スタミナ 25 で前方 6 blocks 以内の最大 4 体を
 * 攻撃力の 40% で斬りつけ、自分へ引き寄せる。スタンは付与しない。</p>
 */
public class MourningScytheItem extends NocterraWeaponItem {
    /** 1 体あたりの攻撃力倍率。 */
    public static final double HIT_MULTIPLIER = 0.4;
    /** 戦技のリーチ。 */
    public static final double RANGE = 6.0;
    /** 引き寄せ対象の最大数。 */
    public static final int MAX_TARGETS = 4;
    /** 引き寄せの初速（blocks/tick）。 */
    public static final double PULL_SPEED = 0.9;
    /** 引き寄せ時の上向き成分。 */
    public static final double PULL_LIFT = 0.25;

    public MourningScytheItem(Item.Properties properties) {
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
        return 25.0;
    }

    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        List<LivingEntity> targets = CombatActions.targetsInFront(player, RANGE, 0.2);
        if (targets.isEmpty()) {
            return false;
        }
        if (!CombatActions.beginSkill(player, stack, this)) {
            return false;
        }

        float damage = CombatActions.meleeDamage(player, stack, HIT_MULTIPLIER);
        int pulled = 0;
        for (LivingEntity target : targets) {
            if (pulled >= MAX_TARGETS) {
                break;
            }
            CombatActions.dealDamage(player, target, damage);

            Vec3 toward = new Vec3(
                    player.getX() - target.getX(),
                    player.getY() + 1.0 - target.getY(),
                    player.getZ() - target.getZ());
            if (toward.lengthSqr() > 1.0E-4) {
                Vec3 pull = toward.normalize().scale(PULL_SPEED).add(0.0, PULL_LIFT, 0.0);
                target.setDeltaMovement(pull);
                // サーバーで決めた速度をクライアントへ送る（26.3 の同期フラグ）
                target.syncVelocity = true;
                target.needsSync = true;
            }
            pulled++;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.8F);
        return true;
    }
}

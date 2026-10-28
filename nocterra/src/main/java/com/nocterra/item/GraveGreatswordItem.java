package com.nocterra.item;

import com.nocterra.combat.CombatActions;
import com.nocterra.combat.CombatAttachments;
import com.nocterra.combat.CombatData;
import com.nocterra.combat.CombatSync;
import com.nocterra.combat.WeaponElement;
import com.nocterra.combat.WeaponType;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * 墓所の大剣（大剣 / 攻撃力 11）。
 *
 * <p>戦技「溜め斬り」: 右クリック長押しで溜め、離した瞬間に前方広範囲を斬る。
 * 0.6 秒（12 tick）未満では発動しない。最大 1.25 秒（25 tick）。
 * ダメージは攻撃力の 1.6〜2.6 倍（溜め時間に比例）。スタミナ 35。</p>
 */
public class GraveGreatswordItem extends NocterraWeaponItem {
    /** 発動に必要な最小溜め（12 tick = 0.6 秒）。 */
    public static final int MIN_CHARGE_TICKS = 12;
    /** 最大溜め（25 tick = 1.25 秒）。これを超えると自動で振り抜く。 */
    public static final int MAX_CHARGE_TICKS = 25;

    public GraveGreatswordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.GREATSWORD;
    }

    @Override
    public WeaponElement defaultElement() {
        return WeaponElement.VOID;
    }

    @Override
    public int skillCooldownTicks() {
        return 60;
    }

    @Override
    public double skillStaminaCost() {
        return 35.0;
    }

    /**
     * 戦技キー（G）から溜め斬りを開始する。
     *
     * <p>溜め斬りは {@link #use} / {@link #releaseUsing} が本体で、コストも解放時に払う。
     * ここではサーバー側で item 使用を開始するだけにする（最大溜めで自動的に振り抜かれ、
     * {@link #releaseUsing} が呼ばれる）。右クリック長押しと同じ結果になるが、
     * キー操作では溜めモーションがクライアントに表示されない点だけ異なる。</p>
     */
    @Override
    public boolean activateSkill(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        player.startUsingItem(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // クライアント側は溜めモーションだけ開始する
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (serverPlayer.getCooldowns().isOnCooldown(serverPlayer.getMainHandItem())) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        // 溜めモーション。26.3 では溜め専用アニメが無いため BOW（引き絞り）を流用する。
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseDuration) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel) || !(entity instanceof ServerPlayer player)) {
            return false;
        }

        int usedTicks = this.getUseDuration(stack, entity) - remainingUseDuration;
        if (usedTicks < MIN_CHARGE_TICKS) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }

        CombatData data = CombatAttachments.get(player);
        if (!data.spendStamina(skillStaminaCost())) {
            CombatActions.notifyInsufficientStamina(player);
            return false;
        }
        player.getCooldowns().addCooldown(stack, skillCooldownTicks());

        float chargeRatio = (float) (usedTicks - MIN_CHARGE_TICKS)
                / (float) Math.max(1, MAX_CHARGE_TICKS - MIN_CHARGE_TICKS);
        double multiplier = 1.6 + 1.0 * Math.min(1.0F, chargeRatio);
        float damage = CombatActions.meleeDamage(player, stack, multiplier);

        List<LivingEntity> targets = CombatActions.targetsInFront(player, 4.0, 0.2);
        if (targets.isEmpty()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 0.7F);
        } else {
            for (LivingEntity target : targets) {
                CombatActions.dealDamage(player, target, damage);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
        }

        CombatSync.sync(player);
        return true;
    }
}

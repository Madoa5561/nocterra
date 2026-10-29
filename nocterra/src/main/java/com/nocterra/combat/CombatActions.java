package com.nocterra.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 戦闘アクションのサーバー権威ロジック。
 *
 * <p>クライアントから届くのは「要求」だけで、スタミナ消費・ダメージ・無敵・
 * クールダウンはすべてここ（サーバー）で決定する。クライアントは結果を
 * {@code CombatSyncPayload} で受け取り表示にのみ使う。</p>
 */
public final class CombatActions {
    private CombatActions() {}

    /** 前方判定の既定 dot（約 70 度）。0.35 ≒ cos(70°)。 */
    public static final double DEFAULT_AIM_DOT = 0.35;

    // ---------------------------------------------------------------- 回避

    /**
     * 回避: スタミナ 25 を消費し、前方へ 0.6 秒ダッシュ。
     * ダッシュ中は {@code invulnerableTicks}（0.25 秒）の間、
     * {@code LivingIncomingDamageEvent} 側でダメージをキャンセルする。
     */
    public static boolean dodge(ServerPlayer player) {
        CombatData data = CombatAttachments.get(player);
        if (data.getDodgeCooldown() > 0 || data.getDodgeTicks() > 0) {
            return false;
        }
        if (!data.spendStamina(CombatBalance.dodgeStaminaCost)) {
            notifyInsufficientStamina(player);
            return false;
        }

        data.setDodgeDirection(horizontalLook(player));
        data.setDodgeTicks(CombatBalance.dodgeDurationTicks);
        data.setDodgeCooldown(CombatBalance.dodgeCooldownTicks);
        data.setInvulnerableTicks(CombatBalance.dodgeInvulnerableTicks);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.6F);
        CombatSync.sync(player);
        return true;
    }

    // ---------------------------------------------------------------- 強攻撃

    /**
     * 強攻撃: 武器カテゴリごとのスタミナコストを払い、正面の単体へ高倍率ダメージ。
     */
    public static boolean heavyAttack(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof CombatWeapon weapon)) {
            notify(player, "nocterra.message.no_weapon");
            return false;
        }

        CombatData data = CombatAttachments.get(player);
        if (data.getHeavyCooldown() > 0) {
            return false;
        }

        WeaponType type = weapon.weaponType();
        if (!data.spendStamina(type.heavyAttackStaminaCost())) {
            notifyInsufficientStamina(player);
            return false;
        }
        data.setHeavyCooldown(CombatBalance.heavyAttackCooldownTicks);

        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * CombatBalance.heavyAttackMultiplier
                * type.heavyAttackMultiplier());
        damage += CombatComponents.enhancement(stack) * CombatBalance.enhancementDamagePerLevel;

        com.nocterra.combat.WeaponEffects.swing(player, type.reach());
        LivingEntity target = findTarget(player, type.reach(), DEFAULT_AIM_DOT);
        if (target != null && dealDamage(player, target, damage)) {
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.0F);
        }

        CombatSync.sync(player);
        return true;
    }

    // ---------------------------------------------------------------- パリィ

    /**
     * パリィ: 0.5 秒の受付ウィンドウを開く。ウィンドウ中の被弾は
     * {@code LivingIncomingDamageEvent} でキャンセルし、攻撃者を 2 秒スタンさせる。
     */
    public static boolean parry(ServerPlayer player) {
        CombatData data = CombatAttachments.get(player);
        if (data.getParryCooldown() > 0 || data.getParryTicks() > 0) {
            return false;
        }
        data.setParryTicks(CombatBalance.parryWindowTicks);
        data.setParryCooldown(CombatBalance.parryCooldownTicks);
        CombatSync.sync(player);
        return true;
    }

    /** パリィ成功時のスタン（移動速度低下 2 秒）。 */
    public static void stun(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS,
                CombatBalance.parryStunTicks,
                CombatBalance.parryStunAmplifier,
                false,
                true));
    }

    // ---------------------------------------------------------------- 戦技

    /** メインハンドの武器戦技を発動する（右クリック / 戦技キー共通）。 */
    public static boolean useWeaponSkill(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof CombatWeapon weapon)) {
            notify(player, "nocterra.message.no_weapon");
            return false;
        }
        return weapon.activateSkill(player, stack);
    }

    /**
     * 戦技の標準前処理。クールダウン確認 → スタミナ消費 → クールダウン設定を行い、
     * 発動可能なら true を返す。各武器の {@code activateSkill} の先頭で呼ぶ。
     *
     * <p>溜め斬りのように「開始時ではなく解放時にコストを払う」戦技は、
     * このヘルパーを使わず自前でクールダウン／スタミナを扱う。</p>
     */
    public static boolean beginSkill(ServerPlayer player, ItemStack stack, CombatWeapon weapon) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        CombatData data = CombatAttachments.get(player);
        if (!data.spendStamina(weapon.skillStaminaCost())) {
            notifyInsufficientStamina(player);
            return false;
        }
        player.getCooldowns().addCooldown(stack, weapon.skillCooldownTicks());
        CombatSync.sync(player);
        return true;
    }

    // ---------------------------------------------------------------- ダメージ共通

    /** プレイヤーの近接攻撃ダメージを返す（武器属性補正込み）。 */
    public static float meleeDamage(ServerPlayer player, ItemStack stack, double multiplier) {
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
        damage += CombatComponents.enhancement(stack) * CombatBalance.enhancementDamagePerLevel;
        return (float) damage;
    }

    /** 通常ダメージ。無敵時間の影響を受ける（通常の被弾と同じ）。 */
    public static boolean dealDamage(ServerPlayer player, LivingEntity target, float damage) {
        boolean hit = target.hurtServer(player.level(), player.level().damageSources().playerAttack(player), damage);
        if (hit) {
            com.nocterra.combat.WeaponEffects.hit(player, target);
        }
        return hit;
    }

    /** 無敵時間をリセットしてからダメージを与える（3 連斬りのように連続ヒットさせる戦技用）。 */
    public static boolean dealDamageBypassingInvulnerability(ServerPlayer player, LivingEntity target, float damage) {
        target.setInvulnerableTime(0);
        return dealDamage(player, target, damage);
    }

    // ---------------------------------------------------------------- 対象探索

    /** プレイヤーの正面・指定リーチ内でもっとも近い LivingEntity。 */
    public static LivingEntity findTarget(ServerPlayer player, double range, double minDot) {
        List<LivingEntity> targets = targetsInFront(player, range, minDot);
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * プレイヤーの正面コーン内の LivingEntity を近い順に返す。
     *
     * @param range  リーチ（blocks）
     * @param minDot 視線方向との内積の下限（1.0 = 正面のみ、0.0 = 真横まで）
     */
    public static List<LivingEntity> targetsInFront(ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(range, 2.0, range);

        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : player.level().getEntities(player, area,
                candidate -> candidate instanceof LivingEntity living && living.isAlive() && living != player)) {
            LivingEntity living = (LivingEntity) entity;
            Vec3 to = new Vec3(
                    living.getX() - eye.x,
                    living.getY() + living.getBbHeight() * 0.5 - eye.y,
                    living.getZ() - eye.z);
            double distance = to.length();
            if (distance > range || distance < 1.0E-3) {
                continue;
            }
            if (to.normalize().dot(look) < minDot) {
                continue;
            }
            result.add(living);
        }
        result.sort(Comparator.comparingDouble(living -> living.distanceToSqr(player)));
        return result;
    }

    // ---------------------------------------------------------------- 小道具

    /**
     * 視線の水平成分。真上・真下を向いていて水平成分が無い場合は yaw から作る。
     */
    public static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            double yaw = Math.toRadians(player.getYRot());
            horizontal = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        }
        return horizontal.normalize();
    }

    /** 武器スタックのレジストリ ID（コンボの「同じ武器か」判定に使う）。 */
    public static String weaponId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    // ---------------------------------------------------------------- 魔力消費型の戦技

    /**
     * 魔力消費型戦技の標準前処理。クールダウン確認 → 魔力消費 → クールダウン設定。
     *
     * @return 発動可能なら true
     */
    public static boolean beginManaSkill(ServerPlayer player, ItemStack stack, double manaCost, int cooldownTicks) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        CombatData data = CombatAttachments.get(player);
        if (!data.spendMana(manaCost)) {
            notify(player, "nocterra.message.mana_insufficient");
            return false;
        }
        player.getCooldowns().addCooldown(stack, cooldownTicks);
        CombatSync.sync(player);
        return true;
    }

    /** 魔法ダメージ（近接扱いにしないためコンボ倍率は乗らない）。 */
    public static boolean dealMagicDamage(ServerPlayer player, LivingEntity target, float damage) {
        return target.hurtServer(player.level(),
                player.level().damageSources().indirectMagic(player, player), damage);
    }

    /** 移動速度低下を付与する（スタン・氷結用）。 */
    public static void applySlowness(LivingEntity target, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amplifier, false, true));
    }

    /** 着火（秒指定）。 */
    public static void ignite(LivingEntity target, float seconds) {
        target.igniteForSeconds(seconds);
    }

    /** 攻撃者から遠ざかる方向へ吹き飛ばす（サーバー速度同期込み）。 */
    public static void pushAway(ServerPlayer source, LivingEntity target, double strength) {
        Vec3 away = new Vec3(target.getX() - source.getX(), 0.0, target.getZ() - source.getZ());
        if (away.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 push = away.normalize().scale(strength);
        target.push(push.x, 0.25, push.z);
        target.syncVelocity = true;
        target.needsSync = true;
    }

    /** 周囲のプレイヤー（自分を含む）。王笏の支援戦技などで使う。 */
    public static List<ServerPlayer> alliesNearby(ServerPlayer player, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof ServerPlayer)) {
            result.add((ServerPlayer) entity);
        }
        result.add(player);
        return result;
    }

    public static void notifyInsufficientStamina(ServerPlayer player) {
        notify(player, "nocterra.message.stamina_insufficient");
    }

    /** アクション失敗のフィードバック（サーバーからのみ呼ぶ）。 */
    public static void notify(ServerPlayer player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }
}

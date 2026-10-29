package com.nocterra.combat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 武器の属性に応じたヒット・スイングのエフェクト。所有: Lead。
 *
 * <p>{@link WeaponElement}（Data Component）を見て、命中地点と振りの軌跡にパーティクルを出す。
 * サーバー側で発生させるため、マルチプレイ（PVP含む）でも全員に同じエフェクトが見える。</p>
 */
public final class WeaponEffects {
    private WeaponEffects() {}

    /** 命中時のバースト。 */
    public static void hit(ServerPlayer player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        WeaponElement element = CombatComponents.element(stack);
        int enhancement = CombatComponents.enhancement(stack);
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);

        int count = 10 + enhancement * 4;
        switch (element) {
            case ASHEN -> {
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, count, 0.35, 0.35, 0.35, 0.12);
                level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, count / 2, 0.5, 0.3, 0.5, 0.02);
                sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.4F);
            }
            case EMBER -> {
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, count, 0.3, 0.35, 0.3, 0.05);
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 3, 0.25, 0.25, 0.25, 0.0);
                sound(level, at, SoundEvents.FIRECHARGE_USE, 0.5F, 1.6F);
            }
            case FROST -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, count, 0.35, 0.4, 0.35, 0.03);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.02);
                sound(level, at, SoundEvents.GLASS_BREAK, 0.4F, 1.8F);
            }
            case VOID -> {
                level.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, count, 0.4, 0.4, 0.4, 0.25);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, count / 2, 0.3, 0.3, 0.3, 0.02);
                sound(level, at, SoundEvents.SOUL_ESCAPE.value(), 0.5F, 1.2F);
            }
            default -> level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.1);
        }

        // 強化段階に応じた追加のきらめき
        if (enhancement > 0) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, enhancement * 3,
                    0.25, 0.25, 0.25, 0.05);
        }
    }

    /** 振りの軌跡（強攻撃・戦技）。 */
    public static void swing(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WeaponElement element = CombatComponents.element(player.getMainHandItem());
        ParticleOptions particle = switch (element) {
            case ASHEN -> ParticleTypes.CRIT;
            case EMBER -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case VOID -> ParticleTypes.PORTAL;
            default -> ParticleTypes.SWEEP_ATTACK;
        };
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(0, -0.25, 0);
        for (int i = -3; i <= 3; i++) {
            double angle = Math.toRadians(i * 22.5);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 dir = new Vec3(look.x * cos - look.z * sin, look.y, look.x * sin + look.z * cos);
            Vec3 point = origin.add(dir.scale(radius));
            level.sendParticles(particle, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound,
                              float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}

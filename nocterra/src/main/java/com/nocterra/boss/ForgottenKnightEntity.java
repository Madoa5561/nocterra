package com.nocterra.boss;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 隠しボス「忘却された騎士」（WHITE ボスバー）。高速・パリィ依存。
 *
 * <p>予兆が短く手数が多い（パリィ前提の調整）。フェーズ 1: 忘却の斬撃（予兆 12t）/ 幻影連撃（予兆 14t・2 ヒット）。
 * フェーズ 2（HP 50%）: 予兆が 10/12 tick に短縮され、移動速度と攻撃力が上がる。</p>
 */
public class ForgottenKnightEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(ForgottenKnightEntity.class, EntityDataSerializers.INT);

    public ForgottenKnightEntity(EntityType<? extends ForgottenKnightEntity> type, Level level) {
        super(type, level, "forgotten_knight", BossEvent.BossBarColor.WHITE);
        this.xpReward = 360;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 380.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
    }

    @Override
    protected EntityDataAccessor<Integer> phaseAccessor() {
        return DATA_PHASE;
    }

    @Override
    protected float phaseThreshold(int phase) {
        return 0.5F;
    }

    @Override
    protected ParticleOptions effectParticle() {
        return ParticleTypes.ENCHANTED_HIT;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.RAVAGER_ATTACK : SoundEvents.WITHER_HURT;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return 30.25;
    }

    @Override
    protected int windupFor(int variant) {
        boolean fast = this.getPhase() >= 2;
        if (variant == 0) {
            return fast ? 10 : 12;
        }
        return fast ? 12 : 14;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean single = this.random.nextInt(5) < 3;
        this.startAttack(single ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.phantomCombo(level);
            default -> this.forgottenSlash(level);
        }
    }

    /** 忘却の斬撃: 高速単発。 */
    private void forgottenSlash(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 25.0) {
            return;
        }
        float damage = this.getPhase() >= 2 ? 16.0F : 12.0F;
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        Vec3 direction = awayDirection(this, target);
        if (direction != Vec3.ZERO) {
            target.push(direction.x * 0.5, 0.3, direction.z * 0.5);
        }
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1.1, target.getZ(), 22, 0.4, 0.5, 0.4, 0.05);
        this.playSound(SoundEvents.WITHER_HURT, 1.0F, 1.3F);
    }

    /** 幻影連撃: 2 ヒット（8+8 / フェーズ2は 10+10）。 */
    private void phantomCombo(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 20.25) {
            return;
        }
        float hit = this.getPhase() >= 2 ? 10.0F : 8.0F;
        target.hurtServer(level, level.damageSources().mobAttack(this), hit);
        target.hurtServer(level, level.damageSources().mobAttack(this), hit);
        Vec3 direction = awayDirection(this, target);
        if (direction != Vec3.ZERO) {
            target.push(direction.x * 0.8, 0.35, direction.z * 0.8);
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                target.getX(), target.getY() + 1.1, target.getZ(), 30, 0.5, 0.6, 0.5, 0.06);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.2F);
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(16.0);
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.40);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}

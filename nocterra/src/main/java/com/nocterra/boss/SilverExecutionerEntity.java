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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ボス「白銀の処刑者」（永劫の氷原 / WHITE ボスバー）。鈍足・高火力・凍結。
 *
 * <p>フェーズ 1: 処刑の斧（予兆 20t・近接 13 + 移動速度低下 II）/ 凍てつく断罪（予兆 24t・半径 5 に 9 + 移動速度低下 II）。<br>
 * フェーズ 2（HP 50%）: ダメージと凍結が強化され、移動速度がやや上がる。</p>
 */
public class SilverExecutionerEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(SilverExecutionerEntity.class, EntityDataSerializers.INT);

    private static final double FROST_RADIUS = 5.0;

    public SilverExecutionerEntity(EntityType<? extends SilverExecutionerEntity> type, Level level) {
        super(type, level, "silver_executioner", BossEvent.BossBarColor.WHITE);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 340.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 40.0);
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
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.POWDER_SNOW_BREAK;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.RAVAGER_ATTACK : SoundEvents.GLASS_BREAK;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return 36.0;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 20 : 24;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean axe = this.random.nextInt(5) < (this.getPhase() >= 2 ? 3 : 4);
        this.startAttack(axe ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.frostJudgment(level);
            default -> this.executionAxe(level);
        }
    }

    /** 処刑の斧: 近接 + 凍結（移動速度低下 II / フェーズ2 は III）。 */
    private void executionAxe(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 25.0) {
            return;
        }
        float damage = this.getPhase() >= 2 ? 16.0F : 13.0F;
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        int amplifier = this.getPhase() >= 2 ? 2 : 1;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 140, amplifier));
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + 1.1, target.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
        this.playSound(SoundEvents.GLASS_BREAK, 1.2F, 0.7F);
    }

    /** 凍てつく断罪: 半径 5 にダメージ + 凍結。 */
    private void frostJudgment(ServerLevel level) {
        float damage = this.getPhase() >= 2 ? 12.0F : 9.0F;
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(FROST_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2));
        }
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0) * i / 40.0;
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX() + Math.cos(angle) * FROST_RADIUS,
                    this.getY() + 0.3 + (i % 6) * 0.3,
                    this.getZ() + Math.sin(angle) * FROST_RADIUS,
                    2, 0.1, 0.1, 0.1, 0.03);
        }
        this.playSound(SoundEvents.GLASS_BREAK, 1.4F, 0.5F);
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.22);
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

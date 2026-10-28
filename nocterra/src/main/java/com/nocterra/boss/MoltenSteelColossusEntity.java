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

/**
 * ボス「灼鋼の巨神」（黒鉄の火山 / RED ボスバー）。ノックバック耐性 1.0・炎無効・高火力範囲。
 *
 * <p>フェーズ 1: 灼熱の拳（予兆 20t・近接 14 + 着火）/ 溶岩噴出（予兆 24t・半径 7 に 12 + 着火）。<br>
 * フェーズ 2（HP 50%）: 範囲が広がり、ダメージと着火時間が強化される。</p>
 */
public class MoltenSteelColossusEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(MoltenSteelColossusEntity.class, EntityDataSerializers.INT);

    private static final double ERUPTION_RADIUS = 7.0;
    private static final double ERUPTION_RADIUS_PHASE2 = 8.5;

    public MoltenSteelColossusEntity(EntityType<? extends MoltenSteelColossusEntity> type, Level level) {
        super(type, level, "molten_steel_colossus", BossEvent.BossBarColor.RED);
        this.xpReward = 340;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
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
        return ParticleTypes.LAVA;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.LAVA_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.RAVAGER_ATTACK : SoundEvents.LAVA_POP;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return 42.25;
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
        boolean fist = this.random.nextInt(5) < (this.getPhase() >= 2 ? 2 : 3);
        this.startAttack(fist ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.lavaEruption(level);
            default -> this.moltenFist(level);
        }
    }

    /** 灼熱の拳: 近接 + 着火 + ノックバック。 */
    private void moltenFist(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 20.25) {
            return;
        }
        float damage = this.getPhase() >= 2 ? 17.0F : 14.0F;
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        target.igniteForSeconds(this.getPhase() >= 2 ? 6.0F : 4.0F);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 1.0E-4) {
            target.push(dx / length * 1.1, 0.5, dz / length * 1.1);
        }
        level.sendParticles(ParticleTypes.LAVA,
                target.getX(), target.getY() + 1.0, target.getZ(), 26, 0.4, 0.6, 0.4, 0.06);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.3F, 0.5F);
    }

    /** 溶岩噴出: 半径 7（フェーズ2は 8.5）に 12/16 ダメージ + 着火 + ノックバック。 */
    private void lavaEruption(ServerLevel level) {
        double radius = this.getPhase() >= 2 ? ERUPTION_RADIUS_PHASE2 : ERUPTION_RADIUS;
        float damage = this.getPhase() >= 2 ? 16.0F : 12.0F;
        float fireSeconds = this.getPhase() >= 2 ? 8.0F : 5.0F;
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().mobAttack(this), damage);
            player.igniteForSeconds(fireSeconds);
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                player.push(dx / length * 1.4, 0.6, dz / length * 1.4);
            }
        }
        for (int i = 0; i < 48; i++) {
            double angle = (Math.PI * 2.0) * i / 48.0;
            level.sendParticles(ParticleTypes.LAVA,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.3 + (i % 8) * 0.3,
                    this.getZ() + Math.sin(angle) * radius,
                    2, 0.1, 0.1, 0.1, 0.04);
        }
        level.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY() + 1.6, this.getZ(), 50, 1.2, 1.2, 1.2, 0.06);
        this.playSound(SoundEvents.LAVA_POP, 1.4F, 0.5F);
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.19);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.LAVA_AMBIENT;
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

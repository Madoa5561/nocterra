package com.nocterra.boss;

import com.nocterra.entity.MourningWispEntity;
import com.nocterra.entity.NocterraEntities;
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
import net.minecraft.world.entity.EntitySpawnReason;
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
 * ストーリーボス「灯火の化身」（YELLOW ボスバー・**3 フェーズ**・光属性）。
 *
 * <p>フェーズ 1（HP 60% まで）: 光輝の斬撃 / 灯火の光弾。<br>
 * フェーズ 2（HP 60%〜30%）: 「灯の精」を 2 体召喚し、光弾が着弾点周囲の炎環を発生させる。<br>
 * フェーズ 3（HP 30% 以下）: 全攻撃が強化され、移動速度が上がる。</p>
 */
public class EmberIncarnationEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(EmberIncarnationEntity.class, EntityDataSerializers.INT);

    private static final double BOLT_RANGE_SQR = 400.0;

    public EmberIncarnationEntity(EntityType<? extends EmberIncarnationEntity> type, Level level) {
        super(type, level, "ember_incarnation", BossEvent.BossBarColor.YELLOW);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 560.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
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
    protected int phaseCount() {
        return 3;
    }

    @Override
    protected float phaseThreshold(int phase) {
        return switch (phase) {
            case 2 -> 0.6F;
            case 3 -> 0.3F;
            default -> 0.0F;
        };
    }

    @Override
    protected ParticleOptions effectParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.FIRE_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.FIREWORK_ROCKET_BLAST;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.RAVAGER_ATTACK : SoundEvents.FIREWORK_ROCKET_BLAST;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return BOLT_RANGE_SQR;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 16 : 22;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean melee = this.distanceToSqr(target) <= 25.0 && this.random.nextInt(5) < 3;
        this.startAttack(melee ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.emberBolt(level);
            default -> this.radiantSlash(level);
        }
    }

    /** 光輝の斬撃: 近接単体（フェーズでダメージ上昇、着火なし）。 */
    private void radiantSlash(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 25.0) {
            return;
        }
        float damage = switch (this.getPhase()) {
            case 3 -> 20.0F;
            case 2 -> 16.0F;
            default -> 13.0F;
        };
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        Vec3 direction = awayDirection(this, target);
        if (direction != Vec3.ZERO) {
            target.push(direction.x * 0.6, 0.4, direction.z * 0.6);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + 1.2, target.getZ(), 26, 0.4, 0.5, 0.4, 0.05);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.2F, 0.8F);
    }

    /** 灯火の光弾: 視線通過時のみ遠距離ダメージ。フェーズ2以降は着弾点に炎環 AoE も発生。 */
    private void emberBolt(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > BOLT_RANGE_SQR) {
            return;
        }
        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        int steps = (int) Math.min(40, Math.max(2, distance));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (!this.hasLineOfSight(target)) {
            return;
        }
        float damage = switch (this.getPhase()) {
            case 3 -> 16.0F;
            case 2 -> 13.0F;
            default -> 11.0F;
        };
        target.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
        level.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.4, 0.5, 0.4, 0.04);
        this.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2F, 0.8F);

        if (this.getPhase() >= 2) {
            // フェーズ2以降: 灯の炎環（半径 6、8/10 ダメージ）
            double radius = this.getPhase() >= 3 ? 7.5 : 6.0;
            float ringDamage = this.getPhase() >= 3 ? 10.0F : 8.0F;
            List<Player> ring = level.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(radius),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
            for (Player player : ring) {
                player.hurtServer(level, level.damageSources().indirectMagic(this, this), ringDamage);
                player.igniteForSeconds(3.0F);
            }
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2.0) * i / 36.0;
                level.sendParticles(ParticleTypes.FLAME,
                        this.getX() + Math.cos(angle) * radius,
                        this.getY() + 0.4 + (i % 6) * 0.25,
                        this.getZ() + Math.sin(angle) * radius,
                        2, 0.1, 0.1, 0.1, 0.03);
            }
        }
    }

    @Override
    protected void summonMinions(ServerLevel level) {
        List<MourningWispEntity> existing = level.getEntitiesOfClass(MourningWispEntity.class,
                this.getBoundingBox().inflate(24.0), LivingEntity::isAlive);
        if (existing.size() >= 4) {
            this.summonCooldown = 100;
            return;
        }
        EntityType<MourningWispEntity> type = NocterraEntities.MOURNING_WISP.get();
        for (int i = 0; i < 2; i++) {
            MourningWispEntity wisp = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (wisp == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double x = this.getX() + Math.cos(angle) * 3.0;
            double z = this.getZ() + Math.sin(angle) * 3.0;
            wisp.snapTo(x, this.getY(), z, this.getYRot(), 0.0F);
            if (this.getTarget() != null) {
                wisp.setTarget(this.getTarget());
            }
            level.addFreshEntity(wisp);
            level.sendParticles(ParticleTypes.END_ROD, x, this.getY() + 1.0, z, 20, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2F, 0.7F);
        this.summonCooldown = 240;
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (phase == 2) {
            if (speed != null) {
                speed.setBaseValue(0.28);
            }
        } else if (phase == 3) {
            if (attack != null) {
                attack.setBaseValue(18.0);
            }
            if (speed != null) {
                speed.setBaseValue(0.32);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FIRE_AMBIENT;
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

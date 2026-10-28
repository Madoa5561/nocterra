package com.nocterra.boss;

import com.nocterra.entity.AshenWraithEntity;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * ストーリーボス「黒冠の王」（PURPLE ボスバー・**3 フェーズ**）。
 *
 * <p>フェーズ 1（HP 60% まで）: 王剣の斬撃 / 虚無の波動。<br>
 * フェーズ 2（HP 60%〜30%）: 亡霊騎士 2 体を召喚し、波動が広がる。<br>
 * フェーズ 3（HP 30% 以下）: 全攻撃が強化され、移動速度が上がる。</p>
 */
public class BlackCrownedKingEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(BlackCrownedKingEntity.class, EntityDataSerializers.INT);

    public BlackCrownedKingEntity(EntityType<? extends BlackCrownedKingEntity> type, Level level) {
        super(type, level, "black_crowned_king", BossEvent.BossBarColor.PURPLE);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
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
        return ParticleTypes.PORTAL;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.RAVAGER_ATTACK : SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return 49.0;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 18 : 24;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean slash = this.random.nextInt(5) < (this.getPhase() >= 2 ? 2 : 3);
        this.startAttack(slash ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.voidWave(level);
            default -> this.crownedSlash(level);
        }
    }

    /** 王剣の斬撃: 近接単体（フェーズでダメージ上昇）。 */
    private void crownedSlash(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 25.0) {
            return;
        }
        float damage = switch (this.getPhase()) {
            case 3 -> 22.0F;
            case 2 -> 18.0F;
            default -> 15.0F;
        };
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        Vec3 direction = awayDirection(this, target);
        if (direction != Vec3.ZERO) {
            target.push(direction.x * 0.5, 0.4, direction.z * 0.5);
        }
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1.2, target.getZ(), 26, 0.4, 0.6, 0.4, 0.05);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.3F, 0.6F);
    }

    /** 虚無の波動: 半径（6/7/8）にダメージ + 引き寄せ + 弱体化。 */
    private void voidWave(ServerLevel level) {
        double radius = switch (this.getPhase()) {
            case 3 -> 8.0;
            case 2 -> 7.0;
            default -> 6.0;
        };
        float damage = switch (this.getPhase()) {
            case 3 -> 15.0F;
            case 2 -> 12.0F;
            default -> 10.0F;
        };
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            Vec3 direction = awayDirection(this, player).scale(-1.0);
            if (direction != Vec3.ZERO) {
                player.push(direction.x * 1.2, 0.35, direction.z * 1.2);
            }
        }
        for (int i = 0; i < 44; i++) {
            double angle = (Math.PI * 2.0) * i / 44.0;
            level.sendParticles(ParticleTypes.PORTAL,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.4 + (i % 8) * 0.3,
                    this.getZ() + Math.sin(angle) * radius,
                    2, 0.1, 0.1, 0.1, 0.03);
        }
        this.playSound(SoundEvents.WITHER_DEATH, 1.2F, 0.7F);
    }

    @Override
    protected void summonMinions(ServerLevel level) {
        List<AshenWraithEntity> existing = level.getEntitiesOfClass(AshenWraithEntity.class,
                this.getBoundingBox().inflate(24.0), LivingEntity::isAlive);
        if (existing.size() >= 4) {
            this.summonCooldown = 100;
            return;
        }
        EntityType<AshenWraithEntity> type = NocterraEntities.ASHEN_WRAITH.get();
        for (int i = 0; i < 2; i++) {
            AshenWraithEntity wraith = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (wraith == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double x = this.getX() + Math.cos(angle) * 3.0;
            double z = this.getZ() + Math.sin(angle) * 3.0;
            wraith.snapTo(x, this.getY(), z, this.getYRot(), 0.0F);
            if (this.getTarget() != null) {
                wraith.setTarget(this.getTarget());
            }
            level.addFreshEntity(wraith);
            level.sendParticles(ParticleTypes.PORTAL, x, this.getY() + 1.0, z, 20, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.6F);
        this.summonCooldown = 240;
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (phase == 2) {
            if (speed != null) {
                speed.setBaseValue(0.24);
            }
        } else if (phase == 3) {
            if (attack != null) {
                attack.setBaseValue(20.0);
            }
            if (speed != null) {
                speed.setBaseValue(0.28);
            }
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

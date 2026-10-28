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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ボス「断罪の騎士長」（亡国の城塞 / RED ボスバー）。
 *
 * <p>フェーズ 1: 断罪の斬撃（近接 11）/ 断罪の審判（半径 6 の裁き 9 + ノックバック）。<br>
 * フェーズ 2（HP 50%）: 亡霊兵 2 体を召喚し、攻撃テンポが上がる。</p>
 */
public class CondemnedKnightLordEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CondemnedKnightLordEntity.class, EntityDataSerializers.INT);

    private static final double JUDGMENT_RADIUS = 6.0;

    public CondemnedKnightLordEntity(EntityType<? extends CondemnedKnightLordEntity> type, Level level) {
        super(type, level, "condemned_knight_lord", BossEvent.BossBarColor.RED);
        this.xpReward = 260;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 9.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
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
    protected float phaseThreshold(int phase) {
        return 0.5F;
    }

    @Override
    protected ParticleOptions effectParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
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
        return 36.0;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 16 : 20;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean slash = this.getPhase() >= 2
                ? this.random.nextInt(5) < 2
                : this.random.nextInt(5) < 3;
        this.startAttack(slash ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.judgment(level);
            default -> this.slash(level);
        }
    }

    /** 断罪の斬撃: 近接単体。 */
    private void slash(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 20.25) {
            return;
        }
        target.hurtServer(level, level.damageSources().mobAttack(this), 11.0F);
        target.push(0.0, 0.35, 0.0);
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1.2, target.getZ(), 24, 0.4, 0.5, 0.4, 0.05);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.2F, 0.7F);
    }

    /** 断罪の審判: 半径 6 の裁き（ダメージ + ノックバック）。 */
    private void judgment(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(JUDGMENT_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().mobAttack(this), 9.0F);
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                player.push(dx / length * 1.2, 0.55, dz / length * 1.2);
            }
        }
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2.0) * i / 36.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + Math.cos(angle) * JUDGMENT_RADIUS * 0.9,
                    this.getY() + 0.3,
                    this.getZ() + Math.sin(angle) * JUDGMENT_RADIUS * 0.9,
                    3, 0.1, 0.2, 0.1, 0.03);
        }
        this.playSound(SoundEvents.WITHER_DEATH, 1.2F, 0.6F);
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
            level.sendParticles(ParticleTypes.SOUL, x, this.getY() + 1.0, z, 20, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.6F);
        this.summonCooldown = 240;
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

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ボス「深淵の海帝」（深淵の海 / BLUE ボスバー）。水中戦対応（溺れない・水陸両用移動）。
 *
 * <p>フェーズ 1: 潮汐の一撃（近接 10 + ノックバック）/ 深海の渦（半径 5 に 8 + 移動速度低下 + 引き寄せ）。<br>
 * フェーズ 2（HP 50%）: 深海の亡霊 2 体を召喚（水中呼吸付き）。</p>
 */
public class AbyssalSeaEmperorEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AbyssalSeaEmperorEntity.class, EntityDataSerializers.INT);

    private static final double VORTEX_RADIUS = 5.0;

    public AbyssalSeaEmperorEntity(EntityType<? extends AbyssalSeaEmperorEntity> type, Level level) {
        super(type, level, "abyssal_sea_emperor", BossEvent.BossBarColor.BLUE);
        this.xpReward = 280;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
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

    // ------------------------------------------------------------------
    // 水陸両用移動
    // ------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        if (this.isUnderWater()) {
            this.moveRelative(0.02F, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travelInWater(input, baseGravity, isFalling, oldY);
        }
    }

    @Override
    protected void moveTowardTarget(ServerLevel level, LivingEntity target, double distSq) {
        if (distSq > 9.0) {
            this.getNavigation().moveTo(target, this.isUnderWater() ? 1.4 : 1.0);
        } else {
            this.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------
    // 設定
    // ------------------------------------------------------------------

    @Override
    protected float phaseThreshold(int phase) {
        return 0.5F;
    }

    @Override
    protected ParticleOptions effectParticle() {
        return ParticleTypes.BUBBLE;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.ELDER_GUARDIAN_CURSE;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.GUARDIAN_ATTACK : SoundEvents.CONDUIT_ACTIVATE;
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
        return variant == 0 ? 16 : 24;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean tide = this.random.nextInt(5) < (this.getPhase() >= 2 ? 2 : 3);
        this.startAttack(tide ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.vortex(level);
            default -> this.tideStrike(level);
        }
    }

    /** 潮汐の一撃: 近接 + ノックバック。 */
    private void tideStrike(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 20.25) {
            return;
        }
        target.hurtServer(level, level.damageSources().mobAttack(this), 10.0F);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 1.0E-4) {
            target.push(dx / length * 0.9, 0.5, dz / length * 0.9);
        }
        level.sendParticles(ParticleTypes.SPLASH,
                target.getX(), target.getY() + 1.0, target.getZ(), 26, 0.4, 0.6, 0.4, 0.08);
        this.playSound(SoundEvents.GUARDIAN_ATTACK, 1.2F, 0.7F);
    }

    /** 深海の渦: 半径 5 にダメージ + 移動速度低下 + 中心へ引き寄せ。 */
    private void vortex(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(VORTEX_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), 8.0F);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            double dx = this.getX() - player.getX();
            double dz = this.getZ() - player.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                player.push(dx / length * 1.1, 0.35, dz / length * 1.1);
            }
        }
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0) * i / 40.0;
            double r = VORTEX_RADIUS * (1.0 - (i % 5) * 0.15);
            level.sendParticles(ParticleTypes.BUBBLE,
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + 0.4 + (i % 6) * 0.3,
                    this.getZ() + Math.sin(angle) * r,
                    2, 0.1, 0.1, 0.1, 0.02);
            level.sendParticles(ParticleTypes.SPLASH,
                    this.getX() + Math.cos(angle) * r * 0.7,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * r * 0.7,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F, 0.7F);
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
            // 水中でも活動できるよう水中呼吸を付与（召喚された亡霊は溺れない）
            wisp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 2400, 0));
            if (this.getTarget() != null) {
                wisp.setTarget(this.getTarget());
            }
            level.addFreshEntity(wisp);
            level.sendParticles(ParticleTypes.BUBBLE, x, this.getY() + 1.0, z, 20, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.CONDUIT_ACTIVATE, 1.4F, 0.6F);
        this.summonCooldown = 240;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }
}

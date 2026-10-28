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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ボス「墜星竜アステル」（星骸の浮島 / PURPLE ボスバー）。飛行・滑空型。
 *
 * <p>フェーズ 1: 星屑のブレス（最大 24m の即時魔法 10、END_ROD の弾道）/ 墜星の爪（近接 12 + ノックバック）。<br>
 * フェーズ 2（HP 50%）: 攻撃力と飛行速度が上がり、ブレスが 12 / 爪が 14 + 半径 6 の流星衝撃に変化する。</p>
 */
public class AstelFallenStarDragonEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AstelFallenStarDragonEntity.class, EntityDataSerializers.INT);

    private static final double BREATH_RANGE_SQR = 576.0;
    private static final double METEOR_RADIUS = 6.0;

    public AstelFallenStarDragonEntity(EntityType<? extends AstelFallenStarDragonEntity> type, Level level) {
        super(type, level, "astel_fallen_star_dragon", BossEvent.BossBarColor.PURPLE);
        this.xpReward = 320;
        this.moveControl = new FlyingMoveControl<>(this, 25, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 420.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FLYING_SPEED, 0.42)
                .add(Attributes.FOLLOW_RANGE, 64.0);
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
    // 飛行移動
    // ------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    @Override
    protected void moveTowardTarget(ServerLevel level, LivingEntity target, double distSq) {
        if (distSq > 16.0) {
            this.getNavigation().moveTo(target, this.getPhase() >= 2 ? 1.4 : 1.2);
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
        return ParticleTypes.END_ROD;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.ENDER_DRAGON_GROWL;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.ENDER_DRAGON_GROWL;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.ENDER_DRAGON_SHOOT : SoundEvents.ENDER_DRAGON_FLAP;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return BREATH_RANGE_SQR;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 24 : 16;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        double distSq = this.distanceToSqr(target);
        // 遠距離はブレス、近距離は爪。フェーズ2は爪の比率が上がる。
        boolean breath = distSq > 36.0 || this.random.nextInt(5) < (this.getPhase() >= 2 ? 2 : 3);
        this.startAttack(breath ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.starClaw(level);
            default -> this.stardustBreath(level);
        }
    }

    /** 星屑のブレス: 視線が通れば遠距離魔法ダメージ。END_ROD の弾道。 */
    private void stardustBreath(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > BREATH_RANGE_SQR) {
            return;
        }
        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        int steps = (int) Math.min(48, Math.max(2, distance));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (this.hasLineOfSight(target)) {
            float damage = this.getPhase() >= 2 ? 12.0F : 10.0F;
            target.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.2F, 0.8F);
        }
    }

    /** 墜星の爪: 近接 + ノックバック。フェーズ2は流星衝撃（半径 6 の AoE）に変化。 */
    private void starClaw(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (this.getPhase() >= 2) {
            List<Player> hit = level.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(METEOR_RADIUS),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
            for (Player player : hit) {
                player.hurtServer(level, level.damageSources().mobAttack(this), 14.0F);
                Vec3 direction = awayDirection(this, player);
                if (direction != Vec3.ZERO) {
                    player.push(direction.x * 1.3, 0.6, direction.z * 1.3);
                }
            }
            for (int i = 0; i < 40; i++) {
                double angle = (Math.PI * 2.0) * i / 40.0;
                level.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * METEOR_RADIUS,
                        this.getY() + 0.2 + (i % 8) * 0.25,
                        this.getZ() + Math.sin(angle) * METEOR_RADIUS,
                        2, 0.1, 0.1, 0.1, 0.03);
            }
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.4F, 0.6F);
            return;
        }
        if (target == null || this.distanceToSqr(target) > 30.25) {
            return;
        }
        target.hurtServer(level, level.damageSources().mobAttack(this), 12.0F);
        Vec3 direction = awayDirection(this, target);
        if (direction != Vec3.ZERO) {
            target.push(direction.x * 1.0, 0.5, direction.z * 1.0);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + 1.2, target.getZ(), 26, 0.4, 0.5, 0.4, 0.05);
        this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 0.7F);
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance flying = this.getAttribute(Attributes.FLYING_SPEED);
        if (flying != null) {
            flying.setBaseValue(0.52);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }
}

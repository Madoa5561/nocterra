package com.nocterra.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 嘆きの湿原の屍鬼。鈍足・高耐久の近接型で、約 8 秒ごとに「墓場の拳」
 * （16 tick の予兆 → 半径 3.5m に範囲ダメージ + ノックバック）を放つ。
 *
 * <p>近傍探索（{@code getEntitiesOfClass}）は攻撃発動時のみ。予兆中は移動停止。</p>
 *
 * <p>アニメーション: {@code animation.nocterra.bog_ghoul.{idle,walk,attack,hurt,death}}</p>
 */
public class BogGhoulEntity extends Monster implements GeoEntity {
    public static final String CONTROLLER = "main";

    public static final String ANIM_IDLE = "animation.nocterra.bog_ghoul.idle";
    public static final String ANIM_WALK = "animation.nocterra.bog_ghoul.walk";
    public static final String ANIM_ATTACK = "animation.nocterra.bog_ghoul.attack";
    public static final String ANIM_HURT = "animation.nocterra.bog_ghoul.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.bog_ghoul.death";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);

    /** 発動間合い（5m）の二乗。 */
    private static final double POUND_TRIGGER_RANGE_SQR = 25.0;
    /** 予兆 16 tick = 0.8 秒。 */
    private static final int POUND_WINDUP_TICKS = 16;
    /** 再使用間隔 160 tick = 8 秒。 */
    private static final int POUND_COOLDOWN_TICKS = 160;
    private static final double POUND_RADIUS = 3.5;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int poundCooldown = 80;
    private int poundWindup;

    public BogGhoulEntity(EntityType<? extends BogGhoulEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.9, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.poundWindup > 0) {
            this.poundWindup--;
            this.getNavigation().stop();
            if (this.poundWindup % 4 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.4, this.getZ(), 8, 0.8, 0.2, 0.8, 0.01);
            }
            if (this.poundWindup == 0) {
                this.executePound(level);
            }
            return;
        }

        if (this.poundCooldown > 0) {
            this.poundCooldown--;
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) <= POUND_TRIGGER_RANGE_SQR) {
            this.poundWindup = POUND_WINDUP_TICKS;
            this.poundCooldown = POUND_COOLDOWN_TICKS;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.triggerAnim(CONTROLLER, "attack");
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.5F);
        }
    }

    private void executePound(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(POUND_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().mobAttack(this), 6.0F);
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                player.push(dx / length * 0.9, 0.4, dz / length * 0.9);
            }
        }
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2.0) * i / 20.0;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX() + Math.cos(angle) * POUND_RADIUS * 0.8,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * POUND_RADIUS * 0.8,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.4F, 0.5F);
    }

    // ------------------------------------------------------------------
    // デバッグ用フック（Lead 所有の dev コマンドから呼ぶ想定）
    // ------------------------------------------------------------------

    /** 開発用: 墓場の拳の予兆を即開始する（対象は現在のターゲット or 指定）。 */
    public boolean debugForcePound(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel)) {
            return false;
        }
        if (target != null) {
            this.setTarget(target);
        }
        LivingEntity current = this.getTarget();
        if (current == null) {
            return false;
        }
        this.poundWindup = POUND_WINDUP_TICKS;
        this.poundCooldown = POUND_COOLDOWN_TICKS;
        this.getLookControl().setLookAt(current, 30.0F, 30.0F);
        this.triggerAnim(CONTROLLER, "attack");
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.5F);
        return true;
    }

    /** 開発用: 予兆の残り tick を短縮する（0 で次 tick に発動）。 */
    public void debugSetPoundWindup(int ticks) {
        this.poundWindup = Math.max(0, Math.min(this.poundWindup, ticks));
    }

    /** 開発用: 状態を 1 行で返す。 */
    public String debugStatus() {
        return "hp=" + this.getHealth() + "/" + this.getMaxHealth()
                + " windup=" + this.poundWindup
                + " poundCd=" + this.poundCooldown;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean damaged = super.hurtServer(level, source, amount);
        if (damaged) {
            this.triggerAnim(CONTROLLER, "hurt");
        }
        return damaged;
    }

    @Override
    public void die(DamageSource source) {
        this.triggerAnim(CONTROLLER, "death");
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BogGhoulEntity>(CONTROLLER, 5, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(ANIM_ATTACK))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<BogGhoulEntity> test) {
        return test.isMoving() ? test.setAndContinue(WALK) : test.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

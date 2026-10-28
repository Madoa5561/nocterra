package com.nocterra.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
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
 * 嘆きの湿原を漂う亡霊。約 7 秒ごとに「魂吸い」（20 tick の予兆 → 12m 以内の対象に
 * 魔法ダメージ + 自己回復）を行う。
 *
 * <p>サーバー権威: 予兆とダメージ判定はサーバーのみ。予兆は索敵結果ではなく
 * 対象の存在と距離だけで開始し、実行時に改めて距離を確認する（毎 tick 総当たりしない）。</p>
 *
 * <p>アニメーション: {@code animation.nocterra.mourning_wisp.{idle,walk,attack,hurt,death}}</p>
 */
public class MourningWispEntity extends Monster implements GeoEntity {
    public static final String CONTROLLER = "main";

    public static final String ANIM_IDLE = "animation.nocterra.mourning_wisp.idle";
    public static final String ANIM_WALK = "animation.nocterra.mourning_wisp.walk";
    public static final String ANIM_ATTACK = "animation.nocterra.mourning_wisp.attack";
    public static final String ANIM_HURT = "animation.nocterra.mourning_wisp.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.mourning_wisp.death";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);

    /** 魂吸いの射程（12m）の二乗。 */
    private static final double DRAIN_RANGE_SQR = 144.0;
    /** 予兆時間 20 tick = 1.0 秒。 */
    private static final int DRAIN_WINDUP_TICKS = 20;
    /** 再使用までの間隔 140 tick = 7 秒。 */
    private static final int DRAIN_COOLDOWN_TICKS = 140;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int drainCooldown = 60;
    private int drainWindup;

    public MourningWispEntity(EntityType<? extends MourningWispEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.8, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.drainWindup > 0) {
            this.drainWindup--;
            this.getNavigation().stop();
            if (this.drainWindup % 5 == 0) {
                level.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.5, 0.5, 0.5, 0.01);
            }
            if (this.drainWindup == 0) {
                this.executeDrain(level);
            }
            return;
        }

        if (this.drainCooldown > 0) {
            this.drainCooldown--;
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) <= DRAIN_RANGE_SQR) {
            this.drainWindup = DRAIN_WINDUP_TICKS;
            this.drainCooldown = DRAIN_COOLDOWN_TICKS;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.triggerAnim(CONTROLLER, "attack");
            this.playSound(SoundEvents.WITHER_AMBIENT, 0.9F, 0.7F);
        }
    }

    private void executeDrain(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > DRAIN_RANGE_SQR) {
            return;
        }
        target.hurtServer(level, level.damageSources().magic(), 4.0F);
        this.heal(4.0F);
        level.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.4, 0.5, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SOUL,
                this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
        this.playSound(SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.8F);
    }

    // ------------------------------------------------------------------
    // デバッグ用フック（Lead 所有の dev コマンドから呼ぶ想定）
    // ------------------------------------------------------------------

    /** 開発用: 魂吸いの予兆を即開始する（対象は現在のターゲット or 指定）。 */
    public boolean debugForceDrain(LivingEntity target) {
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
        this.drainWindup = DRAIN_WINDUP_TICKS;
        this.drainCooldown = DRAIN_COOLDOWN_TICKS;
        this.getLookControl().setLookAt(current, 30.0F, 30.0F);
        this.triggerAnim(CONTROLLER, "attack");
        this.playSound(SoundEvents.WITHER_AMBIENT, 0.9F, 0.7F);
        return true;
    }

    /** 開発用: 予兆の残り tick を短縮する（0 で次 tick に発動）。 */
    public void debugSetDrainWindup(int ticks) {
        this.drainWindup = Math.max(0, Math.min(this.drainWindup, ticks));
    }

    /** 開発用: 状態を 1 行で返す。 */
    public String debugStatus() {
        return "hp=" + this.getHealth() + "/" + this.getMaxHealth()
                + " windup=" + this.drainWindup
                + " drainCd=" + this.drainCooldown;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MourningWispEntity>(CONTROLLER, 4, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(ANIM_ATTACK))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<MourningWispEntity> test) {
        return test.isMoving() ? test.setAndContinue(WALK) : test.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

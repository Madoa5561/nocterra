package com.nocterra.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * 灰樹の森の小型這い虫。素早い近接攻撃が得意で、攻撃を当てるたびに一時的に加速する。
 *
 * <p>アニメーション: {@code animation.nocterra.ash_crawler.{idle,walk,attack,hurt,death}}</p>
 */
public class AshCrawlerEntity extends Monster implements GeoEntity {
    public static final String CONTROLLER = "main";

    public static final String ANIM_IDLE = "animation.nocterra.ash_crawler.idle";
    public static final String ANIM_WALK = "animation.nocterra.ash_crawler.walk";
    public static final String ANIM_ATTACK = "animation.nocterra.ash_crawler.attack";
    public static final String ANIM_HURT = "animation.nocterra.ash_crawler.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.ash_crawler.death";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public AshCrawlerEntity(EntityType<? extends AshCrawlerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 攻撃を当てるたびに短時間だけ加速する（「群れの興奮」）。 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.triggerAnim(CONTROLLER, "attack");
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0));
        }
        return hit;
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
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AshCrawlerEntity>(CONTROLLER, 3, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(ANIM_ATTACK))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<AshCrawlerEntity> test) {
        return test.isMoving() ? test.setAndContinue(WALK) : test.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

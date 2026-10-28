package com.nocterra.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * 灰樹の森に潜む亡霊。近接攻撃に加え、一定間隔で短距離ブリンクを行う。
 *
 * <p>アニメーション: {@code assets/nocterra/geckolib/animations/entity/ashen_wraith.animation.json}
 * （idle / walk / attack / hurt / death）。</p>
 */
public class AshenWraithEntity extends Monster implements GeoEntity {
    /** アニメーションコントローラ名（{@code triggerAnim} の第 1 引数）。 */
    public static final String CONTROLLER = "main";

    /** アニメ名の規約: {@code animation.<modid>.<entity>.<name>}（.animation.json のキーと一致）。 */
    public static final String ANIM_IDLE = "animation.nocterra.ashen_wraith.idle";
    public static final String ANIM_WALK = "animation.nocterra.ashen_wraith.walk";
    public static final String ANIM_ATTACK = "animation.nocterra.ashen_wraith.attack";
    public static final String ANIM_HURT = "animation.nocterra.ashen_wraith.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.ashen_wraith.death";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);

    /** どのブロックも「テレポート先として不正」とはみなさない（安全判定は randomTeleport 側が行う）。 */
    private static final Predicate<BlockState> NEVER_INVALID = state -> false;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int blinkCooldown = 40;

    public AshenWraithEntity(EntityType<? extends AshenWraithEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    // ------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (--this.blinkCooldown > 0) {
            return;
        }

        LivingEntity target = this.getTarget();
        double distSq = target == null ? 0.0 : this.distanceToSqr(target);
        if (target != null && distSq > 9.0 && distSq < 400.0) {
            if (this.tryBlink(level, target)) {
                this.blinkCooldown = 80 + this.random.nextInt(60);
                return;
            }
        }
        this.blinkCooldown = 20;
    }

    /** 対象の近くへ短距離ブリンクする。成功したら true。 */
    private boolean tryBlink(ServerLevel level, LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double tx = target.getX() + (this.random.nextDouble() - 0.5) * 4.0;
            double ty = target.getY();
            double tz = target.getZ() + (this.random.nextDouble() - 0.5) * 4.0;
            if (this.randomTeleport(tx, ty, tz, true, NEVER_INVALID)) {
                if (!this.isSilent()) {
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.55F);
                }
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // デバッグ用フック（Lead 所有の dev コマンドから呼ぶ想定。通常プレイでは未使用）
    // ------------------------------------------------------------------

    /** 開発用: 現在のターゲット（または指定対象）へ即座にブリンクする。 */
    public boolean debugForceBlink(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (target != null) {
            this.setTarget(target);
        }
        LivingEntity current = this.getTarget();
        if (current == null) {
            return false;
        }
        boolean blinked = this.tryBlink(serverLevel, current);
        if (blinked) {
            this.blinkCooldown = 80;
        }
        return blinked;
    }

    /** 開発用: 状態を 1 行で返す（コマンド出力用）。 */
    public String debugStatus() {
        return "hp=" + this.getHealth() + "/" + this.getMaxHealth()
                + " blinkCd=" + this.blinkCooldown;
    }

    // ------------------------------------------------------------------
    // 戦闘フック（アニメーションはサーバーから triggerAnim で同期される）
    // ------------------------------------------------------------------

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.triggerAnim(CONTROLLER, "attack");
        return super.doHurtTarget(level, target);
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

    // ------------------------------------------------------------------
    // サウンド
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AshenWraithEntity>(CONTROLLER, 4, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(ANIM_ATTACK))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<AshenWraithEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(WALK);
        }
        return test.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

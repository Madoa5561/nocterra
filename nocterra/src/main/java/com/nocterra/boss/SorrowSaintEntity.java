package com.nocterra.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.nocterra.entity.MourningWispEntity;
import com.nocterra.entity.NocterraEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ボス「悲嘆の聖女」（嘆きの湿原）。
 *
 * <p>浮遊型のサーバー権威ボス。意思決定は 5 tick 間隔、近傍探索は攻撃発動時のみ。
 * 攻撃はすべて 16〜24 tick の予兆（SOUL/SPLASH パーティクル + 音）を挟む。</p>
 *
 * <ul>
 *   <li>フェーズ 1: 悲嘆の爪（近接 10）/ 涙の弾（最大 20m の即時魔法 8、パーティクルの筋）</li>
 *   <li>フェーズ 2 (HP 50% 以下): 上記 + 嘆きの合唱（半径 7 に 8 ダメージ + 移動速度低下）、亡霊 2 体召喚</li>
 * </ul>
 *
 * <p>アニメーション: {@code animation.nocterra.sorrow_saint.{idle,walk,attack1,attack2,hurt,death,phase2}}</p>
 */
public class SorrowSaintEntity extends Monster implements GeoEntity {
    public static final String CONTROLLER = "main";

    /** フェーズ同期用データアクセサ（1 = 通常, 2 = 嘆きの合唱）。 */
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(SorrowSaintEntity.class, EntityDataSerializers.INT);

    public static final String ANIM_IDLE = "animation.nocterra.sorrow_saint.idle";
    public static final String ANIM_WALK = "animation.nocterra.sorrow_saint.walk";
    public static final String ANIM_ATTACK1 = "animation.nocterra.sorrow_saint.attack1";
    public static final String ANIM_ATTACK2 = "animation.nocterra.sorrow_saint.attack2";
    public static final String ANIM_HURT = "animation.nocterra.sorrow_saint.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.sorrow_saint.death";
    public static final String ANIM_PHASE2 = "animation.nocterra.sorrow_saint.phase2";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);
    private static final RawAnimation PHASE2 = RawAnimation.begin().thenLoop(ANIM_PHASE2);

    /** 攻撃判断は 5 tick ごと。 */
    private static final int DECISION_INTERVAL = 5;
    /** 悲嘆の爪の予兆 16 tick = 0.8 秒。 */
    private static final int CLAW_WINDUP_TICKS = 16;
    /** 涙の弾 / 嘆きの合唱の予兆 24 tick = 1.2 秒。 */
    private static final int BOLT_WINDUP_TICKS = 24;
    private static final int CHOIR_WINDUP_TICKS = 24;
    private static final double CHOIR_RADIUS = 7.0;
    private static final double BOLT_RANGE_SQR = 400.0;
    private static final int MAX_MINIONS = 4;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.nocterra.sorrow_saint"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int attackWindup;
    private int attackCooldown;
    /** 0 = 悲嘆の爪 / 1 = 涙の弾 / 2 = 嘆きの合唱。 */
    private int attackVariant;
    private int summonCooldown = 100;
    /** 開発用: true の間は HP 由来のフェーズ自動更新を行わない。 */
    private boolean debugPhaseLocked;

    public SorrowSaintEntity(EntityType<? extends SorrowSaintEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setPersistenceRequired();
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.bossEvent.setPlayBossMusic(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FLYING_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    // ------------------------------------------------------------------
    // 浮遊移動
    // ------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    /**
     * 浮遊移動。{@code getSpeed()} は {@link FlyingMoveControl} が
     * {@code FLYING_SPEED * speedModifier} で毎 tick 設定する（Allay と同じパターン）。
     * 水中での移動速度は標準の 0.02（バニラ準拠）を維持する。
     */
    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    /**
     * 水中でも溺れない（浮遊ボス）。26.3/NeoForge では {@code canBreatheUnderwater} が非推奨のため、
     * 推奨 API の {@code canDrownInFluidType} を override する。
     */
    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ------------------------------------------------------------------
    // 同期データ / フェーズ
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
    }

    public int getPhase() {
        return this.getEntityData().get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.getEntityData().set(DATA_PHASE, phase);
    }

    // ------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        this.tickAttackTimers(level);

        if (this.tickCount % DECISION_INTERVAL == 0) {
            this.serverDecisionTick(level);
        }
    }

    private void tickAttackTimers(ServerLevel level) {
        if (this.attackWindup > 0) {
            this.attackWindup--;
            this.getNavigation().stop();
            if (this.attackWindup % 4 == 0) {
                this.emitTelegraph(level);
            }
            if (this.attackWindup == 0) {
                this.executeAttack(level);
            }
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }
    }

    private void serverDecisionTick(ServerLevel level) {
        this.updatePhase(level);

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.getNavigation().stop();
            return;
        }

        double distSq = this.distanceToSqr(target);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.attackWindup > 0) {
            return;
        }

        if (distSq > 16.0) {
            this.getNavigation().moveTo(target, 1.0);
        } else {
            this.getNavigation().stop();
        }

        if (this.attackCooldown <= 0 && distSq <= BOLT_RANGE_SQR) {
            this.beginAttack(target);
            return;
        }

        if (this.getPhase() >= 2 && this.summonCooldown <= 0) {
            this.summonMinions(level);
        }
    }

    private void updatePhase(ServerLevel level) {
        if (this.debugPhaseLocked) {
            return;
        }
        int wanted = this.getHealth() <= this.getMaxHealth() * 0.5F ? 2 : 1;
        if (wanted != this.getPhase()) {
            this.applyPhase(level, wanted);
        }
    }

    private void applyPhase(ServerLevel level, int phase) {
        this.setPhase(phase);
        if (phase == 2) {
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.6F, 0.6F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.6, this.getZ(), 80, 1.0, 1.2, 1.0, 0.05);
            AttributeInstance flying = this.getAttribute(Attributes.FLYING_SPEED);
            if (flying != null) {
                flying.setBaseValue(0.40);
            }
            this.summonCooldown = 0;
        }
    }

    // ------------------------------------------------------------------
    // 攻撃: 予兆 -> 発動
    // ------------------------------------------------------------------

    private void beginAttack(LivingEntity target) {
        double distSq = this.distanceToSqr(target);
        int variant;
        if (this.getPhase() >= 2 && distSq <= CHOIR_RADIUS * CHOIR_RADIUS && this.random.nextInt(3) == 0) {
            // 至近距離のフェーズ2のみ「嘆きの合唱」
            variant = 2;
        } else if (distSq <= 25.0) {
            // 近距離: 爪が主体、ときどき涙の弾
            variant = this.random.nextInt(3) < 2 ? 0 : 1;
        } else {
            // 中〜遠距離: 涙の弾
            variant = 1;
        }
        this.startAttack(variant, target);
    }

    private void startAttack(int variant, LivingEntity target) {
        this.attackVariant = variant;
        this.attackWindup = switch (variant) {
            case 1 -> BOLT_WINDUP_TICKS;
            case 2 -> CHOIR_WINDUP_TICKS;
            default -> CLAW_WINDUP_TICKS;
        };
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (variant == 0) {
            this.triggerAnim(CONTROLLER, "attack1");
            this.playSound(SoundEvents.WITHER_AMBIENT, 1.0F, 0.55F);
        } else {
            this.triggerAnim(CONTROLLER, "attack2");
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.1F, 0.7F);
        }
    }

    private void emitTelegraph(ServerLevel level) {
        level.sendParticles(ParticleTypes.SOUL,
                this.getX(), this.getY() + 1.4, this.getZ(), 10, 0.7, 0.7, 0.7, 0.01);
        if (this.attackVariant != 0) {
            level.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 1.4, this.getZ(), 6, 0.5, 0.5, 0.5, 0.01);
        }
    }

    private void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.tearBolt(level);
            case 2 -> this.lamentChoir(level);
            default -> this.griefClaw(level);
        }
        this.attackCooldown = this.getPhase() >= 2 ? 30 : 40;
        this.attackVariant = 0;
    }

    /** 悲嘆の爪: 近接単体。 */
    private void griefClaw(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 25.0) {
            return;
        }
        target.hurtServer(level, level.damageSources().mobAttack(this), 10.0F);
        level.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.6F);
    }

    /** 涙の弾: 視線が通れば即時魔法ダメージ。弾道はパーティクルの筋で表現。 */
    private void tearBolt(ServerLevel level) {
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
            level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.0);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (this.hasLineOfSight(target)) {
            target.hurtServer(level, level.damageSources().indirectMagic(this, this), 8.0F);
            this.playSound(SoundEvents.GENERIC_SPLASH, 1.0F, 1.3F);
        }
    }

    /** 嘆きの合唱: 半径 7 の AoE + 移動速度低下 + 亡霊召喚。 */
    private void lamentChoir(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(CHOIR_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), 8.0F);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        }
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0) * i / 40.0;
            double x = this.getX() + Math.cos(angle) * CHOIR_RADIUS;
            double z = this.getZ() + Math.sin(angle) * CHOIR_RADIUS;
            level.sendParticles(ParticleTypes.SOUL, x, this.getY() + 0.6, z, 2, 0.1, 0.1, 0.1, 0.02);
            level.sendParticles(ParticleTypes.SPLASH, x, this.getY() + 0.2, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        this.playSound(SoundEvents.WITHER_DEATH, 1.2F, 0.7F);
        this.summonMinions(level);
    }

    private void summonMinions(ServerLevel level) {
        List<MourningWispEntity> existing = level.getEntitiesOfClass(MourningWispEntity.class,
                this.getBoundingBox().inflate(24.0), LivingEntity::isAlive);
        if (existing.size() >= MAX_MINIONS) {
            this.summonCooldown = 100;
            return;
        }
        EntityType<MourningWispEntity> wispType = NocterraEntities.MOURNING_WISP.get();
        for (int i = 0; i < 2; i++) {
            MourningWispEntity wisp = wispType.create(level, EntitySpawnReason.MOB_SUMMONED);
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
            level.sendParticles(ParticleTypes.SOUL, x, this.getY() + 1.0, z, 24, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.6F);
        this.summonCooldown = 240;
    }

    // ------------------------------------------------------------------
    // 死亡 / ボスバー
    // ------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        this.triggerAnim(CONTROLLER, "death");
        if (this.level() instanceof ServerLevel serverLevel) {
            this.bossEvent.setProgress(0.0F);
            this.bossEvent.removeAllPlayers();
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 1.4, this.getZ(), 120, 1.4, 1.6, 1.4, 0.06);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            this.bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------
    // デバッグ用フック（Lead 所有の dev コマンドから呼ぶ想定）
    // ------------------------------------------------------------------

    /** 開発用: フェーズを強制する（HP 由来の自動更新を止める）。 */
    public boolean debugForcePhase(int phase) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (phase < 1 || phase > 2) {
            return false;
        }
        this.debugPhaseLocked = true;
        this.applyPhase(serverLevel, phase);
        return true;
    }

    /** 開発用: フェーズ強制を解除する。 */
    public void debugReleasePhaseLock() {
        this.debugPhaseLocked = false;
    }

    /** 開発用: 指定バリアント（0=爪 / 1=涙の弾 / 2=合唱）の予兆を開始する。 */
    public boolean debugForceAttack(int variant, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel)) {
            return false;
        }
        if (variant < 0 || variant > 2) {
            return false;
        }
        if (target != null) {
            this.setTarget(target);
        }
        LivingEntity current = this.getTarget();
        if (current == null) {
            return false;
        }
        this.startAttack(variant, current);
        return true;
    }

    /** 開発用: 予兆の残り tick を短縮する（0 で次 tick に発動）。 */
    public void debugSetAttackWindup(int ticks) {
        // 0 を渡したら「次の tick で発動」になるよう最低 1 に丸める。
        // attackWindup が 0 のとき（攻撃予兆が無いとき）は何もしない。
        if (this.attackWindup > 0) {
            this.attackWindup = Math.max(1, Math.min(this.attackWindup, ticks));
        }
    }

    /** 開発用: 亡霊召喚を即座に実行する（CD を無視）。 */
    public boolean debugSummonMinions() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        this.summonCooldown = 0;
        this.summonMinions(serverLevel);
        return true;
    }

    /** 開発用: 状態を 1 行で返す。 */
    public String debugStatus() {
        return "phase=" + this.getPhase()
                + " hp=" + this.getHealth() + "/" + this.getMaxHealth()
                + " windup=" + this.attackWindup
                + " attackCd=" + this.attackCooldown
                + " summonCd=" + this.summonCooldown
                + " variant=" + this.attackVariant
                + " phaseLocked=" + this.debugPhaseLocked;
    }

    // ------------------------------------------------------------------
    // サウンド
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SorrowSaintEntity>(CONTROLLER, 6, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack1", RawAnimation.begin().thenPlay(ANIM_ATTACK1))
                .triggerableAnim("attack2", RawAnimation.begin().thenPlay(ANIM_ATTACK2))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<SorrowSaintEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(WALK);
        }
        return test.setAndContinue(this.getPhase() >= 2 ? PHASE2 : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

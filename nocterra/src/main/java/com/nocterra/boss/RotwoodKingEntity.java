package com.nocterra.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.nocterra.entity.AshenWraithEntity;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ボス「腐朽の森王」。
 *
 * <p>サーバー権威の状態機械で動く:
 * <ul>
 *   <li>フェーズ 1: 近接 3 種（爪薙ぎ / 拳槌 / 呪詛）</li>
 *   <li>フェーズ 2 (HP 50% 以下): 近接 3 種 + 腐敗の新星（範囲攻撃 + 亡霊召喚）</li>
 * </ul>
 * 攻撃はすべて 0.8 秒（16 tick）の予兆を挟み、予兆中は移動を止めてパーティクルと音で通知する。
 * フェーズは {@link EntityDataAccessor} で同期し、クライアント側のアニメーション選択にも使う。</p>
 */
public class RotwoodKingEntity extends Monster implements GeoEntity {
    /** アニメーションコントローラ名（{@code triggerAnim} の第 1 引数）。 */
    public static final String CONTROLLER = "main";

    /** フェーズを同期するデータアクセサ（1 = 通常, 2 = 激昂）。 */
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(RotwoodKingEntity.class, EntityDataSerializers.INT);

    /** 攻撃判断（近傍探索などを含む）は 5 tick ごと。 */
    private static final int DECISION_INTERVAL = 5;
    /** 攻撃の予兆時間: 16 tick = 0.8 秒。 */
    private static final int WINDUP_TICKS = 16;
    /** フェーズ 2 で召喚する亡霊の最大同時数。 */
    private static final int MAX_MINIONS = 4;

    /** アニメ名の規約: {@code animation.<modid>.<entity>.<name>}（.animation.json のキーと一致）。 */
    public static final String ANIM_IDLE = "animation.nocterra.rotwood_king.idle";
    public static final String ANIM_IDLE_ENRAGED = "animation.nocterra.rotwood_king.idle_enraged";
    public static final String ANIM_WALK = "animation.nocterra.rotwood_king.walk";
    public static final String ANIM_ATTACK_SWEEP = "animation.nocterra.rotwood_king.attack_sweep";
    public static final String ANIM_ATTACK_SLAM = "animation.nocterra.rotwood_king.attack_slam";
    public static final String ANIM_ATTACK_CAST = "animation.nocterra.rotwood_king.attack_cast";
    public static final String ANIM_HURT = "animation.nocterra.rotwood_king.hurt";
    public static final String ANIM_DEATH = "animation.nocterra.rotwood_king.death";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation IDLE_ENRAGED = RawAnimation.begin().thenLoop(ANIM_IDLE_ENRAGED);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(ANIM_WALK);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.nocterra.rotwood_king"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    /** > 0 の間は予兆中（移動停止）。 */
    /** 開発用: true の間は HP 由来のフェーズ自動更新を行わない（{@link #debugForcePhase} 用）。 */
    private boolean debugPhaseLocked;

    private int attackWindup;
    private int attackCooldown;
    /** 0..3: 0 爪薙ぎ / 1 拳槌 / 2 呪詛 / 3 腐敗の新星。 */
    private int attackVariant;
    private int summonCooldown = 100;

    public RotwoodKingEntity(EntityType<? extends RotwoodKingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setPersistenceRequired();
        this.bossEvent.setPlayBossMusic(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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

    /** 毎 tick の軽量タイマー更新。 */
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

    /** 5 tick ごとの意思決定。近傍探索はここ（＝状態遷移時）だけで行う。 */
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

        if (distSq > 9.0) {
            this.getNavigation().moveTo(target, 1.0);
        } else {
            this.getNavigation().stop();
        }

        if (this.attackCooldown <= 0 && distSq <= 36.0) {
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

    /** フェーズを適用する（EntityDataAccessor 同期 + 移行演出 + 速度上昇）。 */
    private void applyPhase(ServerLevel level, int phase) {
        this.setPhase(phase);
        if (phase == 2) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.5F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.8, this.getZ(), 80, 1.0, 1.2, 1.0, 0.05);
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.30);
            }
            this.summonCooldown = 0;
        }
    }

    // ------------------------------------------------------------------
    // 攻撃: 予兆 -> 発動
    // ------------------------------------------------------------------

    private void beginAttack(LivingEntity target) {
        int variant = this.getPhase() >= 2 ? this.random.nextInt(4) : this.random.nextInt(3);
        this.startAttack(variant, target);
    }

    /** 指定バリアントで予兆を開始する（{@link #beginAttack} とデバッグ用の共通経路）。 */
    private void startAttack(int variant, LivingEntity target) {
        this.attackVariant = variant;
        this.attackWindup = WINDUP_TICKS;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (variant) {
            case 1 -> this.triggerAnim(CONTROLLER, "attack_slam");
            case 2 -> this.triggerAnim(CONTROLLER, "attack_cast");
            case 3 -> {
                this.triggerAnim(CONTROLLER, "attack_cast");
                this.playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 0.55F);
            }
            default -> this.triggerAnim(CONTROLLER, "attack_sweep");
        }
    }

    /** 予兆中の演出（パーティクル + 音）。予兆解除の直前に強くする。 */
    private void emitTelegraph(ServerLevel level) {
        double y = this.getY() + 1.6;
        level.sendParticles(ParticleTypes.SOUL,
                this.getX(), y, this.getZ(), 10, 0.7, 0.6, 0.7, 0.01);
        if (this.attackWindup % 8 == 0) {
            this.playSound(SoundEvents.WITHER_AMBIENT, 0.9F, 0.55F);
        }
    }

    private void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 0 -> this.meleeStrike(level, 12.0F);
            case 1 -> this.meleeSlam(level, 3.5, 10.0F);
            case 2 -> this.meleeSlam(level, 4.5, 8.0F);
            default -> this.rotNova(level);
        }
        this.attackCooldown = this.getPhase() >= 2 ? 30 : 50;
        this.attackVariant = 0;
    }

    private void meleeStrike(ServerLevel level, float damage) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 16.0) {
            return;
        }
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        this.applyKnockback(target, 0.6);
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0, target.getZ(),
                18, 0.4, 0.6, 0.4, 0.03);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.2F, 0.7F);
    }

    private void meleeSlam(ServerLevel level, double radius, float damage) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().mobAttack(this), damage);
            this.applyKnockback(player, 1.2);
        }
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0) * i / 24.0;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX() + Math.cos(angle) * radius * 0.7,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * radius * 0.7,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.4F, 0.45F);
    }

    private void rotNova(ServerLevel level) {
        double radius = 6.0;
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().mobAttack(this), 9.0F);
            this.applyKnockback(player, 1.8);
        }
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2.0) * i / 36.0;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.3,
                    this.getZ() + Math.sin(angle) * radius,
                    3, 0.15, 0.15, 0.15, 0.02);
        }
        this.playSound(SoundEvents.WITHER_DEATH, 1.0F, 0.6F);
        this.summonMinions(level);
    }

    private void applyKnockback(LivingEntity target, double strength) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) {
            return;
        }
        target.push(dx / length * strength, 0.45, dz / length * strength);
    }

    private void summonMinions(ServerLevel level) {
        List<AshenWraithEntity> existing = level.getEntitiesOfClass(AshenWraithEntity.class,
                this.getBoundingBox().inflate(24.0), LivingEntity::isAlive);
        if (existing.size() >= MAX_MINIONS) {
            this.summonCooldown = 100;
            return;
        }

        EntityType<AshenWraithEntity> wraithType = NocterraEntities.ASHEN_WRAITH.get();
        for (int i = 0; i < 2; i++) {
            AshenWraithEntity wraith = wraithType.create(level, EntitySpawnReason.MOB_SUMMONED);
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
            // 永続化はしない: ボス戦後にプレイヤーが離れれば通常のデスポーンに任せる
            // （召喚のたびに永続モブが蓄積してワールドを汚さないように）。
            level.addFreshEntity(wraith);
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
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.5, this.getZ(), 120, 1.4, 1.6, 1.4, 0.06);
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
    // デバッグ用フック（Lead 所有の dev コマンドから呼ぶ想定。通常プレイでは未使用）
    // ------------------------------------------------------------------

    /**
     * 開発用: フェーズを強制する。HP を変えずに、同期（{@code EntityDataAccessor}）と
     * 移行演出・クライアント側 idle_enraged 選択を検証できる。
     * 強制中は HP 由来の自動更新を止める（解除は {@link #debugReleasePhaseLock()}）。
     */
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

    /** 開発用: フェーズの強制を解除し、HP 由来の自動更新に戻す。 */
    public void debugReleasePhaseLock() {
        this.debugPhaseLocked = false;
    }

    /**
     * 開発用: 指定バリアントの攻撃予兆を開始する。{@code target} を渡すとその対象を狙う
     * （null なら現在のターゲット）。予兆の後、通常どおり {@link #executeAttack} が走る。
     */
    public boolean debugForceAttack(int variant, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel)) {
            return false;
        }
        if (variant < 0 || variant > 3) {
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

    /** 開発用: 状態を 1 行で返す（コマンド出力用）。 */
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
        controllers.add(new AnimationController<RotwoodKingEntity>(CONTROLLER, 6, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("attack_sweep", RawAnimation.begin().thenPlay(ANIM_ATTACK_SWEEP))
                .triggerableAnim("attack_slam", RawAnimation.begin().thenPlay(ANIM_ATTACK_SLAM))
                .triggerableAnim("attack_cast", RawAnimation.begin().thenPlay(ANIM_ATTACK_CAST))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM_HURT))
                .triggerableAnim("death", RawAnimation.begin().thenPlay(ANIM_DEATH)));
    }

    private PlayState animationPredicate(AnimationTest<RotwoodKingEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(WALK);
        }
        return test.setAndContinue(this.getPhase() >= 2 ? IDLE_ENRAGED : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

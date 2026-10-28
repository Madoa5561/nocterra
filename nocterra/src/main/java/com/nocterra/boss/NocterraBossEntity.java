package com.nocterra.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * NOCTERRA のボス共通基底（サーバー権威の状態機械）。
 *
 * <p>この基底が持つもの:
 * <ul>
 *   <li>{@link ServerBossEvent}（名前は必ず {@code entity.nocterra.<bossId>} の翻訳キー）と
 *       追跡プレイヤーの追加/削除・HP 進捗更新</li>
 *   <li>フェーズ同期（{@link EntityDataAccessor} は各ボスが {@code DATA_PHASE} として定義）、
 *       HP 割合による自動移行（2〜3 フェーズ）</li>
 *   <li>攻撃の予兆ステートマシン（{@link #startAttack} → 0.8〜1.2 秒の予兆 → {@link #executeAttack}）</li>
 *   <li>フェーズ移行の画面演出（リングパーティクル + 咆哮 + 周囲ノックバック）と死亡演出</li>
 *   <li>5 tick 間隔の意思決定（近傍探索は状態遷移時のみ）</li>
 *   <li>{@code debug*} フック（Lead 所有の dev コマンドから呼ぶ）</li>
 * </ul>
 *
 * <p>サブクラスが実装するもの: 属性、アニメーション（idle/walk/attack1/attack2/hurt/death/phase2[/phase3]）、
 * 攻撃バリアント、予兆/移行/死亡のパーティクルとサウンド、移動（地上/水中/飛行）と召喚。</p>
 */
public abstract class NocterraBossEntity extends Monster implements GeoEntity {
    /** GeckoLib のアニメーションコントローラ名。 */
    public static final String CONTROLLER = "main";

    protected static final int DECISION_INTERVAL = 5;
    /** 予兆中の演出間隔（tick）。 */
    protected static final int TELEGRAPH_FX_INTERVAL = 4;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent;
    private final String bossId;
    private final Map<String, RawAnimation> loopCache = new HashMap<>();

    /** > 0 の間は攻撃の予兆中（移動停止）。 */
    protected int attackWindup;
    protected int attackCooldown;
    protected int attackVariant;
    protected int summonCooldown = 100;
    /** 開発用: true の間は HP 由来のフェーズ自動更新を行わない。 */
    protected boolean debugPhaseLocked;

    protected NocterraBossEntity(EntityType<? extends NocterraBossEntity> type, Level level,
                                 String bossId, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossId = bossId;
        this.bossEvent = new ServerBossEvent(
                UUID.randomUUID(),
                Component.translatable("entity.nocterra." + bossId),
                color,
                BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setPlayBossMusic(true);
        this.setPersistenceRequired();
        this.xpReward = 200;
    }

    /** 翻訳キーに使うボス ID（例: {@code condemned_knight_lord}）。 */
    public final String bossId() {
        return this.bossId;
    }

    /** アニメーション名の規約: {@code animation.nocterra.<bossId>.<key>}。 */
    public final String animName(String key) {
        return "animation.nocterra." + this.bossId + "." + key;
    }

    // ------------------------------------------------------------------
    // サブクラスが実装する設定
    // ------------------------------------------------------------------

    /** フェーズ数（2 または 3）。 */
    protected int phaseCount() {
        return 2;
    }

    /**
     * 次フェーズへ移行する HP 割合（例: phase=2 なら 0.5、phase=3 なら 0.25）。
     */
    protected abstract float phaseThreshold(int phase);

    /** 予兆・フェーズ移行・死亡で使うパーティクル（ボスごとに変える）。 */
    protected abstract ParticleOptions effectParticle();

    /** 予兆中の音。 */
    protected abstract SoundEvent telegraphSound();

    /** フェーズ移行時の咆哮音。 */
    protected abstract SoundEvent phaseSound();

    /** 攻撃開始（予兆開始）時の音。 */
    protected abstract SoundEvent attackStartSound(int variant);

    /** 攻撃バリアント数（0 .. count-1）。 */
    protected abstract int attackVariantCount();

    /** 攻撃開始に必要な距離の二乗。 */
    protected abstract double attackRangeSqr();

    /** バリアントごとの予兆 tick（0.8〜1.2 秒 = 16〜24 tick）。 */
    protected abstract int windupFor(int variant);

    /** 予兆開始時のアニメーション（"attack1" / "attack2" を triggerAnim する）。 */
    protected abstract void triggerAttackAnimation(int variant);

    /** 予兆終了時の攻撃本体。 */
    protected abstract void executeAttack(ServerLevel level);

    /** デフォルトの移動。水中ボス・飛行ボスは override する。 */
    protected void moveTowardTarget(ServerLevel level, LivingEntity target, double distSq) {
        if (distSq > 9.0) {
            this.getNavigation().moveTo(target, 1.0);
        } else {
            this.getNavigation().stop();
        }
    }

    /** フェーズ 2 以降の召喚。デフォルトは何もしない（CD だけ進める）。 */
    protected void summonMinions(ServerLevel level) {
        this.summonCooldown = 240;
    }

    /** フェーズ移行時の追加処理（速度上昇など）。 */
    protected void onPhaseApplied(ServerLevel level, int phase) {}

    /** フェーズ移行時にノックバックする半径。 */
    protected double phaseKnockbackRadius() {
        return 7.0;
    }

    // ------------------------------------------------------------------
    // 同期データ / フェーズ
    // ------------------------------------------------------------------

    /** 各ボスが定義した {@code DATA_PHASE} を返す。 */
    protected abstract EntityDataAccessor<Integer> phaseAccessor();

    public int getPhase() {
        return this.getEntityData().get(this.phaseAccessor());
    }

    protected void setPhase(int phase) {
        this.getEntityData().set(this.phaseAccessor(), phase);
    }

    /** 水中でも溺れない（ボスは全員溺死しない）。 */
    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ------------------------------------------------------------------
    // AI / tick
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

    /** 毎 tick の軽量タイマー。予兆→発動の遷移だけを扱う。 */
    protected void tickAttackTimers(ServerLevel level) {
        if (this.attackWindup > 0) {
            this.attackWindup--;
            this.getNavigation().stop();
            if (this.attackWindup % TELEGRAPH_FX_INTERVAL == 0) {
                this.emitTelegraph(level);
            }
            if (this.attackWindup == 0) {
                this.executeAttack(level);
                this.attackCooldown = this.attackCooldownTicks();
                this.attackVariant = 0;
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

    /** 5 tick ごとの意思決定（重い処理はここだけ）。 */
    protected void serverDecisionTick(ServerLevel level) {
        this.updatePhase(level);

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.getNavigation().stop();
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.attackWindup > 0) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        this.moveTowardTarget(level, target, distSq);

        if (this.attackCooldown <= 0 && distSq <= this.attackRangeSqr()) {
            this.beginAttack(target);
            return;
        }

        if (this.getPhase() >= 2 && this.summonCooldown <= 0) {
            this.summonMinions(level);
        }
    }

    protected int attackCooldownTicks() {
        return this.getPhase() >= 2 ? 30 : 50;
    }

    /** バリアントを選んで攻撃を開始する。 */
    protected abstract void beginAttack(LivingEntity target);

    /** 予兆を開始する（デバッグからも同じ経路を使う）。 */
    protected void startAttack(int variant, LivingEntity target) {
        this.attackVariant = variant;
        this.attackWindup = this.windupFor(variant);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.triggerAttackAnimation(variant);
        SoundEvent sound = this.attackStartSound(variant);
        if (sound != null) {
            this.playSound(sound, 1.1F, 0.65F);
        }
    }

    /** 予兆中の演出。必要ならサブクラスで上書きする。 */
    protected void emitTelegraph(ServerLevel level) {
        level.sendParticles(this.effectParticle(),
                this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.7, 0.7, 0.7, 0.01);
        if (this.attackWindup % 8 == 0) {
            SoundEvent sound = this.telegraphSound();
            if (sound != null) {
                this.playSound(sound, 1.0F, 0.55F);
            }
        }
    }

    // ------------------------------------------------------------------
    // フェーズ
    // ------------------------------------------------------------------

    protected void updatePhase(ServerLevel level) {
        if (this.debugPhaseLocked) {
            return;
        }
        float fraction = this.getHealth() / this.getMaxHealth();
        int wanted = 1;
        for (int phase = 2; phase <= this.phaseCount(); phase++) {
            if (fraction <= this.phaseThreshold(phase)) {
                wanted = phase;
            }
        }
        if (wanted != this.getPhase()) {
            this.applyPhase(level, wanted);
        }
    }

    /** フェーズを適用する（同期 + 画面演出 + 周囲ノックバック）。 */
    protected void applyPhase(ServerLevel level, int phase) {
        this.setPhase(phase);

        ParticleOptions particle = this.effectParticle();
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0) * i / 40.0;
            double radius = this.phaseKnockbackRadius();
            level.sendParticles(particle,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + 0.6,
                    this.getZ() + Math.sin(angle) * radius,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
        level.sendParticles(particle, this.getX(), this.getY() + 1.6, this.getZ(), 60, 1.2, 1.2, 1.2, 0.05);

        SoundEvent sound = this.phaseSound();
        if (sound != null) {
            this.playSound(sound, 1.8F, 0.5F);
        }

        this.pushNearbyEntitiesAway(level, this.phaseKnockbackRadius(), 0.8);
        this.onPhaseApplied(level, phase);
        this.summonCooldown = 0;
    }

    /** 周囲の敵（プレイヤー含む）を外側へノックバックする。 */
    protected void pushNearbyEntitiesAway(ServerLevel level, double radius, double strength) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive()
                        && !(entity instanceof Player player
                        && (player.isCreative() || player.isSpectator())));
        for (LivingEntity entity : nearby) {
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                entity.push(dx / length * strength, 0.55, dz / length * strength);
            }
        }
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
            ParticleOptions particle = this.effectParticle();
            for (int i = 0; i < 60; i++) {
                double angle = (Math.PI * 2.0) * i / 60.0;
                serverLevel.sendParticles(particle,
                        this.getX() + Math.cos(angle) * 4.0,
                        this.getY() + 0.5 + (i % 8) * 0.35,
                        this.getZ() + Math.sin(angle) * 4.0,
                        3, 0.2, 0.2, 0.2, 0.05);
            }
            serverLevel.sendParticles(particle,
                    this.getX(), this.getY() + 1.5, this.getZ(), 140, 1.5, 1.6, 1.5, 0.08);
            SoundEvent sound = this.phaseSound();
            if (sound != null) {
                this.playSound(sound, 1.6F, 0.4F);
            }
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
        if (phase < 1 || phase > this.phaseCount()) {
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

    /** 開発用: 指定バリアントの攻撃予兆を開始する。 */
    public boolean debugForceAttack(int variant, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel)) {
            return false;
        }
        if (variant < 0 || variant >= this.attackVariantCount()) {
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

    /** 開発用: 予兆を短縮する（0 で次 tick に発動）。 */
    public void debugSetAttackWindup(int ticks) {
        this.attackWindup = Math.max(0, Math.min(this.attackWindup, ticks));
    }

    /** 開発用: 召喚を即実行する（CD を無視）。 */
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
        return "id=" + this.bossId
                + " phase=" + this.getPhase() + "/" + this.phaseCount()
                + " hp=" + this.getHealth() + "/" + this.getMaxHealth()
                + " windup=" + this.attackWindup
                + " attackCd=" + this.attackCooldown
                + " summonCd=" + this.summonCooldown
                + " variant=" + this.attackVariant
                + " phaseLocked=" + this.debugPhaseLocked;
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<NocterraBossEntity> controller =
                new AnimationController<NocterraBossEntity>(CONTROLLER, 6, this::animationPredicate)
                        .receiveTriggeredAnimations()
                        .triggerableAnim("attack1", RawAnimation.begin().thenPlay(this.animName("attack1")))
                        .triggerableAnim("attack2", RawAnimation.begin().thenPlay(this.animName("attack2")))
                        .triggerableAnim("hurt", RawAnimation.begin().thenPlay(this.animName("hurt")))
                        .triggerableAnim("death", RawAnimation.begin().thenPlay(this.animName("death")));
        if (this.phaseCount() >= 3) {
            controller = controller.triggerableAnim("phase3", RawAnimation.begin().thenPlay(this.animName("phase3")));
        }
        controllers.add(controller);
    }

    /** 停止中はフェーズに応じたループ、移動中は walk。 */
    private PlayState animationPredicate(AnimationTest<NocterraBossEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(this.loopAnim("walk"));
        }
        String idleKey = switch (this.getPhase()) {
            case 1 -> "idle";
            case 2 -> "phase2";
            default -> this.phaseCount() >= 3 ? "phase3" : "phase2";
        };
        return test.setAndContinue(this.loopAnim(idleKey));
    }

    private RawAnimation loopAnim(String key) {
        return this.loopCache.computeIfAbsent(key, k -> RawAnimation.begin().thenLoop(this.animName(k)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /** 咆哮音の共通デフォルト（サブクラスで上書き可）。 */
    protected SoundEvent defaultRoarSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    /** ボス同士の押し合いを無効化（見た目の安定）。 */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** ノックバック距離計算用のユーティリティ。 */
    protected static Vec3 awayDirection(LivingEntity from, LivingEntity to) {
        Vec3 delta = new Vec3(to.getX() - from.getX(), 0.0, to.getZ() - from.getZ());
        double length = delta.length();
        return length < 1.0E-4 ? Vec3.ZERO : delta.scale(1.0 / length);
    }
}

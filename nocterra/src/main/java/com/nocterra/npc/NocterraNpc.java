package com.nocterra.npc;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.nocterra.quest.dialogue.DialogueService;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * NOCTERRA の NPC 共通基底（全 6 体がこの 1 クラスを共有する）。所有: story 担当。
 *
 * <p>NPC の識別子は登録 ID から導出する（{@code nocterra:warden_of_ash} → {@code warden_of_ash}）。
 * 会話内容は {@code data/nocterra/dialogue/<npcId>.json}、外見は
 * {@code assets/nocterra/geckolib/models/entity/<npcId>.geo.json} が担当し、
 * Java 側は「右クリックで会話を開く」「会話中は相手を見る」「迷い子の追従」だけを持つ。</p>
 *
 * <p>仕様:</p>
 * <ul>
 *   <li>HP 40 / ノックバック耐性 1.0 / 永続（デスポーンしない）</li>
 *   <li>攻撃しない（{@link PathfinderMob}。ターゲット選択ゴールを持たない）</li>
 *   <li>いかなるダメージも受けない（{@code /kill} では消せる）</li>
 *   <li>{@link net.minecraft.world.entity.MobCategory#MISC}。自然スポーンしない</li>
 * </ul>
 */
public class NocterraNpc extends PathfinderMob implements GeoEntity {
    /** GeckoLib のアニメーションコントローラ名。 */
    public static final String CONTROLLER = "main";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable private String cachedNpcId;
    @Nullable private RawAnimation idleAnimation;
    @Nullable private RawAnimation walkAnimation;
    /** 迷い子がプレイヤーについてくる時の対象。 */
    @Nullable private UUID followingPlayer;

    public NocterraNpc(EntityType<? extends NocterraNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        String id = this.npcId();
        this.setCustomName(Component.translatable("entity.nocterra." + id));
        this.setCustomNameVisible(true);
    }

    /** 登録 ID の path（例: {@code warden_of_ash}）。 */
    public final String npcId() {
        String cached = this.cachedNpcId;
        if (cached != null) {
            return cached;
        }
        var key = EntityType.getKey(this.getType());
        String id = key == null ? "unknown" : key.getPath();
        this.cachedNpcId = id;
        return id;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 0.6);
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        // 攻撃ゴール・ターゲット選択ゴールは持たない（絶対に攻撃しない）
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.tickFollowingPlayer(level);
    }

    /** 迷い子: 追従対象が近くにいれば付いてくる（遠すぎたら転移）。 */
    private void tickFollowingPlayer(ServerLevel level) {
        if (this.followingPlayer == null) {
            return;
        }
        ServerPlayer target = level.getServer().getPlayerList().getPlayer(this.followingPlayer);
        if (target == null || target.level() != level || !target.isAlive()) {
            this.getNavigation().stop();
            return;
        }
        double distanceSq = this.distanceToSqr(target);
        if (distanceSq > 32.0 * 32.0) {
            this.snapTo(target.getX() + 1.0, target.getY(), target.getZ() + 1.0);
            this.followingPlayer = target.getUUID();
            return;
        }
        if (distanceSq > 16.0) {
            this.getNavigation().moveTo(target, 1.15);
        } else {
            this.getNavigation().stop();
        }
        this.getLookControl().setLookAt(target, 20.0F, 20.0F);
    }

    // ------------------------------------------------------------------ 会話

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            DialogueService.open(serverPlayer, this);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** 会話開始時に話すアニメーションを 1 回だけ再生する。 */
    public void triggerTalkAnimation() {
        this.triggerAnim(CONTROLLER, "talk");
    }

    public void facePlayer(Player player) {
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    /** 会話アクション {@code npc_action} の受け口。NPC 固有処理は {@link NpcBehaviors} が持つ。 */
    public void handleStoryAction(ServerPlayer player, String action) {
        NpcBehaviors.onAction(player, this, action);
    }

    // ------------------------------------------------------------------ 迷い子の追従

    public void followPlayer(ServerPlayer player) {
        this.followingPlayer = player.getUUID();
    }

    @Nullable
    public UUID followingPlayer() {
        return this.followingPlayer;
    }

    public void stopFollowing() {
        this.followingPlayer = null;
    }

    // ------------------------------------------------------------------ 永続化

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.followingPlayer != null) {
            output.putString("nocterra:following", this.followingPlayer.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String raw = input.getStringOr("nocterra:following", "");
        if (!raw.isBlank()) {
            try {
                this.followingPlayer = UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                this.followingPlayer = null;
            }
        }
    }

    // ------------------------------------------------------------------ 生存性

    /** NPC はダメージを受けない。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<NocterraNpc>(CONTROLLER, 5, this::animationPredicate)
                .receiveTriggeredAnimations()
                .triggerableAnim("talk", RawAnimation.begin().thenPlay(this.animationName("talk"))));
    }

    private PlayState animationPredicate(AnimationTest<NocterraNpc> test) {
        if (test.isMoving()) {
            return test.setAndContinue(this.walkAnimation());
        }
        return test.setAndContinue(this.idleAnimation());
    }

    private RawAnimation idleAnimation() {
        RawAnimation animation = this.idleAnimation;
        if (animation == null) {
            animation = RawAnimation.begin().thenLoop(this.animationName("idle"));
            this.idleAnimation = animation;
        }
        return animation;
    }

    private RawAnimation walkAnimation() {
        RawAnimation animation = this.walkAnimation;
        if (animation == null) {
            animation = RawAnimation.begin().thenLoop(this.animationName("walk"));
            this.walkAnimation = animation;
        }
        return animation;
    }

    /** アニメ名の規約: {@code animation.nocterra.<npcId>.<key>}。 */
    public String animationName(String key) {
        return "animation.nocterra." + this.npcId() + "." + key;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}

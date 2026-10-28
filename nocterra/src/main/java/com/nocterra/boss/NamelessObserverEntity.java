package com.nocterra.boss;

import com.nocterra.entity.MourningWispEntity;
import com.nocterra.entity.NocterraEntities;
import java.util.List;
import java.util.function.Predicate;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 隠しボス「名もなき観測者」（PINK ボスバー）。テレポート・幻影。
 *
 * <p>フェーズ 1: 観測の光線（予兆 20t・最大 20m・視線通過時のみ 11 魔法）/ 幻影の爆縮（予兆 24t・半径 6 に 10 + 引き寄せ）。
 * 約 7 秒ごとに対象の近くへテレポートし、フェーズ 2（HP 50%）ではテレポート間隔が短くなり攻撃力が上がる。</p>
 */
public class NamelessObserverEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(NamelessObserverEntity.class, EntityDataSerializers.INT);

    private static final double BEAM_RANGE_SQR = 400.0;
    private static final double COLLAPSE_RADIUS = 6.0;
    private static final Predicate<BlockState> TELEPORT_BLOCK_FILTER = state -> false;

    private int teleportCooldown = 100;

    public NamelessObserverEntity(EntityType<? extends NamelessObserverEntity> type, Level level) {
        super(type, level, "nameless_observer", BossEvent.BossBarColor.PINK);
        this.xpReward = 380;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 460.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
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
    // テレポート（サーバー権威）
    // ------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) <= 64.0) {
            this.teleportCooldown = 20;
            return;
        }
        if (this.tryTeleportNear(level, target)) {
            this.teleportCooldown = this.getPhase() >= 2 ? 80 : 140;
        } else {
            this.teleportCooldown = 20;
        }
    }

    private boolean tryTeleportNear(ServerLevel level, LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = target.getX() + (this.random.nextDouble() - 0.5) * 5.0;
            double y = target.getY();
            double z = target.getZ() + (this.random.nextDouble() - 0.5) * 5.0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.2, this.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            if (this.randomTeleport(x, y, z, true, TELEPORT_BLOCK_FILTER)) {
                level.sendParticles(ParticleTypes.PORTAL,
                        this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.5, 0.9, 0.5, 0.06);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.1F, 0.6F);
                return true;
            }
        }
        return false;
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
        return ParticleTypes.PORTAL;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.ENDERMAN_TELEPORT : SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return BEAM_RANGE_SQR;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 20 : 24;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean beam = this.random.nextInt(5) < (this.getPhase() >= 2 ? 2 : 3);
        this.startAttack(beam ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.collapse(level);
            default -> this.observationBeam(level);
        }
    }

    /** 観測の光線: 視線通過時のみ遠距離魔法ダメージ。 */
    private void observationBeam(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > BEAM_RANGE_SQR) {
            return;
        }
        Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        int steps = (int) Math.min(40, Math.max(2, distance));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (this.hasLineOfSight(target)) {
            float damage = this.getPhase() >= 2 ? 14.0F : 11.0F;
            target.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1.0, target.getZ(), 22, 0.4, 0.5, 0.4, 0.05);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.1F, 1.4F);
        }
    }

    /** 幻影の爆縮: 半径 6 にダメージ + 中心へ引き寄せ。 */
    private void collapse(ServerLevel level) {
        float damage = this.getPhase() >= 2 ? 13.0F : 10.0F;
        double radius = this.getPhase() >= 2 ? 7.5 : COLLAPSE_RADIUS;
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            Vec3 direction = awayDirection(this, player).scale(-1.0);
            if (direction != Vec3.ZERO) {
                player.push(direction.x * 1.3, 0.3, direction.z * 1.3);
            }
        }
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0) * i / 40.0;
            double r = radius * (1.0 - (i % 5) * 0.15);
            level.sendParticles(ParticleTypes.PORTAL,
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + 0.4 + (i % 6) * 0.3,
                    this.getZ() + Math.sin(angle) * r,
                    2, 0.1, 0.1, 0.1, 0.03);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX() + Math.cos(angle) * r * 0.6,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * r * 0.6,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
        this.playSound(SoundEvents.WITHER_DEATH, 1.1F, 1.3F);
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
            if (this.getTarget() != null) {
                wisp.setTarget(this.getTarget());
            }
            level.addFreshEntity(wisp);
            level.sendParticles(ParticleTypes.PORTAL, x, this.getY() + 1.0, z, 20, 0.4, 0.8, 0.4, 0.02);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.8F);
        this.summonCooldown = 240;
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(14.0);
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.28);
        }
        this.teleportCooldown = Math.min(this.teleportCooldown, 20);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}

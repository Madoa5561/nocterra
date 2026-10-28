package com.nocterra.boss;

import com.nocterra.entity.NocterraEntities;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ボス「砂時計の双王」（紅砂の荒野 / YELLOW ボスバー）。2 体で 1 組。
 *
 * <p>召喚されると自動で相方を 1 体スポーンする。**片方が死亡すると、生き残った側が
 * フェーズ 2 に移行して強化**される（攻撃力 +4 / 移動速度上昇 / 周囲ノックバック）。
 * 自分の HP が 30% 以下になった場合も同じ強化に入る。</p>
 */
public class SandglassTwinKingsEntity extends NocterraBossEntity {
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(SandglassTwinKingsEntity.class, EntityDataSerializers.INT);

    private static final String ENRAGED_MESSAGE_KEY = "message.nocterra.sandglass_twin.enraged";
    private static final double VORTEX_RADIUS = 5.0;

    private boolean partnerSpawnChecked;
    private boolean partnerSeenAlive;
    private boolean twinEnraged;

    public SandglassTwinKingsEntity(EntityType<? extends SandglassTwinKingsEntity> type, Level level) {
        super(type, level, "sandglass_twin_kings", BossEvent.BossBarColor.YELLOW);
        this.xpReward = 240;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0);
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
    // 双子ロジック
    // ------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (!this.partnerSpawnChecked) {
            this.partnerSpawnChecked = true;
            this.ensurePartner(level);
        }

        boolean partnerAlive = this.hasLivingPartner(level);
        if (partnerAlive) {
            this.partnerSeenAlive = true;
        } else if (this.partnerSeenAlive && !this.twinEnraged) {
            // 相方が消滅した場合のフォールバック（die() の通知を跨げなかった場合）
            this.onPartnerDeath(level);
        }
    }

    /** 相方が見当たらなければ 1 体だけ自動スポーンする。 */
    private void ensurePartner(ServerLevel level) {
        if (this.hasLivingPartner(level)) {
            this.partnerSeenAlive = true;
            return;
        }
        EntityType<SandglassTwinKingsEntity> type = NocterraEntities.SANDGLASS_TWIN_KINGS.get();
        if (type == null) {
            return;
        }
        SandglassTwinKingsEntity partner = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (partner == null) {
            return;
        }
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        partner.snapTo(this.getX() + Math.cos(angle) * 3.0, this.getY(), this.getZ() + Math.sin(angle) * 3.0,
                this.getYRot(), 0.0F);
        partner.partnerSpawnChecked = true;
        level.addFreshEntity(partner);
        this.partnerSeenAlive = true;
        level.sendParticles(ParticleTypes.END_ROD,
                partner.getX(), partner.getY() + 1.2, partner.getZ(), 30, 0.5, 0.8, 0.5, 0.02);
    }

    private boolean hasLivingPartner(ServerLevel level) {
        return !level.getEntitiesOfClass(SandglassTwinKingsEntity.class,
                this.getBoundingBox().inflate(64.0),
                twin -> twin != this && twin.isAlive()).isEmpty();
    }

    /** 片方の死亡を相方へ通知する。 */
    private void onPartnerDeath(ServerLevel level) {
        if (this.twinEnraged) {
            return;
        }
        this.twinEnraged = true;
        if (this.getPhase() < 2) {
            this.applyPhase(level, 2);
        }
        for (Player player : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(48.0), Player::isAlive)) {
            player.sendSystemMessage(Component.translatable(ENRAGED_MESSAGE_KEY));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel serverLevel) {
            List<SandglassTwinKingsEntity> partners = serverLevel.getEntitiesOfClass(
                    SandglassTwinKingsEntity.class, this.getBoundingBox().inflate(64.0),
                    twin -> twin != this && twin.isAlive());
            for (SandglassTwinKingsEntity partner : partners) {
                partner.onPartnerDeath(serverLevel);
            }
        }
        super.die(source);
    }

    @Override
    protected void updatePhase(ServerLevel level) {
        if (this.debugPhaseLocked || this.getPhase() >= 2) {
            return;
        }
        if (this.twinEnraged || this.getHealth() <= this.getMaxHealth() * 0.3F) {
            this.twinEnraged = true;
            this.applyPhase(level, 2);
        }
    }

    @Override
    protected void onPhaseApplied(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(13.0);
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.32);
        }
    }

    /** デバッグ用: 相方を失った扱いにして強化を発動する。 */
    public boolean debugForceTwinEnrage() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        this.onPartnerDeath(serverLevel);
        return true;
    }

    // ------------------------------------------------------------------
    // 設定
    // ------------------------------------------------------------------

    @Override
    protected float phaseThreshold(int phase) {
        return 0.3F;
    }

    @Override
    protected ParticleOptions effectParticle() {
        return ParticleTypes.END_ROD;
    }

    @Override
    protected SoundEvent telegraphSound() {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    protected SoundEvent phaseSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    protected SoundEvent attackStartSound(int variant) {
        return variant == 0 ? SoundEvents.SAND_BREAK : SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    protected int attackVariantCount() {
        return 2;
    }

    @Override
    protected double attackRangeSqr() {
        return 36.0;
    }

    @Override
    protected int windupFor(int variant) {
        return variant == 0 ? 16 : 22;
    }

    @Override
    protected void triggerAttackAnimation(int variant) {
        this.triggerAnim(CONTROLLER, variant == 0 ? "attack1" : "attack2");
    }

    @Override
    protected void beginAttack(LivingEntity target) {
        boolean slash = this.random.nextInt(5) < (this.getPhase() >= 2 ? 3 : 4);
        this.startAttack(slash ? 0 : 1, target);
    }

    @Override
    protected void executeAttack(ServerLevel level) {
        switch (this.attackVariant) {
            case 1 -> this.sandVortex(level);
            default -> this.sandSlash(level);
        }
    }

    /** 砂の斬撃: 近接 + 移動速度低下。 */
    private void sandSlash(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || this.distanceToSqr(target) > 20.25) {
            return;
        }
        float damage = this.getPhase() >= 2 ? 11.0F : 9.0F;
        target.hurtServer(level, level.damageSources().mobAttack(this), damage);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1.1, target.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        this.playSound(SoundEvents.SAND_BREAK, 1.2F, 0.7F);
    }

    /** 砂時計の渦: 半径 5 にダメージ + 引き寄せ + 移動速度低下。 */
    private void sandVortex(ServerLevel level) {
        float damage = this.getPhase() >= 2 ? 10.0F : 8.0F;
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(VORTEX_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : hit) {
            player.hurtServer(level, level.damageSources().indirectMagic(this, this), damage);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            double dx = this.getX() - player.getX();
            double dz = this.getZ() - player.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0E-4) {
                player.push(dx / length * 1.0, 0.35, dz / length * 1.0);
            }
        }
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2.0) * i / 36.0;
            level.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + Math.cos(angle) * VORTEX_RADIUS * 0.8,
                    this.getY() + 0.4 + (i % 6) * 0.25,
                    this.getZ() + Math.sin(angle) * VORTEX_RADIUS * 0.8,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.1F, 0.6F);
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
}

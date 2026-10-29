package com.nocterra.music;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ボスBGMの再生・停止。所有: Lead。
 *
 * <p>ボスが出現したら、そのボスに対応する音楽を近くのプレイヤーに再生する。
 * ボスが死んだら {@link ClientboundStopSoundPacket} で止める。</p>
 */
public final class BossMusic {
    private BossMusic() {}

    /** ボス ID → 音楽トラック名。 */
    private static final Map<String, String> BOSS_TRACKS = new LinkedHashMap<>();

    static {
        BOSS_TRACKS.put("rotwood_king", "boss_ash");
        BOSS_TRACKS.put("sorrow_saint", "boss_ash");
        BOSS_TRACKS.put("condemned_knight_lord", "boss_ash");
        BOSS_TRACKS.put("black_crowned_king", "boss_void");
        BOSS_TRACKS.put("astel_fallen_star_dragon", "boss_void");
        BOSS_TRACKS.put("abyssal_sea_emperor", "boss_void");
        BOSS_TRACKS.put("nameless_observer", "boss_void");
        BOSS_TRACKS.put("forgotten_knight", "boss_void");
        BOSS_TRACKS.put("molten_steel_colossus", "boss_iron");
        BOSS_TRACKS.put("silver_executioner", "boss_iron");
        BOSS_TRACKS.put("sandglass_twin_kings", "boss_iron");
        BOSS_TRACKS.put("ember_incarnation", "boss_iron");
    }

    private static final double NEARBY_RADIUS = 96.0;

    public static void onBossJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        String track = trackFor(entity);
        if (track == null) {
            return;
        }
        var sound = NocterraSounds.music(track);
        if (sound == null) {
            return;
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                sound, SoundSource.MUSIC, 1.0F, 1.0F);
        Nocterra.LOG.info("[NOCTERRA] boss music '{}' started for {}", track, event.getEntity().getType());
    }

    public static void onBossDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        String track = trackFor(entity);
        if (track == null) {
            return;
        }
        var sound = NocterraSounds.music(track);
        if (sound == null) {
            return;
        }
        var packet = new ClientboundStopSoundPacket(sound.location(), SoundSource.MUSIC);
        for (ServerPlayer player : level.getEntitiesOfClass(
                ServerPlayer.class, entity.getBoundingBox().inflate(NEARBY_RADIUS))) {
            player.connection.send(packet);
        }
        playDefeatEffects(level, entity);
        Nocterra.LOG.info("[NOCTERRA] boss music '{}' stopped", track);
    }

    /** その実体がボスなら音楽トラック名を返す（一般モブは null）。 */
    private static String trackFor(Entity entity) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null || !Nocterra.MOD_ID.equals(typeId.getNamespace())) {
            return null;
        }
        return BOSS_TRACKS.get(typeId.getPath());
    }

    /** 再生中の可能性がある音楽をすべて止める（開発用）。 */
    public static void stopAllFor(ServerPlayer player) {
        Set<String> tracks = new LinkedHashSet<>(BOSS_TRACKS.values());
        for (String track : tracks) {
            var sound = NocterraSounds.music(track);
            if (sound != null) {
                player.connection.send(new ClientboundStopSoundPacket(sound.location(), SoundSource.MUSIC));
            }
        }
    }

    /** テスト・デバッグ用: 指定トラックを再生する。 */
    public static void playFor(ServerPlayer player, String track) {
        var sound = NocterraSounds.music(track);
        if (sound == null) {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.MUSIC, 1.0F, 1.0F);
    }

    /** 利用可能なトラック名。 */
    public static Set<String> tracks() {
        return new LinkedHashSet<>(BOSS_TRACKS.values());
    }

    /** 討伐演出: 粒子・音・タイトル表示。所有: Lead。 */
    private static void playDefeatEffects(ServerLevel level, Entity entity) {
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                at.x, at.y, at.z, 60, 0.8, 1.0, 0.8, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                at.x, at.y, at.z, 40, 0.9, 1.2, 0.9, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                at.x, at.y, at.z, 50, 0.5, 0.8, 0.5, 0.12);
        level.playSound(null, at.x, at.y, at.z,
                net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.55F, 1.35F);

        Component name = entity.getDisplayName();
        for (ServerPlayer player : level.getEntitiesOfClass(
                ServerPlayer.class, entity.getBoundingBox().inflate(NEARBY_RADIUS))) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.translatable("nocterra.boss.defeated.title", name)
                            .withStyle(ChatFormatting.GOLD)));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.translatable("nocterra.boss.defeated.subtitle")
                            .withStyle(ChatFormatting.GRAY)));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                    10, 50, 20));
            player.sendSystemMessage(Component.translatable("nocterra.boss.defeated.message", name)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

}

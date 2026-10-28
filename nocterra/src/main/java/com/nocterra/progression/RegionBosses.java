package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地域（バイオーム）と冠の者の対応。所有: Lead。
 *
 * <p>各地域に置かれた {@code nocterra:region_boss_altar} を右クリックすると、
 * その場のバイオームに対応するボスが召喚される。</p>
 */
public final class RegionBosses {
    private RegionBosses() {}

    /** バイオーム ID → ボス ID。 */
    private static final Map<String, String> BIOME_TO_BOSS = new LinkedHashMap<>();

    static {
        BIOME_TO_BOSS.put("nocterra:ashen_forest", "rotwood_king");
        BIOME_TO_BOSS.put("nocterra:mourning_marsh", "sorrow_saint");
        BIOME_TO_BOSS.put("nocterra:black_crown_ruins", "black_crowned_king");
        BIOME_TO_BOSS.put("nocterra:citadel_wastes", "condemned_knight_lord");
        BIOME_TO_BOSS.put("nocterra:crimson_dunes", "sandglass_twin_kings");
        BIOME_TO_BOSS.put("nocterra:abyssal_deeps", "abyssal_sea_emperor");
        BIOME_TO_BOSS.put("nocterra:starfall_isles", "astel_fallen_star_dragon");
        BIOME_TO_BOSS.put("nocterra:eternal_frost", "silver_executioner");
        BIOME_TO_BOSS.put("nocterra:molten_volcano", "molten_steel_colossus");
    }

    /** その位置のバイオーム ID（文字列）。 */
    public static String biomeAt(ServerLevel level, BlockPos pos) {
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Identifier id = biomes.getKey(level.getBiome(pos).value());
        return id == null ? "?" : id.toString();
    }

    /**
     * その位置のバイオームに対応するボス ID を返す（未対応なら null）。
     */
    public static String bossAt(ServerLevel level, BlockPos pos) {
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Identifier biomeId = biomes.getKey(level.getBiome(pos).value());
        if (biomeId == null) {
            return null;
        }
        return BIOME_TO_BOSS.get(biomeId.toString());
    }

    /**
     * 祭壇の位置に、その地域のボスを召喚する。
     *
     * @return 召喚した場合は true（既に居る／未対応地域は false）
     */
    public static boolean summonAt(ServerLevel level, BlockPos pos, Player player) {
        String bossId = bossAt(level, pos);
        if (bossId == null) {
            player.sendSystemMessage(Component.translatable("message.nocterra.region_altar.none"));
            return false;
        }
        Identifier typeId = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, bossId);
        Registry<EntityType<?>> types = level.getServer().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        EntityType<?> type = types.getOptional(typeId).orElse(null);
        if (type == null) {
            player.sendSystemMessage(Component.translatable("message.nocterra.region_altar.missing", bossId));
            return false;
        }
        var area = new net.minecraft.world.phys.AABB(pos).inflate(48.0);
        boolean already = !level.getEntities((Entity) null, area,
                entity -> entity.getType() == type).isEmpty();
        if (already) {
            player.sendSystemMessage(Component.translatable("message.nocterra.region_altar.already"));
            return false;
        }
        Entity entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            player.sendSystemMessage(Component.translatable("message.nocterra.region_altar.missing", bossId));
            return false;
        }
        entity.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(player);
        }
        level.addFreshEntity(entity);
        player.sendSystemMessage(Component.translatable("message.nocterra.region_altar.awakened",
                Component.translatable("entity.nocterra." + bossId)));
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);
        Nocterra.LOG.info("[NOCTERRA] region altar summoned {} at {}", bossId, pos);
        return true;
    }

    /** 対応表（デバッグ表示用）。 */
    public static Map<String, String> mapping() {
        return Map.copyOf(BIOME_TO_BOSS);
    }
}

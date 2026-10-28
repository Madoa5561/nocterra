package com.nocterra.world.feature;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 「灰の聖域」— 灰樹の森にまれに現れる小さな野営地（11x11）。
 *
 * <p>石畳の平らな広場・焚き火・テント・旗・祭壇を置く。NPC が立つ想定の平らな空間を確保する。
 * Feature なので <b>書き込みはこのチャンク内に閉じ込める</b>（26.3 では FULL な隣チャンクが
 * 読み取り専用になり書き込みが捨てられるため）。そのため中心はチャンク局所座標に固定する。</p>
 */
public record AshSanctuaryFeature() implements Feature {
    public static final MapCodec<AshSanctuaryFeature> CODEC = MapCodec.unit(AshSanctuaryFeature::new);

    /** 11x11 の広場。 */
    private static final int HALF = 5;
    private static final int FOUNDATION_DEPTH = 40;
    private static final int CLEAR_HEIGHT = 10;

    private static final BlockState PAVE_A = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState PAVE_B = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState BORDER = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState FOUNDATION = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState TENT_ROOF = Blocks.WOOL.pick(DyeColor.GRAY).defaultBlockState();
    private static final BlockState TENT_WALL = Blocks.WOOL.pick(DyeColor.BLACK).defaultBlockState();
    private static final BlockState POLE = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
    private static final BlockState BANNER = Blocks.BANNER.pick(DyeColor.GRAY).defaultBlockState();
    private static final BlockState ALTAR = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState CANDLE = Blocks.CANDLE.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState CAMPFIRE = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);

    @Override
    public MapCodec<AshSanctuaryFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // 中心をチャンク局所座標（5..10）に固定 → 11x11 が必ずチャンク内に収まる
        int chunkStartX = origin.getX() & ~15;
        int chunkStartZ = origin.getZ() & ~15;
        int centerX = chunkStartX + 5 + Math.floorMod(hash(chunkStartX, 0, chunkStartZ), 6);
        int centerZ = chunkStartZ + 5 + Math.floorMod(hash(chunkStartZ, 7, chunkStartX), 6);

        int minY = level.getMinY();
        int maxY = minY + level.getHeight() - 1;

        // 地面は葉を除いた heightmap の中央値
        int diameter = HALF * 2 + 1;
        int[] heights = new int[diameter * diameter];
        int index = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                heights[index++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX + dx, centerZ + dz);
            }
        }
        Arrays.sort(heights);
        int floorY = heights[heights.length / 2];

        // 水面（湖など）の上に当たった場合は、水面より上へ広場を持ち上げる。
        // 「水だから生成しない」とすると `/place feature` が失敗して検証できないため、
        // 聖域は石の島（焚き火・テントが水没しない高さ）として必ず置けるようにする。
        int seaLevel = chunkGenerator.getSeaLevel();
        boolean wetSite = !level.getBlockState(new BlockPos(centerX, floorY - 1, centerZ)).getFluidState().isEmpty();
        if (wetSite) {
            floorY = Math.max(floorY, seaLevel + 1);
        }

        if (floorY - FOUNDATION_DEPTH < minY + 1 || floorY + CLEAR_HEIGHT + 4 > maxY) {
            return false;
        }

        // 土台
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                for (int relY = -1; relY >= -FOUNDATION_DEPTH; relY--) {
                    pos.set(centerX + dx, floorY + relY, centerZ + dz);
                    if (level.getBlockState(pos).isSolid()) {
                        break;
                    }
                    level.setBlock(pos, FOUNDATION, 2);
                }
            }
        }

        // 屋根の上まで空ける（木・丘を除去）
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                for (int relY = 1; relY <= CLEAR_HEIGHT; relY++) {
                    pos.set(centerX + dx, floorY + relY, centerZ + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, AIR, 2);
                    }
                }
            }
        }

        // 石畳（外周は縁石）
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean border = Math.abs(dx) == HALF || Math.abs(dz) == HALF;
                BlockState state = border ? BORDER : (((dx + dz) & 1) == 0 ? PAVE_A : PAVE_B);
                set(level, centerX, floorY, centerZ, dx, 0, dz, state);
            }
        }

        // 焚き火（中央）
        set(level, centerX, floorY, centerZ, 0, 1, 0, CAMPFIRE);

        // テント（北西の角、南側が入口）
        for (int dx = -4; dx <= -2; dx++) {
            for (int dz = -4; dz <= -2; dz++) {
                set(level, centerX, floorY, centerZ, dx, 3, dz, TENT_ROOF);
                boolean wallSide = dx == -4 || dz == -4 || dx == -2;
                if (wallSide) {
                    set(level, centerX, floorY, centerZ, dx, 1, dz, TENT_WALL);
                    set(level, centerX, floorY, centerZ, dx, 2, dz, TENT_WALL);
                } else {
                    set(level, centerX, floorY, centerZ, dx, 1, dz, AIR);
                    set(level, centerX, floorY, centerZ, dx, 2, dz, AIR);
                }
            }
        }
        set(level, centerX, floorY, centerZ, -4, 1, -4, POLE);
        set(level, centerX, floorY, centerZ, -4, 2, -4, POLE);

        // 旗（テントの入口脇）
        set(level, centerX, floorY, centerZ, -1, 1, -4, BANNER);
        set(level, centerX, floorY, centerZ, 1, 1, -4, BANNER);

        // 祭壇（南東）: 3 連の台 + キャンドル
        for (int dx = 2; dx <= 4; dx++) {
            set(level, centerX, floorY, centerZ, dx, 1, 3, ALTAR);
            set(level, centerX, floorY, centerZ, dx, 2, 3, CANDLE);
        }
        // ランタン
        set(level, centerX, floorY, centerZ, -3, 1, 3, LANTERN);
        set(level, centerX, floorY, centerZ, 3, 1, -3, LANTERN);

        return true;
    }

    private static void set(WorldGenLevel level, int cx, int floorY, int cz, int dx, int relY, int dz, BlockState state) {
        level.setBlock(new BlockPos(cx + dx, floorY + relY, cz + dz), state, 2);
    }

    private static int hash(int x, int y, int z) {
        int h = x * 0x9E3779B1 + y * 0x85EBCA77 + z * 0xC2B2AE3D;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return h;
    }
}

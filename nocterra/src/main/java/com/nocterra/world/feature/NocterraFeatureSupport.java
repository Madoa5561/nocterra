package com.nocterra.world.feature;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 地域装飾 {@code Feature} の共通ヘルパー。
 *
 * <p><b>制約</b>: 26.3 では生成中のチャンク以外（すでに FULL の隣チャンク）への
 * {@code setBlock} が {@code ImposterProtoChunk(allowWrites=false)} により黙って捨てられる。
 * そのため書き込みは必ず<b>中心チャンク内</b>に収める。{@link #center} はそのための
 * 「チャンク局所座標に固定した中心」を返す。</p>
 */
public final class NocterraFeatureSupport {
    private NocterraFeatureSupport() {}

    /** 決定的な座標ハッシュ（乱数を使わない）。 */
    public static int hash(int x, int y, int z) {
        int h = x * 0x9E3779B1 + y * 0x85EBCA77 + z * 0xC2B2AE3D;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return h;
    }

    /**
     * チャンク内に収まる中心座標を返す（footprint 半幅 {@code half}、余白 1）。
     * 例: half=2 → 中心はチャンク局所 3..12、footprint は 1..14 に収まる。
     */
    public static int center(BlockPos origin, int half, boolean xAxis, int salt) {
        int start = (xAxis ? origin.getX() : origin.getZ()) & ~15;
        int span = 14 - 2 * half;
        int h = hash(start, salt + (xAxis ? 0 : 1), xAxis ? salt : 0);
        return start + half + 1 + Math.floorMod(h, span);
    }

    /** 葉を除いた heightmap の中央値（水面なら水面の高さ）。 */
    public static int medianSurface(WorldGenLevel level, int cx, int cz, int half) {
        int d = half * 2 + 1;
        int[] heights = new int[d * d];
        int i = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                heights[i++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
            }
        }
        Arrays.sort(heights);
        return heights[heights.length / 2];
    }

    /**
     * 広場・台座を置く Y を決める。水辺（湖・海）では {@code seaLevel + 1} まで持ち上げる。
     * これにより {@code /place feature} が水域でも失敗しない。
     */
    public static int floorY(WorldGenLevel level, ChunkGenerator generator, int cx, int cz, int half) {
        int y = medianSurface(level, cx, cz, half);
        BlockPos below = new BlockPos(cx, y - 1, cz);
        if (!level.getBlockState(below).getFluidState().isEmpty()) {
            y = Math.max(y, generator.getSeaLevel() + 1);
        }
        return y;
    }

    /** 基礎を固体に当たるまで（最大 {@code depth}）充填する。 */
    public static void foundation(
            WorldGenLevel level, int cx, int cz, int half, int floorY, BlockState state, int depth) {
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int d = 1; d <= depth; d++) {
                    BlockPos pos = new BlockPos(cx + dx, floorY - d, cz + dz);
                    if (level.getBlockState(pos).isSolid()) {
                        break;
                    }
                    set(level, pos.getX(), pos.getY(), pos.getZ(), state);
                }
            }
        }
    }

    /** 頭上を空気にする。 */
    public static void clearAbove(WorldGenLevel level, int cx, int cz, int half, int floorY, int height) {
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int relY = 1; relY <= height; relY++) {
                    BlockPos pos = new BlockPos(cx + dx, floorY + relY, cz + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        set(level, pos.getX(), pos.getY(), pos.getZ(), Blocks.CAVE_AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    /** NOCTERRA のブロックを実行時解決する（未登録なら fallback）。 */
    public static BlockState resolve(String path, BlockState fallback) {
        Block block = BuiltInRegistries.BLOCK
                .getOptional(Identifier.fromNamespaceAndPath("nocterra", path))
                .filter(b -> b != Blocks.AIR)
                .orElse(null);
        return block == null ? fallback : block.defaultBlockState();
    }

    public static boolean fitsY(WorldGenLevel level, int floorY, int above) {
        return floorY - 40 >= level.getMinY() + 1 && floorY + above <= level.getMaxY();
    }

    public static void set(WorldGenLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }
}

package com.nocterra.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 「星屑の結晶柱」— 星骸の浮島に立つ紫のアメジスト柱（end_stone の基盤の上）。
 */
public record StarfallShardFeature() implements Feature {
    public static final MapCodec<StarfallShardFeature> CODEC = MapCodec.unit(StarfallShardFeature::new);

    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    private static final BlockState CRYSTAL = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState ROD = Blocks.END_ROD.defaultBlockState();
    private static final BlockState FOUNDATION = Blocks.END_STONE.defaultBlockState();

    /** {dx, dz, height} の 3 本。 */
    private static final int[][] SHARDS = {{0, 0, 6}, {-2, 1, 4}, {2, -1, 3}};

    @Override
    public MapCodec<StarfallShardFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 2, true, 91);
        int cz = NocterraFeatureSupport.center(origin, 2, false, 93);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 2);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 14)) {
            return false;
        }

        NocterraFeatureSupport.clearAbove(level, cx, cz, 2, floorY, 12);

        // 5x5 の end_stone 基盤
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                NocterraFeatureSupport.set(level, cx + dx, floorY, cz + dz, END_STONE);
                for (int d = 1; d <= 2; d++) {
                    BlockPos pos = new BlockPos(cx + dx, floorY - d, cz + dz);
                    if (level.getBlockState(pos).isSolid()) {
                        break;
                    }
                    NocterraFeatureSupport.set(level, cx + dx, floorY - d, cz + dz, FOUNDATION);
                }
            }
        }
        // 結晶柱
        for (int[] shard : SHARDS) {
            int sx = cx + shard[0];
            int sz = cz + shard[1];
            int var = Math.floorMod(NocterraFeatureSupport.hash(shard[0], floorY, shard[1]), 2);
            int h = shard[2] + var;
            for (int relY = 1; relY <= h; relY++) {
                NocterraFeatureSupport.set(level, sx, floorY + relY, sz, CRYSTAL);
            }
            NocterraFeatureSupport.set(level, sx, floorY + h + 1, sz, ROD);
        }
        return true;
    }
}

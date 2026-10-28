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
 * 「氷柱」— 永劫の氷原に立つ青い氷の柱（複数）。
 */
public record FrostSpikeFeature() implements Feature {
    public static final MapCodec<FrostSpikeFeature> CODEC = MapCodec.unit(FrostSpikeFeature::new);

    private static final BlockState BASE = Blocks.BLUE_ICE.defaultBlockState();
    private static final BlockState MID = Blocks.PACKED_ICE.defaultBlockState();
    private static final BlockState TIP = Blocks.ICE.defaultBlockState();
    private static final BlockState CAP = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState FOUNDATION = Blocks.PACKED_ICE.defaultBlockState();

    /** {dx, dz, height} の 3 本。 */
    private static final int[][] SPIKES = {{0, 0, 7}, {-2, 2, 5}, {2, -1, 4}};

    @Override
    public MapCodec<FrostSpikeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 2, true, 51);
        int cz = NocterraFeatureSupport.center(origin, 2, false, 53);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 2);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 14)) {
            return false;
        }

        NocterraFeatureSupport.clearAbove(level, cx, cz, 2, floorY, 12);

        for (int[] spike : SPIKES) {
            int sx = cx + spike[0];
            int sz = cz + spike[1];
            int var = Math.floorMod(NocterraFeatureSupport.hash(spike[0], floorY, spike[1]), 2);
            int h = spike[2] + var;
            for (int relY = 0; relY < h; relY++) {
                BlockState state = relY == 0 ? BASE : (relY == h - 1 ? TIP : MID);
                NocterraFeatureSupport.set(level, sx, floorY + relY, sz, state);
            }
            NocterraFeatureSupport.set(level, sx, floorY + h, sz, CAP);
            // 基礎
            for (int d = 1; d <= 2; d++) {
                BlockPos pos = new BlockPos(sx, floorY - d, sz);
                if (level.getBlockState(pos).isSolid()) {
                    break;
                }
                NocterraFeatureSupport.set(level, sx, floorY - d, sz, FOUNDATION);
            }
        }
        return true;
    }
}

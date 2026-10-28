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
 * 「紅砂の露頭」— 紅砂の荒野に現れる赤い砂岩の小丘（5x5）。
 */
public record CrimsonDuneRockFeature() implements Feature {
    public static final MapCodec<CrimsonDuneRockFeature> CODEC = MapCodec.unit(CrimsonDuneRockFeature::new);

    private static final BlockState BASE = Blocks.RED_SANDSTONE.defaultBlockState();
    private static final BlockState MID = Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
    private static final BlockState TOP = Blocks.CUT_RED_SANDSTONE.defaultBlockState();
    private static final BlockState SAND = Blocks.RED_SAND.defaultBlockState();
    private static final BlockState FOUNDATION = Blocks.RED_SANDSTONE.defaultBlockState();

    @Override
    public MapCodec<CrimsonDuneRockFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 2, true, 41);
        int cz = NocterraFeatureSupport.center(origin, 2, false, 43);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 2);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 10)) {
            return false;
        }

        NocterraFeatureSupport.clearAbove(level, cx, cz, 2, floorY, 8);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                int var = Math.floorMod(NocterraFeatureSupport.hash(dx, floorY, dz), 2);
                int h = dist == 2 ? 1 + var : dist == 1 ? 3 + var : 4 + var;
                for (int relY = 0; relY < h; relY++) {
                    BlockState state = relY == 0 ? BASE : (relY == h - 1 ? TOP : MID);
                    NocterraFeatureSupport.set(level, cx + dx, floorY + relY, cz + dz, state);
                }
                // 頂に砂を少し
                if (h > 2 && var == 1) {
                    NocterraFeatureSupport.set(level, cx + dx, floorY + h, cz + dz, SAND);
                }
                // 基礎
                for (int d = 1; d <= 2; d++) {
                    BlockPos pos = new BlockPos(cx + dx, floorY - d, cz + dz);
                    if (level.getBlockState(pos).isSolid()) {
                        break;
                    }
                    NocterraFeatureSupport.set(level, cx + dx, floorY - d, cz + dz, FOUNDATION);
                }
            }
        }
        return true;
    }
}

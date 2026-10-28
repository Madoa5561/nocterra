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
 * 「マグマ噴出口」— 黒鉄の火山に現れる黒石・マグマの噴気孔（5x5）。
 * 溶岩は周囲を固体で囲んだ 1 ブロックのみ（少量）。
 */
public record MoltenVentFeature() implements Feature {
    public static final MapCodec<MoltenVentFeature> CODEC = MapCodec.unit(MoltenVentFeature::new);

    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();

    @Override
    public MapCodec<MoltenVentFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 2, true, 81);
        int cz = NocterraFeatureSupport.center(origin, 2, false, 83);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 2);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 10)) {
            return false;
        }

        NocterraFeatureSupport.clearAbove(level, cx, cz, 2, floorY, 6);
        NocterraFeatureSupport.foundation(level, cx, cz, 2, floorY, BLACKSTONE, 4);

        // 5x5: 外周 BASALT / 内側 MAGMA
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                NocterraFeatureSupport.set(level, cx + dx, floorY, cz + dz, rim ? BASALT : MAGMA);
            }
        }
        // 中央の噴出
        NocterraFeatureSupport.set(level, cx, floorY + 1, cz, MAGMA);
        // 囲まれた溶岩（1 ブロック）
        NocterraFeatureSupport.set(level, cx, floorY, cz + 1, LAVA);
        // 玄武岩の柱
        for (int[] c : new int[][]{{2, 2}, {-2, 1}}) {
            int var = Math.floorMod(NocterraFeatureSupport.hash(c[0], floorY, c[1]), 2);
            for (int relY = 1; relY <= 2 + var; relY++) {
                NocterraFeatureSupport.set(level, cx + c[0], floorY + relY, cz + c[1], BASALT);
            }
        }
        return true;
    }
}

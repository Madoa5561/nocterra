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
 * 「城塞の瓦礫」— 亡国の城塞跡に転がる崩れた石壁片（7 ブロック長の壁の残骸）。
 * 決定的な座標ハッシュで高さ・欠けを作る。書き込みは中心チャンク内のみ。
 */
public record CitadelRubbleFeature() implements Feature {
    public static final MapCodec<CitadelRubbleFeature> CODEC = MapCodec.unit(CitadelRubbleFeature::new);

    private static final BlockState[] WALL_BLOCKS = {
        Blocks.STONE_BRICKS.defaultBlockState(),
        Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
        Blocks.COBBLESTONE.defaultBlockState(),
        Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
    };
    private static final BlockState FOUNDATION = Blocks.COBBLESTONE.defaultBlockState();

    @Override
    public MapCodec<CitadelRubbleFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 3, true, 31);
        int cz = NocterraFeatureSupport.center(origin, 3, false, 37);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 3);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 10)) {
            return false;
        }

        NocterraFeatureSupport.clearAbove(level, cx, cz, 3, floorY, 8);

        // X 方向の壁の残骸
        for (int dx = -3; dx <= 3; dx++) {
            int h = 1 + Math.floorMod(NocterraFeatureSupport.hash(dx, floorY, 0), 3); // 1..3
            boolean broken = Math.floorMod(NocterraFeatureSupport.hash(dx, 7, 3), 8) == 0;
            if (broken) {
                continue;
            }
            for (int relY = 1; relY <= h; relY++) {
                int pick = Math.floorMod(NocterraFeatureSupport.hash(dx, relY, 11), WALL_BLOCKS.length);
                NocterraFeatureSupport.set(level, cx + dx, floorY + relY, cz, WALL_BLOCKS[pick]);
            }
            // 基礎
            for (int d = 1; d <= 3; d++) {
                BlockPos pos = new BlockPos(cx + dx, floorY - d, cz);
                if (level.getBlockState(pos).isSolid()) {
                    break;
                }
                NocterraFeatureSupport.set(level, cx + dx, floorY - d, cz, FOUNDATION);
            }
        }
        // 直交する短い残骸
        for (int dz = 1; dz <= 2; dz++) {
            int h = 1 + Math.floorMod(NocterraFeatureSupport.hash(3, floorY, dz), 2);
            for (int relY = 1; relY <= h; relY++) {
                NocterraFeatureSupport.set(level, cx + 3, floorY + relY, cz + dz, WALL_BLOCKS[relY % WALL_BLOCKS.length]);
            }
        }
        return true;
    }
}

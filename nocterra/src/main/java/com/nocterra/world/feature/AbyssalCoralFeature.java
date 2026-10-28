package com.nocterra.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 「深淵の珊瑚」— 深淵の海の海底に生える水中装飾（3x3）。
 *
 * <p>{@code OCEAN_FLOOR} heightmap で海底を取り、直上が水のときだけサンゴを置く。
 * 陸地では何もしない（false）。</p>
 */
public record AbyssalCoralFeature() implements Feature {
    public static final MapCodec<AbyssalCoralFeature> CODEC = MapCodec.unit(AbyssalCoralFeature::new);

    private static final BlockState CORAL_BLOCK_A = Blocks.TUBE_CORAL_BLOCK.defaultBlockState();
    private static final BlockState CORAL_BLOCK_B = Blocks.BRAIN_CORAL_BLOCK.defaultBlockState();
    private static final BlockState CORAL_PLANT_A = Blocks.TUBE_CORAL.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true);
    private static final BlockState CORAL_PLANT_B = Blocks.FIRE_CORAL.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true);
    private static final BlockState PICKLE = Blocks.SEA_PICKLE.defaultBlockState();

    @Override
    public MapCodec<AbyssalCoralFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 1, true, 101);
        int cz = NocterraFeatureSupport.center(origin, 1, false, 103);
        int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, cx, cz);
        if (floorY <= level.getMinY() + 1 || floorY + 5 > level.getMaxY()) {
            return false;
        }
        // 海底であること: 直上が水、直下が固体
        BlockPos airPos = new BlockPos(cx, floorY, cz);
        if (level.getBlockState(airPos).getFluidState().isEmpty()) {
            return false;
        }
        if (!level.getBlockState(new BlockPos(cx, floorY - 1, cz)).isSolid()) {
            return false;
        }

        // 海底のサンゴ塊
        NocterraFeatureSupport.set(level, cx, floorY, cz, CORAL_BLOCK_A);
        NocterraFeatureSupport.set(level, cx + 1, floorY, cz, CORAL_BLOCK_B);
        NocterraFeatureSupport.set(level, cx - 1, floorY, cz, CORAL_BLOCK_A);
        NocterraFeatureSupport.set(level, cx, floorY, cz + 1, CORAL_BLOCK_B);
        // 水中のサンゴ植物
        NocterraFeatureSupport.set(level, cx, floorY + 1, cz, CORAL_PLANT_A);
        NocterraFeatureSupport.set(level, cx + 1, floorY + 1, cz, CORAL_PLANT_B);
        NocterraFeatureSupport.set(level, cx - 1, floorY + 1, cz, CORAL_PLANT_A);
        // シーピクル
        NocterraFeatureSupport.set(level, cx, floorY + 1, cz + 1, PICKLE);
        return true;
    }
}

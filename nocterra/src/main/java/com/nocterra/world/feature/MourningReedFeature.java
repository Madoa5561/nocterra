package com.nocterra.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 「悼みの湿原」用の植物パッチ。草・シダ・キノコを地表に点在させる。
 *
 * <p>配置は heightmap で地表に落とした origin を中心に 9x9 の範囲へ散らす。
 * 地面が土・砂・泥・粘土のところだけに置く。</p>
 */
public record MourningReedFeature() implements Feature {
    public static final MapCodec<MourningReedFeature> CODEC = MapCodec.unit(MourningReedFeature::new);

    private static final BlockState[] PLANTS = {
        Blocks.SHORT_GRASS.defaultBlockState(),
        Blocks.SHORT_GRASS.defaultBlockState(),
        Blocks.FERN.defaultBlockState(),
        Blocks.FERN.defaultBlockState(),
        Blocks.BROWN_MUSHROOM.defaultBlockState(),
        Blocks.RED_MUSHROOM.defaultBlockState()
    };

    private static final int ATTEMPTS = 16;
    private static final int RADIUS = 4;

    @Override
    public MapCodec<MourningReedFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        boolean placed = false;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = origin.getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = origin.getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos)) {
                continue;
            }
            BlockState ground = level.getBlockState(pos.below());
            if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND)
                    || ground.is(Blocks.MUD) || ground.is(Blocks.CLAY) || ground.is(Blocks.GRAVEL))) {
                continue;
            }
            level.setBlock(pos, PLANTS[random.nextInt(PLANTS.length)], 2);
            placed = true;
        }
        return placed;
    }
}

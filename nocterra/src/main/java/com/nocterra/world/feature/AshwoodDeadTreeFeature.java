package com.nocterra.world.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 灰樹の森の「枯れ木」— 原木の柱と数本の枝だけの、葉を持たない木。
 *
 * <p>原木は {@code nocterra:ashwood_log} を実行時解決する（未登録なら
 * {@link Blocks#STRIPPED_DARK_OAK_LOG} にフォールバック）。登録状況に
 * かかわらずビルド・生成が通るようにするため、コンパイル時依存は張らない。</p>
 */
public record AshwoodDeadTreeFeature() implements Feature {
    public static final MapCodec<AshwoodDeadTreeFeature> CODEC = MapCodec.unit(AshwoodDeadTreeFeature::new);

    private static final Identifier ASHWOOD_LOG_ID = Identifier.fromNamespaceAndPath("nocterra", "ashwood_log");

    @Override
    public MapCodec<AshwoodDeadTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!level.isEmptyBlock(origin)) {
            return false;
        }
        BlockPos groundPos = origin.below();
        BlockState ground = level.getBlockState(groundPos);
        if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.BASE_STONE_OVERWORLD) || ground.is(Blocks.GRAVEL))) {
            return false;
        }

        BlockState log = resolveLog();
        int height = 4 + random.nextInt(4); // 4..7
        // 天井がある場合は生えない
        for (int y = 0; y < height; y++) {
            if (!level.isEmptyBlock(origin.above(y))) {
                return false;
            }
        }

        for (int y = 0; y < height; y++) {
            level.setBlock(origin.above(y), log, 2);
        }

        // 上部から斜めに伸びる枝。原木と同じ素材で「枯れ木」らしくする。
        int branches = 2 + random.nextInt(3); // 2..4
        BlockPos top = origin.above(height - 1);
        Direction[] directions = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        for (int i = 0; i < branches; i++) {
            Direction direction = directions[random.nextInt(directions.length)];
            int length = 1 + random.nextInt(3); // 1..3
            BlockPos branchPos = top.offset(0, -1 - random.nextInt(2), 0);
            for (int step = 0; step < length; step++) {
                branchPos = branchPos.relative(direction);
                if (!level.isEmptyBlock(branchPos)) {
                    break;
                }
                level.setBlock(branchPos, log, 2);
                if (step == length - 1 && random.nextBoolean()) {
                    BlockPos tip = branchPos.above();
                    if (level.isEmptyBlock(tip)) {
                        level.setBlock(tip, log, 2);
                    }
                }
            }
        }

        return true;
    }

    /** 灰樹の原木。未登録なら暗色の原木で代用する。 */
    private static BlockState resolveLog() {
        Block block = BuiltInRegistries.BLOCK.getOptional(ASHWOOD_LOG_ID)
                .filter(b -> b != Blocks.AIR)
                .orElse(Blocks.STRIPPED_DARK_OAK_LOG);
        return block.defaultBlockState();
    }
}

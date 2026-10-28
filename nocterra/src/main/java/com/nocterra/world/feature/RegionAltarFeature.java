package com.nocterra.world.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.slf4j.Logger;

/**
 * 「地域の祭壇」— 各地域の遺構。5x5 の石台座の中央に {@code nocterra:region_boss_altar} を 1 個置く。
 *
 * <p>チャンク内固定・水没地は {@code seaLevel+1} まで持ち上げ・基礎を固体まで充填
 * （FULL 隣チャンク書き込み不可の制約と、{@code /place feature} の失敗回避）。</p>
 */
public record RegionAltarFeature() implements Feature {
    public static final MapCodec<RegionAltarFeature> CODEC = MapCodec.unit(RegionAltarFeature::new);

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState CANDLE = Blocks.CANDLE.defaultBlockState();

    @Override
    public MapCodec<RegionAltarFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = NocterraFeatureSupport.center(origin, 2, true, 21);
        int cz = NocterraFeatureSupport.center(origin, 2, false, 27);
        int floorY = NocterraFeatureSupport.floorY(level, generator, cx, cz, 2);
        if (!NocterraFeatureSupport.fitsY(level, floorY, 12)) {
            return false;
        }

        NocterraFeatureSupport.foundation(level, cx, cz, 2, floorY, TILES, 40);
        NocterraFeatureSupport.clearAbove(level, cx, cz, 2, floorY, 10);

        // 5x5 の台座
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                NocterraFeatureSupport.set(
                        level, cx + dx, floorY, cz + dz, ((dx + dz) & 1) == 0 ? TILES : POLISHED);
            }
        }
        // 四隅の柱
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int relY = 1; relY <= 2; relY++) {
                NocterraFeatureSupport.set(level, cx + c[0], floorY + relY, cz + c[1], CHISELED);
            }
        }
        // 祭壇ブロック（未登録時は仮置き）
        BlockState altar = NocterraFeatureSupport.resolve("region_boss_altar", CHISELED);
        if (altar.equals(CHISELED) && WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[NOCTERRA] ブロック 'nocterra:region_boss_altar' が未登録のため、"
                    + "地域祭壇は仮ブロックで生成されます。");
        }
        NocterraFeatureSupport.set(level, cx, floorY + 1, cz, altar);
        // 祭壇の周囲のキャンドル
        for (int[] c : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            NocterraFeatureSupport.set(level, cx + c[0], floorY + 1, cz + c[1], CANDLE);
        }
        return true;
    }
}

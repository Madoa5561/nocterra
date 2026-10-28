package com.nocterra.world.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.slf4j.Logger;

/**
 * 「灰樹の森の古代神殿」— 15x15 の石造遺跡。
 *
 * <p>26.3 では {@link Feature} はインターフェースで、設定値は {@link MapCodec} の
 * フィールドとして JSON に直接並ぶ（{@code ConfiguredFeature} は存在しない）。
 * この神殿は設定値を持たないので {@code MapCodec.unit} で足りる。</p>
 *
 * <p>生成は <b>乱数に依存しない決定的な配置</b>。座標から一意に決まるので、
 * チャンク生成順が変わっても、どのチャンクが先に生成されても結果は同じ。
 * 書き込み範囲は origin から最大 {@link #HALF} + 1 = 8 ブロックで、
 * FEATURES ステップの書き込み半径（1 チャンク = 16 ブロック）に収まる。</p>
 */
public record AncientTempleFeature() implements Feature {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<AncientTempleFeature> CODEC = MapCodec.unit(AncientTempleFeature::new);

    /** 祭壇ブロック。Lead が {@code NocterraBlocks} に登録するまで存在しない可能性があるため実行時解決する。 */
    private static final Identifier ALTAR_ID = Identifier.fromNamespaceAndPath("nocterra", "rotwood_altar");

    /** 外周半径（15x15）。 */
    private static final int HALF = 7;
    /** 内部（空洞）半径（13x13）。 */
    private static final int INNER_HALF = 6;
    /** 床より上の壁の高さ。 */
    private static final int WALL_HEIGHT = 5;
    /** 土台として埋める最大深さ。 */
    private static final int FOUNDATION_DEPTH = 14;
    /** 屋根より上を空ける高さ（木や丘に埋もれないように）。 */
    private static final int CLEAR_HEIGHT = 10;

    private static final BlockState FOUNDATION = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState PILLAR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState ROOF_A = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState ROOF_B = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState ROOF_C = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

    /** 未登録警告を 1 回だけ出すためのフラグ。 */
    private static final AtomicBoolean ALTAR_WARNED = new AtomicBoolean(false);

    @Override
    public MapCodec<AncientTempleFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int centerX = origin.getX();
        int centerZ = origin.getZ();
        int minY = level.getMinY();
        int maxY = minY + level.getHeight() - 1;

        // ---- 1. 地面の高さを決める -------------------------------------------------
        // WORLD_SURFACE_WG は葉や水面も拾ってしまうため、当たり判定を持つブロックだけを見る
        // OCEAN_FLOOR_WG の中央値を使う。中央値なら木の幹 1〜2 本に引きずられない。
        int diameter = HALF * 2 + 1;
        int[] heights = new int[diameter * diameter];
        int index = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                heights[index++] = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, centerX + dx, centerZ + dz);
            }
        }
        Arrays.sort(heights);
        int floorY = heights[heights.length / 2];

        if (floorY - FOUNDATION_DEPTH < minY + 1 || floorY + 12 > maxY) {
            // 構築できない高度（奈落際・天井際）では何も置かずに false を返す。
            return false;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // ---- 2. 土台 ---------------------------------------------------------------
        // 床下に空洞や水があれば石で埋める（地表にそのまま乗る場合は何もしない）。
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                for (int y = floorY - 1; y >= floorY - FOUNDATION_DEPTH && y > minY; y--) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).isSolid()) {
                        break;
                    }
                    level.setBlock(pos, FOUNDATION, 2);
                }
            }
        }

        // ---- 3. 内部を空洞化 -------------------------------------------------------
        for (int dx = -INNER_HALF; dx <= INNER_HALF; dx++) {
            for (int dz = -INNER_HALF; dz <= INNER_HALF; dz++) {
                for (int y = floorY + 1; y <= floorY + WALL_HEIGHT; y++) {
                    pos.set(centerX + dx, y, centerZ + dz);
                    level.setBlock(pos, AIR, 2);
                }
            }
        }

        // ---- 4. 屋根の上を空ける ---------------------------------------------------
        // 木の幹・葉、斜面の土を除去して神殿が埋もれないようにする（屋根はこの後に建てる）。
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                for (int y = floorY + WALL_HEIGHT + 1; y <= floorY + WALL_HEIGHT + CLEAR_HEIGHT; y++) {
                    pos.set(centerX + dx, y, centerZ + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, AIR, 2);
                    }
                }
            }
        }

        // ---- 5. 床 ---------------------------------------------------------------
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                pos.set(centerX + dx, floorY, centerZ + dz);
                level.setBlock(pos, ((dx + dz) & 1) == 0 ? FLOOR_A : FLOOR_B, 2);
            }
        }

        // ---- 6. 壁（四隅は柱） -----------------------------------------------------
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                boolean xEdge = Math.abs(dx) == HALF;
                boolean zEdge = Math.abs(dz) == HALF;
                if (!xEdge && !zEdge) {
                    continue;
                }
                boolean corner = xEdge && zEdge;
                for (int y = floorY + 1; y <= floorY + WALL_HEIGHT; y++) {
                    BlockState state;
                    if (corner) {
                        state = PILLAR;
                    } else {
                        int h = Math.floorMod(hash(dx, y, dz), 100);
                        state = h < 6 ? WALL_CRACKED : (h < 10 ? WALL_CHISELED : WALL);
                    }
                    pos.set(centerX + dx, y, centerZ + dz);
                    level.setBlock(pos, state, 2);
                }
            }
        }

        // ---- 7. 入口（北側 2x3 の開口部） -------------------------------------------
        for (int y = floorY + 1; y <= floorY + 3; y++) {
            pos.set(centerX - 1, y, centerZ - HALF);
            level.setBlock(pos, AIR, 2);
            pos.set(centerX, y, centerZ - HALF);
            level.setBlock(pos, AIR, 2);
        }

        // ---- 8. 入口前のポーチ -----------------------------------------------------
        for (int dx = -1; dx <= 0; dx++) {
            int px = centerX + dx;
            int pz = centerZ - HALF - 1;
            pos.set(px, floorY, pz);
            level.setBlock(pos, FLOOR_A, 2);
            for (int y = floorY + 1; y <= floorY + 2; y++) {
                pos.set(px, y, pz);
                level.setBlock(pos, AIR, 2);
            }
            for (int y = floorY - 1; y >= floorY - 8 && y > minY; y--) {
                pos.set(px, y, pz);
                if (level.getBlockState(pos).isSolid()) {
                    break;
                }
                level.setBlock(pos, FOUNDATION, 2);
            }
        }

        // ---- 9. 内部の柱 -----------------------------------------------------------
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = floorY + 1; y <= floorY + WALL_HEIGHT; y++) {
                    pos.set(centerX + sx * 4, y, centerZ + sz * 4);
                    level.setBlock(pos, PILLAR, 2);
                }
            }
        }

        // ---- 10. 屋根（全面を塞ぐ。穴あき禁止） -------------------------------------
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                pos.set(centerX + dx, floorY + WALL_HEIGHT + 1, centerZ + dz);
                level.setBlock(pos, ROOF_A, 2);
            }
        }
        // 段状の二層目・三層目
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                pos.set(centerX + dx, floorY + WALL_HEIGHT + 2, centerZ + dz);
                level.setBlock(pos, ROOF_B, 2);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                pos.set(centerX + dx, floorY + WALL_HEIGHT + 3, centerZ + dz);
                level.setBlock(pos, ROOF_C, 2);
            }
        }
        pos.set(centerX, floorY + WALL_HEIGHT + 4, centerZ);
        level.setBlock(pos, ROOF_C, 2);

        // ---- 11. 四隅の塔 ----------------------------------------------------------
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int tx = centerX + sx * HALF;
                int tz = centerZ + sz * HALF;
                for (int y = floorY + WALL_HEIGHT + 1; y <= floorY + WALL_HEIGHT + 2; y++) {
                    pos.set(tx, y, tz);
                    level.setBlock(pos, PILLAR, 2);
                }
                pos.set(tx, floorY + WALL_HEIGHT + 3, tz);
                level.setBlock(pos, ROOF_C, 2);
            }
        }

        // ---- 12. 祭壇 -------------------------------------------------------------
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pos.set(centerX + dx, floorY + 1, centerZ + dz);
                level.setBlock(pos, (dx == 0 && dz == 0) ? WALL_CHISELED : PILLAR, 2);
            }
        }
        BlockState altar = resolveAltarState();
        if (altar != null) {
            pos.set(centerX, floorY + 2, centerZ);
            level.setBlock(pos, altar, 2);
        } else if (ALTAR_WARNED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[NOCTERRA] ブロック '{}' が未登録のため、古代神殿の祭壇を設置できませんでした。"
                            + " NocterraBlocks への登録後に生成される神殿には祭壇が入ります。",
                    ALTAR_ID);
        }

        return true;
    }

    /**
     * 祭壇ブロックの BlockState をレジストリから実行時解決する。
     * 未登録なら {@code null}（コンパイル時依存を作らないための措置）。
     */
    private static BlockState resolveAltarState() {
        Block block = BuiltInRegistries.BLOCK.getOptional(ALTAR_ID).filter(b -> b != Blocks.AIR).orElse(null);
        return block == null ? null : block.defaultBlockState();
    }

    /** 座標から決まる決定的なハッシュ（装飾のばらつき用。乱数は使わない）。 */
    private static int hash(int x, int y, int z) {
        int h = x * 0x9E3779B1 + y * 0x85EBCA77 + z * 0xC2B2AE3D;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return h;
    }
}

package com.nocterra.world.structure;

import com.mojang.logging.LogUtils;
import com.nocterra.world.feature.NocterraFeatureSupport;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.slf4j.Logger;

/**
 * 「ネザーの要塞」の本体。24x24・ネザーレンガの城壁（高さ8）＋四隅の塔＋中央の祭壇＋溶岩堀。
 *
 * <p>溶岩堀は外壁の外側 2 ブロック幅で、外周 1 ブロックの縁で囲って流出を防ぐ。
 * 床は全面に張るので溶岩海に埋まらない。</p>
 */
public class NetherBastionPiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new NetherBastionPiece(tag);

    public static final int SIZE = NetherBastionStructure.SIZE; // 24

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private static final int RIM = 0;
    private static final int OUTER = SIZE - 1;          // 23
    private static final int MOAT_MIN = 1;
    private static final int MOAT_MAX = 2;
    private static final int WALL_MIN = 3;
    private static final int WALL_MAX = SIZE - 4;       // 20
    private static final int WALL_H = 8;
    private static final int TOWER_H = 14;
    private static final int GATE_LX0 = 10;
    private static final int GATE_LX1 = 13;
    private static final int GATE_H = 4;
    private static final int ALTAR_LX = 12;
    private static final int ALTAR_LZ = 12;

    private static final BlockState FOUNDATION = Blocks.NETHER_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.NETHER_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState WALL = Blocks.NETHER_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState WALL_CHISELED = Blocks.CHISELED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState SHROOM = Blocks.SHROOMLIGHT.defaultBlockState();

    public NetherBastionPiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - 20, originZ, originX + SIZE - 1, floorY + 30, originZ + SIZE - 1),
                floorY);
    }

    public NetherBastionPiece(CompoundTag tag) {
        super(TYPE, tag);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        int lx0 = Math.max(0, chunkBB.minX() - this.originX);
        int lx1 = Math.min(SIZE - 1, chunkBB.maxX() - this.originX);
        int lz0 = Math.max(0, chunkBB.minZ() - this.originZ);
        int lz1 = Math.min(SIZE - 1, chunkBB.maxZ() - this.originZ);
        if (lx0 > lx1 || lz0 > lz1) {
            return;
        }

        buildFoundation(level, chunkBB, lx0, lx1, lz0, lz1);
        buildFloor(level, chunkBB, lx0, lx1, lz0, lz1);
        clearAbove(level, chunkBB, lx0, lx1, lz0, lz1);
        buildMoat(level, chunkBB, lx0, lx1, lz0, lz1);
        buildWalls(level, chunkBB);
        buildTowers(level, chunkBB);
        buildGate(level, chunkBB);
        buildAltar(level, chunkBB);
        buildLights(level, chunkBB);
    }

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = -1; relY >= -6; relY--) {
                    if (this.getAt(level, chunkBB, lx, relY, lz).isSolid()) {
                        break;
                    }
                    this.place(level, chunkBB, lx, relY, lz, FOUNDATION);
                }
            }
        }
    }

    private void buildFloor(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                this.place(level, chunkBB, lx, 0, lz, ((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B);
            }
        }
    }

    private void clearAbove(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = 1; relY <= 12; relY++) {
                    if (!this.getAt(level, chunkBB, lx, relY, lz).isAir()) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                    }
                }
            }
        }
    }

    private void buildMoat(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                boolean moat = (lx >= MOAT_MIN && lx <= MOAT_MAX)
                        || (lx >= SIZE - 1 - MOAT_MAX && lx <= SIZE - 1 - MOAT_MIN)
                        || (lz >= MOAT_MIN && lz <= MOAT_MAX)
                        || (lz >= SIZE - 1 - MOAT_MAX && lz <= SIZE - 1 - MOAT_MIN);
                if (moat) {
                    this.place(level, chunkBB, lx, 0, lz, LAVA);
                }
                // 外周の縁（溶岩の流出防止）
                if (lx == RIM || lx == OUTER || lz == RIM || lz == OUTER) {
                    this.place(level, chunkBB, lx, 0, lz, FOUNDATION);
                }
            }
        }
        // 北側の橋（門前）
        for (int lx = GATE_LX0; lx <= GATE_LX1; lx++) {
            for (int lz = 0; lz <= WALL_MIN; lz++) {
                this.place(level, chunkBB, lx, 0, lz, PILLAR);
            }
        }
    }

    private void buildWalls(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = WALL_MIN; lx <= WALL_MAX; lx++) {
            for (int lz = WALL_MIN; lz <= WALL_MAX; lz++) {
                boolean edge = lx == WALL_MIN || lx == WALL_MAX || lz == WALL_MIN || lz == WALL_MAX;
                if (!edge) {
                    continue;
                }
                for (int relY = 1; relY <= WALL_H; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, wallState(lx, relY, lz));
                }
                if (((lx + lz) & 1) == 0) {
                    this.place(level, chunkBB, lx, WALL_H + 1, lz, PILLAR);
                }
            }
        }
    }

    private void buildTowers(WorldGenLevel level, BoundingBox chunkBB) {
        int[] corners = {WALL_MIN, WALL_MAX - 3};
        for (int cx : corners) {
            for (int cz : corners) {
                for (int dx = 0; dx < 4; dx++) {
                    for (int dz = 0; dz < 4; dz++) {
                        int lx = cx + dx;
                        int lz = cz + dz;
                        for (int relY = 1; relY <= TOWER_H; relY++) {
                            boolean shell = dx == 0 || dx == 3 || dz == 0 || dz == 3;
                            this.place(level, chunkBB, lx, relY, lz,
                                    shell ? wallState(lx, relY, lz) : PILLAR);
                        }
                        boolean rim = dx == 0 || dx == 3 || dz == 0 || dz == 3;
                        if (rim && ((dx + dz) & 1) == 0) {
                            this.place(level, chunkBB, lx, TOWER_H + 1, lz, WALL_CHISELED);
                        }
                    }
                }
            }
        }
    }

    private void buildGate(WorldGenLevel level, BoundingBox chunkBB) {
        // 北面の門（4 幅 x 4 高）
        for (int lx = GATE_LX0; lx <= GATE_LX1; lx++) {
            for (int relY = 1; relY <= GATE_H; relY++) {
                this.place(level, chunkBB, lx, relY, WALL_MIN, AIR);
            }
        }
        for (int lx = GATE_LX0 - 1; lx <= GATE_LX1 + 1; lx++) {
            this.place(level, chunkBB, lx, GATE_H + 1, WALL_MIN, WALL_CHISELED);
        }
    }

    private void buildAltar(WorldGenLevel level, BoundingBox chunkBB) {
        // 5x5 の祭壇台座
        for (int lx = ALTAR_LX - 2; lx <= ALTAR_LX + 2; lx++) {
            for (int lz = ALTAR_LZ - 2; lz <= ALTAR_LZ + 2; lz++) {
                boolean edge = lx == ALTAR_LX - 2 || lx == ALTAR_LX + 2
                        || lz == ALTAR_LZ - 2 || lz == ALTAR_LZ + 2;
                this.place(level, chunkBB, lx, 0, lz,
                        edge ? WALL_CHISELED : (((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B));
            }
        }
        // 四隅の柱
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int relY = 1; relY <= 2; relY++) {
                this.place(level, chunkBB, ALTAR_LX + c[0], relY, ALTAR_LZ + c[1], WALL_CHISELED);
            }
        }
        // 祭壇ブロック（実行時解決）
        BlockState altar = NocterraFeatureSupport.resolve("region_boss_altar", WALL_CHISELED);
        if (altar.equals(WALL_CHISELED) && WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[NOCTERRA] ブロック 'nocterra:region_boss_altar' が未登録のため、"
                    + "ネザー要塞の祭壇は仮ブロックで生成されます。");
        }
        this.place(level, chunkBB, ALTAR_LX, 1, ALTAR_LZ, altar);
    }

    private void buildLights(WorldGenLevel level, BoundingBox chunkBB) {
        this.place(level, chunkBB, 6, 0, 6, GLOW);
        this.place(level, chunkBB, 17, 0, 6, GLOW);
        this.place(level, chunkBB, 6, 0, 17, GLOW);
        this.place(level, chunkBB, 17, 0, 17, GLOW);
        this.place(level, chunkBB, WALL_MIN, 5, ALTAR_LZ, SHROOM);
        this.place(level, chunkBB, WALL_MAX, 5, ALTAR_LZ, SHROOM);
        this.place(level, chunkBB, ALTAR_LX, 5, WALL_MIN, SHROOM);
        this.place(level, chunkBB, ALTAR_LX, 5, WALL_MAX, SHROOM);
    }

    private static BlockState wallState(int lx, int relY, int lz) {
        int h = Math.floorMod(hash(lx, relY, lz), 100);
        if (h < 10) {
            return WALL_CRACKED;
        }
        if (h < 18) {
            return WALL_CHISELED;
        }
        return WALL;
    }
}

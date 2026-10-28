package com.nocterra.world.structure;

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

/**
 * 亡国の城塞のピース。24x24 のバウンディングボックスを 1 ピースで持ち、
 * 各チャンクの {@code postProcess} が {@code chunkBB} でクリップされた自分の分だけを置く。
 *
 * <p>乱数は一切使わない（{@link RandomSource} はチャンクごとにシードが違うため、
 * 使うとチャンク境界で模様が変わってしまう）。模様は座標ハッシュで決める。</p>
 */
public class FallenFortressPiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new FallenFortressPiece(tag);

    private static final int SIZE = FallenFortressStructure.SIZE; // 24
    private static final int FOUNDATION_DEPTH = 20;
    private static final int CLEAR_HEIGHT = 22;
    private static final int WALL_HEIGHT = 6;
    private static final int WALL_MIN = 1;
    private static final int WALL_MAX = SIZE - 2; // 22
    private static final int TOWER_HEIGHT = 12;

    private static final BlockState FOUNDATION = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState STONE_BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState STONE_BRICK_CRACKED = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();

    private static final int DUNGEON_DEPTH = 7;

    public FallenFortressPiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - 26, originZ, originX + SIZE - 1, floorY + 24, originZ + SIZE - 1),
                floorY);
    }

    public FallenFortressPiece(CompoundTag tag) {
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
        buildFoundation(level, chunkBB);
        clearAbove(level, chunkBB);
        buildFloor(level, chunkBB);
        buildWalls(level, chunkBB);
        buildTowers(level, chunkBB);
        buildGate(level, chunkBB);
        buildKeep(level, chunkBB);
        buildDungeon(level, chunkBB);
    }

    // ------------------------------------------------------------------ 地形の下処理

    /** 床下の空洞・水・溶岩を石で埋めて土台を作る（水没地でも内部は乾く）。 */
    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                for (int relY = -1; relY >= -FOUNDATION_DEPTH; relY--) {
                    if (this.getAt(level, chunkBB, lx, relY, lz).isSolid()) {
                        break;
                    }
                    this.place(level, chunkBB, lx, relY, lz, ((lx + lz + relY) & 1) == 0 ? FOUNDATION : COBBLE);
                }
            }
        }
    }

    /** 屋根の上を空けて、丘や木に埋もれないようにする（この後に構造物を建てる）。 */
    private void clearAbove(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                for (int relY = 1; relY <= CLEAR_HEIGHT; relY++) {
                    if (!this.getAt(level, chunkBB, lx, relY, lz).isAir()) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                    }
                }
            }
        }
    }

    private void buildFloor(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = WALL_MIN; lx <= WALL_MAX; lx++) {
            for (int lz = WALL_MIN; lz <= WALL_MAX; lz++) {
                this.place(level, chunkBB, lx, 0, lz, ((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B);
            }
        }
    }

    // ------------------------------------------------------------------ 城壁・塔・門

    private void buildWalls(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = WALL_MIN; lx <= WALL_MAX; lx++) {
            for (int lz = WALL_MIN; lz <= WALL_MAX; lz++) {
                boolean edge = lx == WALL_MIN || lx == WALL_MAX || lz == WALL_MIN || lz == WALL_MAX;
                if (!edge) {
                    continue;
                }
                for (int relY = 1; relY <= WALL_HEIGHT; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, wallState(lx, relY, lz));
                }
                // 胸壁（merlon）
                if (((lx + lz) & 1) == 0) {
                    this.place(level, chunkBB, lx, WALL_HEIGHT + 1, lz, PILLAR);
                }
            }
        }
    }

    private void buildTowers(WorldGenLevel level, BoundingBox chunkBB) {
        int[] cornerX = {WALL_MIN, WALL_MAX - 3};
        int[] cornerZ = {WALL_MIN, WALL_MAX - 3};
        for (int cx : cornerX) {
            for (int cz : cornerZ) {
                for (int dx = 0; dx < 4; dx++) {
                    for (int dz = 0; dz < 4; dz++) {
                        int lx = cx + dx;
                        int lz = cz + dz;
                        for (int relY = 1; relY <= TOWER_HEIGHT; relY++) {
                            this.place(level, chunkBB, lx, relY, lz, towerState(dx, relY, dz));
                        }
                        boolean rim = dx == 0 || dx == 3 || dz == 0 || dz == 3;
                        if (rim && ((dx + dz) & 1) == 0) {
                            this.place(level, chunkBB, lx, TOWER_HEIGHT + 1, lz, PILLAR);
                        }
                    }
                }
            }
        }
    }

    private void buildGate(WorldGenLevel level, BoundingBox chunkBB) {
        int gateZ = WALL_MIN;
        int gateMinX = 11;
        int gateMaxX = 13;
        // 3 幅 x 4 高の開口
        for (int lx = gateMinX; lx <= gateMaxX; lx++) {
            for (int relY = 1; relY <= 4; relY++) {
                this.place(level, chunkBB, lx, relY, gateZ, AIR);
            }
            // 入口前（バウンディングボックス端の 1 ブロック）を空けて床を張る
            this.place(level, chunkBB, lx, 0, 0, FLOOR_A);
            for (int relY = 1; relY <= 4; relY++) {
                this.place(level, chunkBB, lx, relY, 0, AIR);
            }
            for (int relY = -1; relY >= -8; relY--) {
                if (this.getAt(level, chunkBB, lx, relY, 0).isSolid()) {
                    break;
                }
                this.place(level, chunkBB, lx, relY, 0, FOUNDATION);
            }
        }
        // 門柱とまぐさ
        for (int lx : new int[]{gateMinX - 1, gateMaxX + 1}) {
            for (int relY = 1; relY <= WALL_HEIGHT; relY++) {
                this.place(level, chunkBB, lx, relY, gateZ, PILLAR);
            }
        }
        for (int lx = gateMinX - 1; lx <= gateMaxX + 1; lx++) {
            this.place(level, chunkBB, lx, WALL_HEIGHT, gateZ, CHISELED);
        }
    }

    // ------------------------------------------------------------------ 中庭の廃墟

    private void buildKeep(WorldGenLevel level, BoundingBox chunkBB) {
        int x0 = 9;
        int x1 = 15;
        int z0 = 6;
        int z1 = 12;
        for (int lx = x0; lx <= x1; lx++) {
            for (int lz = z0; lz <= z1; lz++) {
                boolean edge = lx == x0 || lx == x1 || lz == z0 || lz == z1;
                for (int relY = 1; relY <= 5; relY++) {
                    if (!edge) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                        continue;
                    }
                    // 廃墟らしく上部を欠けさせる（決定的）
                    boolean broken = relY >= 3 && Math.floorMod(hash(lx, relY, lz), 3) == 0;
                    if (broken) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                    } else {
                        this.place(level, chunkBB, lx, relY, lz, wallState(lx, relY, lz));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ 地下牢

    private void buildDungeon(WorldGenLevel level, BoundingBox chunkBB) {
        int dFloor = -DUNGEON_DEPTH;
        int roomX0 = 7;
        int roomX1 = 15;
        int roomZ0 = 3;
        int roomZ1 = 13;

        // 1) 階段（南側から北へ下る 7 段）
        for (int i = 1; i <= DUNGEON_DEPTH; i++) {
            int lz = 21 - i; // 20..14
            int relY = -i;   // -1..-7
            for (int lx = 9; lx <= 11; lx++) {
                this.place(level, chunkBB, lx, relY, lz, PILLAR);
                for (int head = 1; head <= 3; head++) {
                    this.place(level, chunkBB, lx, relY + head, lz, AIR);
                }
            }
            for (int wallY = relY; wallY <= 1; wallY++) {
                this.place(level, chunkBB, 8, wallY, lz, WALL);
                this.place(level, chunkBB, 12, wallY, lz, WALL);
            }
        }

        // 2) 牢室の外殻（床・壁・天井を必ず張る = 周囲が水/溶岩でも内部は乾く）
        for (int lx = roomX0; lx <= roomX1; lx++) {
            for (int lz = roomZ0; lz <= roomZ1; lz++) {
                boolean edge = lx == roomX0 || lx == roomX1 || lz == roomZ0 || lz == roomZ1;
                this.place(level, chunkBB, lx, dFloor, lz, FOUNDATION);
                this.place(level, chunkBB, lx, dFloor + 5, lz, WALL);
                for (int relY = dFloor + 1; relY <= dFloor + 4; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, edge ? wallState(lx, relY, lz) : AIR);
                }
            }
        }

        // 3) 階段と牢室の接続（南壁に 3 幅 x 3 高の開口）
        for (int lx = 9; lx <= 11; lx++) {
            for (int relY = dFloor + 1; relY <= dFloor + 3; relY++) {
                this.place(level, chunkBB, lx, relY, roomZ1, AIR);
            }
        }

        // 4) 最低限の明かり
        this.place(level, chunkBB, 8, dFloor + 5, 8, GLOW);
        this.place(level, chunkBB, 14, dFloor + 5, 8, GLOW);
        this.place(level, chunkBB, 11, dFloor + 5, 11, GLOW);
        this.place(level, chunkBB, 12, -3, 18, GLOW);
        this.place(level, chunkBB, 8, -3, 19, GLOW);
    }

    // ------------------------------------------------------------------ 建材のばらつき

    private static BlockState wallState(int lx, int relY, int lz) {
        int h = Math.floorMod(hash(lx, relY, lz), 100);
        if (h < 8) {
            return WALL_CRACKED;
        }
        if (h < 14) {
            return STONE_BRICK_CRACKED;
        }
        if (h < 22) {
            return STONE_BRICK;
        }
        return WALL;
    }

    private static BlockState towerState(int dx, int relY, int dz) {
        if (relY % 4 == 0) {
            return PILLAR;
        }
        if (dx == 0 || dx == 3 || dz == 0 || dz == 3) {
            return CHISELED;
        }
        return wallState(dx * 7 + 1, relY, dz * 7 + 3);
    }
}

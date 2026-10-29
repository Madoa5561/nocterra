package com.nocterra.world.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * 「灰冠の大城塞」のピース。40x40・4階建て。所有: Lead。
 *
 * <p>各階に床・外周壁・内壁・窓・照明・階段吹き抜け・宝箱・モブスポナーを配置する。
 * 座標は向きに依存しない絶対座標で、チャンク外は {@code chunkBB} でクリップされる。</p>
 */
public class GrandCastlePiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new GrandCastlePiece(tag);

    private static final int SIZE = GrandCastleStructure.SIZE; // 40
    private static final int FLOOR_HEIGHT = 9;
    private static final int FLOORS = 4;
    private static final int WALL_HEIGHT = 7;
    private static final int FOUNDATION_DEPTH = 24;
    private static final int ROOF_Y = FLOOR_HEIGHT * FLOORS; // 36

    private static final BlockState FOUNDATION = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState FLOOR = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState FLOOR_ALT = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState STAIRS = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();

    /** 階ごとのスポナーに出すモブ（nocterra のモブID）。 */
    private static final String[] SPAWNERS = {"ash_crawler", "bog_ghoul", "ashen_wraith", "mourning_wisp"};

    public GrandCastlePiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - FOUNDATION_DEPTH, originZ,
                        originX + SIZE - 1, floorY + ROOF_Y + 16, originZ + SIZE - 1),
                floorY);
    }

    public GrandCastlePiece(CompoundTag tag) {
        super(TYPE, tag);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            net.minecraft.world.level.StructureManager structureManager,
            net.minecraft.world.level.chunk.ChunkGenerator generator,
            net.minecraft.util.RandomSource random,
            BoundingBox chunkBB,
            net.minecraft.world.level.ChunkPos chunkPos,
            net.minecraft.core.BlockPos referencePos) {
        buildFoundation(level, chunkBB);
        clearInterior(level, chunkBB);
        for (int floor = 0; floor < FLOORS; floor++) {
            buildFloor(level, chunkBB, floor);
        }
        buildStairwell(level, chunkBB);
        buildRoof(level, chunkBB);
        buildTowers(level, chunkBB);
        buildGate(level, chunkBB);
        furnish(level, chunkBB);
    }

    // ------------------------------------------------------------------ 骨格

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                for (int relY = -FOUNDATION_DEPTH; relY <= -1; relY++) {
                    BlockState state = relY == -1 ? FLOOR : FOUNDATION;
                    this.place(level, chunkBB, lx, relY, lz, state);
                }
            }
        }
    }

    private void clearInterior(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = 1; lx < SIZE - 1; lx++) {
            for (int lz = 1; lz < SIZE - 1; lz++) {
                for (int relY = 0; relY < ROOF_Y + 2; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, AIR);
                }
            }
        }
    }

    /** 1 フロア分（床・外周壁・内壁・窓・照明）。 */
    private void buildFloor(WorldGenLevel level, BoundingBox chunkBB, int floor) {
        int base = floor * FLOOR_HEIGHT;
        // 床
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                BlockState state = ((lx / 4) + (lz / 4) + floor) % 2 == 0 ? FLOOR : FLOOR_ALT;
                this.place(level, chunkBB, lx, base, lz, state);
            }
        }
        // 外周壁
        for (int relY = base + 1; relY <= base + WALL_HEIGHT; relY++) {
            for (int i = 0; i < SIZE; i++) {
                boolean window = (i % 7 == 3) && (relY == base + 3 || relY == base + 4);
                BlockState outer = window ? BARS : wallState(i, relY, floor, true);
                this.place(level, chunkBB, i, relY, 0, outer);
                this.place(level, chunkBB, i, relY, SIZE - 1, outer);
                this.place(level, chunkBB, 0, relY, i, outer);
                this.place(level, chunkBB, SIZE - 1, relY, i, outer);
            }
        }
        // 内壁（通路の扉つき）
        for (int i = 1; i < SIZE - 1; i++) {
            boolean door = (i % 8 >= 3 && i % 8 <= 5);
            if (door) {
                continue;
            }
            for (int relY = base + 1; relY <= base + WALL_HEIGHT - 1; relY++) {
                this.place(level, chunkBB, 13, relY, i, wallState(i, relY, floor, false));
                this.place(level, chunkBB, 26, relY, i, wallState(i, relY, floor, false));
            }
        }
        // 照明
        this.place(level, chunkBB, 2, base + WALL_HEIGHT - 1, 2, GLOW);
        this.place(level, chunkBB, SIZE - 3, base + WALL_HEIGHT - 1, 2, GLOW);
        this.place(level, chunkBB, 2, base + WALL_HEIGHT - 1, SIZE - 3, GLOW);
        this.place(level, chunkBB, SIZE - 3, base + WALL_HEIGHT - 1, SIZE - 3, GLOW);
        // 最上階以外は天井
        if (floor < FLOORS - 1) {
            for (int lx = 0; lx < SIZE; lx++) {
                for (int lz = 0; lz < SIZE; lz++) {
                    this.place(level, chunkBB, lx, base + WALL_HEIGHT, lz,
                            ((lx + lz) % 5 == 0) ? CHISELED : WALL);
                }
            }
        }
    }

    /** 中央の吹き抜けと階段（各階の床に穴を開けて階段を置く）。 */
    private void buildStairwell(WorldGenLevel level, BoundingBox chunkBB) {
        for (int floor = 1; floor < FLOORS; floor++) {
            int base = floor * FLOOR_HEIGHT;
            for (int lx = 18; lx <= 21; lx++) {
                for (int lz = 18; lz <= 21; lz++) {
                    this.place(level, chunkBB, lx, base, lz, AIR);
                }
            }
        }
        for (int floor = 0; floor < FLOORS - 1; floor++) {
            int base = floor * FLOOR_HEIGHT;
            for (int step = 0; step < FLOOR_HEIGHT - 1; step++) {
                int lx = 18;
                int lz = 18 + Math.min(step, 3);
                this.place(level, chunkBB, lx, base + 1 + step, lz, STAIRS);
                this.place(level, chunkBB, lx + 1, base + 1 + step, lz, AIR);
                this.place(level, chunkBB, lx + 2, base + 1 + step, lz, AIR);
            }
        }
    }

    private void buildRoof(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = 0; lx < SIZE; lx++) {
            for (int lz = 0; lz < SIZE; lz++) {
                this.place(level, chunkBB, lx, ROOF_Y, lz, WALL);
            }
        }
        // 胸壁
        for (int i = 0; i < SIZE; i++) {
            if (i % 2 == 0) {
                this.place(level, chunkBB, i, ROOF_Y + 1, 0, WALL_CRACKED);
                this.place(level, chunkBB, i, ROOF_Y + 1, SIZE - 1, WALL_CRACKED);
                this.place(level, chunkBB, 0, ROOF_Y + 1, i, WALL_CRACKED);
                this.place(level, chunkBB, SIZE - 1, ROOF_Y + 1, i, WALL_CRACKED);
            }
        }
    }

    private void buildTowers(WorldGenLevel level, BoundingBox chunkBB) {
        int[][] corners = {{1, 1}, {SIZE - 8, 1}, {1, SIZE - 8}, {SIZE - 8, SIZE - 8}};
        for (int[] corner : corners) {
            for (int dx = 0; dx < 7; dx++) {
                for (int dz = 0; dz < 7; dz++) {
                    for (int relY = ROOF_Y + 1; relY <= ROOF_Y + 14; relY++) {
                        boolean edge = dx == 0 || dx == 6 || dz == 0 || dz == 6;
                        if (edge) {
                            BlockState state = relY == ROOF_Y + 14
                                    ? WALL_CRACKED
                                    : wallState(dx + dz, relY, 3, false);
                            this.place(level, chunkBB, corner[0] + dx, relY, corner[1] + dz, state);
                        } else {
                            this.place(level, chunkBB, corner[0] + dx, relY, corner[1] + dz, AIR);
                        }
                    }
                }
            }
            this.place(level, chunkBB, corner[0] + 3, ROOF_Y + 13, corner[1] + 3, GLOW);
        }
    }

    private void buildGate(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = SIZE / 2 - 2; lx <= SIZE / 2 + 2; lx++) {
            for (int relY = 1; relY <= 5; relY++) {
                this.place(level, chunkBB, lx, relY, 0, AIR);
            }
        }
        for (int relY = 6; relY <= 8; relY++) {
            for (int lx = SIZE / 2 - 3; lx <= SIZE / 2 + 3; lx++) {
                this.place(level, chunkBB, lx, relY, 0, CHISELED);
            }
        }
    }

    // ------------------------------------------------------------------ 中身

    /** 宝箱とモブスポナーを各階に配置する。 */
    private void furnish(WorldGenLevel level, BoundingBox chunkBB) {
        for (int floor = 0; floor < FLOORS; floor++) {
            int base = floor * FLOOR_HEIGHT + 1;
            // 宝箱（各階 2 個）
            this.placeLootChest(level, chunkBB, 4, base, 4, "grand_castle");
            this.placeLootChest(level, chunkBB, SIZE - 5, base, SIZE - 5, "grand_castle");
            this.placeLootChest(level, chunkBB, SIZE - 5, base, 4, floor >= 2 ? "grand_castle" : "fortress");
            // モブスポナー（各階 1 個）
            this.placeSpawner(level, chunkBB, SIZE / 2 + 6, base, SIZE / 2,
                    SPAWNERS[Math.min(floor, SPAWNERS.length - 1)]);
        }
        // 最上階（玉座の間）に追加の宝箱
        this.placeLootChest(level, chunkBB, SIZE / 2, FLOOR_HEIGHT * (FLOORS - 1) + 1, SIZE / 2 + 5,
                "grand_castle");
    }

    private static BlockState wallState(int lx, int relY, int floor, boolean outer) {
        int h = Math.floorMod(lx * 31 + relY * 17 + floor * 7, 100);
        if (h < 18) {
            return WALL_CRACKED;
        }
        if (h < 24) {
            return PILLAR;
        }
        if (outer && h < 28) {
            return CHISELED;
        }
        return WALL;
    }
}

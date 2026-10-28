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
 * 地下墓所のピース。15x15 の範囲に、地表から 12 ブロック降りる階段と
 * 9x11 の墓所（外殻は防水）を作る。乱数は使わない。
 */
public class MourningCatacombPiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new MourningCatacombPiece(tag);

    private static final int SIZE = MourningCatacombStructure.SIZE; // 15
    private static final int DEPTH = MourningCatacombStructure.DEPTH; // 12

    /** 墓所のフロア（relY = surfaceY からの相対）。 */
    private static final int ROOM_FLOOR = -DEPTH;
    private static final int ROOM_CEILING = ROOM_FLOOR + 5;
    private static final int ROOM_MIN_X = 5;
    private static final int ROOM_MAX_X = 13;
    private static final int ROOM_MIN_Z = 3;
    private static final int ROOM_MAX_Z = 13;

    private static final BlockState FOUNDATION = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState STONE_BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();

    public MourningCatacombPiece(int originX, int originZ, int surfaceY) {
        super(TYPE,
                new BoundingBox(originX, surfaceY - 16, originZ, originX + SIZE - 1, surfaceY + 5, originZ + SIZE - 1),
                surfaceY);
    }

    public MourningCatacombPiece(CompoundTag tag) {
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
        buildRoomShell(level, chunkBB);
        buildStairs(level, chunkBB);
        connectStairsToRoom(level, chunkBB);
        decorate(level, chunkBB);
    }

    /** 墓所の外殻（床・壁・天井）を必ず張る = 周囲が水/溶岩でも内部は乾く。 */
    private void buildRoomShell(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = ROOM_MIN_X; lx <= ROOM_MAX_X; lx++) {
            for (int lz = ROOM_MIN_Z; lz <= ROOM_MAX_Z; lz++) {
                boolean edge = lx == ROOM_MIN_X || lx == ROOM_MAX_X || lz == ROOM_MIN_Z || lz == ROOM_MAX_Z;
                this.place(level, chunkBB, lx, ROOM_FLOOR, lz, FOUNDATION);
                this.place(level, chunkBB, lx, ROOM_CEILING, lz, WALL);
                for (int relY = ROOM_FLOOR + 1; relY < ROOM_CEILING; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, edge ? wallState(lx, relY, lz) : AIR);
                }
            }
        }
        // 床下 4 ブロックを固めて、下から水/溶岩が湧いても漏れないようにする
        for (int lx = ROOM_MIN_X; lx <= ROOM_MAX_X; lx++) {
            for (int lz = ROOM_MIN_Z; lz <= ROOM_MAX_Z; lz++) {
                for (int relY = ROOM_FLOOR - 1; relY >= ROOM_FLOOR - 4; relY--) {
                    this.place(level, chunkBB, lx, relY, lz, FOUNDATION);
                }
            }
        }
    }

    /** 地表から墓所まで 3 幅の階段を掘る。 */
    private void buildStairs(WorldGenLevel level, BoundingBox chunkBB) {
        // 入口（地表）: 3x3 を開けて床を張る
        for (int lx = 1; lx <= 3; lx++) {
            this.place(level, chunkBB, lx, 0, 0, STONE_BRICK);
            for (int relY = 1; relY <= 3; relY++) {
                this.place(level, chunkBB, lx, relY, 0, AIR);
            }
        }
        for (int i = 1; i <= DEPTH; i++) {
            int lz = i;
            int relY = -i;
            for (int lx = 1; lx <= 3; lx++) {
                // 踏面と、その下の目地
                this.place(level, chunkBB, lx, relY, lz, PILLAR);
                for (int below = 1; below <= 2; below++) {
                    this.place(level, chunkBB, lx, relY - below, lz, FOUNDATION);
                }
                for (int head = 1; head <= 3; head++) {
                    this.place(level, chunkBB, lx, relY + head, lz, AIR);
                }
            }
            // 階段の側壁
            for (int wallY = relY; wallY <= 1; wallY++) {
                this.place(level, chunkBB, 0, wallY, lz, wallState(0, wallY, lz));
                this.place(level, chunkBB, 4, wallY, lz, wallState(4, wallY, lz));
            }
        }
        // 入口まわりの縁
        for (int lx = 0; lx <= 4; lx++) {
            for (int lz = 0; lz <= 1; lz++) {
                if (lx == 0 || lx == 4) {
                    this.place(level, chunkBB, lx, 0, lz, STONE_BRICK);
                    this.place(level, chunkBB, lx, 1, lz, PILLAR);
                }
            }
        }
    }

    /** 階段の最下段と墓所を 3 幅の通路でつなぐ。 */
    private void connectStairsToRoom(WorldGenLevel level, BoundingBox chunkBB) {
        int corridorZ = 12;
        for (int lx = 4; lx <= ROOM_MIN_X; lx++) {
            this.place(level, chunkBB, lx, ROOM_FLOOR, corridorZ, PILLAR);
            this.place(level, chunkBB, lx, ROOM_FLOOR, corridorZ + 1, PILLAR);
            for (int relY = ROOM_FLOOR + 1; relY <= ROOM_FLOOR + 3; relY++) {
                this.place(level, chunkBB, lx, relY, corridorZ, AIR);
                this.place(level, chunkBB, lx, relY, corridorZ + 1, AIR);
            }
        }
        // 通路の天井と側壁
        for (int lx = 4; lx <= ROOM_MIN_X; lx++) {
            for (int lz = corridorZ; lz <= corridorZ + 1; lz++) {
                this.place(level, chunkBB, lx, ROOM_FLOOR + 4, lz, WALL);
            }
            for (int relY = ROOM_FLOOR; relY <= ROOM_FLOOR + 4; relY++) {
                this.place(level, chunkBB, lx, relY, corridorZ - 1, WALL);
            }
        }
    }

    private void decorate(WorldGenLevel level, BoundingBox chunkBB) {
        // 四隅の柱は「外殻の角」に置く（内部の角に置くと階段からの通路を塞ぐ）。
        // ただし南西角 (ROOM_MIN_X, ROOM_MAX_Z) は階段からの通路の開口なので柱を置かない。
        for (int[] corner : new int[][]{{ROOM_MIN_X, ROOM_MIN_Z}, {ROOM_MAX_X, ROOM_MIN_Z},
                {ROOM_MAX_X, ROOM_MAX_Z}}) {
            for (int relY = ROOM_FLOOR + 1; relY < ROOM_CEILING; relY++) {
                this.place(level, chunkBB, corner[0], relY, corner[1], PILLAR);
            }
        }
        // 中央の石棺（2 ブロック）
        this.place(level, chunkBB, 9, ROOM_FLOOR + 1, 8, CHISELED);
        this.place(level, chunkBB, 10, ROOM_FLOOR + 1, 8, CHISELED);
        // 明かり
        for (int[] lamp : new int[][]{{ROOM_MIN_X + 2, ROOM_MIN_Z + 2}, {ROOM_MAX_X - 2, ROOM_MIN_Z + 2},
                {ROOM_MIN_X + 2, ROOM_MAX_Z - 2}, {ROOM_MAX_X - 2, ROOM_MAX_Z - 2}, {9, 8}}) {
            this.place(level, chunkBB, lamp[0], ROOM_CEILING, lamp[1], GLOW);
        }
        // 階段の明かり
        this.place(level, chunkBB, 4, -4, 6, GLOW);
        this.place(level, chunkBB, 0, -8, 10, GLOW);
    }

    private static BlockState wallState(int lx, int relY, int lz) {
        int h = Math.floorMod(hash(lx, relY, lz), 100);
        if (h < 12) {
            return WALL_CRACKED;
        }
        return h < 30 ? STONE_BRICK : WALL;
    }
}

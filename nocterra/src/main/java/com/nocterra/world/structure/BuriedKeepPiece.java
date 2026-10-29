package com.nocterra.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 「埋もれた要塞」の本体。14x14 の 2 部屋の小要塞で、北面 3x7 の入口が周囲の洞窟/岩に開く。
 *
 * <p>座標は {@link NocterraStructurePiece} の絶対座標ヘルパーで計算し、{@code chunkBB} で
 * クリップする（FULL 隣チャンクへの書き込みは捨てられるため）。</p>
 */
public class BuriedKeepPiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new BuriedKeepPiece(tag);

    public static final int SIZE = BuriedKeepStructure.SIZE; // 14

    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"));

    private static final int WALL_MIN = 1;
    private static final int WALL_MAX = SIZE - 2; // 12
    private static final int WALL_H = 5;
    private static final int PARTITION_Z = 7;

    private static final BlockState FOUNDATION = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState[] WALL_BLOCKS = {
        Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
        Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
        Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
        Blocks.STONE_BRICKS.defaultBlockState(),
        Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
    };
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState CHEST = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState TORCH = Blocks.TORCH.defaultBlockState();

    public BuriedKeepPiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - 14, originZ, originX + SIZE - 1, floorY + 10, originZ + SIZE - 1),
                floorY);
    }

    public BuriedKeepPiece(CompoundTag tag) {
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
        clearAbove(level, chunkBB, lx0, lx1, lz0, lz1);
        buildFloor(level, chunkBB, lx0, lx1, lz0, lz1);
        buildWalls(level, chunkBB);
        buildPartition(level, chunkBB);
        carveEntrance(level, chunkBB);
        decorate(level, chunkBB);
            this.scatterLootChests(level, chunkBB, "keep");
}

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = -1; relY >= -8; relY--) {
                    if (this.getAt(level, chunkBB, lx, relY, lz).isSolid()) {
                        break;
                    }
                    this.place(level, chunkBB, lx, relY, lz, FOUNDATION);
                }
            }
        }
    }

    private void clearAbove(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = 1; relY <= 8; relY++) {
                    if (!this.getAt(level, chunkBB, lx, relY, lz).isAir()) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                    }
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

    private void buildWalls(WorldGenLevel level, BoundingBox chunkBB) {
        for (int lx = WALL_MIN; lx <= WALL_MAX; lx++) {
            for (int lz = WALL_MIN; lz <= WALL_MAX; lz++) {
                boolean edge = lx == WALL_MIN || lx == WALL_MAX || lz == WALL_MIN || lz == WALL_MAX;
                if (!edge) {
                    continue;
                }
                int h = WALL_H - Math.floorMod(hash(lx, 3, lz), 3); // 3..5（崩れ）
                if (Math.floorMod(hash(lx, 7, lz), 11) == 0) {
                    continue; // 完全に崩落した区間
                }
                for (int relY = 1; relY <= h; relY++) {
                    int pick = Math.floorMod(hash(lx, relY, lz), WALL_BLOCKS.length);
                    this.place(level, chunkBB, lx, relY, lz, WALL_BLOCKS[pick]);
                }
            }
        }
    }

    private void buildPartition(WorldGenLevel level, BoundingBox chunkBB) {
        // 2 部屋に分ける仕切り壁（入口と反対側に扉）
        for (int lx = WALL_MIN + 1; lx <= WALL_MAX - 1; lx++) {
            for (int relY = 1; relY <= 4; relY++) {
                if (lx >= 5 && lx <= 7 && relY <= 3) {
                    continue; // 扉
                }
                int pick = Math.floorMod(hash(lx, relY, PARTITION_Z), WALL_BLOCKS.length);
                this.place(level, chunkBB, lx, relY, PARTITION_Z, WALL_BLOCKS[pick]);
            }
        }
    }

    private void carveEntrance(WorldGenLevel level, BoundingBox chunkBB) {
        // 北面の入口（3 幅 x 3 高）。バウンディングボックス端（lz=0）まで開け、
        // 周囲の洞窟/岩に口を開く。
        for (int lx = 5; lx <= 7; lx++) {
            for (int relY = 1; relY <= 3; relY++) {
                this.place(level, chunkBB, lx, relY, WALL_MIN, AIR);
                this.place(level, chunkBB, lx, relY, 0, AIR);
            }
            // 入口上の崩落（縦穴）: 上の洞窟と繋がればそのまま入口になる
            for (int relY = 4; relY <= 8; relY++) {
                this.place(level, chunkBB, lx, relY, 0, AIR);
                this.place(level, chunkBB, lx, relY, 1, AIR);
            }
        }
    }

    private void decorate(WorldGenLevel level, BoundingBox chunkBB) {
        // 宝箱（部屋 A）
        this.place(level, chunkBB, 3, 1, 3, CHEST);
        BlockPos chestPos = new BlockPos(this.originX + 3, this.baseY + 1, this.originZ + 3);
        if (chunkBB.isInside(chestPos)
                && level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(LOOT);
            container.setLootTableSeed((long) this.baseY * 31L + this.originX * 17L + this.originZ * 13L);
        }
        // 明かり（床に埋め込むグロウストーン + 松明）
        this.place(level, chunkBB, 10, 0, 3, GLOW);
        this.place(level, chunkBB, 3, 0, 10, GLOW);
        this.place(level, chunkBB, 10, 0, 10, GLOW);
        this.place(level, chunkBB, 5, 1, 4, TORCH);
        this.place(level, chunkBB, 8, 1, 9, TORCH);
        // 瓦礫
        for (int[] c : new int[][]{{4, 1}, {9, 2}, {6, 9}, {10, 5}}) {
            int pick = Math.floorMod(hash(c[0], 1, c[1]), WALL_BLOCKS.length);
            this.place(level, chunkBB, c[0], 1, c[1], WALL_BLOCKS[pick]);
        }
    }
}

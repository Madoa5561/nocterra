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
 * 「エンドの城塞」の本体。18x18・エンドストーン/黒曜石/プルプァの塔（高さ16）＋中央広場＋祭壇。
 *
 * <p>浮遊島から落ちないよう基礎を下へ 12 ブロック伸ばす。</p>
 */
public class EndCitadelPiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new EndCitadelPiece(tag);

    public static final int SIZE = EndCitadelStructure.SIZE; // 18
    private static final int TOWER_MIN = 6; // 5x5 の中央塔 x/z 6..10
    private static final int TOWER_MAX = 10;
    private static final int TOWER_H = 16;
    private static final int PILLAR_H = 16;
    private static final int CENTER = 8;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private static final BlockState FOUNDATION = Blocks.END_STONE.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.END_STONE.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.END_STONE_BRICKS.defaultBlockState();
    private static final BlockState BORDER = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState TOWER = Blocks.PURPUR_BLOCK.defaultBlockState();
    private static final BlockState TOWER_EDGE = Blocks.END_STONE_BRICKS.defaultBlockState();
    private static final BlockState PILLAR = Blocks.PURPUR_PILLAR.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState ROD = Blocks.END_ROD.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();

    public EndCitadelPiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - 16, originZ, originX + SIZE - 1, floorY + 24, originZ + SIZE - 1),
                floorY);
    }

    public EndCitadelPiece(CompoundTag tag) {
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
        buildCornerPillars(level, chunkBB);
        buildCentralTower(level, chunkBB);
        buildPlaza(level, chunkBB);
    }

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = -1; relY >= -12; relY--) {
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
                for (int relY = 1; relY <= 20; relY++) {
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
                boolean border = lx == 0 || lx == SIZE - 1 || lz == 0 || lz == SIZE - 1;
                BlockState state = border ? BORDER : (((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B);
                this.place(level, chunkBB, lx, 0, lz, state);
            }
        }
    }

    private void buildCornerPillars(WorldGenLevel level, BoundingBox chunkBB) {
        for (int cx : new int[]{1, SIZE - 3}) {
            for (int cz : new int[]{1, SIZE - 3}) {
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        for (int relY = 1; relY <= PILLAR_H; relY++) {
                            BlockState state = relY == PILLAR_H ? PILLAR : OBSIDIAN;
                            this.place(level, chunkBB, cx + dx, relY, cz + dz, state);
                        }
                        this.place(level, chunkBB, cx + dx, PILLAR_H + 1, cz + dz, ROD);
                    }
                }
            }
        }
    }

    private void buildCentralTower(WorldGenLevel level, BoundingBox chunkBB) {
        // 5x5 の塔
        for (int lx = TOWER_MIN; lx <= TOWER_MAX; lx++) {
            for (int lz = TOWER_MIN; lz <= TOWER_MAX; lz++) {
                boolean edge = lx == TOWER_MIN || lx == TOWER_MAX || lz == TOWER_MIN || lz == TOWER_MAX;
                if (edge) {
                    boolean corner = (lx == TOWER_MIN || lx == TOWER_MAX) && (lz == TOWER_MIN || lz == TOWER_MAX);
                    for (int relY = 1; relY <= TOWER_H; relY++) {
                        this.place(level, chunkBB, lx, relY, lz, corner ? TOWER_EDGE : TOWER);
                    }
                }
                // 屋根
                this.place(level, chunkBB, lx, TOWER_H + 1, lz, TOWER);
            }
        }
        // 塔頂の飾り
        this.place(level, chunkBB, CENTER, TOWER_H + 2, CENTER, ROD);
        // 4 面のアーチ（入口）
        for (int lx = CENTER - 1; lx <= CENTER + 1; lx++) {
            for (int relY = 1; relY <= 3; relY++) {
                this.place(level, chunkBB, lx, relY, TOWER_MIN, AIR);
                this.place(level, chunkBB, lx, relY, TOWER_MAX, AIR);
            }
        }
        for (int lz = CENTER - 1; lz <= CENTER + 1; lz++) {
            for (int relY = 1; relY <= 3; relY++) {
                this.place(level, chunkBB, TOWER_MIN, relY, lz, AIR);
                this.place(level, chunkBB, TOWER_MAX, relY, lz, AIR);
            }
        }
        // 内部を空洞化
        for (int lx = TOWER_MIN + 1; lx <= TOWER_MAX - 1; lx++) {
            for (int lz = TOWER_MIN + 1; lz <= TOWER_MAX - 1; lz++) {
                for (int relY = 1; relY <= TOWER_H - 1; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, AIR);
                }
            }
        }
        // 祭壇（3x3 の台座 + 中央に region_boss_altar）
        for (int lx = CENTER - 1; lx <= CENTER + 1; lx++) {
            for (int lz = CENTER - 1; lz <= CENTER + 1; lz++) {
                this.place(level, chunkBB, lx, 1, lz, TOWER_EDGE);
            }
        }
        BlockState altar = NocterraFeatureSupport.resolve("region_boss_altar", PILLAR);
        if (altar.equals(PILLAR) && WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[NOCTERRA] ブロック 'nocterra:region_boss_altar' が未登録のため、"
                    + "エンド城塞の祭壇は仮ブロックで生成されます。");
        }
        this.place(level, chunkBB, CENTER, 2, CENTER, altar);
    }

    private void buildPlaza(WorldGenLevel level, BoundingBox chunkBB) {
        // 広場の柱（プルプァ）
        for (int[] c : new int[][]{{4, 4}, {13, 4}, {4, 13}, {13, 13}}) {
            for (int relY = 1; relY <= 4; relY++) {
                this.place(level, chunkBB, c[0], relY, c[1], PILLAR);
            }
            this.place(level, chunkBB, c[0], 5, c[1], ROD);
        }
        // 床の明かり
        this.place(level, chunkBB, 3, 0, 9, GLOW);
        this.place(level, chunkBB, 14, 0, 9, GLOW);
        this.place(level, chunkBB, 9, 0, 3, GLOW);
        this.place(level, chunkBB, 9, 0, 14, GLOW);
    }
}

package com.nocterra.world.structure;

import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.slf4j.Logger;

/**
 * 「黒冠城」— 40x40 の最終ダンジョン。外周壁＋大門（5x7）＋門塔＋四隅の塔＋中庭＋本丸（玉座の間）。
 *
 * <p>2x2 チャンクを超えるため、必ず {@link NocterraStructurePiece} の絶対座標ヘルパー
 * （{@code place}/{@code getAt}）と {@code chunkBB} クリップを使う。各チャンクが
 * 自分の分だけを置くので、隣チャンクが FULL でも欠けない。</p>
 *
 * <p>乱数は使わない（チャンクごとにシードが違うため）。模様は座標ハッシュで決める。</p>
 */
public class BlackCrownCastlePiece extends NocterraStructurePiece {
    public static final StructurePieceType TYPE = (context, tag) -> new BlackCrownCastlePiece(tag);

    public static final int SIZE = BlackCrownCastleStructure.SIZE; // 40
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int FOUNDATION_DEPTH = 24;
    private static final int CLEAR_HEIGHT = 36;

    private static final int WALL_MIN = 1;
    private static final int WALL_MAX = SIZE - 2; // 38
    private static final int WALL_H = 9;
    private static final int TOWER_H = 18;
    private static final int GATE_TOWER_H = 24;

    /** 大門の開口（北側 z=1）。 */
    private static final int GATE_MIN_LX = 18;
    private static final int GATE_MAX_LX = 22;
    private static final int GATE_H = 7; // 5 幅 x 7 高

    /** 門塔: 5x5 を 2 本。 */
    private static final int[] GATE_TOWER_X = {12, 24};

    /** 本丸（玉座の間）。 */
    private static final int KEEP_X0 = 10;
    private static final int KEEP_X1 = 29;
    private static final int KEEP_Z0 = 18;
    private static final int KEEP_Z1 = 36;
    private static final int KEEP_FLOOR = 4;      // 本丸の床ブロックの relY（中庭より 4 高い）
    private static final int KEEP_WALL_TOP = 14;  // 壁の最上段
    private static final int KEEP_CEILING = 15;   // 天井
    private static final int THRONE_LX = 19;
    private static final int THRONE_LZ = 34;

    private static final Identifier THRONE_ID = Identifier.fromNamespaceAndPath("nocterra", "black_crown_throne");
    private static final AtomicBoolean THRONE_WARNED = new AtomicBoolean(false);

    // 建材（黒冠城 = 黒石系 + 深層岩）
    private static final BlockState FOUNDATION = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState FLOOR_A = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState PILLAR = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState GILDED = Blocks.GILDED_BLACKSTONE.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SHROOMLIGHT.defaultBlockState();
    private static final BlockState GLOW = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

    public BlackCrownCastlePiece(int originX, int originZ, int floorY) {
        super(TYPE,
                new BoundingBox(originX, floorY - 30, originZ, originX + SIZE - 1, floorY + 38, originZ + SIZE - 1),
                floorY);
    }

    public BlackCrownCastlePiece(CompoundTag tag) {
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
        // このチャンクが担当するローカル範囲だけを回す（40x40x36 の全走査を避ける）
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
        buildOuterWall(level, chunkBB);
        buildCornerTowers(level, chunkBB);
        buildGateTowers(level, chunkBB);
        buildGreatGate(level, chunkBB);
        buildKeep(level, chunkBB, lx0, lx1, lz0, lz1);
        buildStairsToKeep(level, chunkBB);
        buildThroneRoom(level, chunkBB);
        buildLights(level, chunkBB);
    }

    // ------------------------------------------------------------------ 地形

    private void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = lx0; lx <= lx1; lx++) {
            for (int lz = lz0; lz <= lz1; lz++) {
                for (int relY = -1; relY >= -FOUNDATION_DEPTH; relY--) {
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
                for (int relY = 1; relY <= CLEAR_HEIGHT; relY++) {
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
                if (lx < WALL_MIN || lx > WALL_MAX || lz < WALL_MIN || lz > WALL_MAX) {
                    continue;
                }
                this.place(level, chunkBB, lx, 0, lz, ((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B);
            }
        }
    }

    // ------------------------------------------------------------------ 外周・塔・門

    private void buildOuterWall(WorldGenLevel level, BoundingBox chunkBB) {
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

    private void buildCornerTowers(WorldGenLevel level, BoundingBox chunkBB) {
        int[] cornerX = {WALL_MIN, WALL_MAX - 5}; // 1..6 / 33..38
        int[] cornerZ = {WALL_MIN, WALL_MAX - 5};
        for (int cx : cornerX) {
            for (int cz : cornerZ) {
                for (int dx = 0; dx < 6; dx++) {
                    for (int dz = 0; dz < 6; dz++) {
                        int lx = cx + dx;
                        int lz = cz + dz;
                        for (int relY = 1; relY <= TOWER_H; relY++) {
                            this.place(level, chunkBB, lx, relY, lz, towerState(dx, relY, dz, 6));
                        }
                        boolean rim = dx == 0 || dx == 5 || dz == 0 || dz == 5;
                        if (rim && ((dx + dz) & 1) == 0) {
                            this.place(level, chunkBB, lx, TOWER_H + 1, lz, PILLAR);
                        }
                    }
                }
            }
        }
    }

    private void buildGateTowers(WorldGenLevel level, BoundingBox chunkBB) {
        for (int gx : GATE_TOWER_X) {
            for (int dx = 0; dx < 5; dx++) {
                for (int dz = 0; dz < 5; dz++) {
                    int lx = gx + dx;
                    int lz = WALL_MIN + dz;
                    for (int relY = 1; relY <= GATE_TOWER_H; relY++) {
                        this.place(level, chunkBB, lx, relY, lz, towerState(dx, relY, dz, 5));
                    }
                    boolean rim = dx == 0 || dx == 4 || dz == 0 || dz == 4;
                    if (rim && ((dx + dz) & 1) == 0) {
                        this.place(level, chunkBB, lx, GATE_TOWER_H + 1, lz, PILLAR);
                    }
                }
            }
        }
    }

    private void buildGreatGate(WorldGenLevel level, BoundingBox chunkBB) {
        int gateZ = WALL_MIN;
        // 5 幅 x 7 高の開口
        for (int lx = GATE_MIN_LX; lx <= GATE_MAX_LX; lx++) {
            for (int relY = 1; relY <= GATE_H; relY++) {
                this.place(level, chunkBB, lx, relY, gateZ, AIR);
            }
        }
        // 両脇の門柱（gilded blackstone の縁取り）
        for (int lx : new int[]{GATE_MIN_LX - 1, GATE_MAX_LX + 1}) {
            for (int relY = 1; relY <= GATE_H + 1; relY++) {
                this.place(level, chunkBB, lx, relY, gateZ, relY % 3 == 0 ? GILDED : WALL_CHISELED);
            }
        }
        // まぐさ + その上の飾り
        for (int lx = GATE_MIN_LX - 1; lx <= GATE_MAX_LX + 1; lx++) {
            this.place(level, chunkBB, lx, GATE_H + 1, gateZ, GILDED);
            this.place(level, chunkBB, lx, GATE_H + 2, gateZ, WALL_CHISELED);
        }
        // 入口前のアプローチ（バウンディングボックス端の 1 ブロック）
        for (int lx = GATE_MIN_LX - 1; lx <= GATE_MAX_LX + 1; lx++) {
            this.place(level, chunkBB, lx, 0, 0, FLOOR_A);
            for (int relY = 1; relY <= GATE_H + 1; relY++) {
                this.place(level, chunkBB, lx, relY, 0, AIR);
            }
            for (int relY = -1; relY >= -10; relY--) {
                if (this.getAt(level, chunkBB, lx, relY, 0).isSolid()) {
                    break;
                }
                this.place(level, chunkBB, lx, relY, 0, FOUNDATION);
            }
        }
    }

    // ------------------------------------------------------------------ 本丸

    private void buildKeep(WorldGenLevel level, BoundingBox chunkBB, int lx0, int lx1, int lz0, int lz1) {
        for (int lx = Math.max(KEEP_X0, lx0); lx <= Math.min(KEEP_X1, lx1); lx++) {
            for (int lz = Math.max(KEEP_Z0, lz0); lz <= Math.min(KEEP_Z1, lz1); lz++) {
                // 基壇（中庭から 4 段高い）
                for (int relY = 1; relY < KEEP_FLOOR; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, lx % 4 == 0 ? PILLAR : FOUNDATION);
                }
                // 床
                this.place(level, chunkBB, lx, KEEP_FLOOR, lz,
                        ((lx + lz) & 1) == 0 ? FLOOR_A : FLOOR_B);
                // 壁（外周リング）
                boolean edge = lx == KEEP_X0 || lx == KEEP_X1 || lz == KEEP_Z0 || lz == KEEP_Z1;
                if (edge) {
                    for (int relY = KEEP_FLOOR + 1; relY <= KEEP_WALL_TOP; relY++) {
                        this.place(level, chunkBB, lx, relY, lz, wallState(lx, relY, lz));
                    }
                } else {
                    // 内部を空洞化
                    for (int relY = KEEP_FLOOR + 1; relY <= KEEP_WALL_TOP; relY++) {
                        this.place(level, chunkBB, lx, relY, lz, AIR);
                    }
                }
                // 天井
                this.place(level, chunkBB, lx, KEEP_CEILING, lz, WALL);
            }
        }

        // 本丸の四隅の柱（外殻の角）
        for (int[] c : new int[][]{{KEEP_X0, KEEP_Z0}, {KEEP_X1, KEEP_Z0}, {KEEP_X0, KEEP_Z1}, {KEEP_X1, KEEP_Z1}}) {
            for (int relY = KEEP_FLOOR + 1; relY <= KEEP_WALL_TOP; relY++) {
                this.place(level, chunkBB, c[0], relY, c[1], PILLAR);
            }
        }

        // 本丸の屋上の胸壁（廃墟感のアクセント）
        for (int lx = KEEP_X0; lx <= KEEP_X1; lx++) {
            for (int lz = KEEP_Z0; lz <= KEEP_Z1; lz++) {
                boolean edge = lx == KEEP_X0 || lx == KEEP_X1 || lz == KEEP_Z0 || lz == KEEP_Z1;
                if (edge && ((lx + lz) & 1) == 0) {
                    this.place(level, chunkBB, lx, KEEP_CEILING + 1, lz, PILLAR);
                }
            }
        }

        // 北面の入口（玉座の間へつながる 3 幅 x 4 高）
        for (int lx = 19; lx <= 21; lx++) {
            for (int relY = KEEP_FLOOR + 1; relY <= KEEP_FLOOR + 4; relY++) {
                this.place(level, chunkBB, lx, relY, KEEP_Z0, AIR);
            }
        }
    }

    private void buildStairsToKeep(WorldGenLevel level, BoundingBox chunkBB) {
        // 中庭から本丸の床（relY 4）へ上がる 4 段。z が大きいほど高い。
        for (int i = 0; i < 4; i++) {
            int lz = 17 - i;
            int topRel = 4 - i;
            for (int lx = 19; lx <= 21; lx++) {
                for (int relY = 1; relY <= topRel; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, PILLAR);
                }
                for (int relY = topRel + 1; relY <= 8; relY++) {
                    this.place(level, chunkBB, lx, relY, lz, AIR);
                }
            }
        }
        // 階段の袖壁
        for (int lz = 14; lz <= 17; lz++) {
            for (int relY = 1; relY <= 5; relY++) {
                this.place(level, chunkBB, 18, relY, lz, WALL);
                this.place(level, chunkBB, 22, relY, lz, WALL);
            }
        }
    }

    // ------------------------------------------------------------------ 玉座の間

    private void buildThroneRoom(WorldGenLevel level, BoundingBox chunkBB) {
        // 玉座の台座（1 段）
        for (int lx = 17; lx <= 21; lx++) {
            for (int lz = 32; lz <= 35; lz++) {
                this.place(level, chunkBB, lx, KEEP_FLOOR + 1, lz, WALL_CHISELED);
            }
        }
        // 玉座（登録前は chiseled で仮置き）
        BlockState throne = resolveThroneState();
        if (throne != null) {
            this.place(level, chunkBB, THRONE_LX, KEEP_FLOOR + 2, THRONE_LZ, throne);
        } else {
            if (THRONE_WARNED.compareAndSet(false, true)) {
                LOGGER.warn("[NOCTERRA] ブロック '{}' が未登録のため、黒冠城の玉座は仮ブロックで生成されます。"
                        + " NocterraBlocks へ登録後に生成される城には玉座が入ります。", THRONE_ID);
            }
            this.place(level, chunkBB, THRONE_LX, KEEP_FLOOR + 2, THRONE_LZ, WALL_CHISELED);
        }

        // 玉座の間の柱（左右 2 列）
        for (int lz : new int[]{21, 25, 29, 33}) {
            for (int relY = KEEP_FLOOR + 1; relY <= KEEP_WALL_TOP; relY++) {
                this.place(level, chunkBB, 13, relY, lz, PILLAR);
                this.place(level, chunkBB, 26, relY, lz, PILLAR);
            }
        }
        // 玉座背後の飾り壁
        for (int lx = 17; lx <= 21; lx++) {
            for (int relY = KEEP_FLOOR + 2; relY <= KEEP_FLOOR + 6; relY++) {
                this.place(level, chunkBB, lx, relY, KEEP_Z1 - 1, relY == KEEP_FLOOR + 4 ? GILDED : WALL_CHISELED);
            }
        }
    }

    private void buildLights(WorldGenLevel level, BoundingBox chunkBB) {
        // 玉座の間の天井照明（天井ブロックを置換）
        for (int lx : new int[]{14, 19, 25}) {
            for (int lz : new int[]{22, 26, 30, 34}) {
                this.place(level, chunkBB, lx, KEEP_CEILING, lz, LIGHT);
            }
        }
        // 中庭の誘導灯（床に埋め込む）
        for (int lx : new int[]{GATE_MIN_LX, GATE_MAX_LX}) {
            for (int lz : new int[]{6, 11}) {
                this.place(level, chunkBB, lx, 0, lz, GLOW);
            }
        }
        for (int[] c : new int[][]{{5, 5}, {34, 5}, {5, 34}, {34, 34}}) {
            this.place(level, chunkBB, c[0], 0, c[1], GLOW);
        }
        // 玉座を照らす
        this.place(level, chunkBB, THRONE_LX, KEEP_WALL_TOP, THRONE_LZ, LIGHT);
        this.place(level, chunkBB, THRONE_LX - 2, KEEP_WALL_TOP, THRONE_LZ, LIGHT);
        this.place(level, chunkBB, THRONE_LX + 2, KEEP_WALL_TOP, THRONE_LZ, LIGHT);
    }

    // ------------------------------------------------------------------ 建材

    private static BlockState resolveThroneState() {
        Block block = BuiltInRegistries.BLOCK.getOptional(THRONE_ID).filter(b -> b != Blocks.AIR).orElse(null);
        return block == null ? null : block.defaultBlockState();
    }

    private static BlockState wallState(int lx, int relY, int lz) {
        int h = Math.floorMod(hash(lx, relY, lz), 100);
        if (h < 8) {
            return WALL_CRACKED;
        }
        if (h < 16) {
            return WALL_CHISELED;
        }
        return WALL;
    }

    private static BlockState towerState(int dx, int relY, int dz, int size) {
        if (relY % 5 == 0) {
            return PILLAR;
        }
        if (dx == 0 || dx == size - 1 || dz == 0 || dz == size - 1) {
            return WALL_CHISELED;
        }
        return wallState(dx * 7 + 1, relY, dz * 7 + 3);
    }
}

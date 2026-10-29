package com.nocterra.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * NOCTERRA の構造物ピース共通基底。
 *
 * <p><b>座標系について（重要）</b>: {@link StructurePiece#placeBlock} は「このピースの
 * バウンディングボックス最小角からのローカル座標」を受け取りますが、その変換
 * ({@code getWorldX/Y/Z}) は <b>orientation が null のときローカル座標をそのまま返します</b>
 * （{@code StructurePiece#getWorldX} 参照）。orientation を設定し忘れると構造物が
 * ワールド原点付近の生ローカル座標に建ってしまうため、この基底では orientation に依存せず
 * <b>ワールド絶対座標を自前で計算</b>して {@code chunkBB} でクリップします。</p>
 *
 * <p>{@code chunkBB} は「いま生成中のチャンクの書き込み可能範囲」なので、
 * 各チャンクが自分の分だけを置くことになり、隣チャンクが既に FULL でも欠けません。</p>
 */
public abstract class NocterraStructurePiece extends StructurePiece {
    /** 構造物の基準ワールド X（バウンディングボックス最小 X）。 */
    protected final int originX;
    /** 構造物の基準ワールド Z（バウンディングボックス最小 Z）。 */
    protected final int originZ;
    /** 構造物の基準 Y（城塞の床ブロックの Y / 地下墓所の地表ブロックの Y）。 */
    protected final int baseY;

    protected NocterraStructurePiece(StructurePieceType type, BoundingBox box, int baseY) {
        super(type, 0, box);
        this.originX = box.minX();
        this.originZ = box.minZ();
        this.baseY = baseY;
        // 念のため（自前の絶対座標ヘルパーは orientation を使わない）
        this.setOrientation(Direction.SOUTH);
    }

    protected NocterraStructurePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.originX = this.boundingBox.minX();
        this.originZ = this.boundingBox.minZ();
        this.baseY = tag.getIntOr("BaseY", tag.getIntOr("baseY", 0));
        this.setOrientation(Direction.SOUTH);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("BaseY", this.baseY);
    }


    /**
     * 指定ローカル座標に宝箱を置く（空気の場所のみ）。チャンク外・埋まった場所は何もしない。
     * 所有: Lead。
     */
    protected void placeLootChest(WorldGenLevel level, BoundingBox chunkBB,
                                  int localX, int relY, int localZ, String lootId) {
        int wx = this.originX + localX;
        int wy = this.baseY + relY;
        int wz = this.originZ + localZ;
        if (wx < chunkBB.minX() || wx > chunkBB.maxX()
                || wy < chunkBB.minY() || wy > chunkBB.maxY()
                || wz < chunkBB.minZ() || wz > chunkBB.maxZ()) {
            return;
        }
        BlockPos pos = new BlockPos(wx, wy, wz);
        BlockState here = level.getBlockState(pos);
        // 構造物の内部は CAVE_AIR で埋まっているため、両方を空気として扱う
        if (!here.isAir() && !here.is(Blocks.CAVE_AIR)) {
            return;
        }
        if (!level.getBlockState(pos.below()).isSolid()) {
            return;
        }
        level.setBlock(pos, Blocks.CHEST.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH), 2);
        if (level.getBlockState(pos).is(Blocks.CHEST)
                && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.setLootTable(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("nocterra", "chests/" + lootId)));
        }
    }

    /**
     * 構造物内の候補地点を走査して宝箱を最大 2 個置く。
     * 壁の中・空中（床なし）は自動でスキップする。
     */
    protected void scatterLootChests(WorldGenLevel level, BoundingBox chunkBB, String lootId) {
        int sizeX = this.boundingBox.maxX() - this.boundingBox.minX() + 1;
        int sizeZ = this.boundingBox.maxZ() - this.boundingBox.minZ() + 1;
        int[] xs = {sizeX / 2, sizeX / 4, (sizeX * 3) / 4, sizeX / 2 + 2, sizeX / 2 - 2};
        int[] zs = {sizeZ / 2, sizeZ / 4, (sizeZ * 3) / 4, sizeZ / 2 + 2, sizeZ / 2 - 2};
        int placed = 0;
        for (int i = 0; i < xs.length && placed < 2; i++) {
            int lx = Math.max(2, Math.min(sizeX - 3, xs[i]));
            int lz = Math.max(2, Math.min(sizeZ - 3, zs[i]));
            // 床の高さは構造物ごとに違うので、上から下へ走査して「空気＋真下が固体」を探す
            for (int relY = 4; relY >= -6 && placed < 2; relY--) {
                this.placeLootChest(level, chunkBB, lx, relY, lz, lootId);
                if (this.chestPlaced(level, lx, relY, lz)) {
                    placed++;
                    break;
                }
            }
        }
    }

    private boolean chestPlaced(WorldGenLevel level, int localX, int relY, int localZ) {
        return level.getBlockState(new BlockPos(this.originX + localX, this.baseY + relY, this.originZ + localZ))
                .is(Blocks.CHEST);
    }

    /** ローカル X/Z・基準 Y からの相対 Y でワールド絶対座標に置く。 */
    protected void place(
            WorldGenLevel level, BoundingBox chunkBB, int localX, int relY, int localZ, BlockState state) {
        BlockPos pos = new BlockPos(this.originX + localX, this.baseY + relY, this.originZ + localZ);
        if (chunkBB.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    /** チャンク外なら AIR を返す読み取り。 */
    protected BlockState getAt(WorldGenLevel level, BoundingBox chunkBB, int localX, int relY, int localZ) {
        BlockPos pos = new BlockPos(this.originX + localX, this.baseY + relY, this.originZ + localZ);
        return chunkBB.isInside(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    protected boolean isAirAt(WorldGenLevel level, BoundingBox chunkBB, int localX, int relY, int localZ) {
        return this.getAt(level, chunkBB, localX, relY, localZ).isAir();
    }

    /** 決定的な座標ハッシュ（乱数を使うとチャンクごとに結果が変わるため）。 */
    protected static int hash(int x, int y, int z) {
        int h = x * 0x9E3779B1 + y * 0x85EBCA77 + z * 0xC2B2AE3D;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return h;
    }
}

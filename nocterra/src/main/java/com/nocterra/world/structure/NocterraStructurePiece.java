package com.nocterra.world.structure;

import net.minecraft.core.BlockPos;
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

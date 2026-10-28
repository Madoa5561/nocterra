package com.nocterra.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * 「黒冠城」— 40x40 の最終ダンジョン（{@code nocterra:black_crown_ruins} にのみ生成）。
 *
 * <p>2x2 チャンクを超えるため Feature ではなく Structure で実装する。
 * {@code StructureStart} を 1 回だけ計算・保存し、各チャンクが
 * {@code placeInChunk} で自分の分だけを置くので生成順に依存しない。</p>
 */
public class BlackCrownCastleStructure extends Structure {
    public static final int SIZE = 40;
    public static final MapCodec<BlackCrownCastleStructure> CODEC =
            simpleCodec(BlackCrownCastleStructure::new);
    public static final StructureType<BlackCrownCastleStructure> TYPE = () -> CODEC;

    public BlackCrownCastleStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        LevelHeightAccessor height = context.heightAccessor();

        // 地面の高さはノイズから決定的に求める（どのチャンクから呼んでも同じ値）
        int highest = height.getMinY() - 1;
        for (int sx = 0; sx <= SIZE; sx += 8) {
            for (int sz = 0; sz <= SIZE; sz += 8) {
                int y = context.chunkGenerator()
                        .getFirstOccupiedHeight(minX + sx, minZ + sz, Heightmap.Types.WORLD_SURFACE_WG, height,
                                context.randomState());
                if (y > highest) {
                    highest = y;
                }
            }
        }

        // バウンディングボックス (floorY-30 .. floorY+38) がワールド内に収まること
        if (highest - 30 <= height.getMinY() || highest + 38 >= height.getMaxY()) {
            return Optional.empty();
        }
        // 水没地は避ける（最終ダンジョンが水没すると台無しになるため）。
        // getFirstOccupiedHeight はノイズ由来なので、湖底のような低地は海面未満になる。
        if (highest < context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }

        final int floorY = highest;
        BlockPos center = new BlockPos(minX + SIZE / 2, floorY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder builder) -> builder.addPiece(new BlackCrownCastlePiece(minX, minZ, floorY))));
    }
}

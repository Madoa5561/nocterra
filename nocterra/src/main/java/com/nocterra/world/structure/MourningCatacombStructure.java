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
 * 「嘆きの湿原の地下墓所」— 地表から階段で降りる地下の墓所。
 *
 * <p>Feature ではなく Structure で実装している（複数チャンクにまたがっても各チャンクが
 * 自分の分だけを置くため、生成順に依存せず欠けない）。</p>
 */
public class MourningCatacombStructure extends Structure {
    public static final int SIZE = 15;
    /** 地表から墓所の床までの深さ。 */
    public static final int DEPTH = 12;
    public static final MapCodec<MourningCatacombStructure> CODEC =
            simpleCodec(MourningCatacombStructure::new);
    public static final StructureType<MourningCatacombStructure> TYPE = () -> CODEC;

    public MourningCatacombStructure(Structure.StructureSettings settings) {
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

        // 地表の高さはノイズから決定的に求める（どのチャンクから計算しても同じ）。
        int surfaceY = context.chunkGenerator()
                .getFirstOccupiedHeight(minX + SIZE / 2, minZ + SIZE / 2, Heightmap.Types.WORLD_SURFACE_WG, height,
                        context.randomState());

        // 水没地（地表が海面より低い）でも生成できるようにする:
        // 入口の高さを水面より 1 上へ持ち上げ、階段坑は側壁で密閉するので中は乾く。
        // （以前は「海面以下は生成拒否」にしていたため湿原で失敗が多かった）
        int seaLevel = context.chunkGenerator().getSeaLevel();
        if (surfaceY < seaLevel) {
            surfaceY = seaLevel + 1;
        }

        if (surfaceY - 16 <= height.getMinY() + 1 || surfaceY + 6 >= height.getMaxY()) {
            return Optional.empty();
        }

        final int startY = surfaceY;
        BlockPos center = new BlockPos(minX + SIZE / 2, startY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder builder) -> builder.addPiece(new MourningCatacombPiece(minX, minZ, startY))));
    }
}

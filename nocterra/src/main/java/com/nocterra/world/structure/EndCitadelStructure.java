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
 * 「エンドの城塞」— {@code minecraft:the_end} の浮島に建つ 18x18 の城塞。
 *
 * <p>浮遊島から落ちないよう、基礎を下へ 12 ブロック伸ばす。
 * 虚無（島が無い場所）には生成しない。</p>
 */
public class EndCitadelStructure extends Structure {
    public static final int SIZE = 18;
    public static final MapCodec<EndCitadelStructure> CODEC = simpleCodec(EndCitadelStructure::new);
    public static final StructureType<EndCitadelStructure> TYPE = () -> CODEC;

    public EndCitadelStructure(Structure.StructureSettings settings) {
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

        int highest = height.getMinY() - 1;
        for (int sx = 0; sx <= SIZE; sx += 6) {
            for (int sz = 0; sz <= SIZE; sz += 6) {
                int y = context.chunkGenerator()
                        .getFirstOccupiedHeight(minX + sx, minZ + sz, Heightmap.Types.WORLD_SURFACE_WG, height,
                                context.randomState());
                if (y > highest) {
                    highest = y;
                }
            }
        }

        // 虚無（浮島の外）には置かない
        if (highest <= height.getMinY() + 20) {
            return Optional.empty();
        }
        final int baseY = highest;
        if (baseY - 16 <= height.getMinY() || baseY + 24 >= height.getMaxY()) {
            return Optional.empty();
        }

        BlockPos center = new BlockPos(minX + SIZE / 2, baseY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder builder) -> builder.addPiece(new EndCitadelPiece(minX, minZ, baseY))));
    }
}

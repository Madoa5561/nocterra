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
 * 「灰冠の大城塞」— 40x40・地上4階建ての大規模ダンジョン。所有: Lead。
 *
 * <p>稀にしか生成されない（structure_set の spacing を広く取る）。内部には各階に宝箱と
 * モブスポナーがあり、奥へ進むほど強い敵が出る。</p>
 */
public class GrandCastleStructure extends Structure {
    public static final int SIZE = 40;
    public static final MapCodec<GrandCastleStructure> CODEC = simpleCodec(GrandCastleStructure::new);
    public static final StructureType<GrandCastleStructure> TYPE = () -> CODEC;

    public GrandCastleStructure(Structure.StructureSettings settings) {
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

        // バウンディングボックス (floorY-30 .. floorY+44) がワールド外に出ないこと
        if (highest - 30 <= height.getMinY() || highest + 44 >= height.getMaxY()) {
            return Optional.empty();
        }

        final int floorY = highest;
        BlockPos center = new BlockPos(minX + SIZE / 2, floorY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(
                center, (StructurePiecesBuilder builder) ->
                builder.addPiece(new GrandCastlePiece(minX, minZ, floorY))));
    }
}

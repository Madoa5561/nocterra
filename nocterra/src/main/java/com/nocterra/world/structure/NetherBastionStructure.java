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
 * 「ネザーの要塞」— {@code minecraft:the_nether} に生成される 24x24 のネザーレンガ要塞。
 *
 * <p>溶岩海（y=31 付近）に沈まないよう、床の基準 Y は
 * {@code max(地形の最高点, 33)} にする。溶岩堀は外壁の外側 2 ブロック幅。</p>
 */
public class NetherBastionStructure extends Structure {
    public static final int SIZE = 24;
    /** ネザーの溶岩海より確実に上に置くための最低 Y。 */
    private static final int MIN_BASE_Y = 33;

    public static final MapCodec<NetherBastionStructure> CODEC = simpleCodec(NetherBastionStructure::new);
    public static final StructureType<NetherBastionStructure> TYPE = () -> CODEC;

    public NetherBastionStructure(Structure.StructureSettings settings) {
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

        final int baseY = Math.max(highest, MIN_BASE_Y);
        if (baseY - 20 <= height.getMinY() || baseY + 30 >= height.getMaxY()) {
            return Optional.empty();
        }

        BlockPos center = new BlockPos(minX + SIZE / 2, baseY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder builder) -> builder.addPiece(new NetherBastionPiece(minX, minZ, baseY))));
    }
}

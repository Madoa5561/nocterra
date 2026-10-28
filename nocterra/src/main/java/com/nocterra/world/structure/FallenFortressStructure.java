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
 * 「亡国の城塞」— 24x24 の城壁・四隅の塔・3x4 の門・中庭・地下牢を持つ要塞跡。
 *
 * <p><b>Feature ではなく Structure として実装している理由</b>: 24x24 は 1 チャンクに収まらず、
 * Feature が隣チャンクへ {@code setBlock} すると、そのチャンクが既に FULL の場合
 * {@code ImposterProtoChunk(allowWrites=false)} により<b>書き込みが黙って捨てられる</b>。
 * Structure は {@code StructureStart} を 1 回だけ計算して保存し、
 * 各チャンクが {@link net.minecraft.world.level.levelgen.structure.StructurePiece#postProcess}
 * で自分のチャンク分だけを置くため、生成順に依存せず絶対に欠けない。</p>
 */
public class FallenFortressStructure extends Structure {
    public static final int SIZE = 24;
    public static final MapCodec<FallenFortressStructure> CODEC = simpleCodec(FallenFortressStructure::new);
    public static final StructureType<FallenFortressStructure> TYPE = () -> CODEC;

    public FallenFortressStructure(Structure.StructureSettings settings) {
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

        // 地面の高さは「生成済みチャンクの状態」ではなくノイズから決定的に求める
        // （getFirstOccupiedHeight は baseHeight を計算するだけなので、いつ・どのチャンクから
        //  呼んでも同じ値になる。→ 全チャンクで同じ構造物が復元できる）。
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

        // バウンディングボックス (floorY-26 .. floorY+24) がワールドの外に出ないことを保証する
        if (highest - 26 <= height.getMinY() || highest + 24 >= height.getMaxY()) {
            return Optional.empty();
        }

        final int floorY = highest;
        BlockPos center = new BlockPos(minX + SIZE / 2, floorY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(
                center, (StructurePiecesBuilder builder) -> builder.addPiece(new FallenFortressPiece(minX, minZ, floorY))));
    }
}

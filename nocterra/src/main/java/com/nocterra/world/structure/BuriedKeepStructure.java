package com.nocterra.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * 「埋もれた要塞」— 地下（y = -40..-20）に埋まった 14x14 の小要塞。
 *
 * <p>地表には出ない。深さはチャンク座標から決定的に選ぶ。地下構造物なので
 * {@code step: underground_structures} / {@code terrain_adaptation: beard_thin}。</p>
 */
public class BuriedKeepStructure extends Structure {
    public static final int SIZE = 14;
    public static final MapCodec<BuriedKeepStructure> CODEC = simpleCodec(BuriedKeepStructure::new);
    public static final StructureType<BuriedKeepStructure> TYPE = () -> CODEC;

    public BuriedKeepStructure(Structure.StructureSettings settings) {
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

        // 深さ y = -40..-20 を決定的に選ぶ（乱数は使わない）
        int chunkX = minX >> 4;
        int chunkZ = minZ >> 4;
        int h = (chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77);
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        int baseY = -40 + Math.floorMod(h, 21);

        // バウンディングボックス (baseY-14 .. baseY+10) がワールド内に収まること
        if (baseY - 14 <= height.getMinY() || baseY + 10 >= height.getMaxY()) {
            return Optional.empty();
        }

        BlockPos center = new BlockPos(minX + SIZE / 2, baseY, minZ + SIZE / 2);
        return Optional.of(new Structure.GenerationStub(center,
                (StructurePiecesBuilder builder) -> builder.addPiece(new BuriedKeepPiece(minX, minZ, baseY))));
    }
}

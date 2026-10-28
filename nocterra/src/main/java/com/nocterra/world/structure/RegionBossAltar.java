package com.nocterra.world.structure;

import com.nocterra.progression.RegionBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 地域の祭壇。所有: Lead（worldgen 担当は設置するだけ）。
 *
 * <p>右クリックすると、その場のバイオームに対応する冠の者（{@link RegionBosses}）を召喚する。
 * 8 地域すべてに置かれる想定で、1 種類のブロックを共用する。</p>
 */
public class RegionBossAltar extends Block {
    public RegionBossAltar(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        RegionBosses.summonAt(serverLevel, pos, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}

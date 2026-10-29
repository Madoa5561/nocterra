package com.nocterra.item;

import com.nocterra.registry.NocterraBlocks;
import com.nocterra.world.portal.AshenPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * エンバーシャード: 灰冠の門の骨組みに使用すると通過面を生成する。
 */
public class EmberShardItem extends Item {
    public EmberShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        if (!clickedState.is(NocterraBlocks.GATE_FRAME.get())) {
            return InteractionResult.PASS;
        }

        Direction.Axis axis = context.getClickedFace().getAxis();
        if (!axis.isHorizontal()) {
            axis = context.getHorizontalDirection().getAxis();
        }

        // 骨組みをクリックするため、隣接する内部マスを起点に形状を探索する
        if (!level.isClientSide()) {
            boolean lit = AshenPortalShape.igniteNearFrame(level, clicked, axis);
            if (!lit) {
                return InteractionResult.PASS;
            }
            level.playSound(null, clicked, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 0.6F);
        }

        return InteractionResult.SUCCESS;
    }
}

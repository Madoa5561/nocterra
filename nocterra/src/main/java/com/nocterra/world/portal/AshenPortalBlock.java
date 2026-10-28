package com.nocterra.world.portal;

import com.nocterra.world.NocterraDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 灰冠の門の通過面。触れたエンティティをノクテラ／オーバーワールドへ送る。
 * 判定・移動はすべてサーバー権威で行う。
 */
public class AshenPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public AshenPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        return !wrongAxis && !AshenPortalShape.isCompletePortal(level, pos, axis)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> targetKey = currentLevel.dimension().equals(NocterraDimensions.NOCTERRA)
                ? Level.OVERWORLD
                : NocterraDimensions.NOCTERRA;

        ServerLevel target = currentLevel.getServer().getLevel(targetKey);
        if (target == null) {
            return null;
        }

        BlockPos landing = AshenPortalShape.findOrCreateLanding(target, portalEntryPos);
        Vec3 position = Vec3.atBottomCenterOf(landing);

        // 到着後はポータルクールダウンを必ず付け、即時の往復ループを防ぐ
        TeleportTransition.PostTeleportTransition post = Entity::setPortalCooldown;

        return new TeleportTransition(
                target,
                position,
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                Set.of(),
                post);
    }
}

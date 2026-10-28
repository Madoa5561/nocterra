package com.nocterra.world.portal;

import com.nocterra.registry.NocterraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 灰冠の門（Ancient Gate）の形状判定と生成。
 *
 * <p>バニラの {@link net.minecraft.world.level.portal.PortalShape} と同じ算法を、
 * 骨組みブロック {@code nocterra:gate_frame} 用に移植したもの。内部 2×3 以上を許容する。</p>
 */
public final class AshenPortalShape {
    public static final int MIN_WIDTH = 2;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_WIDTH = 21;
    public static final int MAX_HEIGHT = 21;

    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int width;
    private final int height;

    private AshenPortalShape(Direction.Axis axis, int numPortalBlocks, Direction rightDir,
                             BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.numPortalBlocks = numPortalBlocks;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    // ---------------------------------------------------------------- queries

    public Direction.Axis axis() {
        return this.axis;
    }

    public BlockPos bottomLeft() {
        return this.bottomLeft;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public Direction rightDir() {
        return this.rightDir;
    }

    public boolean isValid() {
        return this.width >= MIN_WIDTH && this.width <= MAX_WIDTH
                && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static boolean isCompletePortal(LevelReader level, BlockPos pos, Direction.Axis axis) {
        return findAnyShape(level, pos, axis).isComplete();
    }

    public static boolean isFrame(BlockState state) {
        return state.is(NocterraBlocks.GATE_FRAME.get());
    }

    public static boolean isPortal(BlockState state) {
        return state.is(NocterraBlocks.ASHEN_PORTAL.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(Blocks.FIRE) || isPortal(state);
    }

    // -------------------------------------------------------------- searching

    public static Optional<AshenPortalShape> findEmptyPortalShape(LevelReader level, BlockPos pos,
                                                                  Direction.Axis preferredAxis) {
        return findPortalShape(level, pos, shape -> shape.isValid() && shape.numPortalBlocks == 0, preferredAxis);
    }

    public static Optional<AshenPortalShape> findPortalShape(LevelReader level, BlockPos pos,
                                                             Predicate<AshenPortalShape> isValid,
                                                             Direction.Axis preferredAxis) {
        Optional<AshenPortalShape> first = Optional.of(findAnyShape(level, pos, preferredAxis)).filter(isValid);
        if (first.isPresent()) {
            return first;
        }
        Direction.Axis other = preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(findAnyShape(level, pos, other)).filter(isValid);
    }

    public static AshenPortalShape findAnyShape(LevelReader level, BlockPos pos, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = calculateBottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new AshenPortalShape(axis, 0, rightDir, pos, 0, 0);
        }
        int width = calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return new AshenPortalShape(axis, 0, rightDir, bottomLeft, 0, 0);
        }
        int[] portalBlockCount = new int[1];
        int height = calculateHeight(level, bottomLeft, rightDir, width, portalBlockCount);
        return new AshenPortalShape(axis, portalBlockCount[0], rightDir, bottomLeft, width, height);
    }

    private static @Nullable BlockPos calculateBottomLeft(LevelReader level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - MAX_HEIGHT);
        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction leftDir = rightDir.getOpposite();
        int edge = getDistanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        return edge < 0 ? null : pos.relative(leftDir, edge);
    }

    private static int calculateWidth(LevelReader level, BlockPos bottomLeft, Direction rightDir) {
        int width = getDistanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
    }

    private static int getDistanceUntilEdgeAboveFrame(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int width = 0; width <= MAX_WIDTH; width++) {
            cursor.set(pos).move(direction, width);
            BlockState state = level.getBlockState(cursor);
            if (!isEmpty(state)) {
                if (isFrame(state)) {
                    return width;
                }
                break;
            }
            BlockState belowState = level.getBlockState(cursor.move(Direction.DOWN));
            if (!isFrame(belowState)) {
                break;
            }
        }
        return 0;
    }

    private static int calculateHeight(LevelReader level, BlockPos bottomLeft, Direction rightDir, int width,
                                       int[] portalBlockCount) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = getDistanceUntilTop(level, bottomLeft, rightDir, cursor, width, portalBlockCount);
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return 0;
        }
        return hasTopFrame(level, bottomLeft, rightDir, cursor, width, height) ? height : 0;
    }

    private static boolean hasTopFrame(LevelReader level, BlockPos bottomLeft, Direction rightDir,
                                       BlockPos.MutableBlockPos cursor, int width, int height) {
        for (int i = 0; i < width; i++) {
            BlockPos.MutableBlockPos framePos = cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
            if (!isFrame(level.getBlockState(framePos))) {
                return false;
            }
        }
        return true;
    }

    private static int getDistanceUntilTop(LevelReader level, BlockPos bottomLeft, Direction rightDir,
                                           BlockPos.MutableBlockPos cursor, int width, int[] portalBlockCount) {
        for (int height = 0; height < MAX_HEIGHT; height++) {
            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1);
            if (!isFrame(level.getBlockState(cursor))) {
                return height;
            }
            cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, width);
            if (!isFrame(level.getBlockState(cursor))) {
                return height;
            }
            for (int i = 0; i < width; i++) {
                cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
                BlockState state = level.getBlockState(cursor);
                if (!isEmpty(state)) {
                    return height;
                }
                if (isPortal(state)) {
                    portalBlockCount[0]++;
                }
            }
        }
        return MAX_HEIGHT;
    }

    // --------------------------------------------------------------- creation

    public void createPortalBlocks(LevelAccessor level) {
        BlockState portalState = NocterraBlocks.ASHEN_PORTAL.get().defaultBlockState()
                .setValue(AshenPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft,
                        this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> level.setBlock(pos, portalState, 18));
    }

    /**
     * 指定位置を内部左下として、骨組みのみを構築する。
     *
     * @return 内部左下の座標
     */
    public static BlockPos buildGate(LevelAccessor level, BlockPos interiorBottomLeft, Direction.Axis axis,
                                     int width, int height) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState frame = NocterraBlocks.GATE_FRAME.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int i = -1; i <= width; i++) {
            level.setBlock(interiorBottomLeft.relative(right, i).below(), frame, 3);
            level.setBlock(interiorBottomLeft.relative(right, i).above(height), frame, 3);
        }
        for (int h = 0; h < height; h++) {
            level.setBlock(interiorBottomLeft.relative(right, -1).above(h), frame, 3);
            level.setBlock(interiorBottomLeft.relative(right, width).above(h), frame, 3);
            for (int i = 0; i < width; i++) {
                level.setBlock(interiorBottomLeft.relative(right, i).above(h), air, 3);
            }
        }
        return interiorBottomLeft;
    }

    /** 骨組みを建てたうえで通過面も生成する（帰還ポータル用）。 */
    public static BlockPos buildGateAndIgnite(LevelAccessor level, BlockPos interiorBottomLeft, Direction.Axis axis,
                                              int width, int height) {
        BlockPos bottomLeft = buildGate(level, interiorBottomLeft, axis, width, height);
        AshenPortalShape shape = findAnyShape(level, bottomLeft, axis);
        if (shape.isValid()) {
            shape.createPortalBlocks(level);
        }
        return bottomLeft;
    }

    /**
     * 到着地点を決める。既存の門があればその前、無ければ現地に門を建てて通過面も生成する。
     *
     * @return 到着座標（通過面の外側に出すことで即時再転送を避ける）
     */
    public static BlockPos findOrCreateLanding(ServerLevel level, BlockPos entryPos) {
        int x = entryPos.getX();
        int z = entryPos.getZ();
        int minY = level.getMinY() + 8;
        int startY = Math.min(entryPos.getY(), level.getMaxY() - 16);

        for (int probe = 0; probe < 64; probe++) {
            int y = startY - probe;
            if (y <= minY) {
                break;
            }
            BlockPos ground = new BlockPos(x, y, z);
            Optional<AshenPortalShape> existing = findPortalShape(level, ground.above(), s -> s.isComplete(),
                    Direction.Axis.X);
            if (existing.isPresent()) {
                return arrivalSpot(existing.get());
            }
            if (isSolidGround(level, ground)) {
                BlockPos bottom = buildGateAndIgnite(level, ground.above(), Direction.Axis.X, MIN_WIDTH, MIN_HEIGHT);
                return bottom.relative(Direction.SOUTH, 1);
            }
        }

        // 地面が無ければ空中に足場を作る
        BlockPos platform = new BlockPos(x, Math.max(minY + 4, 72), z);
        BlockState stone = NocterraBlocks.ASHSTONE.get().defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(platform.offset(dx, -1, dz), stone, 3);
            }
        }
        BlockPos bottom = buildGateAndIgnite(level, platform, Direction.Axis.X, MIN_WIDTH, MIN_HEIGHT);
        return bottom.relative(Direction.SOUTH, 1);
    }

    private static BlockPos arrivalSpot(AshenPortalShape shape) {
        Direction normal = shape.axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        return shape.bottomLeft.relative(normal, 1);
    }

    private static boolean isSolidGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolid() && state.isSolidRender();
    }
}

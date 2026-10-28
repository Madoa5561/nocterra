package com.nocterra.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nocterra.world.NocterraDimensions;
import com.nocterra.world.portal.AshenPortalShape;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 開発・検証用コマンド。/nocterra ...（権限レベル 2 以上）
 *
 * <p>門の形状判定と通過面生成は実際のアイテム使用と同じ経路を通すため、
 * サーバー単体での動作検証に使える。</p>
 */
public final class NocterraCommands {
    private NocterraCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        NocterraDevCommands.register(dispatcher);
        NocterraStoryCommands.register(dispatcher);

        dispatcher.register(
                Commands.literal("nocterra")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("gate")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(NocterraCommands::buildAndIgniteGate)))
                        .then(Commands.literal("info")
                                .executes(NocterraCommands::info))
                        .then(Commands.literal("probe")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(NocterraCommands::probe)))
                        .then(Commands.literal("entities")
                                .executes(context -> entities(context, ""))
                                .then(Commands.argument("filter", StringArgumentType.word())
                                        .executes(context -> entities(context,
                                                StringArgumentType.getString(context, "filter"))))));
    }

    private static int buildAndIgniteGate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");

        AshenPortalShape.buildGate(level, pos, Direction.Axis.X,
                AshenPortalShape.MIN_WIDTH, AshenPortalShape.MIN_HEIGHT);

        var shape = AshenPortalShape.findPortalShape(level, pos, s -> s.isValid(), Direction.Axis.X);
        if (shape.isEmpty()) {
            source.sendFailure(Component.literal("gate shape not detected at " + pos));
            return 0;
        }
        AshenPortalShape detected = shape.get();
        detected.createPortalBlocks(level);

        StringBuilder readback = new StringBuilder();
        BlockPos bottom = detected.bottomLeft();
        Direction right = detected.rightDir();
        for (int h = 0; h < detected.height(); h++) {
            for (int i = 0; i < detected.width(); i++) {
                BlockPos p = bottom.relative(right, i).relative(Direction.UP, h);
                BlockState st = level.getBlockState(p);
                readback.append(' ').append(p.toShortString()).append('=')
                        .append(st.getBlock().getName().getString());
            }
        }

        source.sendSuccess(() -> Component.literal(
                "gate built at " + pos + " (axis=" + detected.axis()
                        + ", w=" + detected.width() + ", h=" + detected.height()
                        + ", blocks:" + readback + ")"), true);
        return 1;
    }

    private static int probe(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        BlockState state = level.getBlockState(pos);
        source.sendSuccess(() -> Component.literal(
                "probe[" + level.dimension().identifier() + "] " + pos.toShortString() + " = "
                        + state.getBlock().getName().getString() + " (" + state + ")"), false);
        return 1;
    }

    /** 実行ディメンション内のエンティティを列挙。filter は type の部分一致。 */
    private static int entities(CommandContext<CommandSourceStack> context, String filter) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        List<Entity> matched = new ArrayList<>();
        int total = 0;
        for (Entity entity : level.getAllEntities()) {
            total++;
            if (filter.isEmpty() || entity.getType().toShortString().contains(filter)) {
                matched.add(entity);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matched.size() && i < 24; i++) {
            Entity entity = matched.get(i);
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(entity.getType().toShortString()).append('@')
                    .append(String.format("%.1f,%.1f,%.1f", entity.getX(), entity.getY(), entity.getZ()));
        }
        String filterLabel = filter.isEmpty() ? "*" : filter;
        final int matchedCount = matched.size();
        final int totalCount = total;
        source.sendSuccess(() -> Component.literal(
                "entities[" + level.dimension().identifier() + "] total=" + totalCount
                        + " match(" + filterLabel + ")=" + matchedCount + ": " + sb), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        String dimensionId = level.dimension().identifier().toString();
        boolean isNocterra = level.dimension().equals(NocterraDimensions.NOCTERRA);
        source.sendSuccess(() -> Component.literal(
                "NOCTERRA dev info: dimension=" + dimensionId
                        + " minY=" + level.getMinY()
                        + " isNocterra=" + isNocterra), false);
        return 1;
    }
}

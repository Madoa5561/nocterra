package com.nocterra.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nocterra.Nocterra;
import com.nocterra.progression.EndingHandler;
import com.nocterra.progression.ProgressionAttachments;
import com.nocterra.progression.ProgressionData;
import com.nocterra.progression.ProgressionEvents;
import com.nocterra.progression.SigilType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

/**
 * 物語（刻印・エンディング・手帳）の検証用コマンド。所有: Lead。
 *
 * <p>既存の {@link NocterraDevCommands} とは独立した dispatcher 登録にしてある
 * （入れ子の .then 連鎖を壊さないため）。</p>
 */
public final class NocterraStoryCommands {
    private NocterraStoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nocterra")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("story")
                        .then(Commands.literal("sigils").executes(NocterraStoryCommands::grantAllSigils))
                        .then(Commands.literal("clear").executes(NocterraStoryCommands::clearSigils))
                        .then(Commands.literal("status").executes(NocterraStoryCommands::status))
                        .then(Commands.literal("journal").executes(NocterraStoryCommands::useJournal))
                        .then(Commands.literal("altar").executes(NocterraStoryCommands::testAltar))
                        .then(Commands.literal("ignite")
                                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                        .executes(ctx -> ignite(ctx,
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "y"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z")))))))
                        .then(Commands.literal("items")
                                .executes(NocterraStoryCommands::listItems)
                                .then(Commands.argument("filter", StringArgumentType.word())
                                        .executes(ctx -> listItems(ctx, StringArgumentType.getString(ctx, "filter")))))
                        .then(Commands.literal("ending")
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .executes(ctx -> ending(ctx, StringArgumentType.getString(ctx, "kind")))))));
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var level = source.getLevel();
        var pos = source.getPosition();
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return null;
        }
        return players.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(pos)))
                .orElse(null);
    }

    private static int grantAllSigils(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        ProgressionEvents.grantAll(player);
        ProgressionData data = ProgressionAttachments.get(player);
        source.sendSuccess(() -> Component.literal("granted sigils: " + data.sigilCount()
                + "/" + SigilType.values().length), false);
        return 1;
    }

    private static int clearSigils(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        player.setData(ProgressionAttachments.progressionType(), new ProgressionData());
        source.sendSuccess(() -> Component.literal("progression cleared"), false);
        return 1;
    }

    /** 探索手帳のアイテム処理をサーバー側から実行して検証する。 */
    private static int useJournal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
                com.nocterra.registry.NocterraStoryItems.EXPLORER_JOURNAL.get());
        net.minecraft.world.InteractionResult result = stack.use(
                player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        source.sendSuccess(() -> Component.literal("journal used: " + result), false);
        return 1;
    }

    /** 現在地のバイオームで地域祭壇の召喚ロジックを実行（検証用）。 */
    private static int testAltar(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            source.sendFailure(Component.literal("not a server level"));
            return 0;
        }
        String biome = com.nocterra.progression.RegionBosses.biomeAt(serverLevel, player.blockPosition());
        String boss = com.nocterra.progression.RegionBosses.bossAt(serverLevel, player.blockPosition());
        boolean ok = com.nocterra.progression.RegionBosses.summonAt(
                serverLevel, player.blockPosition(), player);
        source.sendSuccess(() -> Component.literal("altar test: biome=" + biome + " boss=" + boss + " summoned=" + ok), false);
        return ok ? 1 : 0;
    }

    /** 骨組みブロックの点火をサーバー側から検証する（EmberShardItem と同じ経路）。 */
    private static int ignite(CommandContext<CommandSourceStack> context, int x, int y, int z) {
        CommandSourceStack source = context.getSource();
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        var level = source.getLevel();
        boolean lit = com.nocterra.world.portal.AshenPortalShape.igniteNearFrame(
                level, pos, net.minecraft.core.Direction.Axis.X);
        source.sendSuccess(() -> Component.literal("ignite at " + pos + " -> " + lit), false);
        return lit ? 1 : 0;
    }

    /** 登録済みの NOCTERRA アイテムを一覧する（検証用）。 */
    private static int listItems(CommandContext<CommandSourceStack> context) {
        return listItems(context, "");
    }

    private static int listItems(CommandContext<CommandSourceStack> context, String filter) {
        CommandSourceStack source = context.getSource();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet()) {
            net.minecraft.resources.Identifier id = entry.getKey().identifier();
            if (!Nocterra.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            if (!filter.isEmpty() && !id.getPath().contains(filter)) {
                continue;
            }
            names.add(id.getPath());
        }
        java.util.Collections.sort(names);
        source.sendSuccess(() -> Component.literal("nocterra items (" + names.size() + "): " + String.join(", ", names)), false);
        return names.size();
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        ProgressionData data = ProgressionAttachments.get(player);
        source.sendSuccess(() -> Component.literal("sigils=" + data.sigilCount()
                + "/" + SigilType.values().length
                + " bosses=" + data.defeatedBosses().size()
                + " npcs=" + data.discoveredNpcs().size()
                + " quests=" + data.completedQuests().size()
                + " normalEnding=" + data.normalEndingSeen()
                + " trueEnding=" + data.trueEndingSeen()), false);
        return 1;
    }

    private static int ending(CommandContext<CommandSourceStack> context, String kind) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = player(context);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        if ("true".equalsIgnoreCase(kind)) {
            EndingHandler.playTrueEnding(player);
        } else if ("normal".equalsIgnoreCase(kind)) {
            EndingHandler.playNormalEnding(player);
        } else {
            source.sendFailure(Component.literal("kind must be 'normal' or 'true'"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("ending triggered: " + kind), false);
        return 1;
    }
}

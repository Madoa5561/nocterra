package com.nocterra.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

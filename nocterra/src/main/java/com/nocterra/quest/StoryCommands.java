package com.nocterra.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nocterra.npc.NocterraNpc;
import com.nocterra.npc.NocterraNpcs;
import com.nocterra.quest.dialogue.DialogueService;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * ストーリー検証用コマンド。所有: story 担当。
 *
 * <p>{@code /nocterra quest ...} と {@code /nocterra npc ...} を <b>独立した
 * dispatcher 登録</b>で追加する（Lead の {@code NocterraStoryCommands} と同じ方式で、
 * Brigadier が同名 literal {@code nocterra} をマージする）。
 * 権限は {@link Commands#LEVEL_GAMEMASTERS}。</p>
 */
public final class StoryCommands {
    private StoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nocterra")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("quest")
                        .then(Commands.literal("status").executes(StoryCommands::status))
                        .then(Commands.literal("list").executes(StoryCommands::list))
                        .then(Commands.literal("start")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> start(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> complete(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> reset(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("resetall").executes(StoryCommands::resetAll)))
                .then(Commands.literal("npc")
                        .then(Commands.literal("list").executes(StoryCommands::npcList))
                        .then(Commands.literal("summon")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> npcSummon(context,
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("talk")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> npcTalk(context,
                                                StringArgumentType.getString(context, "id")))))));
    }

    // ------------------------------------------------------------------ quest

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer direct = source.getPlayer();
        if (direct != null) {
            return direct;
        }
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        List<ServerPlayer> players = level.players();
        return players.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())))
                .orElse(null);
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[story] " + QuestManager.debugStatus(player)), false);
        StoryData story = StoryAttachments.get(player);
        source.sendSuccess(() -> Component.literal("[story] lore=" + story.lore()
                + " flags=" + story.flags()), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (StoryRepository.quests().isEmpty()) {
            source.sendFailure(Component.literal("no quests loaded (is the data pack present?)"));
            return 0;
        }
        StringBuilder builder = new StringBuilder("[story] quests: ");
        for (QuestDefinition definition : StoryRepository.quests()) {
            builder.append(definition.questId()).append('(').append(definition.type()).append(") ");
        }
        source.sendSuccess(() -> Component.literal(builder.toString()), false);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context, String id) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        if (StoryRepository.quest(id) == null) {
            source.sendFailure(Component.literal("unknown quest: " + id));
            return 0;
        }
        boolean ok = QuestManager.offer(player, id);
        source.sendSuccess(() -> Component.literal("quest start " + id + " -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int complete(CommandContext<CommandSourceStack> context, String id) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        boolean ok = QuestManager.complete(player, id);
        source.sendSuccess(() -> Component.literal("quest complete " + id + " -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> context, String id) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        StoryAttachments.get(player).resetQuest(id);
        source.sendSuccess(() -> Component.literal("quest reset " + id), false);
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        StoryData story = StoryAttachments.get(player);
        for (String id : List.copyOf(story.activeQuests().keySet())) {
            story.resetQuest(id);
        }
        for (String id : List.copyOf(story.completedQuests())) {
            story.resetQuest(id);
        }
        source.sendSuccess(() -> Component.literal("all quest state reset"), false);
        return 1;
    }

    // ------------------------------------------------------------------ npc

    private static int npcList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("[story] npcs: " + NocterraNpcs.ids()), false);
        return 1;
    }

    private static int npcSummon(CommandContext<CommandSourceStack> context, String id) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        NocterraNpc npc = NocterraNpcs.spawn(level, id, pos);
        if (npc == null) {
            source.sendFailure(Component.literal("unknown npc: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("summoned nocterra:" + id + " at " + pos.toShortString()),
                false);
        return 1;
    }

    /** その場で会話を開く（会話 UI とサーバー側エンジンの検証用）。 */
    private static int npcTalk(CommandContext<CommandSourceStack> context, String id) {
        ServerPlayer player = player(context);
        CommandSourceStack source = context.getSource();
        if (player == null) {
            source.sendFailure(Component.literal("no player"));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        NocterraNpc npc = level.getEntitiesOfClass(NocterraNpc.class,
                        player.getBoundingBox().inflate(48.0),
                        candidate -> id.equals(candidate.npcId()))
                .stream().findFirst().orElse(null);
        if (npc == null) {
            source.sendFailure(Component.literal("no '" + id + "' npc within 48 blocks of the player"));
            return 0;
        }
        DialogueService.open(player, npc);
        source.sendSuccess(() -> Component.literal("dialogue opened: " + id), false);
        return 1;
    }
}

package com.nocterra.core;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nocterra.boss.RotwoodKingEntity;
import com.nocterra.boss.SorrowSaintEntity;
import com.nocterra.entity.AshenWraithEntity;
import com.nocterra.entity.BogGhoulEntity;
import com.nocterra.entity.MourningWispEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 郢ｧ・ｵ郢晢ｽｼ郢晁・繝ｻ陷雁・ｽｽ阮吶・AI郢晢ｽｻ霑･・ｶ隲ｷ遏ｩ繝ｻ驕假ｽｻ郢ｧ蜻茨ｽ､諛・ｽｨ・ｼ邵ｺ蜷ｶ・狗ｸｺ貅假ｽ∫ｸｺ・ｮ鬮｢迢怜験郢ｧ・ｳ郢晄ｧｭﾎｦ郢晏ｳｨﾂ繧亥恍隴帙・ Lead邵ｲ繝ｻ *
 * <p>郢ｧ・ｯ郢晢ｽｩ郢ｧ・､郢ｧ・｢郢晢ｽｳ郢晏現繝ｻ郢晄ｧｭ縺育ｹｧ・ｹ隰ｫ蝣ｺ・ｽ諛岩・邵ｺ蜉ｱ縲堤ｸｲ竏壹・郢ｧ・ｹ邵ｺ・ｮ郢晁ｼ斐♂郢晢ｽｼ郢ｧ・ｺ驕假ｽｻ髯ｦ蠕後・闔莠･繝ｻ隰ｾ・ｻ隰ｦ繝ｻ繝ｻ陷ｿ・ｬ陜溷｣ｹﾂ繝ｻ * 闔・｡鬮ｴ鄙ｫ繝ｻ郢晄じﾎ懃ｹ晢ｽｳ郢ｧ・ｯ邵ｲ竏ｽ・ｺ・｡鬮ｴ繝ｻ郢晢ｽｯ郢ｧ・､郢ｧ・ｹ郢昴・邵ｺ・ｮ鬯ｲ繧・ｲｾ邵ｺ繝ｻﾂ竏晢ｽｱ蝓ｼ・ｬ・ｼ邵ｺ・ｮ驕ｽ繝ｻ蟲・ｬｾ・ｻ隰ｦ繝ｻ・帝具ｽｺ霓｣・ｫ邵ｺ・ｧ邵ｺ髦ｪ・狗ｸｲ繝ｻ * 郢ｧ・ｨ郢晢ｽｳ郢昴・縺・ｹ昴・縺・屁・ｴ邵ｺ・ｮ {@code debug*} 郢晁ｼ斐Ε郢ｧ・ｯ繝ｻ繝ｻntity 隲｡繝ｻ・ｽ轣假ｽｮ貅ｯ・｣繝ｻ・ｼ蟲ｨ・定惱・ｼ邵ｺ・ｶ邵ｺ・ｰ邵ｺ莉｣ﾂ繝ｻ/p>
 */
public final class NocterraDevCommands {
    private NocterraDevCommands() {}

    private static final double SEARCH_RADIUS = 64.0;

    static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nocterra")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dev")
                        .then(Commands.literal("boss")
                                .then(Commands.literal("status").executes(NocterraDevCommands::bossStatus))
                                .then(Commands.literal("unlock").executes(NocterraDevCommands::bossUnlock))
                                .then(Commands.literal("summon").executes(NocterraDevCommands::bossSummon))
                                .then(Commands.literal("phase")
                                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 2))
                                                .executes(ctx -> bossPhase(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "phase")))))
                                .then(Commands.literal("attack")
                                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, 3))
                                                .executes(ctx -> bossAttack(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "variant")))))
                                .then(Commands.literal("windup")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> bossWindup(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "ticks"))))))
                        // 汎用: NocterraBossEntity 基底を使う新ボス群（knight/emperor 等すべて対象）
                        .then(Commands.literal("nboss")
                                .then(Commands.literal("status").executes(NocterraDevCommands::nbossStatus))
                                .then(Commands.literal("unlock").executes(NocterraDevCommands::nbossUnlock))
                                .then(Commands.literal("summon").executes(NocterraDevCommands::nbossSummon))
                                .then(Commands.literal("phase")
                                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 3))
                                                .executes(ctx -> nbossPhase(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "phase")))))
                                .then(Commands.literal("attack")
                                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, 4))
                                                .executes(ctx -> nbossAttack(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "variant")))))
                                .then(Commands.literal("windup")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> nbossWindup(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "ticks"))))))
                        .then(Commands.literal("twin")
                                .then(Commands.literal("enrage").executes(NocterraDevCommands::twinEnrage))
                                .then(Commands.literal("status").executes(ctx -> statusOf(ctx,
                                        com.nocterra.boss.SandglassTwinKingsEntity.class,
                                        com.nocterra.boss.SandglassTwinKingsEntity::debugStatus))))
                        .then(Commands.literal("wraith")
                                .then(Commands.literal("blink").executes(NocterraDevCommands::wraithBlink))
                                .then(Commands.literal("status").executes(ctx -> statusOf(ctx,
                                        AshenWraithEntity.class, AshenWraithEntity::debugStatus))))
                        .then(Commands.literal("wisp")
                                .then(Commands.literal("drain").executes(NocterraDevCommands::wispDrain))
                                .then(Commands.literal("windup")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> wispWindup(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "ticks")))))
                                .then(Commands.literal("status").executes(ctx -> statusOf(ctx,
                                        MourningWispEntity.class, MourningWispEntity::debugStatus))))
                        .then(Commands.literal("saint")
                                .then(Commands.literal("status").executes(NocterraDevCommands::saintStatus))
                                .then(Commands.literal("unlock").executes(NocterraDevCommands::saintUnlock))
                                .then(Commands.literal("summon").executes(NocterraDevCommands::saintSummon))
                                .then(Commands.literal("phase")
                                        .then(Commands.argument("phase", IntegerArgumentType.integer(1, 2))
                                                .executes(ctx -> saintPhase(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "phase")))))
                                .then(Commands.literal("attack")
                                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, 2))
                                                .executes(ctx -> saintAttack(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "variant")))))
                                .then(Commands.literal("windup")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> saintWindup(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "ticks"))))))
                        .then(Commands.literal("ghoul")
                                .then(Commands.literal("pound").executes(NocterraDevCommands::ghoulPound))
                                .then(Commands.literal("windup")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                                .executes(ctx -> ghoulWindup(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "ticks")))))
                                .then(Commands.literal("status").executes(ctx -> statusOf(ctx,
                                        BogGhoulEntity.class, BogGhoulEntity::debugStatus))))));
    }

    // ------------------------------------------------------------------ boss

    private static int bossStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king within " + (int) SEARCH_RADIUS + " blocks"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("boss: " + boss.debugStatus()), false);
        return 1;
    }

    private static int bossPhase(CommandContext<CommandSourceStack> context, int phase) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king nearby"));
            return 0;
        }
        boolean ok = boss.debugForcePhase(phase);
        source.sendSuccess(() -> Component.literal(
                "boss phase -> " + phase + " (" + (ok ? "ok" : "failed") + "): " + boss.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int bossUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king nearby"));
            return 0;
        }
        boss.debugReleasePhaseLock();
        source.sendSuccess(() -> Component.literal("boss phase lock released: " + boss.debugStatus()), false);
        return 1;
    }

    private static int bossSummon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king nearby"));
            return 0;
        }
        boolean ok = boss.debugSummonMinions();
        source.sendSuccess(() -> Component.literal("boss summon -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int bossAttack(CommandContext<CommandSourceStack> context, int variant) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king nearby"));
            return 0;
        }
        ServerPlayer target = targetPlayer(source);
        if (target == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        boolean ok = boss.debugForceAttack(variant, target);
        source.sendSuccess(() -> Component.literal("boss attack variant " + variant + " -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int bossWindup(CommandContext<CommandSourceStack> context, int ticks) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        RotwoodKingEntity boss = nearest(source, RotwoodKingEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no rotwood_king nearby"));
            return 0;
        }
        boss.debugSetAttackWindup(ticks);
        source.sendSuccess(() -> Component.literal("boss windup set to " + ticks), false);
        return 1;
    }

    // --------------------------------------------------------------- others

    private static int wraithBlink(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        AshenWraithEntity wraith = nearest(source, AshenWraithEntity.class);
        if (wraith == null) {
            source.sendFailure(Component.literal("no ashen_wraith nearby"));
            return 0;
        }
        ServerPlayer player = targetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        Vec3 before = wraith.position();
        boolean ok = wraith.debugForceBlink(player);
        source.sendSuccess(() -> Component.literal(String.format("wraith blink %s: (%.1f,%.1f,%.1f) -> (%.1f,%.1f,%.1f) [%s]",
                ok ? "ok" : "failed",
                before.x, before.y, before.z,
                wraith.getX(), wraith.getY(), wraith.getZ(),
                wraith.debugStatus())), false);
        return ok ? 1 : 0;
    }

    private static int wispDrain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MourningWispEntity wisp = nearest(source, MourningWispEntity.class);
        if (wisp == null) {
            source.sendFailure(Component.literal("no mourning_wisp nearby"));
            return 0;
        }
        ServerPlayer player = targetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        boolean ok = wisp.debugForceDrain(player);
        source.sendSuccess(() -> Component.literal("wisp drain " + (ok ? "started" : "failed")
                + " (player hp=" + player.getHealth() + "): " + wisp.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int wispWindup(CommandContext<CommandSourceStack> context, int ticks) {
        CommandSourceStack source = context.getSource();
        MourningWispEntity wisp = nearest(source, MourningWispEntity.class);
        if (wisp == null) {
            source.sendFailure(Component.literal("no mourning_wisp nearby"));
            return 0;
        }
        wisp.debugSetDrainWindup(ticks);
        source.sendSuccess(() -> Component.literal("wisp windup set to " + ticks), false);
        return 1;
    }

    private static int ghoulPound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BogGhoulEntity ghoul = nearest(source, BogGhoulEntity.class);
        if (ghoul == null) {
            source.sendFailure(Component.literal("no bog_ghoul nearby"));
            return 0;
        }
        ServerPlayer player = targetPlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        boolean ok = ghoul.debugForcePound(player);
        source.sendSuccess(() -> Component.literal("ghoul pound " + (ok ? "started" : "failed")
                + " (player hp=" + player.getHealth() + "): " + ghoul.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int ghoulWindup(CommandContext<CommandSourceStack> context, int ticks) {
        CommandSourceStack source = context.getSource();
        BogGhoulEntity ghoul = nearest(source, BogGhoulEntity.class);
        if (ghoul == null) {
            source.sendFailure(Component.literal("no bog_ghoul nearby"));
            return 0;
        }
        ghoul.debugSetPoundWindup(ticks);
        source.sendSuccess(() -> Component.literal("ghoul windup set to " + ticks), false);
        return 1;
    }

    // ------------------------------------------------------------------ saint

    private static int saintStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("saint: " + saint.debugStatus()), false);
        return 1;
    }

    private static int saintPhase(CommandContext<CommandSourceStack> context, int phase) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        boolean ok = saint.debugForcePhase(phase);
        source.sendSuccess(() -> Component.literal("saint phase -> " + phase + " (" + ok + "): " + saint.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int saintUnlock(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        saint.debugReleasePhaseLock();
        source.sendSuccess(() -> Component.literal("saint phase lock released: " + saint.debugStatus()), false);
        return 1;
    }

    private static int saintSummon(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        boolean ok = saint.debugSummonMinions();
        source.sendSuccess(() -> Component.literal("saint summon -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int saintAttack(CommandContext<CommandSourceStack> context, int variant) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        ServerPlayer target = targetPlayer(source);
        if (target == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        boolean ok = saint.debugForceAttack(variant, target);
        source.sendSuccess(() -> Component.literal("saint attack variant " + variant + " -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int saintWindup(CommandContext<CommandSourceStack> context, int ticks) {
        CommandSourceStack source = context.getSource();
        SorrowSaintEntity saint = nearest(source, SorrowSaintEntity.class);
        if (saint == null) {
            source.sendFailure(Component.literal("no sorrow_saint nearby"));
            return 0;
        }
        saint.debugSetAttackWindup(ticks);
        source.sendSuccess(() -> Component.literal("saint windup set to " + ticks), false);
        return 1;
    }

    // ------------------------------------------------------- generic bosses

    private static int twinEnrage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.SandglassTwinKingsEntity twin =
                nearest(source, com.nocterra.boss.SandglassTwinKingsEntity.class);
        if (twin == null) {
            source.sendFailure(Component.literal("no sandglass_twin_kings nearby"));
            return 0;
        }
        boolean ok = twin.debugForceTwinEnrage();
        source.sendSuccess(() -> Component.literal("twin enrage -> " + ok + ": " + twin.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int nbossStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby (any of the NocterraBossEntity subclasses)"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("boss: " + boss.debugStatus()), false);
        return 1;
    }

    private static int nbossPhase(CommandContext<CommandSourceStack> context, int phase) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby"));
            return 0;
        }
        boolean ok = boss.debugForcePhase(phase);
        source.sendSuccess(() -> Component.literal("boss phase -> " + phase + " (" + ok + "): " + boss.debugStatus()), false);
        return ok ? 1 : 0;
    }

    private static int nbossUnlock(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby"));
            return 0;
        }
        boss.debugReleasePhaseLock();
        source.sendSuccess(() -> Component.literal("boss phase lock released: " + boss.debugStatus()), false);
        return 1;
    }

    private static int nbossSummon(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby"));
            return 0;
        }
        boolean ok = boss.debugSummonMinions();
        source.sendSuccess(() -> Component.literal("boss summon -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int nbossAttack(CommandContext<CommandSourceStack> context, int variant) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby"));
            return 0;
        }
        ServerPlayer target = targetPlayer(source);
        if (target == null) {
            source.sendFailure(Component.literal("no player in this dimension"));
            return 0;
        }
        boolean ok = boss.debugForceAttack(variant, target);
        source.sendSuccess(() -> Component.literal("boss attack variant " + variant + " -> " + ok), false);
        return ok ? 1 : 0;
    }

    private static int nbossWindup(CommandContext<CommandSourceStack> context, int ticks) {
        CommandSourceStack source = context.getSource();
        com.nocterra.boss.NocterraBossEntity boss = nearest(source, com.nocterra.boss.NocterraBossEntity.class);
        if (boss == null) {
            source.sendFailure(Component.literal("no nocterra boss nearby"));
            return 0;
        }
        boss.debugSetAttackWindup(ticks);
        source.sendSuccess(() -> Component.literal("boss windup set to " + ticks), false);
        return 1;
    }

    // ---------------------------------------------------------------- utils

    private static ServerPlayer targetPlayer(CommandSourceStack source) {
        List<ServerPlayer> players = source.getLevel().players();
        if (players.isEmpty()) {
            return null;
        }
        Vec3 pos = source.getPosition();
        return players.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(pos)))
                .orElse(null);
    }

    private static <T extends Entity> T nearest(CommandSourceStack source, Class<T> type) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        AABB area = new AABB(pos, pos).inflate(SEARCH_RADIUS);
        List<? extends T> found = level.getEntitiesOfClass(type, area);
        return found.stream()
                .filter(Entity::isAlive)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(pos)))
                .orElse(null);
    }

    private static <T extends Entity> int statusOf(CommandContext<CommandSourceStack> context, Class<T> type,
                                                   Function<T, String> status) {
        CommandSourceStack source = context.getSource();
        T entity = nearest(source, type);
        if (entity == null) {
            source.sendFailure(Component.literal("no " + type.getSimpleName() + " nearby"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(entity.getType().toShortString() + ": " + status.apply(entity)), false);
        return 1;
    }
}

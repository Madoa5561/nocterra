package com.nocterra.item;

import com.nocterra.progression.ProgressionAttachments;
import com.nocterra.progression.ProgressionData;
import com.nocterra.progression.SigilType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 探索手帳。右クリックで現在の発見状況をチャットにまとめて表示する。所有: Lead。
 *
 * <p>26.3 の {@code Screen} API は描画方式が刷新されており（{@code render(GuiGraphics)} が存在しない）、
 * カスタム画面の実装リスクが高いため、確実に動作するチャット形式＋バニラ進捗画面で代替している。</p>
 */
public class ExplorerJournalItem extends Item {
    public ExplorerJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ProgressionData data = ProgressionAttachments.get(serverPlayer);
        int total = SigilType.values().length;

        header(player, "journal.nocterra.title");
        line(player, Component.translatable("journal.nocterra.sigils", data.sigilCount(), total)
                .withStyle(ChatFormatting.GOLD));
        for (SigilType type : SigilType.values()) {
            boolean owned = data.hasSigil(type);
            Component mark = Component.literal(owned ? "◆ " : "◇ ")
                    .withStyle(owned ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY);
            player.sendSystemMessage(mark.copy()
                    .append(Component.translatable(type.translationKey())
                            .withStyle(owned ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY))
                    .append(Component.literal("  (" + type.regionJp() + ")")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        line(player, Component.translatable("journal.nocterra.bosses", data.defeatedBosses().size())
                .withStyle(ChatFormatting.RED));
        line(player, Component.translatable("journal.nocterra.npcs", data.discoveredNpcs().size())
                .withStyle(ChatFormatting.AQUA));
        line(player, Component.translatable("journal.nocterra.quests", data.completedQuests().size())
                .withStyle(ChatFormatting.GREEN));

        if (data.canTrueEnding()) {
            line(player, Component.translatable("journal.nocterra.ready.true").withStyle(ChatFormatting.GOLD));
        } else if (data.canNormalEnding()) {
            line(player, Component.translatable("journal.nocterra.ready.normal").withStyle(ChatFormatting.YELLOW));
        } else {
            line(player, Component.translatable("journal.nocterra.ready.locked", 5 - data.sigilCount())
                    .withStyle(ChatFormatting.GRAY));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void header(Player player, String key) {
        player.sendSystemMessage(Component.translatable(key)
                .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
    }

    private static void line(Player player, Component component) {
        player.sendSystemMessage(component);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nocterra.journal.use").withStyle(ChatFormatting.GRAY));
    }
}

package com.nocterra.item;

import com.nocterra.networking.journal.JournalDataPayload;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * 探索手帳。右クリックで GUI を開き、発見状況（刻印・撃破・NPC・クエスト・結末）を一覧表示する。所有: Lead。
 *
 * <p>進行データはサーバー権威のため、サーバーが {@link JournalDataPayload} を送り、
 * クライアントが {@code JournalScreen} を開く。チャットにも要約を出す（GUI を閉じても残るように）。</p>
 */
public class ExplorerJournalItem extends Item {
    public ExplorerJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            // クライアントはサーバーからの payload で画面を開く
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ProgressionData data = ProgressionAttachments.get(serverPlayer);
        int total = SigilType.values().length;

        // 1) GUI 用データ
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new JournalDataPayload(
                data.sigilMask(),
                data.defeatedBosses().size(),
                data.discoveredNpcs().size(),
                data.completedQuests().size(),
                data.normalEndingSeen(),
                data.trueEndingSeen()));

        // 2) チャット要約（GUI を閉じた後にも残る）
        player.sendSystemMessage(Component.translatable("journal.nocterra.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.translatable("journal.nocterra.sigils",
                        data.sigilCount(), total)
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
        player.sendSystemMessage(Component.translatable("journal.nocterra.bosses",
                data.defeatedBosses().size()).withStyle(ChatFormatting.RED));
        player.sendSystemMessage(Component.translatable("journal.nocterra.npcs",
                data.discoveredNpcs().size()).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("journal.nocterra.quests",
                data.completedQuests().size()).withStyle(ChatFormatting.GREEN));

        if (data.canTrueEnding()) {
            player.sendSystemMessage(Component.translatable("journal.nocterra.ready.true")
                    .withStyle(ChatFormatting.GOLD));
        } else if (data.canNormalEnding()) {
            player.sendSystemMessage(Component.translatable("journal.nocterra.ready.normal")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.translatable("journal.nocterra.ready.locked",
                    5 - data.sigilCount()).withStyle(ChatFormatting.GRAY));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nocterra.journal.use").withStyle(ChatFormatting.GRAY));
    }
}

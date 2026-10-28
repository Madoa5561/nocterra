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

import java.util.List;

/**
 * 王の刻印。右クリックで「同調」し、プレイヤーの進行データに記録する。所有: Lead。
 */
public class SigilItem extends Item {
    private final SigilType type;

    public SigilItem(SigilType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public SigilType sigilType() {
        return this.type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ProgressionData data = ProgressionAttachments.get(player);
        boolean isNew = data.addSigil(this.type);
        if (player instanceof ServerPlayer serverPlayer) {
            if (isNew) {
                player.sendSystemMessage(Component.translatable(
                        "message.nocterra.sigil.attuned",
                        Component.translatable(this.type.translationKey()),
                        data.sigilCount(), SigilType.values().length).withStyle(ChatFormatting.GOLD));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.0F, 1.4F);
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.nocterra.sigil.already",
                        Component.translatable(this.type.translationKey())).withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nocterra.sigil.region", this.type.regionJp())
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("tooltip.nocterra.sigil.use").withStyle(ChatFormatting.GRAY));
    }
}

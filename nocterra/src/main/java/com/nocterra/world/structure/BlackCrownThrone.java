package com.nocterra.world.structure;

import com.nocterra.progression.EndingHandler;
import com.nocterra.progression.ProgressionAttachments;
import com.nocterra.progression.ProgressionData;
import com.nocterra.progression.SigilType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 黒冠の玉座。所有: Lead。
 *
 * <p>右クリックで刻印数を判定し、5 個以上で通常エンディング、8 個で真エンディングへ分岐する。
 * 不足時は必要数を提示する（攻略不能にならないよう、刻印は何度でも再入手できる）。</p>
 */
public class BlackCrownThrone extends Block {
    public BlackCrownThrone(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ProgressionData data = ProgressionAttachments.get(serverPlayer);
        int count = data.sigilCount();
        int total = SigilType.values().length;

        if (data.canTrueEnding()) {
            EndingHandler.playTrueEnding(serverPlayer);
            particleBurst(level, pos);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (data.canNormalEnding()) {
            EndingHandler.playNormalEnding(serverPlayer);
            particleBurst(level, pos);
            return InteractionResult.SUCCESS_SERVER;
        }

        player.sendSystemMessage(Component.translatable(
                "message.nocterra.throne.locked", count, total, 5).withStyle(ChatFormatting.RED));
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void particleBurst(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 1.2, 1.0, 1.2, 0.02);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 40, 0.8, 0.6, 0.8, 0.01);
        }
    }
}

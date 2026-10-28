package com.nocterra.progression;

import com.nocterra.Nocterra;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;

/**
 * ボス撃破の検出と刻印の付与。所有: Lead（サーバー権威）。
 *
 * <p>ボスが死んだら、そのボス固有の刻印を「撃破したプレイヤー」に付与する
 * （とどめを刺したプレイヤーがいない場合は、周囲 64 ブロックのプレイヤー全員に付与）。
 * あわせてチャットで刻印獲得を通知する。</p>
 */
public final class ProgressionEvents {
    private ProgressionEvents() {}

    /** ボス ID → 表示名の翻訳キー（`entity.nocterra.<id>`）。 */
    private static final String BOSS_PREFIX = "entity." + Nocterra.MOD_ID + ".";

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null || !Nocterra.MOD_ID.equals(typeId.getNamespace())) {
            return;
        }
        SigilType sigil = SigilType.byBossId(typeId.getPath());
        if (sigil == null) {
            return;
        }
        // とどめのプレイヤー、いなければ近傍のプレイヤー全員
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player : null;
        if (killer != null) {
            grant(killer, sigil);
            return;
        }
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                    ServerPlayer.class, entity.getBoundingBox().inflate(64.0))) {
                grant(player, sigil);
            }
        }
    }

    private static void grant(ServerPlayer player, SigilType sigil) {
        ProgressionData data = ProgressionAttachments.get(player);
        boolean isNew = data.addSigil(sigil);
        data.addDefeatedBoss(sigil.bossId());
        if (!isNew) {
            return;
        }
        player.sendSystemMessage(Component.translatable(
                        "message.nocterra.sigil.granted",
                        Component.translatable(BOSS_PREFIX + sigil.bossId()),
                        Component.translatable(sigil.translationKey()),
                        data.sigilCount(), SigilType.values().length)
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        if (data.canNormalEnding() && !data.canTrueEnding()) {
            player.sendSystemMessage(Component.translatable("message.nocterra.sigil.ready.normal")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        if (data.canTrueEnding()) {
            player.sendSystemMessage(Component.translatable("message.nocterra.sigil.ready.true")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
        Nocterra.LOG.info("[NOCTERRA] {} obtained sigil {} ({}/{})",
                player.getName().getString(), sigil.sigilKey(), data.sigilCount(), SigilType.values().length);
    }

    /** 開発・デバッグ用: 全刻印を付与する。 */
    public static void grantAll(ServerPlayer player) {
        for (SigilType type : SigilType.values()) {
            ProgressionAttachments.get(player).addSigil(type);
        }
    }

    private static final Map<String, SigilType> BY_BOSS = Map.of();
}

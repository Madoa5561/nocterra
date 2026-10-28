package com.nocterra.combat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 戦闘のゲームバス（{@link NeoForge#EVENT_BUS}）リスナ。
 *
 * <p>登録は {@link NocterraCombat#register} から {@link #register()} を呼ぶ。
 * すべてサーバー権威。クライアント側で同じ計算はしない。</p>
 *
 * <p>26.3 の被弾イベントは {@link LivingIncomingDamageEvent}
 * （{@code LivingEntity#hurt} 内で無敵判定の後に発火、キャンセル可能）。
 * スタミナ制御もこのイベント／{@link ServerTickEvent.Post} の 2 か所だけで完結する。</p>
 */
public final class CombatHandler {
    private CombatHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CombatHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(CombatHandler::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatHandler::onPlayerLoggedOut);
    }

    // ---------------------------------------------------------------- tick

    private static void onServerTick(ServerTickEvent.Post event) {
        CombatScheduler.tick();

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        CombatData data = CombatAttachments.get(player);

        // タイマー
        if (data.getDodgeCooldown() > 0) {
            data.setDodgeCooldown(data.getDodgeCooldown() - 1);
        }
        if (data.getParryCooldown() > 0) {
            data.setParryCooldown(data.getParryCooldown() - 1);
        }
        if (data.getHeavyCooldown() > 0) {
            data.setHeavyCooldown(data.getHeavyCooldown() - 1);
        }
        if (data.getInvulnerableTicks() > 0) {
            data.setInvulnerableTicks(data.getInvulnerableTicks() - 1);
        }
        if (data.getParryTicks() > 0) {
            data.setParryTicks(data.getParryTicks() - 1);
        }

        // 回避ダッシュ。サーバーで決めた速度をクライアントへ送るため
        // 26.3 の syncVelocity / needsSync フラグを立てる。
        if (data.getDodgeTicks() > 0) {
            data.setDodgeTicks(data.getDodgeTicks() - 1);
            Vec3 direction = data.getDodgeDirection();
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(
                    direction.x * CombatBalance.dodgeSpeed,
                    current.y,
                    direction.z * CombatBalance.dodgeSpeed);
            player.syncVelocity = true;
            player.needsSync = true;
        }

        // 自然回復（スタミナ・魔力）
        data.setRegenTimer(data.getRegenTimer() + 1);
        if (data.getRegenTimer() >= CombatBalance.regenIntervalTicks) {
            data.setRegenTimer(0);
            data.addStamina(CombatBalance.staminaRegenPerInterval());
            data.addMana(CombatBalance.manaRegenPerInterval());
        }

        // コンボ時間切れ
        if (data.getComboWindow() > 0) {
            data.setComboWindow(data.getComboWindow() - 1);
            if (data.getComboWindow() == 0) {
                data.resetCombo();
            }
        }

        // 定期同期（スタミナ・魔力・コンボ）
        CombatSync.syncIfDue(player);
    }

    // ---------------------------------------------------------------- 被弾

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        if (event.getEntity() instanceof ServerPlayer victim) {
            CombatData victimData = CombatAttachments.get(victim);

            // 回避の無敵フレーム（invulnerableTime ではなく専用フラグでキャンセル）
            if (victimData.getInvulnerableTicks() > 0) {
                event.setCanceled(true);
                return;
            }

            // パリィ: ダメージ 0 + 攻撃者スタン
            if (victimData.getParryTicks() > 0
                    && source.getEntity() instanceof LivingEntity attacker
                    && attacker != victim) {
                victimData.setParryTicks(0);
                event.setCanceled(true);
                CombatActions.stun(attacker);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.9F);
                CombatSync.sync(victim);
                return;
            }
        }

        if (event.getAmount() <= 0.0F) {
            return;
        }

        // コンボ倍率と強化補正（プレイヤーの近接攻撃のみ。矢・魔法は対象外）
        if (source.getEntity() instanceof ServerPlayer attacker
                && source.getDirectEntity() == attacker
                && event.getEntity() != attacker) {
            applyComboAndEnhancement(attacker, event);
        }
    }

    private static void applyComboAndEnhancement(ServerPlayer attacker, LivingIncomingDamageEvent event) {
        CombatData data = CombatAttachments.get(attacker);
        ItemStack stack = attacker.getMainHandItem();

        // 武器が変わったらコンボは途切れる
        String weaponId = CombatActions.weaponId(stack);
        if (!weaponId.equals(data.getLastWeapon())) {
            data.setComboHits(0);
            data.setLastWeapon(weaponId);
        }

        float amount = event.getAmount();
        amount += CombatComponents.enhancement(stack) * CombatBalance.enhancementDamagePerLevel;

        double multiplier = data.getComboMultiplier();
        CombatWeapon weapon = stack.getItem() instanceof CombatWeapon candidate ? candidate : null;
        LivingEntity target = event.getEntity();
        if (weapon != null) {
            multiplier *= weapon.meleeDamageMultiplier(attacker, target);
        }
        event.setAmount((float) (amount * multiplier));

        // 武器固有の近接ヒット効果（着火・移動速度低下・ノックバックなど）
        if (weapon != null) {
            weapon.onMeleeHit(attacker, target);
        }

        int maxHits = Math.max(1, CombatBalance.comboMultipliers.length);
        int nextHits = data.getComboHits() + 1;
        data.setComboHits(nextHits >= maxHits ? 0 : nextHits);
        data.setComboWindow(CombatBalance.comboWindowTicks);
    }

    // ---------------------------------------------------------------- 切断

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CombatScheduler.clear(player);

        // 一時状態を破棄する（スタミナ・魔力は Attachment により永続化される）
        CombatData data = CombatAttachments.get(player);
        data.setDodgeTicks(0);
        data.setDodgeCooldown(0);
        data.setInvulnerableTicks(0);
        data.setParryTicks(0);
        data.setParryCooldown(0);
        data.setHeavyCooldown(0);
        data.setRegenTimer(0);
        data.setSyncTimer(0);
        data.resetCombo();
    }
}

package com.nocterra.combat.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NOCTERRA 戦闘 payload の登録まとめ。
 *
 * <p><b>このクラスは自分では登録しない。</b> Lead 所有の
 * {@code com.nocterra.networking.NocterraNetwork#registerCombatPayloads} の
 * 中で次の 1 行を呼んでもらう（{@code NocterraNetwork} は combat 担当の編集スコープ外）:</p>
 *
 * <pre>{@code
 * private static void registerCombatPayloads(PayloadRegistrar registrar) {
 *     com.nocterra.combat.network.CombatPayloads.register(registrar);
 * }
 * }</pre>
 */
public final class CombatPayloads {
    private CombatPayloads() {}

    public static void register(PayloadRegistrar registrar) {
        // サーバー → クライアント: HUD 同期
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC, CombatSyncPayload::handle);

        // クライアント → サーバー: アクション要求
        registrar.playToServer(DodgeRequestPayload.TYPE, DodgeRequestPayload.STREAM_CODEC, DodgeRequestPayload::handle);
        registrar.playToServer(HeavyAttackRequestPayload.TYPE, HeavyAttackRequestPayload.STREAM_CODEC,
                HeavyAttackRequestPayload::handle);
        registrar.playToServer(ParryRequestPayload.TYPE, ParryRequestPayload.STREAM_CODEC, ParryRequestPayload::handle);
        registrar.playToServer(SkillRequestPayload.TYPE, SkillRequestPayload.STREAM_CODEC, SkillRequestPayload::handle);
    }
}

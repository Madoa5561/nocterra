package com.nocterra.networking;

import com.nocterra.Nocterra;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NOCTERRA のネットワーク登録点。所有: Lead。
 *
 * <p>Payload を追加する場合は {@link #registerPayloads} に
 * {@code registrar.playToClient/playToServer/playBidirectional} を追記する。
 * CustomPacketPayload.Type は必ず自前 namespace で作ること:
 * {@code new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("nocterra", "..."))}。</p>
 */
public final class NocterraNetwork {
    private NocterraNetwork() {}

    /** プロトコルバージョン。payload の互換性が変わったら上げる。 */
    public static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modBus) {
        modBus.addListener(NocterraNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        Nocterra.LOG.debug("NOCTERRA network payload handler registrar created (version {})", PROTOCOL_VERSION);
        registerCombatPayloads(registrar);
    }

    /** combat サブシステムの payload 登録（実装は CombatPayloads 側）。 */
    private static void registerCombatPayloads(PayloadRegistrar registrar) {
        com.nocterra.combat.network.CombatPayloads.register(registrar);
    }
}

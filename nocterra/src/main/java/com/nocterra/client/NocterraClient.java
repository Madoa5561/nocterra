package com.nocterra.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * クライアント専用の初期化点。所有: client 担当。
 *
 * <p>専用サーバーではロードされない（{@link com.nocterra.Nocterra} が {@code Dist.CLIENT} を確認して呼ぶ）。
 * レンダラ登録は {@link EntityRenderersEvent.RegisterRenderers} の 1 か所に集約する。</p>
 */
public final class NocterraClient {
    private NocterraClient() {}

    public static void initialize(IEventBus modBus) {
        modBus.addListener(NocterraClient::registerRenderers);
        modBus.addListener(NocterraClient::registerKeyMappings);
        // クライアント tick / HUD 描画はここに追加する
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        NocterraClientRenderers.register(event);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        NocterraKeybinds.register(event);
    }
}

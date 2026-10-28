package com.nocterra.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * エンティティ／ブロックエンティティのレンダラ登録。所有: entity 担当。
 *
 * <p>{@link NocterraClient} から呼ばれる。render state + submit node 方式（26.3）に注意。</p>
 */
public final class NocterraClientRenderers {
    private NocterraClientRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        // entity 担当がここに registerEntityRenderer を追加する
    }
}

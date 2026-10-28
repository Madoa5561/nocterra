package com.nocterra.client;

import com.nocterra.client.render.AshenWraithRenderer;
import com.nocterra.client.render.RotwoodKingRenderer;
import com.nocterra.entity.NocterraEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * エンティティ／ブロックエンティティのレンダラ登録。所有: entity 担当。
 *
 * <p>{@link NocterraClient} から呼ばれる。26.3 は render state + submit node 方式
 * （GeckoLib 5.5.7 の {@code GeoEntityRenderer} がそのまま対応している）。</p>
 */
public final class NocterraClientRenderers {
    private NocterraClientRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NocterraEntities.ASHEN_WRAITH.get(),
                context -> new AshenWraithRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ROTWOOD_KING.get(),
                context -> new RotwoodKingRenderer(context));
    }
}

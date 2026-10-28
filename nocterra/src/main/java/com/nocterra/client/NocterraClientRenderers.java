package com.nocterra.client;

import com.nocterra.client.render.AshCrawlerRenderer;
import com.nocterra.client.render.AshenWraithRenderer;
import com.nocterra.client.render.BogGhoulRenderer;
import com.nocterra.client.render.MourningWispRenderer;
import com.nocterra.client.render.RotwoodKingRenderer;
import com.nocterra.client.render.SorrowSaintRenderer;
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
        event.registerEntityRenderer(NocterraEntities.ASH_CRAWLER.get(),
                context -> new AshCrawlerRenderer(context));
        event.registerEntityRenderer(NocterraEntities.MOURNING_WISP.get(),
                context -> new MourningWispRenderer(context));
        event.registerEntityRenderer(NocterraEntities.BOG_GHOUL.get(),
                context -> new BogGhoulRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ROTWOOD_KING.get(),
                context -> new RotwoodKingRenderer(context));
        event.registerEntityRenderer(NocterraEntities.SORROW_SAINT.get(),
                context -> new SorrowSaintRenderer(context));
    }
}

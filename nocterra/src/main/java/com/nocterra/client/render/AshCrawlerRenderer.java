package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.entity.AshCrawlerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 這い虫のレンダラ。 */
public final class AshCrawlerRenderer extends GeoEntityRenderer<AshCrawlerEntity, EntityRenderState> {
    public AshCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new AshCrawlerModel());
        this.shadowRadius = 0.35F;
    }
}

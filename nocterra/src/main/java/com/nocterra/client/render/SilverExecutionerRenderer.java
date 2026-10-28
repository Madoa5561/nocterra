package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.SilverExecutionerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 白銀の処刑者のレンダラ。 */
public final class SilverExecutionerRenderer
        extends GeoEntityRenderer<SilverExecutionerEntity, EntityRenderState> {
    public SilverExecutionerRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverExecutionerModel());
        this.shadowRadius = 0.8F;
    }
}

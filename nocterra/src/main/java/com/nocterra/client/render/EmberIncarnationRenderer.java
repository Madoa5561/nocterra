package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.EmberIncarnationEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 灯火の化身のレンダラ。 */
public final class EmberIncarnationRenderer
        extends GeoEntityRenderer<EmberIncarnationEntity, EntityRenderState> {
    public EmberIncarnationRenderer(EntityRendererProvider.Context context) {
        super(context, new EmberIncarnationModel());
        this.shadowRadius = 0.8F;
    }
}

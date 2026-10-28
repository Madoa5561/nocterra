package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.entity.MourningWispEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 嘆きの亡霊のレンダラ。 */
public final class MourningWispRenderer extends GeoEntityRenderer<MourningWispEntity, EntityRenderState> {
    public MourningWispRenderer(EntityRendererProvider.Context context) {
        super(context, new MourningWispModel());
        this.shadowRadius = 0.3F;
    }
}

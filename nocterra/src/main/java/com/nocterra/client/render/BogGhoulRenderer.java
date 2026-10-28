package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.entity.BogGhoulEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 沼の屍鬼のレンダラ。 */
public final class BogGhoulRenderer extends GeoEntityRenderer<BogGhoulEntity, EntityRenderState> {
    public BogGhoulRenderer(EntityRendererProvider.Context context) {
        super(context, new BogGhoulModel());
        this.shadowRadius = 0.6F;
    }
}

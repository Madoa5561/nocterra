package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.AbyssalSeaEmperorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 深淵の海帝のレンダラ。 */
public final class AbyssalSeaEmperorRenderer
        extends GeoEntityRenderer<AbyssalSeaEmperorEntity, EntityRenderState> {
    public AbyssalSeaEmperorRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalSeaEmperorModel());
        this.shadowRadius = 1.0F;
    }
}

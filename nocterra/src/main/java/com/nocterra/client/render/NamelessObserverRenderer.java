package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.NamelessObserverEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 名もなき観測者のレンダラ。 */
public final class NamelessObserverRenderer
        extends GeoEntityRenderer<NamelessObserverEntity, EntityRenderState> {
    public NamelessObserverRenderer(EntityRendererProvider.Context context) {
        super(context, new NamelessObserverModel());
        this.shadowRadius = 0.6F;
    }
}

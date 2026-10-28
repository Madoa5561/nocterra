package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.CondemnedKnightLordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 断罪の騎士長のレンダラ。 */
public final class CondemnedKnightLordRenderer
        extends GeoEntityRenderer<CondemnedKnightLordEntity, EntityRenderState> {
    public CondemnedKnightLordRenderer(EntityRendererProvider.Context context) {
        super(context, new CondemnedKnightLordModel());
        this.shadowRadius = 0.9F;
    }
}

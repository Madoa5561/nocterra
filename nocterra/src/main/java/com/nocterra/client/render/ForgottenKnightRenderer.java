package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.ForgottenKnightEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 忘却された騎士のレンダラ。 */
public final class ForgottenKnightRenderer
        extends GeoEntityRenderer<ForgottenKnightEntity, EntityRenderState> {
    public ForgottenKnightRenderer(EntityRendererProvider.Context context) {
        super(context, new ForgottenKnightModel());
        this.shadowRadius = 0.7F;
    }
}

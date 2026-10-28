package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.AstelFallenStarDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 墜星竜アステルのレンダラ。 */
public final class AstelFallenStarDragonRenderer
        extends GeoEntityRenderer<AstelFallenStarDragonEntity, EntityRenderState> {
    public AstelFallenStarDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new AstelFallenStarDragonModel());
        this.shadowRadius = 1.4F;
    }
}

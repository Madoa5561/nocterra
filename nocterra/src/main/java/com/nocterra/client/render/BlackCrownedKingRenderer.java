package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.BlackCrownedKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 黒冠の王のレンダラ。 */
public final class BlackCrownedKingRenderer
        extends GeoEntityRenderer<BlackCrownedKingEntity, EntityRenderState> {
    public BlackCrownedKingRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackCrownedKingModel());
        this.shadowRadius = 1.0F;
    }
}

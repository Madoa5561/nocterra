package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.SandglassTwinKingsEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 砂時計の双王のレンダラ。 */
public final class SandglassTwinKingsRenderer
        extends GeoEntityRenderer<SandglassTwinKingsEntity, EntityRenderState> {
    public SandglassTwinKingsRenderer(EntityRendererProvider.Context context) {
        super(context, new SandglassTwinKingsModel());
        this.shadowRadius = 0.8F;
    }
}

package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.MoltenSteelColossusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** 灼鋼の巨神のレンダラ。 */
public final class MoltenSteelColossusRenderer
        extends GeoEntityRenderer<MoltenSteelColossusEntity, EntityRenderState> {
    public MoltenSteelColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new MoltenSteelColossusModel());
        this.shadowRadius = 1.5F;
    }
}

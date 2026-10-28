package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.entity.AshenWraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 灰樹の森の亡霊のレンダラ。26.3 は render state + submit node 方式で、
 * {@link GeoEntityRenderer} がその配線をすべて持つ（GECKOLIB5.md §4.2）。
 */
public final class AshenWraithRenderer extends GeoEntityRenderer<AshenWraithEntity, EntityRenderState> {
    public AshenWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new AshenWraithModel());
        this.shadowRadius = 0.45F;
    }
}

package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.SorrowSaintEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** ボス「悲嘆の聖女」のレンダラ（HP バーは {@code ServerBossEvent} 側）。 */
public final class SorrowSaintRenderer extends GeoEntityRenderer<SorrowSaintEntity, EntityRenderState> {
    public SorrowSaintRenderer(EntityRendererProvider.Context context) {
        super(context, new SorrowSaintModel());
        this.shadowRadius = 0.9F;
    }
}

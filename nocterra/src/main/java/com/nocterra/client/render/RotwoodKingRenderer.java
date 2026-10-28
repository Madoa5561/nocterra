package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.boss.RotwoodKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * ボス「腐朽の森王」のレンダラ。
 *
 * <p>HP バーは {@code ServerBossEvent} 側で描画されるため、ここではモデル描画のみを担当する。</p>
 */
public final class RotwoodKingRenderer extends GeoEntityRenderer<RotwoodKingEntity, EntityRenderState> {
    public RotwoodKingRenderer(EntityRendererProvider.Context context) {
        super(context, new RotwoodKingModel());
        this.shadowRadius = 1.1F;
    }
}

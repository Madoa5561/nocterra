package com.nocterra.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.nocterra.npc.NocterraNpc;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * NPC 共通のレンダラ。所有: story 担当。
 *
 * <p>26.3 は render state + submit node 方式で、{@link GeoEntityRenderer} が
 * その配線をすべて持つ（GECKOLIB5.md §4.2）。NPC ごとの差はモデル ID だけ。</p>
 */
public final class NpcRenderer extends GeoEntityRenderer<NocterraNpc, EntityRenderState> {
    public NpcRenderer(EntityRendererProvider.Context context, String npcId) {
        super(context, new NpcModel(npcId));
        this.shadowRadius = 0.4F;
    }
}

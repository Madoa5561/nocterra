package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.npc.NocterraNpc;
import net.minecraft.resources.Identifier;

/**
 * NPC 共通の GeckoLib モデル。所有: story 担当。
 *
 * <p>{@link DefaultedEntityGeoModel} が NPC ID からパスを自動生成する（GECKOLIB5.md §7.2）:
 * <ul>
 *   <li>モデル: {@code assets/nocterra/geckolib/models/entity/<npcId>.geo.json}</li>
 *   <li>アニメ: {@code assets/nocterra/geckolib/animations/entity/<npcId>.animation.json}</li>
 *   <li>テクスチャ: {@code assets/nocterra/textures/entity/<npcId>.png}</li>
 * </ul>
 */
public final class NpcModel extends DefaultedEntityGeoModel<NocterraNpc> {
    public NpcModel(String npcId) {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, npcId));
    }
}

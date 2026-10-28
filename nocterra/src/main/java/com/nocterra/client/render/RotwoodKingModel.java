package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.RotwoodKingEntity;
import net.minecraft.resources.Identifier;

/**
 * ボス「腐朽の森王」の GeckoLib モデル。
 *
 * <ul>
 *   <li>モデル: {@code assets/nocterra/geckolib/models/entity/rotwood_king.geo.json}</li>
 *   <li>アニメ: {@code assets/nocterra/geckolib/animations/entity/rotwood_king.animation.json}</li>
 *   <li>テクスチャ: {@code assets/nocterra/textures/entity/rotwood_king.png}</li>
 * </ul>
 */
public final class RotwoodKingModel extends DefaultedEntityGeoModel<RotwoodKingEntity> {
    public RotwoodKingModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "rotwood_king"));
    }
}

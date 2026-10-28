package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.SorrowSaintEntity;
import net.minecraft.resources.Identifier;

/**
 * ボス「悲嘆の聖女」の GeckoLib モデル。
 *
 * <ul>
 *   <li>モデル: {@code assets/nocterra/geckolib/models/entity/sorrow_saint.geo.json}</li>
 *   <li>アニメ: {@code assets/nocterra/geckolib/animations/entity/sorrow_saint.animation.json}</li>
 *   <li>テクスチャ: {@code assets/nocterra/textures/entity/sorrow_saint.png}</li>
 * </ul>
 */
public final class SorrowSaintModel extends DefaultedEntityGeoModel<SorrowSaintEntity> {
    public SorrowSaintModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "sorrow_saint"));
    }
}

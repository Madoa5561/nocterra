package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.CondemnedKnightLordEntity;
import net.minecraft.resources.Identifier;

/** 断罪の騎士長の GeckoLib モデル（{@code .../entity/condemned_knight_lord.*}）。 */
public final class CondemnedKnightLordModel extends DefaultedEntityGeoModel<CondemnedKnightLordEntity> {
    public CondemnedKnightLordModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "condemned_knight_lord"));
    }
}

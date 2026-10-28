package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.SilverExecutionerEntity;
import net.minecraft.resources.Identifier;

/** 白銀の処刑者の GeckoLib モデル（{@code .../entity/silver_executioner.*}）。 */
public final class SilverExecutionerModel extends DefaultedEntityGeoModel<SilverExecutionerEntity> {
    public SilverExecutionerModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "silver_executioner"));
    }
}

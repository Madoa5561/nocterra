package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.EmberIncarnationEntity;
import net.minecraft.resources.Identifier;

/** 灯火の化身の GeckoLib モデル（{@code .../entity/ember_incarnation.*}）。 */
public final class EmberIncarnationModel extends DefaultedEntityGeoModel<EmberIncarnationEntity> {
    public EmberIncarnationModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "ember_incarnation"));
    }
}

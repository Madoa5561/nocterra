package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.NamelessObserverEntity;
import net.minecraft.resources.Identifier;

/** 名もなき観測者の GeckoLib モデル（{@code .../entity/nameless_observer.*}）。 */
public final class NamelessObserverModel extends DefaultedEntityGeoModel<NamelessObserverEntity> {
    public NamelessObserverModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "nameless_observer"));
    }
}

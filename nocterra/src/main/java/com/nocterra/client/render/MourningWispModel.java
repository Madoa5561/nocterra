package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.entity.MourningWispEntity;
import net.minecraft.resources.Identifier;

/** 嘆きの亡霊の GeckoLib モデル（{@code .../entity/mourning_wisp.*}）。 */
public final class MourningWispModel extends DefaultedEntityGeoModel<MourningWispEntity> {
    public MourningWispModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "mourning_wisp"));
    }
}

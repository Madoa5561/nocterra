package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.entity.BogGhoulEntity;
import net.minecraft.resources.Identifier;

/** 沼の屍鬼の GeckoLib モデル（{@code .../entity/bog_ghoul.*}）。 */
public final class BogGhoulModel extends DefaultedEntityGeoModel<BogGhoulEntity> {
    public BogGhoulModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "bog_ghoul"));
    }
}

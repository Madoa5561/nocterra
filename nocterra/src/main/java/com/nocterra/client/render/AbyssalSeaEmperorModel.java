package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.AbyssalSeaEmperorEntity;
import net.minecraft.resources.Identifier;

/** 深淵の海帝の GeckoLib モデル（{@code .../entity/abyssal_sea_emperor.*}）。 */
public final class AbyssalSeaEmperorModel extends DefaultedEntityGeoModel<AbyssalSeaEmperorEntity> {
    public AbyssalSeaEmperorModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "abyssal_sea_emperor"));
    }
}

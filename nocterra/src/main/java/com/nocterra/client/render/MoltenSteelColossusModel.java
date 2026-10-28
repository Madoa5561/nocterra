package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.MoltenSteelColossusEntity;
import net.minecraft.resources.Identifier;

/** 灼鋼の巨神の GeckoLib モデル（{@code .../entity/molten_steel_colossus.*}）。 */
public final class MoltenSteelColossusModel extends DefaultedEntityGeoModel<MoltenSteelColossusEntity> {
    public MoltenSteelColossusModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "molten_steel_colossus"));
    }
}

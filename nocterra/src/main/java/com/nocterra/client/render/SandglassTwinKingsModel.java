package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.SandglassTwinKingsEntity;
import net.minecraft.resources.Identifier;

/** 砂時計の双王の GeckoLib モデル（{@code .../entity/sandglass_twin_kings.*}）。 */
public final class SandglassTwinKingsModel extends DefaultedEntityGeoModel<SandglassTwinKingsEntity> {
    public SandglassTwinKingsModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "sandglass_twin_kings"));
    }
}

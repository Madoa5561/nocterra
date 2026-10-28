package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.BlackCrownedKingEntity;
import net.minecraft.resources.Identifier;

/** 黒冠の王の GeckoLib モデル（{@code .../entity/black_crowned_king.*}）。 */
public final class BlackCrownedKingModel extends DefaultedEntityGeoModel<BlackCrownedKingEntity> {
    public BlackCrownedKingModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "black_crowned_king"));
    }
}

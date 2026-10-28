package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.ForgottenKnightEntity;
import net.minecraft.resources.Identifier;

/** 忘却された騎士の GeckoLib モデル（{@code .../entity/forgotten_knight.*}）。 */
public final class ForgottenKnightModel extends DefaultedEntityGeoModel<ForgottenKnightEntity> {
    public ForgottenKnightModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "forgotten_knight"));
    }
}

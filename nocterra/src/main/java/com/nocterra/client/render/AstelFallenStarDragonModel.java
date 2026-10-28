package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.boss.AstelFallenStarDragonEntity;
import net.minecraft.resources.Identifier;

/** 墜星竜アステルの GeckoLib モデル（{@code .../entity/astel_fallen_star_dragon.*}）。 */
public final class AstelFallenStarDragonModel extends DefaultedEntityGeoModel<AstelFallenStarDragonEntity> {
    public AstelFallenStarDragonModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "astel_fallen_star_dragon"));
    }
}

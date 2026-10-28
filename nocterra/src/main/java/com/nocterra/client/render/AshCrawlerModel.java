package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.entity.AshCrawlerEntity;
import net.minecraft.resources.Identifier;

/** 這い虫の GeckoLib モデル（{@code assets/nocterra/geckolib/{models,animations}/entity/ash_crawler.*}）。 */
public final class AshCrawlerModel extends DefaultedEntityGeoModel<AshCrawlerEntity> {
    public AshCrawlerModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "ash_crawler"));
    }
}

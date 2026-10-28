package com.nocterra.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.nocterra.Nocterra;
import com.nocterra.entity.AshenWraithEntity;
import net.minecraft.resources.Identifier;

/**
 * 灰樹の森の亡霊の GeckoLib モデル。
 *
 * <p>{@link DefaultedEntityGeoModel} がパスを自動生成する（GECKOLIB5.md §7.2）:
 * <ul>
 *   <li>モデル: {@code assets/nocterra/geckolib/models/entity/ashen_wraith.geo.json}</li>
 *   <li>アニメ: {@code assets/nocterra/geckolib/animations/entity/ashen_wraith.animation.json}</li>
 *   <li>テクスチャ: {@code assets/nocterra/textures/entity/ashen_wraith.png}</li>
 * </ul>
 */
public final class AshenWraithModel extends DefaultedEntityGeoModel<AshenWraithEntity> {
    public AshenWraithModel() {
        super(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "ashen_wraith"));
    }
}

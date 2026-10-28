package com.nocterra.entity;

import com.nocterra.Nocterra;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NOCTERRA のエンティティ型登録。所有: entity/boss 担当。
 *
 * <p>エンティティを追加する場合はここに {@code ENTITIES.register(...)} を追記し、
 * {@code com.nocterra.entity.NocterraAttributes}（属性）と
 * {@code com.nocterra.client.NocterraClient}（レンダラ登録の委譲）も更新すること。</p>
 */
public final class NocterraEntities {
    private NocterraEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Nocterra.MOD_ID);

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}

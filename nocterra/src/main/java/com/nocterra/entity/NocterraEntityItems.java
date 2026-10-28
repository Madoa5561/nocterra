package com.nocterra.entity;

import com.nocterra.Nocterra;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * エンティティ担当が所有するアイテム（検証用スポーンエッグ）。
 *
 * <p>26.3 のバニラ {@link SpawnEggItem} は {@code Item.Properties#spawnEgg(EntityType)} で
 * {@code minecraft:entity_data} コンポーネントを持つ。NeoForge の {@code DeferredSpawnEggItem} は
 * 26.3.0.99 には存在しないため、こちらを使う。</p>
 *
 * <p><b>未対応（Lead / client 担当のスコープ）:</b>
 * <ul>
 *   <li>クリエイティブタブ（{@code com.nocterra.registry.NocterraItems}）への追加</li>
 *   <li>アイテムモデル {@code assets/nocterra/models/item/*_spawn_egg.json}
 *       （現状はモデル未定義のためゲーム内では市松模様で表示される。機能は正常）</li>
 *   <li>{@code lang/en_us.json} / {@code lang/ja_jp.json} への表示名マージ</li>
 * </ul>
 * 現状は {@code /give @s nocterra:ashen_wraith_spawn_egg} で検証可能。</p>
 */
public final class NocterraEntityItems {
    private NocterraEntityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    public static final DeferredItem<Item> ASHEN_WRAITH_SPAWN_EGG = ITEMS.registerItem(
            "ashen_wraith_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ASHEN_WRAITH.get()));

    public static final DeferredItem<Item> ROTWOOD_KING_SPAWN_EGG = ITEMS.registerItem(
            "rotwood_king_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ROTWOOD_KING.get()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

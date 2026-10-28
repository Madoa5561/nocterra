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

    public static final DeferredItem<Item> ASH_CRAWLER_SPAWN_EGG = ITEMS.registerItem(
            "ash_crawler_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ASH_CRAWLER.get()));

    public static final DeferredItem<Item> MOURNING_WISP_SPAWN_EGG = ITEMS.registerItem(
            "mourning_wisp_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.MOURNING_WISP.get()));

    public static final DeferredItem<Item> BOG_GHOUL_SPAWN_EGG = ITEMS.registerItem(
            "bog_ghoul_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.BOG_GHOUL.get()));

    public static final DeferredItem<Item> ROTWOOD_KING_SPAWN_EGG = ITEMS.registerItem(
            "rotwood_king_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ROTWOOD_KING.get()));

    public static final DeferredItem<Item> SORROW_SAINT_SPAWN_EGG = ITEMS.registerItem(
            "sorrow_saint_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.SORROW_SAINT.get()));

    public static final DeferredItem<Item> CONDEMNED_KNIGHT_LORD_SPAWN_EGG = ITEMS.registerItem(
            "condemned_knight_lord_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.CONDEMNED_KNIGHT_LORD.get()));

    public static final DeferredItem<Item> ABYSSAL_SEA_EMPEROR_SPAWN_EGG = ITEMS.registerItem(
            "abyssal_sea_emperor_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ABYSSAL_SEA_EMPEROR.get()));

    public static final DeferredItem<Item> SANDGLASS_TWIN_KINGS_SPAWN_EGG = ITEMS.registerItem(
            "sandglass_twin_kings_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.SANDGLASS_TWIN_KINGS.get()));

    public static final DeferredItem<Item> ASTEL_FALLEN_STAR_DRAGON_SPAWN_EGG = ITEMS.registerItem(
            "astel_fallen_star_dragon_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.ASTEL_FALLEN_STAR_DRAGON.get()));

    public static final DeferredItem<Item> SILVER_EXECUTIONER_SPAWN_EGG = ITEMS.registerItem(
            "silver_executioner_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.SILVER_EXECUTIONER.get()));

    public static final DeferredItem<Item> MOLTEN_STEEL_COLOSSUS_SPAWN_EGG = ITEMS.registerItem(
            "molten_steel_colossus_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.MOLTEN_STEEL_COLOSSUS.get()));

    public static final DeferredItem<Item> BLACK_CROWNED_KING_SPAWN_EGG = ITEMS.registerItem(
            "black_crowned_king_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.BLACK_CROWNED_KING.get()));

    public static final DeferredItem<Item> EMBER_INCARNATION_SPAWN_EGG = ITEMS.registerItem(
            "ember_incarnation_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.EMBER_INCARNATION.get()));

    public static final DeferredItem<Item> FORGOTTEN_KNIGHT_SPAWN_EGG = ITEMS.registerItem(
            "forgotten_knight_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.FORGOTTEN_KNIGHT.get()));

    public static final DeferredItem<Item> NAMELESS_OBSERVER_SPAWN_EGG = ITEMS.registerItem(
            "nameless_observer_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(NocterraEntities.NAMELESS_OBSERVER.get()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

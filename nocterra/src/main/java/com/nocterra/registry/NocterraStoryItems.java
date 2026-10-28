package com.nocterra.registry;

import com.nocterra.Nocterra;
import com.nocterra.item.SigilItem;
import com.nocterra.progression.SigilType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * 物語用アイテム（王の刻印 8 種・探索手帳）。所有: Lead。
 */
public final class NocterraStoryItems {
    private NocterraStoryItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    /** 刻印 → アイテム の対応。 */
    public static final Map<SigilType, DeferredItem<Item>> SIGILS = new EnumMap<>(SigilType.class);

    static {
        for (SigilType type : SigilType.values()) {
            DeferredItem<Item> item = ITEMS.registerItem(
                    "sigil_" + type.sigilKey(),
                    properties -> new SigilItem(type, properties),
                    properties -> properties.stacksTo(1).fireResistant());
            SIGILS.put(type, item);
        }
    }

    /** 探索手帳。 */
    public static final DeferredItem<Item> EXPLORER_JOURNAL = ITEMS.registerItem(
            "explorer_journal",
            com.nocterra.item.ExplorerJournalItem::new,
            properties -> properties.stacksTo(1));

    public static Item sigilItem(SigilType type) {
        return SIGILS.get(type).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

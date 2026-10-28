package com.nocterra.registry;

import com.nocterra.Nocterra;
import com.nocterra.item.EmberShardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NOCTERRA のアイテム登録とクリエイティブタブ。所有: Lead。
 */
public final class NocterraItems {
    private NocterraItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nocterra.MOD_ID);

    /** 灰冠の門を点火する破片。 */
    public static final DeferredItem<Item> EMBER_SHARD = ITEMS.registerItem(
            "ember_shard",
            EmberShardItem::new,
            properties -> properties.stacksTo(16));

    // ---- ブロックアイテム ----
    public static final DeferredItem<BlockItem> GATE_FRAME = ITEMS.registerSimpleBlockItem(NocterraBlocks.GATE_FRAME);
    public static final DeferredItem<BlockItem> ASHWOOD_LOG = ITEMS.registerSimpleBlockItem(NocterraBlocks.ASHWOOD_LOG);
    public static final DeferredItem<BlockItem> ASHWOOD_PLANKS = ITEMS.registerSimpleBlockItem(NocterraBlocks.ASHWOOD_PLANKS);
    public static final DeferredItem<BlockItem> ASHSTONE = ITEMS.registerSimpleBlockItem(NocterraBlocks.ASHSTONE);
    public static final DeferredItem<BlockItem> ASH_COAL_ORE = ITEMS.registerSimpleBlockItem(NocterraBlocks.ASH_COAL_ORE);
    public static final DeferredItem<BlockItem> ROTWOOD_ALTAR = ITEMS.registerSimpleBlockItem(NocterraBlocks.ROTWOOD_ALTAR);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NOCTERRA_TAB = CREATIVE_TABS.register(
            "nocterra",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nocterra"))
                    .icon(() -> new ItemStack(EMBER_SHARD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(EMBER_SHARD.get());
                        output.accept(GATE_FRAME.get());
                        output.accept(ASHWOOD_LOG.get());
                        output.accept(ASHWOOD_PLANKS.get());
                        output.accept(ASHSTONE.get());
                        output.accept(ASH_COAL_ORE.get());
                        output.accept(ROTWOOD_ALTAR.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.ASHEN_WRAITH_SPAWN_EGG.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.ASH_CRAWLER_SPAWN_EGG.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.MOURNING_WISP_SPAWN_EGG.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.BOG_GHOUL_SPAWN_EGG.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.ROTWOOD_KING_SPAWN_EGG.get());
                        output.accept(com.nocterra.entity.NocterraEntityItems.SORROW_SAINT_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}

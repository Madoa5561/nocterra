package com.nocterra.material;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ドロップ素材 6 種。所有: combat 担当。
 *
 * <p>敵ドロップ → 素材 → クラフトの進行ループの「素材」側。
 * 入手元は {@code data/nocterra/loot_table/entities/*.json} に追記している
 * （既存プールは保持したまま pools を追加）。</p>
 *
 * <p>登録は {@link com.nocterra.combat.NocterraCombat#register} から呼ばれる。
 * クリエイティブタブへは {@link #addToCreativeTab} で追加する
 * （{@code NocterraItems} は Lead 所有のため直接編集しない）。</p>
 */
public final class NocterraMaterials {
    private NocterraMaterials() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    /** 灰の繊維: ashen_wraith / ash_crawler から 30〜40%。 */
    public static final DeferredItem<Item> ASHEN_FIBER = ITEMS.registerSimpleItem("ashen_fiber", properties -> properties);

    /** 亡霊の燐粉: mourning_wisp から 35%。 */
    public static final DeferredItem<Item> WISP_DUST = ITEMS.registerSimpleItem("wisp_dust", properties -> properties);

    /** 沼鉄: bog_ghoul から 30%。 */
    public static final DeferredItem<Item> BOG_IRON = ITEMS.registerSimpleItem("bog_iron", properties -> properties);

    /** 砕けた冠片: 全ボスから 100%（1〜2 個）。 */
    public static final DeferredItem<Item> SHATTERED_CROWN =
            ITEMS.registerSimpleItem("shattered_crown", properties -> properties);

    /** 深淵の鱗: abyssal_sea_emperor / sorrow_saint から 100%。 */
    public static final DeferredItem<Item> ABYSSAL_SCALE =
            ITEMS.registerSimpleItem("abyssal_scale", properties -> properties);

    /** 火核: molten_steel_colossus / ember_incarnation から 100%。 */
    public static final DeferredItem<Item> EMBER_CORE = ITEMS.registerSimpleItem("ember_core", properties -> properties);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** 既存の {@code NocterraItems.NOCTERRA_TAB} へ素材を追加する。 */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != NocterraItems.NOCTERRA_TAB.get()) {
            return;
        }
        event.accept(new ItemStack(ASHEN_FIBER.get()));
        event.accept(new ItemStack(WISP_DUST.get()));
        event.accept(new ItemStack(BOG_IRON.get()));
        event.accept(new ItemStack(SHATTERED_CROWN.get()));
        event.accept(new ItemStack(ABYSSAL_SCALE.get()));
        event.accept(new ItemStack(EMBER_CORE.get()));
    }
}

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

    // ---------------------------------------------------------------- 地域固有素材 8 種
    // 地域ボス 100% / その地域の一般モブ 15〜25%。武器・強化レシピで消費する。

    /** 朽葉の樹脂: rotwood_king 100% / ashen_wraith 20%。 */
    public static final DeferredItem<Item> ROTWOOD_RESIN =
            ITEMS.registerSimpleItem("rotwood_resin", properties -> properties);

    /** ベールの絹: sorrow_saint 100% / mourning_wisp 20%。 */
    public static final DeferredItem<Item> VEIL_SILK =
            ITEMS.registerSimpleItem("veil_silk", properties -> properties);

    /** 墓土の塵: condemned_knight_lord 100% / bog_ghoul 25% / ash_crawler 15%。 */
    public static final DeferredItem<Item> GRAVE_DUST =
            ITEMS.registerSimpleItem("grave_dust", properties -> properties);

    /** 黄金の砂: sandglass_twin_kings 100%。 */
    public static final DeferredItem<Item> GOLDEN_SAND =
            ITEMS.registerSimpleItem("golden_sand", properties -> properties);

    /** 潮の真珠: abyssal_sea_emperor 100%。 */
    public static final DeferredItem<Item> TIDAL_PEARL =
            ITEMS.registerSimpleItem("tidal_pearl", properties -> properties);

    /** 星屑の欠片: astel_fallen_star_dragon 100%。 */
    public static final DeferredItem<Item> STAR_SHARD =
            ITEMS.registerSimpleItem("star_shard", properties -> properties);

    /** 霜の結晶: silver_executioner 100%。 */
    public static final DeferredItem<Item> FROST_CRYSTAL =
            ITEMS.registerSimpleItem("frost_crystal", properties -> properties);

    /** 火山の鱗: molten_steel_colossus 100%。 */
    public static final DeferredItem<Item> VOLCANIC_SCALE =
            ITEMS.registerSimpleItem("volcanic_scale", properties -> properties);

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
        event.accept(new ItemStack(ROTWOOD_RESIN.get()));
        event.accept(new ItemStack(VEIL_SILK.get()));
        event.accept(new ItemStack(GRAVE_DUST.get()));
        event.accept(new ItemStack(GOLDEN_SAND.get()));
        event.accept(new ItemStack(TIDAL_PEARL.get()));
        event.accept(new ItemStack(STAR_SHARD.get()));
        event.accept(new ItemStack(FROST_CRYSTAL.get()));
        event.accept(new ItemStack(VOLCANIC_SCALE.get()));
    }
}

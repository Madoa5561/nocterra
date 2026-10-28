package com.nocterra.item;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NOCTERRA 武器の登録。所有: combat 担当。
 *
 * <p>攻撃力は {@code Item.Properties#sword(ToolMaterial, float, float)} で設定する。
 * 26.3 の {@code ToolMaterial} は {@code attackDamageBonus} を持つため、実際の
 * ツールチップ攻撃力は {@code 1（プレイヤー基礎） + baseline + material.attackDamageBonus} になる。
 * ここでは {@link ToolMaterial#IRON}（bonus 2.0）を使い、baseline を調整して攻撃力を表現している。</p>
 *
 * <p>既存 3 種（灰木の刃 6 / 墓所の大剣 11 / 嘆きの大鎌 9）に加え、
 * ドロップ素材から作る 4 種（灰繊維のサーベル 7 / 沼鉄の大剣 12 / 燐粉の短剣 5 / 冠砕き 14）を登録する。
 * レシピは {@code data/nocterra/recipe/<id>.json}。</p>
 */
public final class NocterraWeapons {
    private NocterraWeapons() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    /** 灰木の刃（片手剣）。攻撃力 6 = 1 + 3 + 2。 */
    public static final DeferredItem<AshwoodBladeItem> ASHWOOD_BLADE = ITEMS.registerItem(
            "ashwood_blade",
            AshwoodBladeItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 3.0F, -2.4F));

    /** 墓所の大剣（大剣）。攻撃力 11 = 1 + 8 + 2。 */
    public static final DeferredItem<GraveGreatswordItem> GRAVE_GREATSWORD = ITEMS.registerItem(
            "grave_greatsword",
            GraveGreatswordItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 8.0F, -3.2F));

    /** 嘆きの大鎌（鎌）。攻撃力 9 = 1 + 6 + 2。 */
    public static final DeferredItem<MourningScytheItem> MOURNING_SCYTHE = ITEMS.registerItem(
            "mourning_scythe",
            MourningScytheItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 6.0F, -2.8F));

    // ---------------------------------------------------------------- ドロップ素材から作る武器

    /** 灰繊維のサーベル（片手剣）。攻撃力 7 = 1 + 4 + 2。 */
    public static final DeferredItem<AshenSabreItem> ASHEN_SABRE = ITEMS.registerItem(
            "ashen_sabre",
            AshenSabreItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 4.0F, -2.2F));

    /** 沼鉄の大剣（大剣）。攻撃力 12 = 1 + 9 + 2。 */
    public static final DeferredItem<BogIronGreatswordItem> BOG_IRON_GREATSWORD = ITEMS.registerItem(
            "bog_iron_greatsword",
            BogIronGreatswordItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 9.0F, -3.2F));

    /** 燐粉の短剣（片手剣）。攻撃力 5 = 1 + 2 + 2、攻撃速度は速い。 */
    public static final DeferredItem<WispglassDaggerItem> WISPGLASS_DAGGER = ITEMS.registerItem(
            "wispglass_dagger",
            WispglassDaggerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 2.0F, -1.6F));

    /** 冠砕き（大剣）。攻撃力 14 = 1 + 11 + 2。 */
    public static final DeferredItem<CrownbreakerItem> CROWNBREAKER = ITEMS.registerItem(
            "crownbreaker",
            CrownbreakerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 11.0F, -3.4F));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /**
     * 既存のクリエイティブタブ（{@link NocterraItems#NOCTERRA_TAB}）へ武器を追加する。
     * {@code BuildCreativeModeTabContentsEvent} は mod バスイベントなので
     * {@link com.nocterra.combat.NocterraCombat#register} からリスナ登録する。
     */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != NocterraItems.NOCTERRA_TAB.get()) {
            return;
        }
        event.accept(new ItemStack(ASHWOOD_BLADE.get()));
        event.accept(new ItemStack(GRAVE_GREATSWORD.get()));
        event.accept(new ItemStack(MOURNING_SCYTHE.get()));
        event.accept(new ItemStack(ASHEN_SABRE.get()));
        event.accept(new ItemStack(BOG_IRON_GREATSWORD.get()));
        event.accept(new ItemStack(WISPGLASS_DAGGER.get()));
        event.accept(new ItemStack(CROWNBREAKER.get()));
    }
}

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
 * NOCTERRA 武器 3 種の登録。所有: combat 担当。
 *
 * <p>攻撃力は {@code Item.Properties#sword(ToolMaterial, float, float)} で設定する。
 * 26.3 の {@code ToolMaterial} は {@code attackDamageBonus} を持つため、実際の
 * ツールチップ攻撃力は {@code 1（プレイヤー基礎） + baseline + material.attackDamageBonus} になる。
 * ここでは {@link ToolMaterial#IRON}（bonus 2.0）を使い、baseline を調整して
 * 「片手剣 6 / 大剣 11 / 鎌 9」を表現している。</p>
 *
 * <p>レシピ JSON は Lead がまとめて作成する（data/ は combat 担当の編集スコープ外）。
 * 推奨レシピは報告に含めている。</p>
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
    }
}

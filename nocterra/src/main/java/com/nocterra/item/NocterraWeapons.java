package com.nocterra.item;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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
 * <p>全 17 種:</p>
 * <ul>
 *   <li>基本 3: 灰木の刃 6 / 墓所の大剣 11 / 嘆きの大鎌 9</li>
 *   <li>ドロップ素材 4: 灰繊維のサーベル 7 / 沼鉄の大剣 12 / 燐粉の短剣 5 / 冠砕き 14</li>
 *   <li>地域素材 10: 樹脂の長槍 8 / ベールの杖 4 / 墓土の戦槌 13 / 砂刃 9 / 潮の三叉槍 10 /
 *       星屑の薙刀 15 / 霜の大鎌 12 / 火山の大槌 16 / 白刃の短剣 6 / 冠の王笏 7</li>
 * </ul>
 * <p>レシピは {@code data/nocterra/recipe/<id>.json}。</p>
 */
public final class NocterraWeapons {
    private NocterraWeapons() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    /** 槍のリーチ +1.5 を付与する属性 modifier ID。 */
    public static final Identifier SPEAR_REACH_ID = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "spear_reach");

    // ---------------------------------------------------------------- 基本 3 種

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

    /** 灰繊維のサーベル（片手剣）。攻撃力 7 = 1 + 4 + 2。攻撃速度 1.6。 */
    public static final DeferredItem<AshenSabreItem> ASHEN_SABRE = ITEMS.registerItem(
            "ashen_sabre",
            AshenSabreItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 4.0F, -2.4F));

    /** 沼鉄の大剣（大剣）。攻撃力 12 = 1 + 9 + 2。攻撃速度 1.0。 */
    public static final DeferredItem<BogIronGreatswordItem> BOG_IRON_GREATSWORD = ITEMS.registerItem(
            "bog_iron_greatsword",
            BogIronGreatswordItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 9.0F, -3.0F));

    /** 燐粉の短剣（短剣）。攻撃力 4 = 1 + 1 + 2。攻撃速度 2.4。 */
    public static final DeferredItem<WispglassDaggerItem> WISPGLASS_DAGGER = ITEMS.registerItem(
            "wispglass_dagger",
            WispglassDaggerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 1.0F, -1.6F));

    /** 冠砕き（大剣）。攻撃力 14 = 1 + 11 + 2。攻撃速度 1.0。 */
    public static final DeferredItem<CrownbreakerItem> CROWNBREAKER = ITEMS.registerItem(
            "crownbreaker",
            CrownbreakerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 11.0F, -3.0F));

    // ---------------------------------------------------------------- 地域素材 10 種

    /** 樹脂の長槍（槍）。攻撃力 8 = 1 + 7。リーチ +1.5（ENTITY_INTERACTION_RANGE）。 */
    public static final DeferredItem<ResinSpearItem> RESIN_SPEAR = ITEMS.registerItem(
            "resin_spear",
            ResinSpearItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 5.0F, -2.6F)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, spearAttributes()));

    /** ベールの杖（杖）。攻撃力 4 = 1 + 3。 */
    public static final DeferredItem<VeilWandItem> VEIL_WAND = ITEMS.registerItem(
            "veil_wand",
            VeilWandItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 1.0F, -2.0F));

    /** 墓土の戦槌（槌）。攻撃力 13 = 1 + 12。攻撃速度は遅い。 */
    public static final DeferredItem<GraveHammerItem> GRAVE_HAMMER = ITEMS.registerItem(
            "grave_hammer",
            GraveHammerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 10.0F, -3.6F));

    /** 砂刃（曲刀）。攻撃力 9 = 1 + 8。攻撃速度 +0.4（baseline -2.0）。 */
    public static final DeferredItem<SandglassBladeItem> SANDGLASS_BLADE = ITEMS.registerItem(
            "sandglass_blade",
            SandglassBladeItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 6.0F, -2.0F));

    /** 潮の三叉槍（三叉槍）。攻撃力 10 = 1 + 9。 */
    public static final DeferredItem<TidalTridentItem> TIDAL_TRIDENT = ITEMS.registerItem(
            "tidal_trident",
            TidalTridentItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 7.0F, -2.8F));

    /** 星屑の薙刀（薙刀）。攻撃力 15 = 1 + 14。 */
    public static final DeferredItem<StarfallGlaiveItem> STARFALL_GLAIVE = ITEMS.registerItem(
            "starfall_glaive",
            StarfallGlaiveItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 12.0F, -3.2F));

    /** 霜の大鎌（大鎌）。攻撃力 12 = 1 + 11。 */
    public static final DeferredItem<FrostScytheItem> FROST_SCYTHE = ITEMS.registerItem(
            "frost_scythe",
            FrostScytheItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 9.0F, -2.8F));

    /** 火山の大槌（大槌）。攻撃力 16 = 1 + 15。 */
    public static final DeferredItem<VolcanicMaulItem> VOLCANIC_MAUL = ITEMS.registerItem(
            "volcanic_maul",
            VolcanicMaulItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 13.0F, -3.8F));

    /** 白刃の短剣（短剣）。攻撃力 6 = 1 + 5。攻撃速度が速い。 */
    public static final DeferredItem<PaleDaggerItem> PALE_DAGGER = ITEMS.registerItem(
            "pale_dagger",
            PaleDaggerItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 3.0F, -1.4F));

    /** 冠の王笏（王笏）。攻撃力 7 = 1 + 6。 */
    public static final DeferredItem<CrownScepterItem> CROWN_SCEPTER = ITEMS.registerItem(
            "crown_scepter",
            CrownScepterItem::new,
            properties -> properties.sword(ToolMaterial.IRON, 4.0F, -2.4F));

    /**
     * 槍用の属性。{@code .sword(...)} が付ける属性を上書きし、
     * 攻撃力 7 / 攻撃速度 -2.6 / 攻撃間合い +1.5 をまとめて設定する。
     */
    private static ItemAttributeModifiers spearAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 7.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.6, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(SPEAR_REACH_ID, 1.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

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
        // 基本 3
        event.accept(new ItemStack(ASHWOOD_BLADE.get()));
        event.accept(new ItemStack(GRAVE_GREATSWORD.get()));
        event.accept(new ItemStack(MOURNING_SCYTHE.get()));
        // ドロップ素材 4
        event.accept(new ItemStack(ASHEN_SABRE.get()));
        event.accept(new ItemStack(BOG_IRON_GREATSWORD.get()));
        event.accept(new ItemStack(WISPGLASS_DAGGER.get()));
        event.accept(new ItemStack(CROWNBREAKER.get()));
        // 地域素材 10
        event.accept(new ItemStack(RESIN_SPEAR.get()));
        event.accept(new ItemStack(VEIL_WAND.get()));
        event.accept(new ItemStack(GRAVE_HAMMER.get()));
        event.accept(new ItemStack(SANDGLASS_BLADE.get()));
        event.accept(new ItemStack(TIDAL_TRIDENT.get()));
        event.accept(new ItemStack(STARFALL_GLAIVE.get()));
        event.accept(new ItemStack(FROST_SCYTHE.get()));
        event.accept(new ItemStack(VOLCANIC_MAUL.get()));
        event.accept(new ItemStack(PALE_DAGGER.get()));
        event.accept(new ItemStack(CROWN_SCEPTER.get()));
    }
}

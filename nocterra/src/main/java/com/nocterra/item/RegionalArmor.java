package com.nocterra.item;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraItems;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 地域素材の防具 2 セット。所有: combat 担当。
 *
 * <ul>
 *   <li><b>樹脂装備</b>（森 / 軽装）: 防御 頭1 胴3 脚2 足1（合計7）、耐久係数 8、エンチャント 18。</li>
 *   <li><b>霜装備</b>（氷 / 重装）: 防御 頭2 胴6 脚5 足2（合計15）、耐久係数 20、toughness 1.0、
 *       knockback resistance 0.05、エンチャント 8。</li>
 * </ul>
 *
 * <p>26.3 の {@code ArmorMaterial} は plain record（登録不要）。装備アセットはクライアントリソース
 * {@code assets/nocterra/equipment/<name>.json} + {@code textures/entity/equipment/humanoid{,_leggings}/}。</p>
 */
public final class RegionalArmor {
    private RegionalArmor() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    // ---------------------------------------------------------------- 樹脂装備（軽装）

    /** 樹脂装備の修理素材タグ（Lead が data/nocterra/tags/item/repairs_resin_armor.json を作成済み）。 */
    public static final TagKey<Item> REPAIRS_RESIN_ARMOR =
            ItemTags.create(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "repairs_resin_armor"));

    public static final ResourceKey<EquipmentAsset> RESIN_ARMOR_ASSET = asset("resin_armor");

    public static final ArmorMaterial RESIN_ARMOR_MATERIAL = new ArmorMaterial(
            8,
            Map.of(
                    ArmorType.HELMET, 1,
                    ArmorType.CHESTPLATE, 3,
                    ArmorType.LEGGINGS, 2,
                    ArmorType.BOOTS, 1,
                    ArmorType.BODY, 3),
            18,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0F,
            0.0F,
            REPAIRS_RESIN_ARMOR,
            RESIN_ARMOR_ASSET);

    public static final DeferredItem<Item> RESIN_HELMET = ITEMS.registerItem(
            "resin_helmet", Item::new,
            properties -> properties.humanoidArmor(RESIN_ARMOR_MATERIAL, ArmorType.HELMET));

    public static final DeferredItem<Item> RESIN_CHESTPLATE = ITEMS.registerItem(
            "resin_chestplate", Item::new,
            properties -> properties.humanoidArmor(RESIN_ARMOR_MATERIAL, ArmorType.CHESTPLATE));

    public static final DeferredItem<Item> RESIN_LEGGINGS = ITEMS.registerItem(
            "resin_leggings", Item::new,
            properties -> properties.humanoidArmor(RESIN_ARMOR_MATERIAL, ArmorType.LEGGINGS));

    public static final DeferredItem<Item> RESIN_BOOTS = ITEMS.registerItem(
            "resin_boots", Item::new,
            properties -> properties.humanoidArmor(RESIN_ARMOR_MATERIAL, ArmorType.BOOTS));

    // ---------------------------------------------------------------- 霜装備（重装）

    /** 霜装備の修理素材タグ（Lead が data/nocterra/tags/item/repairs_frost_armor.json を作成済み）。 */
    public static final TagKey<Item> REPAIRS_FROST_ARMOR =
            ItemTags.create(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "repairs_frost_armor"));

    public static final ResourceKey<EquipmentAsset> FROST_ARMOR_ASSET = asset("frost_armor");

    public static final ArmorMaterial FROST_ARMOR_MATERIAL = new ArmorMaterial(
            20,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2,
                    ArmorType.BODY, 6),
            8,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.0F,
            0.05F,
            REPAIRS_FROST_ARMOR,
            FROST_ARMOR_ASSET);

    public static final DeferredItem<Item> FROST_HELMET = ITEMS.registerItem(
            "frost_helmet", Item::new,
            properties -> properties.humanoidArmor(FROST_ARMOR_MATERIAL, ArmorType.HELMET));

    public static final DeferredItem<Item> FROST_CHESTPLATE = ITEMS.registerItem(
            "frost_chestplate", Item::new,
            properties -> properties.humanoidArmor(FROST_ARMOR_MATERIAL, ArmorType.CHESTPLATE));

    public static final DeferredItem<Item> FROST_LEGGINGS = ITEMS.registerItem(
            "frost_leggings", Item::new,
            properties -> properties.humanoidArmor(FROST_ARMOR_MATERIAL, ArmorType.LEGGINGS));

    public static final DeferredItem<Item> FROST_BOOTS = ITEMS.registerItem(
            "frost_boots", Item::new,
            properties -> properties.humanoidArmor(FROST_ARMOR_MATERIAL, ArmorType.BOOTS));

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, name));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** 既存の {@code NocterraItems.NOCTERRA_TAB} へ防具 8 種を追加する。 */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != NocterraItems.NOCTERRA_TAB.get()) {
            return;
        }
        event.accept(new ItemStack(RESIN_HELMET.get()));
        event.accept(new ItemStack(RESIN_CHESTPLATE.get()));
        event.accept(new ItemStack(RESIN_LEGGINGS.get()));
        event.accept(new ItemStack(RESIN_BOOTS.get()));
        event.accept(new ItemStack(FROST_HELMET.get()));
        event.accept(new ItemStack(FROST_CHESTPLATE.get()));
        event.accept(new ItemStack(FROST_LEGGINGS.get()));
        event.accept(new ItemStack(FROST_BOOTS.get()));
    }
}

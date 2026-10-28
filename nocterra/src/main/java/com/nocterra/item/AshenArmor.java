package com.nocterra.item;

import com.nocterra.Nocterra;
import com.nocterra.registry.NocterraItems;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
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
 * 灰装備セット（頭・胴・脚・足）。所有: combat 担当。
 *
 * <p>26.3 の {@code ArmorMaterial} は <b>レジストリではなく plain record</b> なので、
 * {@code new ArmorMaterial(...)} するだけでよい（登録不要）。装備の見た目は
 * {@link EquipmentAsset} の {@link ResourceKey} で参照し、実体はクライアントリソース
 * {@code assets/nocterra/equipment/ashen_armor.json} と
 * {@code assets/nocterra/textures/entity/equipment/humanoid/...png}（64x32）。
 * テクスチャ解決は {@code EquipmentClientInfo.Layer#getTextureLocation} が
 * {@code textures/entity/equipment/<layerType>/<texture>.png} を組み立てる（実ソース確認済み）。</p>
 *
 * <p>防御力は「革と鉄の中間」（頭2 / 胴4 / 脚3 / 足1、合計10）。
 * 革: 1/3/2/1、鉄: 2/6/5/2 の中間値。耐久係数 12（革5 / 鉄15 の中間）。</p>
 */
public final class AshenArmor {
    private AshenArmor() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nocterra.MOD_ID);

    /** 装備アセット ID。クライアント側 JSON: {@code assets/nocterra/equipment/ashen_armor.json}。 */
    public static final ResourceKey<EquipmentAsset> ASHEN_ARMOR_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "ashen_armor"));

    /** 灰装備の素材定義。登録不要の plain record。 */
    public static final ArmorMaterial ASHEN_ARMOR_MATERIAL = new ArmorMaterial(
            12,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 4,
                    ArmorType.LEGGINGS, 3,
                    ArmorType.BOOTS, 1,
                    ArmorType.BODY, 4),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0F,
            0.0F,
            // 専用タグは data/nocterra/tags の作成が必要なため、既存タグで代用（修理素材は革）。
            ItemTags.REPAIRS_LEATHER_ARMOR,
            ASHEN_ARMOR_ASSET);

    public static final DeferredItem<Item> ASHEN_HELMET = ITEMS.registerItem(
            "ashen_helmet",
            Item::new,
            properties -> properties.humanoidArmor(ASHEN_ARMOR_MATERIAL, ArmorType.HELMET));

    public static final DeferredItem<Item> ASHEN_CHESTPLATE = ITEMS.registerItem(
            "ashen_chestplate",
            Item::new,
            properties -> properties.humanoidArmor(ASHEN_ARMOR_MATERIAL, ArmorType.CHESTPLATE));

    public static final DeferredItem<Item> ASHEN_LEGGINGS = ITEMS.registerItem(
            "ashen_leggings",
            Item::new,
            properties -> properties.humanoidArmor(ASHEN_ARMOR_MATERIAL, ArmorType.LEGGINGS));

    public static final DeferredItem<Item> ASHEN_BOOTS = ITEMS.registerItem(
            "ashen_boots",
            Item::new,
            properties -> properties.humanoidArmor(ASHEN_ARMOR_MATERIAL, ArmorType.BOOTS));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** 既存の {@code NocterraItems.NOCTERRA_TAB} へ防具を追加する。 */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != NocterraItems.NOCTERRA_TAB.get()) {
            return;
        }
        event.accept(new ItemStack(ASHEN_HELMET.get()));
        event.accept(new ItemStack(ASHEN_CHESTPLATE.get()));
        event.accept(new ItemStack(ASHEN_LEGGINGS.get()));
        event.accept(new ItemStack(ASHEN_BOOTS.get()));
    }
}

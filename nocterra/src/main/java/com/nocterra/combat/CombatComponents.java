package com.nocterra.combat;

import com.mojang.serialization.Codec;
import com.nocterra.Nocterra;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 武器用 Data Component の登録。
 *
 * <p>26.3 では {@code DataComponentType.builder()} が NeoForge のアクセストランスフォーマで
 * public になっており、{@code DeferredRegister.DataComponents#registerComponentType} が使える
 * （実測: {@code javap net.minecraft.core.component.DataComponentType} に public な builder() を確認）。</p>
 */
public final class CombatComponents {
    private CombatComponents() {}

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nocterra.MOD_ID);

    /** 強化段階（+0〜+5）。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENHANCEMENT =
            DATA_COMPONENTS.registerComponentType("enhancement", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 武器属性。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponElement>> ELEMENT =
            DATA_COMPONENTS.registerComponentType("weapon_element", builder -> builder
                    .persistent(WeaponElement.CODEC)
                    .networkSynchronized(WeaponElement.STREAM_CODEC));

    /** 強化段階（未設定は +0）。 */
    public static int enhancement(ItemStack stack) {
        Integer value = stack.get(ENHANCEMENT.get());
        return value == null ? 0 : Math.max(0, Math.min(5, value));
    }

    /** 属性（未設定は武器クラスの既定値）。 */
    public static WeaponElement element(ItemStack stack) {
        WeaponElement value = stack.get(ELEMENT.get());
        if (value != null) {
            return value;
        }
        return stack.getItem() instanceof CombatWeapon weapon ? weapon.defaultElement() : WeaponElement.NONE;
    }

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}

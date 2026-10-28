package com.nocterra.world;

import com.nocterra.Nocterra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * NOCTERRA のディメンションキー。所有: Lead。
 *
 * <p>実体は {@code data/nocterra/dimension/nocterra.json} と
 * {@code data/nocterra/dimension_type/nocterra.json}（データパック側）。</p>
 */
public final class NocterraDimensions {
    private NocterraDimensions() {}

    public static final ResourceKey<Level> NOCTERRA = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "nocterra"));

    public static final ResourceKey<DimensionType> NOCTERRA_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "nocterra"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, path);
    }
}

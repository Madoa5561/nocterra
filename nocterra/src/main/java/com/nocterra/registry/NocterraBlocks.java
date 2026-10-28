package com.nocterra.registry;

import com.nocterra.Nocterra;
import com.nocterra.world.portal.AshenPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NOCTERRA のブロック登録。所有: Lead。
 * 追加する場合はここに register を足し、対応するブロックアイテムを {@link NocterraItems} に登録する。
 */
public final class NocterraBlocks {
    private NocterraBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nocterra.MOD_ID);

    // ---- 古代の門 ----
    /** 灰冠の門の骨組み。 */
    public static final DeferredBlock<Block> GATE_FRAME = BLOCKS.registerSimpleBlock(
            "gate_frame",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(6.0F, 12.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops());

    /** ノクテラへの通過面。アイテム化しない。 */
    public static final DeferredBlock<AshenPortalBlock> ASHEN_PORTAL = BLOCKS.registerBlock(
            "ashen_portal",
            AshenPortalBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollision()
                    .lightLevel(state -> 11)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .randomTicks()
                    .sound(SoundType.GLASS));

    // ---- 灰樹の森の建材 ----
    public static final DeferredBlock<Block> ASHWOOD_LOG = BLOCKS.registerSimpleBlock(
            "ashwood_log",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 4.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> ASHWOOD_PLANKS = BLOCKS.registerSimpleBlock(
            "ashwood_planks",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> ASHSTONE = BLOCKS.registerSimpleBlock(
            "ashstone",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    /** 灰煤鉱石。エンバーシャードの素材。 */
    public static final DeferredBlock<Block> ASH_COAL_ORE = BLOCKS.registerSimpleBlock(
            "ash_coal_ore",
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}

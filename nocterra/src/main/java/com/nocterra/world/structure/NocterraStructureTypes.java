package com.nocterra.world.structure;

import com.nocterra.Nocterra;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * カスタム {@code Structure} の型と、{@code StructurePiece} の型を登録する。
 *
 * <p>{@code Registries.STRUCTURE_TYPE}（worldgen/structure_type）と
 * {@code Registries.STRUCTURE_PIECE}（worldgen/structure_piece）はいずれも
 * バニラの built-in レジストリなので、{@link RegisterEvent} で登録する。
 * レジストリごとにイベントが飛ぶため、フラグもレジストリ別に持つ。</p>
 */
@EventBusSubscriber(modid = Nocterra.MOD_ID)
public final class NocterraStructureTypes {
    private NocterraStructureTypes() {}

    public static final Identifier FALLEN_FORTRESS = id("fallen_fortress");
    public static final Identifier MOURNING_CATACOMB = id("mourning_catacomb");
    public static final Identifier FALLEN_FORTRESS_PIECE = id("fallen_fortress_piece");
    public static final Identifier MOURNING_CATACOMB_PIECE = id("mourning_catacomb_piece");

    private static final AtomicBoolean TYPES_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean PIECES_REGISTERED = new AtomicBoolean(false);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, path);
    }

    /** 明示登録用フォールバック（{@code Nocterra} から呼んでも可。冪等）。 */
    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(NocterraStructureTypes::onRegister);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistry(Registries.STRUCTURE_TYPE) != null && TYPES_REGISTERED.compareAndSet(false, true)) {
            event.register(Registries.STRUCTURE_TYPE, FALLEN_FORTRESS, () -> FallenFortressStructure.TYPE);
            event.register(Registries.STRUCTURE_TYPE, MOURNING_CATACOMB, () -> MourningCatacombStructure.TYPE);
        }
        if (event.getRegistry(Registries.STRUCTURE_PIECE) != null && PIECES_REGISTERED.compareAndSet(false, true)) {
            event.register(Registries.STRUCTURE_PIECE, FALLEN_FORTRESS_PIECE, () -> FallenFortressPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, MOURNING_CATACOMB_PIECE, () -> MourningCatacombPiece.TYPE);
        }
    }
}

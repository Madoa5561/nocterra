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
    public static final Identifier BLACK_CROWN_CASTLE = id("black_crown_castle");
    public static final Identifier BURIED_KEEP = id("buried_keep");
    public static final Identifier NETHER_BASTION = id("nether_bastion");
    public static final Identifier END_CITADEL = id("end_citadel");
    public static final Identifier FALLEN_FORTRESS_PIECE = id("fallen_fortress_piece");
    public static final Identifier MOURNING_CATACOMB_PIECE = id("mourning_catacomb_piece");
    public static final Identifier BLACK_CROWN_CASTLE_PIECE = id("black_crown_castle_piece");
    public static final Identifier BURIED_KEEP_PIECE = id("buried_keep_piece");
    public static final Identifier NETHER_BASTION_PIECE = id("nether_bastion_piece");
    public static final Identifier END_CITADEL_PIECE = id("end_citadel_piece");
    public static final Identifier GRAND_CASTLE = id("grand_castle");
    public static final Identifier GRAND_CASTLE_PIECE = id("grand_castle_piece");

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
            event.register(Registries.STRUCTURE_TYPE, BLACK_CROWN_CASTLE, () -> BlackCrownCastleStructure.TYPE);
            event.register(Registries.STRUCTURE_TYPE, BURIED_KEEP, () -> BuriedKeepStructure.TYPE);
            event.register(Registries.STRUCTURE_TYPE, NETHER_BASTION, () -> NetherBastionStructure.TYPE);
            event.register(Registries.STRUCTURE_TYPE, END_CITADEL, () -> EndCitadelStructure.TYPE);
            event.register(Registries.STRUCTURE_TYPE, GRAND_CASTLE, () -> GrandCastleStructure.TYPE);
        }
        if (event.getRegistry(Registries.STRUCTURE_PIECE) != null && PIECES_REGISTERED.compareAndSet(false, true)) {
            event.register(Registries.STRUCTURE_PIECE, FALLEN_FORTRESS_PIECE, () -> FallenFortressPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, MOURNING_CATACOMB_PIECE, () -> MourningCatacombPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, BLACK_CROWN_CASTLE_PIECE, () -> BlackCrownCastlePiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, BURIED_KEEP_PIECE, () -> BuriedKeepPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, NETHER_BASTION_PIECE, () -> NetherBastionPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, END_CITADEL_PIECE, () -> EndCitadelPiece.TYPE);
            event.register(Registries.STRUCTURE_PIECE, GRAND_CASTLE_PIECE, () -> GrandCastlePiece.TYPE);
        }
    }
}

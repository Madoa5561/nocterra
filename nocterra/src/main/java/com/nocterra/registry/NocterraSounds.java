package com.nocterra.registry;

import com.nocterra.Nocterra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 12 ボス分の音声（Fish Audio で生成した OGG）を SoundEvent として登録する。
 * 所有: Lead。音声ファイルは {@code assets/nocterra/sounds/boss/<boss_id>_<1|2>.ogg}。
 *
 * <p>{@code assets/nocterra/sounds.json} のキーと 1:1 で対応する。</p>
 */
public final class NocterraSounds {
    private NocterraSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Nocterra.MOD_ID);

    /** ボスID → {voice1, voice2} の SoundEvent。 */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>[]> BOSS_VOICES = new LinkedHashMap<>();

    public static final String[] BOSS_IDS = {
            "rotwood_king", "sorrow_saint", "condemned_knight_lord", "sandglass_twin_kings",
            "abyssal_sea_emperor", "astel_fallen_star_dragon", "silver_executioner",
            "molten_steel_colossus", "black_crowned_king", "ember_incarnation",
            "forgotten_knight", "nameless_observer"
    };

    @SuppressWarnings("unchecked")
    private static final DeferredHolder<SoundEvent, SoundEvent>[][] REGISTERED = new DeferredHolder[BOSS_IDS.length][];

    static {
        for (int i = 0; i < BOSS_IDS.length; i++) {
            String id = BOSS_IDS[i];
            DeferredHolder<SoundEvent, SoundEvent>[] pair = new DeferredHolder[2];
            for (int v = 1; v <= 2; v++) {
                final String path = "boss/" + id + "_" + v;
                // SoundEvent の ID は sounds.json のキーと 1:1（nocterra:boss_<id>_voice<v>）
                pair[v - 1] = SOUNDS.register("boss_" + id + "_voice" + v,
                        () -> SoundEvent.createVariableRangeEvent(
                                Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, path)));
            }
            REGISTERED[i] = pair;
            BOSS_VOICES.put(id, pair);
        }
    }

    /** ボス ID に対応するボイスを返す（未知 ID なら null）。 */
    public static SoundEvent voice(String bossId, int variant) {
        for (int i = 0; i < BOSS_IDS.length; i++) {
            if (BOSS_IDS[i].equals(bossId)) {
                int idx = Math.max(0, Math.min(1, variant - 1));
                return REGISTERED[i][idx].get();
            }
        }
        return null;
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}

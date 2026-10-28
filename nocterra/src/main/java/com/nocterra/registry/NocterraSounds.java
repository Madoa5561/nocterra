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
                // 重要: SoundEvent の location() は登録キーと一致させる必要がある。
                // SoundEngine は sound.location() で sounds.json のエントリを引くため、
                // ここがずれると "Missing sound for event" になる。
                final String key = "boss_" + id + "_voice" + v;
                final Identifier location = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, key);
                pair[v - 1] = SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(location));
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

    // ------------------------------------------------------------------ music

    /** 音楽トラック名（sounds.json のキー `music.nocterra.<name>` に対応）。 */
    public static final String[] MUSIC_TRACKS = {
            "boss_ash", "boss_void", "boss_iron", "ambient_nocterra"
    };

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MUSIC =
            new LinkedHashMap<>();

    static {
        for (String track : MUSIC_TRACKS) {
            final String key = "music.nocterra." + track;
            final Identifier location = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, key);
            // BGM は広い範囲に聞かせたいので固定レンジ（256 ブロック）を使う
            MUSIC.put(track, SOUNDS.register(key,
                    () -> SoundEvent.createFixedRangeEvent(location, 256.0F)));
        }
    }

    /** 音楽トラックの SoundEvent（未知なら null）。 */
    public static SoundEvent music(String track) {
        DeferredHolder<SoundEvent, SoundEvent> holder = MUSIC.get(track);
        return holder == null ? null : holder.get();
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}

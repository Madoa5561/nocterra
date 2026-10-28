package com.nocterra.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * プレイヤーごとの物語の進行データ。所有: Lead（サーバー権威）。
 *
 * <p>刻印はビットマスクで保存する（8 個までは int で足りる）。
 * 撃破ボス・発見 NPC・発見地点・クエスト完了・エンディング閲覧を記録する。</p>
 */
public final class ProgressionData {
    /** 刻印マスク（{@link SigilType#bit()} の OR）。 */
    private int sigilMask;
    private final Set<String> defeatedBosses = new LinkedHashSet<>();
    private final Set<String> discoveredNpcs = new LinkedHashSet<>();
    private final Set<String> discoveredPois = new LinkedHashSet<>();
    private final Set<String> completedQuests = new LinkedHashSet<>();
    private boolean normalEndingSeen;
    private boolean trueEndingSeen;

    public ProgressionData() {}

    // ------------------------------------------------------------------ codec

    public static final com.mojang.serialization.MapCodec<ProgressionData> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("sigils", 0).forGetter(ProgressionData::sigilMask),
            Codec.STRING.listOf().optionalFieldOf("bosses", List.of()).forGetter(d -> List.copyOf(d.defeatedBosses)),
            Codec.STRING.listOf().optionalFieldOf("npcs", List.of()).forGetter(d -> List.copyOf(d.discoveredNpcs)),
            Codec.STRING.listOf().optionalFieldOf("pois", List.of()).forGetter(d -> List.copyOf(d.discoveredPois)),
            Codec.STRING.listOf().optionalFieldOf("quests", List.of()).forGetter(d -> List.copyOf(d.completedQuests)),
            Codec.BOOL.optionalFieldOf("normalEnding", false).forGetter(ProgressionData::normalEndingSeen),
            Codec.BOOL.optionalFieldOf("trueEnding", false).forGetter(ProgressionData::trueEndingSeen)
    ).apply(instance, ProgressionData::fromCodec));

    public static final Codec<ProgressionData> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ProgressionData::sigilMask,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), d -> new ArrayList<>(d.defeatedBosses),
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), d -> new ArrayList<>(d.discoveredNpcs),
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), d -> new ArrayList<>(d.discoveredPois),
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), d -> new ArrayList<>(d.completedQuests),
            ByteBufCodecs.BOOL, ProgressionData::normalEndingSeen,
            ByteBufCodecs.BOOL, ProgressionData::trueEndingSeen,
            ProgressionData::new
    );

    private static ProgressionData fromCodec(int sigils, List<String> bosses, List<String> npcs,
                                             List<String> pois, List<String> quests,
                                             boolean normalEnding, boolean trueEnding) {
        ProgressionData data = new ProgressionData();
        data.sigilMask = sigils;
        data.defeatedBosses.addAll(bosses);
        data.discoveredNpcs.addAll(npcs);
        data.discoveredPois.addAll(pois);
        data.completedQuests.addAll(quests);
        data.normalEndingSeen = normalEnding;
        data.trueEndingSeen = trueEnding;
        return data;
    }

    private ProgressionData(int sigils, List<String> bosses, List<String> npcs, List<String> pois,
                            List<String> quests, boolean normalEnding, boolean trueEnding) {
        this.sigilMask = sigils;
        this.defeatedBosses.addAll(bosses);
        this.discoveredNpcs.addAll(npcs);
        this.discoveredPois.addAll(pois);
        this.completedQuests.addAll(quests);
        this.normalEndingSeen = normalEnding;
        this.trueEndingSeen = trueEnding;
    }

    // ------------------------------------------------------------------ state

    public int sigilMask() {
        return this.sigilMask;
    }

    public int sigilCount() {
        return Integer.bitCount(this.sigilMask);
    }

    public boolean hasSigil(SigilType type) {
        return (this.sigilMask & type.bit()) != 0;
    }

    /** @return 新規に記録した場合 true */
    public boolean addSigil(SigilType type) {
        if (hasSigil(type)) {
            return false;
        }
        this.sigilMask |= type.bit();
        return true;
    }

    public Set<String> defeatedBosses() {
        return this.defeatedBosses;
    }

    public void addDefeatedBoss(String bossId) {
        this.defeatedBosses.add(bossId);
        SigilType type = SigilType.byBossId(bossId);
        if (type != null) {
            addSigil(type);
        }
    }

    public Set<String> discoveredNpcs() {
        return this.discoveredNpcs;
    }

    public boolean addDiscoveredNpc(String npcId) {
        return this.discoveredNpcs.add(npcId);
    }

    public Set<String> discoveredPois() {
        return this.discoveredPois;
    }

    public boolean addDiscoveredPoi(String poiId) {
        return this.discoveredPois.add(poiId);
    }

    public Set<String> completedQuests() {
        return this.completedQuests;
    }

    public boolean addCompletedQuest(String questId) {
        return this.completedQuests.add(questId);
    }

    public boolean normalEndingSeen() {
        return this.normalEndingSeen;
    }

    public boolean trueEndingSeen() {
        return this.trueEndingSeen;
    }

    public void markNormalEnding() {
        this.normalEndingSeen = true;
    }

    public void markTrueEnding() {
        this.trueEndingSeen = true;
        this.normalEndingSeen = true;
    }

    /** 真エンディング解放条件（刻印 8 個すべて）。 */
    public boolean canTrueEnding() {
        return Integer.bitCount(this.sigilMask) >= SigilType.values().length;
    }

    /** 通常エンディング解放条件（刻印 5 個以上）。 */
    public boolean canNormalEnding() {
        return Integer.bitCount(this.sigilMask) >= 5;
    }
}

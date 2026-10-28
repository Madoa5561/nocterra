package com.nocterra.core;

import com.nocterra.registry.NocterraSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * ボス出現時に専用のボイス（Fish Audio で生成した OGG）を鳴らす。所有: Lead。
 *
 * <p>エンティティ型の名前空間が {@code nocterra} で、{@link NocterraSounds#BOSS_IDS} に含まれる
 * 場合のみ再生する（一般モブは対象外）。サーバー側でのみ実行される。</p>
 */
public final class NocterraBossVoice {
    private NocterraBossVoice() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide() || event.loadedFromDisk()) {
            return;
        }
        Entity entity = event.getEntity();
        Identifier typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null || !"nocterra".equals(typeId.getNamespace())) {
            return;
        }
        String bossId = typeId.getPath();
        boolean isBoss = false;
        for (String id : NocterraSounds.BOSS_IDS) {
            if (id.equals(bossId)) {
                isBoss = true;
                break;
            }
        }
        if (!isBoss) {
            return;
        }
        SoundEvent voice = NocterraSounds.voice(bossId, 1);
        if (voice == null) {
            return;
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                voice, SoundSource.HOSTILE, 1.4F, 1.0F);
    }
}

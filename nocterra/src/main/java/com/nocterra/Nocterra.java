package com.nocterra;

import com.nocterra.client.NocterraClient;
import com.nocterra.combat.NocterraCombat;
import com.nocterra.entity.NocterraEntities;
import com.nocterra.networking.NocterraNetwork;
import com.nocterra.registry.NocterraBlocks;
import com.nocterra.registry.NocterraItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOCTERRA — 灰冠の残響
 * Minecraft 26.3 / NeoForge 26.3 / Java 25
 *
 * <p>レジストリの集約点。各サブシステムは自身の {@code register(IEventBus)} を持ち、
 * ここから呼び出される（DeferredRegister の生成は各サブシステム側で行う）。</p>
 */
@Mod(Nocterra.MOD_ID)
public final class Nocterra {
    public static final String MOD_ID = "nocterra";
    public static final Logger LOG = LoggerFactory.getLogger("NOCTERRA");

    public Nocterra(IEventBus modBus, Dist dist, ModContainer container) {
        long startedAt = System.nanoTime();

        NocterraBlocks.register(modBus);
        NocterraItems.register(modBus);
        NocterraEntities.register(modBus);
        NocterraCombat.register(modBus);
        NocterraNetwork.register(modBus);
        com.nocterra.registry.NocterraSounds.register(modBus);

        if (dist.isClient()) {
            NocterraClient.initialize(modBus);
        }

        // ゲームバス（サーバー／クライアント共通）
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.nocterra.core.NocterraCommands::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.nocterra.core.NocterraBossVoice::onEntityJoin);

        LOG.info("NOCTERRA bootstrap complete in {} ms (mod id={}, side={})",
                (System.nanoTime() - startedAt) / 1_000_000, MOD_ID, dist);
    }
}

package com.nocterra;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOCTERRA - 灰冠の残響
 * Minecraft 26.3 / NeoForge 26.3 / Java 25
 */
@Mod(Nocterra.MOD_ID)
public final class Nocterra {
    public static final String MOD_ID = "nocterra";
    public static final Logger LOG = LoggerFactory.getLogger("NOCTERRA");

    public Nocterra() {
        LOG.info("NOCTERRA bootstrap: loaded mod id={}", MOD_ID);
    }
}

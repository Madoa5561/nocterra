package com.nocterra.client;

import com.nocterra.client.render.AbyssalSeaEmperorRenderer;
import com.nocterra.client.render.AshCrawlerRenderer;
import com.nocterra.client.render.AshenWraithRenderer;
import com.nocterra.client.render.AstelFallenStarDragonRenderer;
import com.nocterra.client.render.BlackCrownedKingRenderer;
import com.nocterra.client.render.BogGhoulRenderer;
import com.nocterra.client.render.CondemnedKnightLordRenderer;
import com.nocterra.client.render.EmberIncarnationRenderer;
import com.nocterra.client.render.ForgottenKnightRenderer;
import com.nocterra.client.render.MoltenSteelColossusRenderer;
import com.nocterra.client.render.MourningWispRenderer;
import com.nocterra.client.render.NamelessObserverRenderer;
import com.nocterra.client.render.NpcRenderer;
import com.nocterra.client.render.RotwoodKingRenderer;
import com.nocterra.client.render.SandglassTwinKingsRenderer;
import com.nocterra.client.render.SilverExecutionerRenderer;
import com.nocterra.client.render.SorrowSaintRenderer;
import com.nocterra.entity.NocterraEntities;
import com.nocterra.npc.NocterraNpcs;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * エンティティ／ブロックエンティティのレンダラ登録。所有: entity 担当。
 *
 * <p>{@link NocterraClient} から呼ばれる。26.3 は render state + submit node 方式
 * （GeckoLib 5.5.7 の {@code GeoEntityRenderer} がそのまま対応している）。</p>
 */
public final class NocterraClientRenderers {
    private NocterraClientRenderers() {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NocterraEntities.ASHEN_WRAITH.get(),
                context -> new AshenWraithRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ASH_CRAWLER.get(),
                context -> new AshCrawlerRenderer(context));
        event.registerEntityRenderer(NocterraEntities.MOURNING_WISP.get(),
                context -> new MourningWispRenderer(context));
        event.registerEntityRenderer(NocterraEntities.BOG_GHOUL.get(),
                context -> new BogGhoulRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ROTWOOD_KING.get(),
                context -> new RotwoodKingRenderer(context));
        event.registerEntityRenderer(NocterraEntities.SORROW_SAINT.get(),
                context -> new SorrowSaintRenderer(context));
        event.registerEntityRenderer(NocterraEntities.CONDEMNED_KNIGHT_LORD.get(),
                context -> new CondemnedKnightLordRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ABYSSAL_SEA_EMPEROR.get(),
                context -> new AbyssalSeaEmperorRenderer(context));
        event.registerEntityRenderer(NocterraEntities.SANDGLASS_TWIN_KINGS.get(),
                context -> new SandglassTwinKingsRenderer(context));
        event.registerEntityRenderer(NocterraEntities.ASTEL_FALLEN_STAR_DRAGON.get(),
                context -> new AstelFallenStarDragonRenderer(context));
        event.registerEntityRenderer(NocterraEntities.SILVER_EXECUTIONER.get(),
                context -> new SilverExecutionerRenderer(context));
        event.registerEntityRenderer(NocterraEntities.MOLTEN_STEEL_COLOSSUS.get(),
                context -> new MoltenSteelColossusRenderer(context));
        event.registerEntityRenderer(NocterraEntities.BLACK_CROWNED_KING.get(),
                context -> new BlackCrownedKingRenderer(context));
        event.registerEntityRenderer(NocterraEntities.EMBER_INCARNATION.get(),
                context -> new EmberIncarnationRenderer(context));
        event.registerEntityRenderer(NocterraEntities.FORGOTTEN_KNIGHT.get(),
                context -> new ForgottenKnightRenderer(context));
        event.registerEntityRenderer(NocterraEntities.NAMELESS_OBSERVER.get(),
                context -> new NamelessObserverRenderer(context));

        // ---- NPC（story 担当） ----
        event.registerEntityRenderer(NocterraNpcs.WARDEN_OF_ASH.get(),
                context -> new NpcRenderer(context, "warden_of_ash"));
        event.registerEntityRenderer(NocterraNpcs.MARSH_WIDOW.get(),
                context -> new NpcRenderer(context, "marsh_widow"));
        event.registerEntityRenderer(NocterraNpcs.BLIND_SCRIBE.get(),
                context -> new NpcRenderer(context, "blind_scribe"));
        event.registerEntityRenderer(NocterraNpcs.GATE_KEEPER.get(),
                context -> new NpcRenderer(context, "gate_keeper"));
        event.registerEntityRenderer(NocterraNpcs.ASHEN_MERCHANT.get(),
                context -> new NpcRenderer(context, "ashen_merchant"));
        event.registerEntityRenderer(NocterraNpcs.LOST_CHILD.get(),
                context -> new NpcRenderer(context, "lost_child"));
    }
}

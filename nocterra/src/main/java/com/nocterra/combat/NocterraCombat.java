package com.nocterra.combat;

import com.nocterra.Nocterra;
import com.nocterra.item.NocterraWeapons;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * 戦闘システム（スタミナ／魔力／回避／パリィ／コンボ／戦技／武器）の登録点。所有: combat 担当。
 *
 * <p>サーバー権威。クライアントは表示のみ。登録するもの:</p>
 * <ul>
 *   <li>{@link CombatAttachments}: プレイヤーごとのスタミナ・魔力（AttachmentType）</li>
 *   <li>{@link CombatComponents}: 武器の強化段階・属性（Data Components）</li>
 *   <li>{@link NocterraWeapons}: 武器 3 種（DeferredRegister.Items）</li>
 *   <li>{@link CombatHandler}: 回避の無敵・パリィ・コンボ・自然回復（ゲームバス）</li>
 *   <li>クリエイティブタブへの武器追加</li>
 * </ul>
 *
 * <p>payload の登録は Lead 所有の {@code NocterraNetwork} 側で
 * {@link com.nocterra.combat.network.CombatPayloads#register} を呼んでもらう
 * （このクラスからは登録しない）。</p>
 */
public final class NocterraCombat {
    private NocterraCombat() {}

    public static void register(IEventBus modBus) {
        // レジストリ
        CombatAttachments.register(modBus);
        CombatComponents.register(modBus);
        NocterraWeapons.register(modBus);

        // クリエイティブタブ（mod バス）
        modBus.addListener(NocterraCombat::buildCreativeTabContents);

        // ゲームバス（サーバー権威の戦闘処理）
        CombatHandler.register();

        Nocterra.LOG.info("NOCTERRA combat bootstrap: attachments, data components, weapons and combat handlers registered");
    }

    private static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        NocterraWeapons.addToCreativeTab(event);
    }
}

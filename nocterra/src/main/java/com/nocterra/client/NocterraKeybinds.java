package com.nocterra.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nocterra.Nocterra;
import com.nocterra.client.hud.CombatHud;
import com.nocterra.combat.network.DodgeRequestPayload;
import com.nocterra.combat.network.HeavyAttackRequestPayload;
import com.nocterra.combat.network.ParryRequestPayload;
import com.nocterra.combat.network.SkillRequestPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/**
 * キーバインド登録とクライアント側の入力処理。所有: combat 担当。
 *
 * <p>既定キー: 回避 = R、強攻撃 = V、パリィ = F、戦技 = G、魔法 = C。
 * すべて {@link KeyMapping} なので設定画面で変更できる。
 * キー入力は payload（要求）としてサーバーへ送るだけで、判定はサーバーが行う。</p>
 *
 * <p>クライアント tick / HUD のリスナもここでゲームバスに登録する
 * （このクラスは {@code Dist.CLIENT} でのみロードされるため安全）。</p>
 */
public final class NocterraKeybinds {
    private NocterraKeybinds() {}

    /** キーカテゴリ。 */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "combat"));

    public static final KeyMapping DODGE = new KeyMapping(
            "key.nocterra.dodge", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping HEAVY_ATTACK = new KeyMapping(
            "key.nocterra.heavy_attack", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping PARRY = new KeyMapping(
            "key.nocterra.parry", InputConstants.Type.KEYBOARD, InputConstants.KEY_F, CATEGORY);
    public static final KeyMapping SKILL = new KeyMapping(
            "key.nocterra.skill", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping MAGIC = new KeyMapping(
            "key.nocterra.magic", InputConstants.Type.KEYBOARD, InputConstants.KEY_C, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(DODGE);
        event.register(HEAVY_ATTACK);
        event.register(PARRY);
        event.register(SKILL);
        event.register(MAGIC);

        // クライアント tick（キー → payload 送信）と HUD 描画。
        // どちらもゲームバスイベントで、register() はクライアントでのみ呼ばれる。
        NeoForge.EVENT_BUS.addListener(NocterraKeybinds::onClientTick);
        NeoForge.EVENT_BUS.addListener(CombatHud::onRenderGui);
        Nocterra.LOG.info("NOCTERRA combat keybinds registered (R/V/F/G/C)");
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }

        // consumeClick() は GUI 表示中には積まれない（KeyboardHandler が画面を判定する）
        if (DODGE.consumeClick()) {
            ClientPacketDistributor.sendToServer(new DodgeRequestPayload());
        }
        if (HEAVY_ATTACK.consumeClick()) {
            ClientPacketDistributor.sendToServer(new HeavyAttackRequestPayload());
        }
        if (PARRY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ParryRequestPayload());
        }
        if (SKILL.consumeClick()) {
            ClientPacketDistributor.sendToServer(new SkillRequestPayload());
        }
        if (MAGIC.consumeClick()) {
            // 魔法システム（com.nocterra.magic）実装時に payload を送る。現状は予約のみ。
        }
    }
}

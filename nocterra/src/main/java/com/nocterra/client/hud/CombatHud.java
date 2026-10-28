package com.nocterra.client.hud;

import com.nocterra.combat.CombatClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * スタミナ／魔力バーとコンボ倍率の HUD。所有: combat 担当。
 *
 * <p>26.3 の HUD 描画は {@code GuiGraphicsExtractor}。{@link RenderGuiEvent.Post} は
 * ゲームバス（{@code NeoForge.EVENT_BUS}）で発火するので、
 * {@code com.nocterra.client.NocterraKeybinds#register} からリスナ登録している。</p>
 *
 * <p>表示値は {@link CombatClientCache}（サーバー同期のキャッシュ）のみを参照する。
 * クライアント側でスタミナを計算・消費しない。</p>
 */
public final class CombatHud {
    private CombatHud() {}

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_GAP = 2;

    /** スタミナバー（緑）。 */
    private static final int STAMINA_COLOR = 0xFF57B36A;
    /** 魔力バー（青）。 */
    private static final int MANA_COLOR = 0xFF5A8BE8;
    private static final int EMPTY_COLOR = 0x90000000;
    private static final int BORDER_COLOR = 0xC0000000;
    private static final int COMBO_COLOR = 0xFFFFD27F;
    private static final int PARRY_COLOR = 0xFF9FE8FF;

    /** ホットバー上端からさらに上に配置する（XP バーの上）。 */
    private static final int BOTTOM_OFFSET = 58;

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int staminaY = graphics.guiHeight() - BOTTOM_OFFSET;
        int manaY = staminaY + BAR_HEIGHT + BAR_GAP;

        drawBar(graphics, x, staminaY, CombatClientCache.getStamina(), CombatClientCache.getMaxStamina(), STAMINA_COLOR);
        drawBar(graphics, x, manaY, CombatClientCache.getMana(), CombatClientCache.getMaxMana(), MANA_COLOR);

        // コンボ倍率（1.0 より大きいときだけ）
        float multiplier = CombatClientCache.getComboMultiplier();
        if (CombatClientCache.getComboHits() > 0 && multiplier > 1.0F) {
            graphics.text(minecraft.font, String.format("x%.2f", multiplier),
                    x + BAR_WIDTH + 4, manaY - 2, COMBO_COLOR);
        }

        // パリィ受付中
        if (CombatClientCache.isParryActive()) {
            graphics.text(minecraft.font, "PARRY", x - 34, staminaY - 1, PARRY_COLOR);
        }
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int x, int y, float value, float max, int color) {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, EMPTY_COLOR);

        float ratio = max <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, value / max));
        int filled = Math.round(BAR_WIDTH * ratio);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        }
    }
}

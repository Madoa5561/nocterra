package com.nocterra.client.journal;

import com.nocterra.networking.journal.JournalDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * 探索手帳の GUI。所有: Lead。
 *
 * <p>26.3 の描画 API は {@link Screen#extractRenderState} 形式（{@code render(GuiGraphics)} は存在しない）。</p>
 */
public final class JournalScreen extends Screen {
    private static final int COLOR_BG = 0xF0181420;
    private static final int COLOR_BORDER = 0xFF6B4A9E;
    private static final int COLOR_TITLE = 0xFFFFD46B;
    private static final int COLOR_TEXT = 0xFFE8E0F0;
    private static final int COLOR_DIM = 0xFF8A8298;
    private static final int COLOR_OWNED = 0xFFC9A0FF;
    private static final int COLOR_HINT = 0xFF8A8298;

    private static final String[] SIGIL_KEYS = {
            "rotwood", "sorrow", "condemned", "sandglass",
            "abyssal", "astel", "silver", "molten"
    };

    private final JournalDataPayload data;

    public JournalScreen(JournalDataPayload data) {
        super(Component.translatable("journal.nocterra.title"));
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int w = this.width;
        int h = this.height;
        int panelWidth = Math.min(320, w - 20);
        int x0 = (w - panelWidth) / 2;
        int rows = SIGIL_KEYS.length + 6;
        int panelHeight = Math.min(h - 20, 26 + rows * 11);
        int y0 = (h - panelHeight) / 2;

        graphics.fill(x0 - 2, y0 - 2, x0 + panelWidth + 2, y0 + panelHeight + 2, 0xC00B0714);
        graphics.fill(x0, y0, x0 + panelWidth, y0 + panelHeight, COLOR_BG);
        graphics.fill(x0, y0, x0 + panelWidth, y0 + 2, COLOR_BORDER);
        graphics.fill(x0, y0 + panelHeight - 2, x0 + panelWidth, y0 + panelHeight, 0xFF4A3568);
        graphics.fill(x0, y0, x0 + 2, y0 + panelHeight, 0xFF4A3568);
        graphics.fill(x0 + panelWidth - 2, y0, x0 + panelWidth, y0 + panelHeight, 0xFF4A3568);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int w = this.width;
        int h = this.height;
        int panelWidth = Math.min(320, w - 20);
        int x0 = (w - panelWidth) / 2;
        int rows = SIGIL_KEYS.length + 6;
        int panelHeight = Math.min(h - 20, 26 + rows * 11);
        int y0 = (h - panelHeight) / 2;

        int line = this.font.lineHeight + 1;
        int x = x0 + 12;
        int y = y0 + 8;

        graphics.text(this.font, Component.translatable("journal.nocterra.title"), x, y, COLOR_TITLE);
        y += line + 3;

        int owned = Integer.bitCount(this.data.sigilMask());
        graphics.text(this.font,
                Component.translatable("journal.nocterra.sigils", owned, SIGIL_KEYS.length), x, y, COLOR_TEXT);
        y += line;

        for (int i = 0; i < SIGIL_KEYS.length; i++) {
            boolean has = (this.data.sigilMask() & (1 << i)) != 0;
            String mark = has ? "\u25c6 " : "\u25c7 ";
            Component name = Component.translatable("item.nocterra.sigil_" + SIGIL_KEYS[i]);
            graphics.text(this.font, Component.literal(mark).append(name), x + 4, y,
                    has ? COLOR_OWNED : COLOR_DIM);
            y += line - 1;
        }

        y += 2;
        graphics.text(this.font,
                Component.translatable("journal.nocterra.bosses", this.data.bossCount()), x, y, COLOR_TEXT);
        y += line;
        graphics.text(this.font,
                Component.translatable("journal.nocterra.npcs", this.data.npcCount()), x, y, COLOR_TEXT);
        y += line;
        graphics.text(this.font,
                Component.translatable("journal.nocterra.quests", this.data.questCount()), x, y, COLOR_TEXT);
        y += line + 2;

        Component status;
        int color;
        if (this.data.trueEnding()) {
            status = Component.translatable("journal.nocterra.ready.true");
            color = COLOR_TITLE;
        } else if (this.data.normalEnding()) {
            status = Component.translatable("journal.nocterra.ready.normal");
            color = 0xFFFFE9A8;
        } else if (owned >= 8) {
            status = Component.translatable("journal.nocterra.ready.true");
            color = COLOR_TITLE;
        } else if (owned >= 5) {
            status = Component.translatable("journal.nocterra.ready.normal");
            color = 0xFFFFE9A8;
        } else {
            status = Component.translatable("journal.nocterra.ready.locked", 5 - owned);
            color = COLOR_DIM;
        }
        graphics.text(this.font, status, x, y, color);

        graphics.text(this.font, Component.translatable("journal.nocterra.close"), x, y0 + panelHeight - 14,
                COLOR_HINT);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }
}

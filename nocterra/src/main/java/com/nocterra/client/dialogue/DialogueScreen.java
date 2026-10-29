package com.nocterra.client.dialogue;

import com.mojang.blaze3d.platform.InputConstants;
import com.nocterra.npc.network.DialogueOpenPayload;
import com.nocterra.npc.network.DialogueSelectPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 会話 UI。所有: story 担当。
 *
 * <p>画面下部に「NPC 名 / 本文 / 選択肢」を表示する。操作:</p>
 * <ul>
 *   <li>数字キー 1〜9 … 選択肢を選ぶ</li>
 *   <li>マウスクリック … 選択肢を選ぶ</li>
 *   <li>Esc … 会話を閉じる（サーバーにも通知）</li>
 * </ul>
 *
 * <p><b>レイアウト方針（実機フィードバック反映）</b>: パネルの高さは
 * 「画面の GUI 高さ − 余白」を上限として、名前・選択肢・ヒントを<b>先に確保</b>し、
 * 残りを本文に割り当てる。本文が入りきらない場合は最後の 1 行を「…」に置き換えて
 * パネルが画面外へはみ出さないことを保証する。選択肢は常に画面内に表示される。</p>
 *
 * <p>本文は {@code font.split} で行に分解し、その分解結果をそのまま描画する
 * （測定と描画で行数がずれないようにするため）。</p>
 *
 * <p>26.3 の描画 API は {@link Screen#extractRenderState} 方式
 * （{@code render(GuiGraphics)} は存在しない）。</p>
 */
public final class DialogueScreen extends Screen {
    private static final int COLOR_TEXT = 0xFFE8E0F0;
    private static final int COLOR_NAME = 0xFFFFD46B;
    private static final int COLOR_CHOICE = 0xFFBFAEDD;
    private static final int COLOR_CHOICE_HOVER = 0xFFFFE9A8;
    private static final int COLOR_CHOICE_DISABLED = 0xFF6A6278;
    private static final int COLOR_HINT = 0xFF8A8298;
    private static final int COLOR_SEPARATOR = 0xFF6B4A9E;

    /** 横幅の上限（広い画面で本文が縦に伸びすぎないように）。 */
    private static final int MAX_PANEL_WIDTH = 560;
    private static final int MIN_PANEL_WIDTH = 200;
    /** 画面端からの余白。 */
    private static final int SCREEN_MARGIN = 6;
    private static final int CHOICE_ROW_HEIGHT = 13;

    private final int npcEntityId;
    private String npcId;
    private String textKey;
    private List<DialogueOpenPayload.Choice> choices = List.of();

    /** 描画時に確定する選択肢の当たり判定（x0,y0,x1,y1）。 */
    private final List<int[]> choiceBounds = new ArrayList<>();
    private int hovered = -1;
    private boolean debugLogged;

    /** 連打で 2 回選択が飛ぶのを防ぐ。 */
    private long lockUntil;
    private boolean closing;
    private boolean closingFromServer;

    public DialogueScreen(DialogueOpenPayload payload) {
        super(Component.translatable("entity.nocterra." + payload.npcId()));
        this.npcEntityId = payload.npcEntityId();
        this.npcId = payload.npcId();
        this.textKey = payload.textKey();
        this.choices = List.copyOf(payload.choices());
    }

    public int npcEntityId() {
        return this.npcEntityId;
    }

    /** サーバーから次のノードが届いた時に内容だけ差し替える。 */
    public void update(DialogueOpenPayload payload) {
        this.npcId = payload.npcId();
        this.textKey = payload.textKey();
        this.choices = List.copyOf(payload.choices());
        this.hovered = -1;
        this.lockUntil = 0L;
        this.debugLogged = false;
    }

    /** サーバー主導のクローズ。 */
    public void closeFromServer() {
        this.closingFromServer = true;
        this.onClose();
    }

    // ------------------------------------------------------------------ 画面

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fillGradient(0, 0, width, height, 0x20000000, 0x60000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        int lineHeight = this.font.lineHeight;
        int bodyLineHeight = lineHeight + 1;

        int panelWidth = Math.min(MAX_PANEL_WIDTH, screenWidth - SCREEN_MARGIN * 2);
        panelWidth = Math.max(MIN_PANEL_WIDTH, panelWidth);
        int innerWidth = Math.max(40, panelWidth - 28);
        Component body = Component.translatable(this.textKey);
        List<FormattedCharSequence> bodyLines = this.font.split(body, innerWidth);

        // ---- 高さの確保順: 名前 → 選択肢 → ヒント → 本文 ----
        int padTop = 7;
        int padBottom = 5;
        int nameHeight = lineHeight;
        int separatorGap = 4;
        int bodyGap = 5;
        int choiceGap = this.choices.isEmpty() ? 0 : 6;
        int choiceBlock = this.choices.isEmpty() ? 0 : this.choices.size() * CHOICE_ROW_HEIGHT;
        int hintHeight = lineHeight + 3;

        int maxPanelHeight = Math.max(48, screenHeight - SCREEN_MARGIN * 2);

        // ヒントは入る余裕があるときだけ出す（入力は数字キーで分かるため優先度は低い）
        int fixedWithoutHint = padTop + nameHeight + separatorGap + bodyGap
                + choiceGap + choiceBlock + padBottom;
        boolean showHint = fixedWithoutHint + hintHeight + bodyLineHeight <= maxPanelHeight;
        int fixed = fixedWithoutHint + (showHint ? hintHeight : 0);

        int bodySpace = Math.max(0, maxPanelHeight - fixed);
        int maxBodyLines = bodySpace / bodyLineHeight;
        int bodyLineCount = Math.min(bodyLines.size(), maxBodyLines);
        boolean truncated = bodyLineCount < bodyLines.size();
        // 入りきらない場合は最後の 1 行を「…」表示に使う（ブロック高さは変えない）
        int textLineCount = truncated ? Math.max(0, bodyLineCount - 1) : bodyLineCount;
        int bodyBlockHeight = bodyLineCount * bodyLineHeight;

        int panelHeight = Math.min(maxPanelHeight, fixed + bodyBlockHeight);
        int x0 = (screenWidth - panelWidth) / 2;
        int y0 = screenHeight - panelHeight - SCREEN_MARGIN;
        if (y0 < SCREEN_MARGIN) {
            y0 = SCREEN_MARGIN;
        }
        int x1 = x0 + panelWidth;
        int y1 = y0 + panelHeight;

        if (!this.debugLogged) {
            this.debugLogged = true;
            com.nocterra.Nocterra.LOG.info(
                    "[NOCTERRA] dialogue panel: screen={}x{} panel={}x{} y0={} bodyLines={}/{} truncated={}",
                    screenWidth, screenHeight, panelWidth, panelHeight, y0,
                    textLineCount, bodyLines.size(), truncated);
        }

        // 外枠（影 → 本体 → 金属枠）
        graphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xC00B0714);
        graphics.fill(x0, y0, x1, y1, 0xF0161026);
        graphics.fill(x0, y0, x1, y0 + 2, 0xFF7A5AA8);
        graphics.fill(x0, y1 - 2, x1, y1, 0xFF4A3568);
        graphics.fill(x0, y0, x0 + 2, y1, 0xFF4A3568);
        graphics.fill(x1 - 2, y0, x1, y1, 0xFF4A3568);

        // NPC 名
        int nameY = y0 + padTop;
        graphics.text(this.font,
                Component.translatable("entity.nocterra." + this.npcId).withStyle(ChatFormatting.GOLD),
                x0 + 14, nameY, COLOR_NAME);

        // 区切り線
        int separatorY = nameY + nameHeight + 2;
        graphics.fill(x0 + 14, separatorY, x1 - 14, separatorY + 1, COLOR_SEPARATOR);

        // 本文（split した行をそのまま描画 = 測定と描画が一致する）
        int bodyY = separatorY + 1 + bodyGap;
        for (int i = 0; i < textLineCount; i++) {
            graphics.text(this.font, bodyLines.get(i), x0 + 14, bodyY + i * bodyLineHeight, COLOR_TEXT);
        }
        if (truncated && bodyLineCount > 0) {
            graphics.text(this.font, Component.translatable("dialogue.nocterra.more"),
                    x0 + 14, bodyY + textLineCount * bodyLineHeight, COLOR_HINT);
        }

        // 選択肢（常に画面内）
        this.choiceBounds.clear();
        this.hovered = -1;
        int choiceY = bodyY + bodyBlockHeight + choiceGap;
        int left = x0 + 12;
        int right = x1 - 12;
        for (int i = 0; i < this.choices.size(); i++) {
            DialogueOpenPayload.Choice choice = this.choices.get(i);
            int rowTop = choiceY + i * CHOICE_ROW_HEIGHT;
            int rowBottom = rowTop + CHOICE_ROW_HEIGHT - 2;
            boolean hover = mouseX >= left && mouseX <= right && mouseY >= rowTop && mouseY <= rowBottom;
            if (hover && choice.enabled()) {
                this.hovered = i;
                graphics.fill(left, rowTop - 1, right, rowBottom, 0x407A5AA8);
                graphics.fill(left, rowBottom, right, rowBottom + 1, 0x807A5AA8);
            }
            int color = !choice.enabled() ? COLOR_CHOICE_DISABLED
                    : (hover ? COLOR_CHOICE_HOVER : COLOR_CHOICE);
            Component label = Component.literal("[" + (i + 1) + "] ")
                    .append(Component.translatable(choice.textKey()));
            graphics.text(this.font, label, left + 4, rowTop + 1, color);
            this.choiceBounds.add(new int[] {left, rowTop - 1, right, rowBottom});
        }

        // 操作ヒント
        if (showHint) {
            graphics.text(this.font, Component.translatable("dialogue.nocterra.hint"),
                    x0 + 14, y1 - padBottom - lineHeight, COLOR_HINT);
        }
    }

    // ------------------------------------------------------------------ 入力

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 26.3 の KeyEvent#key() は GLFW キーコードではなく InputConstants（SDL スキャンコード）を返す。
        // 例: "1" は 49 ではなく 30。必ず InputConstants の定数で比較する。
        int index = choiceIndexFor(event.key());
        if (index >= 0 && index < this.choices.size()) {
            this.select(index);
            return true;
        }
        return super.keyPressed(event);
    }

    /** 数字キー（テンキー含む）→ 選択肢インデックス。該当なしは -1。 */
    private static int choiceIndexFor(int key) {
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
            return key - InputConstants.KEY_1;
        }
        if (key >= InputConstants.KEY_NUMPAD1 && key <= InputConstants.KEY_NUMPAD9) {
            return key - InputConstants.KEY_NUMPAD1;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < this.choiceBounds.size(); i++) {
                int[] bounds = this.choiceBounds.get(i);
                if (event.x() >= bounds[0] && event.x() <= bounds[2]
                        && event.y() >= bounds[1] && event.y() <= bounds[3]) {
                    if (i < this.choices.size() && this.choices.get(i).enabled()) {
                        this.select(i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void select(int index) {
        long now = System.currentTimeMillis();
        if (now < this.lockUntil || this.closing) {
            return;
        }
        this.lockUntil = now + 400L;
        ClientPacketDistributor.sendToServer(new DialogueSelectPayload(this.npcEntityId, index));
    }

    // ------------------------------------------------------------------ 終了

    @Override
    public void onClose() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        if (!this.closingFromServer) {
            ClientPacketDistributor.sendToServer(
                    new DialogueSelectPayload(this.npcEntityId, DialogueSelectPayload.CLOSE));
        }
        super.onClose();
    }

    @Override
    public void removed() {
        DialogueClient.onScreenRemoved(this);
        super.removed();
    }
}

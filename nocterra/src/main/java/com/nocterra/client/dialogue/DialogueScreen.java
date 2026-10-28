package com.nocterra.client.dialogue;

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
 * <p>本文・選択肢は翻訳キーとして受け取り、この画面で解決する
 * （サーバーは文字列そのものを送らない）。選択結果は index のみをサーバーへ返し、
 * 次ノードの決定・条件判定・報酬はすべてサーバーが行う。</p>
 *
 * <p>26.3 の描画 API は {@link GuiGraphicsExtractor#extractRenderState} 方式
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

    private final int npcEntityId;
    private String npcId;
    private String textKey;
    private List<DialogueOpenPayload.Choice> choices = List.of();

    /** 描画時に確定する選択肢の当たり判定（x0,y0,x1,y1）。 */
    private final List<int[]> choiceBounds = new ArrayList<>();
    private int hovered = -1;

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
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        int panelWidth = Math.min(430, Math.max(240, guiWidth - 32));
        int innerWidth = panelWidth - 32;
        Component body = Component.translatable(this.textKey);
        List<FormattedCharSequence> bodyLines = this.font.split(body, innerWidth);
        int bodyHeight = Math.max(1, bodyLines.size()) * (this.font.lineHeight + 1);
        int choiceBlockHeight = this.choices.isEmpty() ? 0 : 8 + this.choices.size() * 15;
        int panelHeight = 14 + this.font.lineHeight + 6 + bodyHeight + choiceBlockHeight + 22;

        int x0 = (guiWidth - panelWidth) / 2;
        int y0 = guiHeight - panelHeight - 12;
        int x1 = x0 + panelWidth;
        int y1 = y0 + panelHeight;

        // 外枠（影 → 本体 → 金属枠）
        graphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xC00B0714);
        graphics.fill(x0, y0, x1, y1, 0xF0161026);
        graphics.fill(x0, y0, x1, y0 + 2, 0xFF7A5AA8);
        graphics.fill(x0, y1 - 2, x1, y1, 0xFF4A3568);
        graphics.fill(x0, y0, x0 + 2, y1, 0xFF4A3568);
        graphics.fill(x1 - 2, y0, x1, y1, 0xFF4A3568);

        // NPC 名
        graphics.text(this.font,
                Component.translatable("entity.nocterra." + this.npcId).withStyle(ChatFormatting.GOLD),
                x0 + 16, y0 + 12, COLOR_NAME);
        int separatorY = y0 + 12 + this.font.lineHeight + 1;
        graphics.fill(x0 + 16, separatorY, x1 - 16, separatorY + 1, COLOR_SEPARATOR);

        // 本文
        int bodyY = separatorY + 6;
        graphics.textWithWordWrap(this.font, body, x0 + 16, bodyY, innerWidth, COLOR_TEXT);

        // 選択肢
        this.choiceBounds.clear();
        this.hovered = -1;
        int choiceY = bodyY + bodyHeight + 8;
        for (int i = 0; i < this.choices.size(); i++) {
            DialogueOpenPayload.Choice choice = this.choices.get(i);
            int rowTop = choiceY + i * 15;
            int rowBottom = rowTop + 13;
            int left = x0 + 12;
            int right = x1 - 12;
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
            graphics.text(this.font, label, left + 4, rowTop + 2, color);
            this.choiceBounds.add(new int[] {left, rowTop - 1, right, rowBottom});
        }

        // 操作ヒント
        graphics.text(this.font, Component.translatable("dialogue.nocterra.hint"),
                x0 + 16, y1 - 15, COLOR_HINT);
    }

    // ------------------------------------------------------------------ 入力

    @Override
    public boolean keyPressed(KeyEvent event) {
        int index = event.key() - 49; // GLFW_KEY_1
        if (index >= 0 && index < 9 && index < this.choices.size()) {
            this.select(index);
            return true;
        }
        return super.keyPressed(event);
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

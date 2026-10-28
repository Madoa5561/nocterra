package com.nocterra.client.hud;

import com.nocterra.Nocterra;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * NOCTERRA のボス専用 HP バー。所有: Lead。
 *
 * <p>バニラのボスバー（182×5）をキャンセルし、装飾枠・グラデーション・
 * ポートレート・フェーズ表示・アニメーションする光沢を持つ独自バーを描く。
 * {@code entity.nocterra.*} の翻訳キーを持つボスだけが対象（バニラのボスはそのまま）。</p>
 */
public final class NocterraBossOverlay {
    private NocterraBossOverlay() {}

    /** 枠の外寸（バニラの 182 幅より広い）。 */
    private static final int FRAME_WIDTH = 216;
    private static final int FRAME_HEIGHT = 30;
    private static final int PORTRAIT_SIZE = 22;
    private static final int INNER_PAD = 4;
    /** 縦方向の占有量（次のバーとの間隔）。 */
    private static final int ROW_HEIGHT = FRAME_HEIGHT + 12;

    public static void onBossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        String bossId = nocterraBossId(event.getBossEvent().getName());
        if (bossId == null) {
            return;
        }
        event.setCanceled(true);
        event.setIncrement(ROW_HEIGHT);

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        LerpingBossEvent boss = event.getBossEvent();

        int guiWidth = graphics.guiWidth();
        int left = (guiWidth - FRAME_WIDTH) / 2;
        int top = event.getY() - 2;
        int right = left + FRAME_WIDTH;
        int bottom = top + FRAME_HEIGHT;

        drawGlow(graphics, left, top, right, bottom, boss.getProgress());
        drawPlate(graphics, left, top, right, bottom);
        drawCorners(graphics, left, top, right, bottom);
        drawPortrait(graphics, bossId, left, top);
        drawBar(graphics, left, top, right, bottom, boss);
        drawPhasePips(graphics, right, top, boss.getProgress());
        drawName(graphics, mc, boss, left, right, top);
    }

    // ------------------------------------------------------------------ parts

    /** バーの背後に脈動するオーラ。 */
    private static void drawGlow(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, float progress) {
        long millis = System.currentTimeMillis();
        double wave = 0.5 + 0.5 * Math.sin(millis / 420.0);
        int alpha = (int) (36 + 46 * wave);
        int color = (alpha << 24) | 0x8A4FD0;
        graphics.fill(left - 3, top - 3, right + 3, bottom + 3, color);
        int innerAlpha = (int) (20 + 28 * wave);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, (innerAlpha << 24) | 0xB070FF);
    }

    /** 外枠＋内枠＋プレート。 */
    private static void drawPlate(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        // 外周（黒に近い紫）
        graphics.fill(left, top, right, bottom, 0xFF120C1C);
        // 金属風の外枠 2px
        graphics.fill(left, top, right, top + 2, 0xFF6B4A9E);
        graphics.fill(left, bottom - 2, right, bottom, 0xFF3A2456);
        graphics.fill(left, top, left + 2, bottom, 0xFF5A3C86);
        graphics.fill(right - 2, top, right, bottom, 0xFF5A3C86);
        // 内側の面
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF1A1226);
        // 上端のハイライト
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0xFF8E6BC8);
        // 下端の影
        graphics.fill(left + 2, bottom - 4, right - 2, bottom - 2, 0xFF0B0710);
    }

    /** 四隅の飾り（小さな菱形＋光点）。 */
    private static void drawCorners(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        int gold = 0xFFE8C874;
        int goldDim = 0xFF8A6A2E;
        int[][] corners = {{left, top}, {right - 5, top}, {left, bottom - 5}, {right - 5, bottom - 5}};
        for (int[] c : corners) {
            int x = c[0];
            int y = c[1];
            graphics.fill(x + 1, y, x + 4, y + 5, goldDim);
            graphics.fill(x, y + 1, x + 5, y + 4, goldDim);
            graphics.fill(x + 1, y + 1, x + 4, y + 4, 0xFF120C1C);
            graphics.fill(x + 2, y + 2, x + 3, y + 3, gold);
        }
    }

    /** 左側のボスポートレート。 */
    private static void drawPortrait(GuiGraphicsExtractor graphics, String bossId, int left, int top) {
        int px = left + 3;
        int py = top + 4;
        Identifier sprite = Identifier.fromNamespaceAndPath(Nocterra.MOD_ID, "boss_portrait/" + bossId);
        // 背景板＋枠
        graphics.fill(px - 1, py - 1, px + PORTRAIT_SIZE + 1, py + PORTRAIT_SIZE + 1, 0xFF0B0710);
        graphics.fill(px - 1, py - 1, px + PORTRAIT_SIZE + 1, py, 0xFF6B4A9E);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, px, py, PORTRAIT_SIZE, PORTRAIT_SIZE);
    }

    /** 本体バー（グラデーション＋ノッチ＋光沢）。 */
    private static void drawBar(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom,
                                LerpingBossEvent boss) {
        int barLeft = left + PORTRAIT_SIZE + 8;
        int barRight = right - 8;
        int barTop = top + 9;
        int barBottom = bottom - 8;
        int barWidth = barRight - barLeft;

        // 溝（背景）
        graphics.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, 0xFF0A0610);
        graphics.fill(barLeft, barTop, barRight, barBottom, 0xFF2A1B3D);

        int fillWidth = Mth.clamp((int) (barWidth * boss.getProgress()), 0, barWidth);
        if (fillWidth > 0) {
            int[] colors = barColors(boss.getColor());
            // 縦グラデーション（明→暗）
            graphics.fillGradient(barLeft, barTop, barLeft + fillWidth, barBottom, colors[0], colors[1]);
            // 上端の光
            graphics.fill(barLeft, barTop, barLeft + fillWidth, barTop + 1, colors[2]);
            // 下端の影
            graphics.fill(barLeft, barBottom - 1, barLeft + fillWidth, barBottom, 0xFF120A1E);
            // 流れる光沢
            long millis = System.currentTimeMillis();
            int shimmer = (int) ((millis / 18) % (barWidth + 60)) - 30;
            int sx0 = Math.max(barLeft, barLeft + shimmer);
            int sx1 = Math.min(barLeft + fillWidth, barLeft + shimmer + 26);
            if (sx1 > sx0) {
                graphics.fill(sx0, barTop, sx1, barBottom, 0x33FFFFFF);
            }
            // 10% ごとのノッチ
            for (int i = 1; i < 10; i++) {
                int nx = barLeft + barWidth * i / 10;
                if (nx <= barLeft + fillWidth) {
                    graphics.fill(nx, barTop, nx + 1, barBottom, 0x66000000);
                }
            }
        }
    }

    /** 右端のフェーズ表示（HP 50% 超で 2 つ点灯）。 */
    private static void drawPhasePips(GuiGraphicsExtractor graphics, int right, int top, float progress) {
        int pipY = top + FRAME_HEIGHT - 9;
        int pipX = right - 6;
        boolean second = progress <= 0.5F;
        for (int i = 0; i < 2; i++) {
            int x = pipX - i * 6;
            boolean lit = i == 0 || second;
            int color = lit ? 0xFFFF7A3C : 0xFF3A2456;
            graphics.fill(x, pipY, x + 4, pipY + 1, color);
            graphics.fill(x + 1, pipY - 1, x + 3, pipY + 2, color);
        }
    }

    /** ボス名（金縁取り付き）。 */
    private static void drawName(GuiGraphicsExtractor graphics, Minecraft mc, LerpingBossEvent boss,
                                 int left, int right, int top) {
        Component name = boss.getName().copy().withStyle(ChatFormatting.BOLD);
        int width = mc.font.width(name);
        int x = left + PORTRAIT_SIZE + 8 + (right - 8 - (left + PORTRAIT_SIZE + 8) - width) / 2;
        int y = top - 11;
        // 影と発光
        graphics.text(mc.font, name, x + 1, y + 1, 0xFF000000, false);
        graphics.text(mc.font, name, x, y, 0xFFE8C874, false);
    }

    // ------------------------------------------------------------------ utils

    private static int[] barColors(BossEvent.BossBarColor color) {
        return switch (color) {
            case PINK -> new int[]{0xFFFF9AC4, 0xFFB33A6B, 0xFFFFC9DE};
            case BLUE -> new int[]{0xFF8FC4FF, 0xFF2A5AA8, 0xFFCDE6FF};
            case RED -> new int[]{0xFFFF8A7A, 0xFF9E2A20, 0xFFFFC2B8};
            case GREEN -> new int[]{0xFF9BE58A, 0xFF2E7A2A, 0xFFD2FFC2};
            case YELLOW -> new int[]{0xFFFFE58A, 0xFF9E7A20, 0xFFFFF3C2};
            case PURPLE -> new int[]{0xFFCFA6FF, 0xFF5B2E9E, 0xFFEEDCFF};
            case WHITE -> new int[]{0xFFFFFFFF, 0xFF9E9E9E, 0xFFFFFFFF};
        };
    }

    /** バーの名前から NOCTERRA のボス ID を取り出す。対象外なら null。 */
    private static String nocterraBossId(Component name) {
        if (name.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            String prefix = "entity." + Nocterra.MOD_ID + ".";
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
        }
        return null;
    }
}

package com.injaa.phonev3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class PhoneV31Screen extends Screen {
    private enum Page { HOME, SNAKE, TAP }
    private Page page = Page.HOME;
    private final Random random = new Random();
    private int px, py, pw = 210, ph = 350;
    private int snakeTick, snakeDirX = 1, snakeDirY = 0;
    private final Deque<int[]> snake = new ArrayDeque<>();
    private int foodX = 8, foodY = 8, snakeScore;
    private int tapX, tapY, tapScore, tapTicks = 20 * 30;

    public PhoneV31Screen() { super(Component.literal("Advanced Phone V3.1")); }

    @Override protected void init() {
        pw = Math.min(230, width - 24);
        ph = Math.min(390, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        resetSnake();
        resetTap();
    }

    @Override public void tick() {
        if (page == Page.SNAKE && ++snakeTick >= 5) {
            snakeTick = 0;
            moveSnake();
        }
        if (page == Page.TAP && tapTicks > 0) tapTicks--;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(px, py, px + pw, py + ph, 0xFF05070B);
        g.fill(px + 4, py + 4, px + pw - 4, py + ph - 4, 0xFF101723);
        g.fill(px + 8, py + 8, px + pw - 8, py + 34, 0xFF182638);
        g.drawString(font, minecraftTime(), px + 14, py + 16, 0xFFFFFFFF, false);
        String battery = battery() + "%";
        int bw = font.width(battery);
        g.drawString(font, battery, px + pw - 15 - bw, py + 16, battery() <= 20 ? 0xFFFF6B6B : 0xFF9CFFB3, false);
        g.fill(px + pw - 16, py + 13, px + pw - 12, py + 24, 0xFFB8C2D1);
        g.fill(px + pw - 12, py + 16, px + pw - 10, py + 21, 0xFFB8C2D1);

        if (page == Page.HOME) renderHome(g);
        else if (page == Page.SNAKE) renderSnake(g);
        else renderTap(g);
        super.render(g, mx, my, pt);
    }

    private void renderHome(GuiGraphics g) {
        g.drawCenteredString(font, "ADVANCED PHONE", px + pw / 2, py + 48, 0xFFFFFFFF);
        g.drawCenteredString(font, "Minecraft Day " + dayNumber(), px + pw / 2, py + 62, 0xFF8FA4BE);
        int y = py + 84;
        app(g, px + 16, y, "FLASH", flashlight() ? "ON" : "OFF", flashlight() ? 0xFF39C66D : 0xFF34465A);
        app(g, px + 116, y, "PHONE", "APPS", 0xFF3366D6);
        app(g, px + 16, y + 82, "SNAKE", "GAME", 0xFF1B8F78);
        app(g, px + 116, y + 82, "TAP", "RUSH", 0xFFE28A2D);
        app(g, px + 16, y + 164, "CAMERA", "BASE APP", 0xFF8747C6);
        app(g, px + 116, y + 164, "NOTES", "BASE APP", 0xFFB94E6C);
        g.drawCenteredString(font, "Flashlight follows where you look", px + pw / 2, py + ph - 26, 0xFF8393A7);
    }

    private void app(GuiGraphics g, int x, int y, String a, String b, int color) {
        g.fill(x, y, x + 82, y + 64, 0xFF0A0F17);
        g.fill(x + 4, y + 4, x + 78, y + 38, color);
        g.drawCenteredString(font, a, x + 41, y + 16, 0xFFFFFFFF);
        g.drawCenteredString(font, b, x + 41, y + 46, 0xFFC4CFDD);
    }

    private void renderSnake(GuiGraphics g) {
        g.drawString(font, "SNAKE  Score: " + snakeScore, px + 14, py + 48, 0xFFFFFFFF, false);
        int ox = px + 24, oy = py + 78, cell = 9;
        g.fill(ox - 3, oy - 3, ox + 18 * cell + 3, oy + 18 * cell + 3, 0xFF06090E);
        g.fill(ox + foodX * cell + 2, oy + foodY * cell + 2, ox + foodX * cell + 7, oy + foodY * cell + 7, 0xFFFFD166);
        for (int[] s : snake) g.fill(ox + s[0] * cell + 1, oy + s[1] * cell + 1, ox + s[0] * cell + 8, oy + s[1] * cell + 8, 0xFF6BE585);
        g.drawCenteredString(font, "Arrow keys • ESC/Home to leave", px + pw / 2, py + ph - 28, 0xFF93A6BC);
    }

    private void renderTap(GuiGraphics g) {
        g.drawString(font, "TAP RUSH  Score: " + tapScore, px + 14, py + 48, 0xFFFFFFFF, false);
        g.drawString(font, "Time: " + Math.max(0, tapTicks / 20) + "s", px + 14, py + 62, 0xFF9FB3CA, false);
        int ax = px + 18, ay = py + 82, aw = pw - 36, ah = ph - 120;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF080D14);
        if (tapTicks > 0) {
            g.fill(ax + tapX, ay + tapY, ax + tapX + 28, ay + tapY + 28, 0xFFEFA43A);
            g.drawCenteredString(font, "+", ax + tapX + 14, ay + tapY + 10, 0xFFFFFFFF);
        } else g.drawCenteredString(font, "TIME!  Score: " + tapScore, px + pw / 2, py + ph / 2, 0xFFFFFFFF);
        g.drawCenteredString(font, "Tap the square • Backspace = Home", px + pw / 2, py + ph - 28, 0xFF93A6BC);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (page == Page.HOME) {
            int y = py + 84;
            if (inside(mx, my, px + 16, y, 82, 64)) { sendAction("flashlight_toggle"); return true; }
            if (inside(mx, my, px + 116, y, 82, 64)) { PhoneUpgradeHooks.openOriginalPhone(); return true; }
            if (inside(mx, my, px + 16, y + 82, 82, 64)) { page = Page.SNAKE; resetSnake(); return true; }
            if (inside(mx, my, px + 116, y + 82, 82, 64)) { page = Page.TAP; resetTap(); return true; }
            if (inside(mx, my, px + 16, y + 164, 82, 64) || inside(mx, my, px + 116, y + 164, 82, 64)) { PhoneUpgradeHooks.openOriginalPhone(); return true; }
        } else if (page == Page.TAP && tapTicks > 0) {
            int ax = px + 18, ay = py + 82;
            if (inside(mx, my, ax + tapX, ay + tapY, 28, 28)) { tapScore++; randomizeTap(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.SNAKE) {
            if (keyCode == GLFW.GLFW_KEY_UP && snakeDirY != 1) { snakeDirX = 0; snakeDirY = -1; return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN && snakeDirY != -1) { snakeDirX = 0; snakeDirY = 1; return true; }
            if (keyCode == GLFW.GLFW_KEY_LEFT && snakeDirX != 1) { snakeDirX = -1; snakeDirY = 0; return true; }
            if (keyCode == GLFW.GLFW_KEY_RIGHT && snakeDirX != -1) { snakeDirX = 1; snakeDirY = 0; return true; }
        }
        if ((page == Page.SNAKE || page == Page.TAP) && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
            page = Page.HOME; return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void resetSnake() {
        snake.clear();
        snake.addFirst(new int[]{7, 9}); snake.addLast(new int[]{6, 9}); snake.addLast(new int[]{5, 9});
        snakeDirX = 1; snakeDirY = 0; snakeScore = 0; snakeTick = 0; newFood();
    }

    private void moveSnake() {
        int[] h = snake.peekFirst();
        int nx = h[0] + snakeDirX, ny = h[1] + snakeDirY;
        if (nx < 0 || nx >= 18 || ny < 0 || ny >= 18) { resetSnake(); return; }
        for (int[] s : snake) if (s[0] == nx && s[1] == ny) { resetSnake(); return; }
        snake.addFirst(new int[]{nx, ny});
        if (nx == foodX && ny == foodY) { snakeScore++; newFood(); } else snake.removeLast();
    }

    private void newFood() { foodX = random.nextInt(18); foodY = random.nextInt(18); }
    private void resetTap() { tapScore = 0; tapTicks = 20 * 30; randomizeTap(); }
    private void randomizeTap() { tapX = random.nextInt(Math.max(1, pw - 36 - 28)); tapY = random.nextInt(Math.max(1, ph - 120 - 28)); }
    private boolean inside(double x, double y, int bx, int by, int w, int h) { return x >= bx && x < bx + w && y >= by && y < by + h; }

    private String minecraftTime() {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return "--:--";
        long day = mc.level.getDayTime(); int mins = (int)(((day + 6000L) % 24000L) * 1440L / 24000L);
        return String.format("%02d:%02d", mins / 60, mins % 60);
    }
    private long dayNumber() { Minecraft mc = Minecraft.getInstance(); return mc.level == null ? 1 : mc.level.getDayTime() / 24000L + 1; }

    private static int battery() { return intField("battery", 100); }
    private static boolean flashlight() { return boolField("flashlight", false); }
    private static int intField(String name, int def) {
        try { Class<?> c = Class.forName("com.injaa.chhalavaphone.client.ClientPhoneState"); Field f = c.getField(name); return f.getInt(null); } catch (Throwable t) { return def; }
    }
    private static boolean boolField(String name, boolean def) {
        try { Class<?> c = Class.forName("com.injaa.chhalavaphone.client.ClientPhoneState"); Field f = c.getField(name); return f.getBoolean(null); } catch (Throwable t) { return def; }
    }
    private static void sendAction(String action) {
        try { Class<?> c = Class.forName("com.injaa.chhalavaphone.network.PhoneNetwork"); Method m = c.getMethod("sendAction", String.class); m.invoke(null, action); } catch (Throwable ignored) { }
    }

    @Override public boolean isPauseScreen() { return false; }
}

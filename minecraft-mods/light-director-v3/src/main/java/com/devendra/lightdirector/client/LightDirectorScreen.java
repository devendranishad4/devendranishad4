package com.devendra.lightdirector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LightDirectorScreen extends Screen {
    private static final int[] RANGES = {100, 250, 500, 1000, 1500, 2500};
    private static final double[] SPEEDS = {0.2D, 0.35D, 0.5D, 0.75D, 1.0D, 1.2D, 1.25D, 1.5D, 2.0D, 3.0D};

    private int rangeIndex = 3;
    private int speedIndex = 4;
    private Button rangeButton;
    private Button speedButton;

    public LightDirectorScreen() {
        super(Component.literal("Light Director"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new LightDirectorScreen());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 86;
        int w = 150;
        int h = 20;

        addRenderableWidget(Button.builder(Component.literal("BLACKOUT"), b -> command("lightdirector blackout"))
                .bounds(cx - 76, y, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("FLICKER"), b -> command("lightdirector flicker"))
                .bounds(cx - 76, y + 24, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("RESTORE ALL LIGHTS"), b -> command("lightdirector restore"))
                .bounds(cx - 76, y + 48, w, h).build());

        rangeButton = addRenderableWidget(Button.builder(rangeLabel(), b -> {
            rangeIndex = (rangeIndex + 1) % RANGES.length;
            b.setMessage(rangeLabel());
            command("lightdirector range " + RANGES[rangeIndex]);
        }).bounds(cx - 76, y + 76, w, h).build());

        speedButton = addRenderableWidget(Button.builder(speedLabel(), b -> {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
            b.setMessage(speedLabel());
            command("lightdirector speed " + SPEEDS[speedIndex]);
        }).bounds(cx - 76, y + 100, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("STATUS"), b -> command("lightdirector status"))
                .bounds(cx - 76, y + 128, 72, h).build());

        addRenderableWidget(Button.builder(Component.literal("CLOSE"), b -> onClose())
                .bounds(cx + 2, y + 128, 72, h).build());
    }

    private Component rangeLabel() {
        return Component.literal("RANGE: " + RANGES[rangeIndex] + " blocks");
    }

    private Component speedLabel() {
        return Component.literal("FLICKER SPEED: " + SPEEDS[speedIndex] + "x");
    }

    private void command(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand(command);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int top = this.height / 2 - 116;
        graphics.drawCenteredString(this.font, "LIGHT DIRECTOR", cx, top, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Cinematic Lighting Control", cx, top + 13, 0xA0A0A0);
        graphics.drawCenteredString(this.font, "Loaded chunks only • low-lag scan", cx, top + 26, 0x707070);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

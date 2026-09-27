package com.injaa.train31.client;

import com.injaa.train31.Train31Mod;
import com.injaa.train31.Train31Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Train31Mod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRuntime {
    private ClientRuntime() {}
    private static int storyTick = 0;
    private static float fogStrength = 0f;
    private static int cameraEntityId = -1;
    private static boolean cctv = false;
    private static boolean storyActive = false;

    public static void accept(Train31Network.ClientState s) {
        storyTick = s.storyTick();
        fogStrength = Math.max(0f, Math.min(1f, s.fogStrength()));
        cameraEntityId = s.cameraEntityId();
        cctv = s.cctv();
        storyActive = s.storyActive();
        applyCamera();
    }

    private static void applyCamera() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (cctv && cameraEntityId >= 0) {
            Entity e = mc.level.getEntity(cameraEntityId);
            if (e != null) mc.setCameraEntity(e);
        } else if (mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (cctv) applyCamera();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        if (fogStrength <= 0.001f) return;
        float far = 72.0f - 66.0f * fogStrength;
        float near = Math.max(0.0f, far * 0.08f);
        e.setNearPlaneDistance(near);
        e.setFarPlaneDistance(far);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void hud(RenderGuiOverlayEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        if (storyActive) {
            int safeTick = Math.max(0, storyTick);
            int elapsedMinutes = Math.min(15, safeTick / 1200);
            int minute = 45 + elapsedMinutes;
            int hour = 11;
            if (minute >= 60) { minute -= 60; hour = 12; }
            String time = String.format("%d:%02d PM", hour, minute);
            g.drawString(mc.font, time, 12, 12, 0xF2F2F2, true);
        }
        if (cctv) {
            int w = e.getWindow().getGuiScaledWidth();
            int h = e.getWindow().getGuiScaledHeight();
            for (int y = 0; y < h; y += 4) g.fill(0, y, w, y + 1, 0x25000000);
            g.fill(0,0,w,2,0x55000000); g.fill(0,h-2,w,h,0x55000000);
            g.fill(0,0,2,h,0x55000000); g.fill(w-2,0,w,h,0x55000000);
        }
    }
}

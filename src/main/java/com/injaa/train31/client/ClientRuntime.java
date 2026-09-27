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
        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();

        if (storyActive) {
            int safeTick = Math.max(0, storyTick);
            int elapsedMinutes = Math.min(15, safeTick / 1200);
            int minute = 45 + elapsedMinutes;
            int hour = 11;
            if (minute >= 60) { minute -= 60; hour = 12; }
            String time = String.format("%d:%02d PM", hour, minute);
            g.drawString(mc.font, time, 12, 12, 0xF2F2F2, true);

            // Final scene: stylized dark-red horror vignette and shaking threat fragments.
            if (safeTick >= 17400) {
                int phase = safeTick - 17400;
                int pulse = (int)(18 + 18 * Math.abs(Math.sin(phase * 0.11)));
                int alpha = Math.min(120, 48 + pulse);
                int edge = Math.max(12, Math.min(42, w / 10));

                g.fill(0, 0, w, h, (alpha << 24) | 0x350000);
                g.fill(0, 0, edge, h, 0x99500000);
                g.fill(w-edge, 0, w, h, 0x99500000);
                g.fill(0, 0, w, Math.max(8, edge/2), 0x77500000);
                g.fill(0, h-Math.max(8, edge/2), w, h, 0x77500000);

                int shake = Math.max(1, 5 - Math.min(4, phase / 100));
                int j1 = ((phase / 2) % (shake * 2 + 1)) - shake;
                int j2 = ((phase / 3 + 3) % (shake * 2 + 1)) - shake;
                int text = 0xFFE2E2;
                int dark = 0xFF6A6A;

                g.drawString(mc.font, "I WILL KILL YOU", 8 + j1, h/5 + j2, text, true);
                g.drawString(mc.font, "I WILL KILL YOU", w - mc.font.width("I WILL KILL YOU") - 10 - j2, h/3 + j1, dark, true);
                g.drawString(mc.font, "I WILL KILL YOU", 16 - j2, h/2 + j1, dark, true);
                g.drawString(mc.font, "I WILL KILL YOU", w - mc.font.width("I WILL KILL YOU") - 16 + j1, (h*3)/5 - j2, text, true);
                g.drawString(mc.font, "I WILL KILL YOU", 10 + j2, (h*4)/5 - j1, text, true);
                g.drawString(mc.font, "I WILL KILL YOU", w - mc.font.width("I WILL KILL YOU") - 8 - j1, (h*5)/6 + j2, dark, true);

                if ((phase / 20) % 3 != 1) {
                    String name = "INJAA...";
                    g.drawString(mc.font, name, (w - mc.font.width(name))/2 + j1, h/2 - 24 + j2, 0xFFFFEAEA, true);
                }
            }
        }

        if (cctv) {
            for (int y = 0; y < h; y += 4) g.fill(0, y, w, y + 1, 0x25000000);
            g.fill(0,0,w,2,0x55000000); g.fill(0,h-2,w,h,0x55000000);
            g.fill(0,0,2,h,0x55000000); g.fill(w-2,0,w,h,0x55000000);
        }
    }
}

package com.injaa.phonev3.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="phonev3addon", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class PhoneUpgradeHooks {
    private static final String ORIGINAL = "com.injaa.chhalavaphone.client.PhoneScreen";
    private static boolean allowOriginal;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.screen;
        if (s == null) { allowOriginal = false; return; }
        if (s.getClass().getName().equals(ORIGINAL)) {
            if (!allowOriginal) mc.setScreen(new PhoneV31Screen());
        } else if (!(s instanceof PhoneV31Screen)) allowOriginal = false;
    }

    public static void openOriginalPhone() {
        try {
            allowOriginal = true;
            Class<?> c = Class.forName(ORIGINAL);
            Screen s = (Screen)c.getDeclaredConstructor().newInstance();
            Minecraft.getInstance().setScreen(s);
        } catch (Throwable ignored) { }
    }
    private PhoneUpgradeHooks() { }
}

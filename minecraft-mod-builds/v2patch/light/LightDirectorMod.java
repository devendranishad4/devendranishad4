package com.injaa.lightdirector;

import com.injaa.lightdirector.network.ModNetwork;
import com.injaa.lightdirector.registry.ModCreativeTabs;
import com.injaa.lightdirector.registry.ModItems;
import com.injaa.lightdirector.server.LightDirectorManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LightDirectorMod.MODID)
public class LightDirectorMod {
    public static final String MODID = "lightdirector";
    public LightDirectorMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModNetwork.init();
        MinecraftForge.EVENT_BUS.register(LightDirectorManager.INSTANCE);
    }
}

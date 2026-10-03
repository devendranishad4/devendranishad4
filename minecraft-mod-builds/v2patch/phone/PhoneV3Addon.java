package com.injaa.phonev3;

import com.injaa.phonev3.registry.PhoneCreativeTab;
import com.injaa.phonev3.server.DirectionalPhoneLight;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("phonev3addon")
public class PhoneV3Addon {
    public PhoneV3Addon() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        PhoneCreativeTab.TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(new DirectionalPhoneLight());
    }
}

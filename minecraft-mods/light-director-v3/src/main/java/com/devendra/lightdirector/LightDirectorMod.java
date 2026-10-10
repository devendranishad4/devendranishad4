package com.devendra.lightdirector;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(LightDirectorMod.MODID)
public final class LightDirectorMod {
    public static final String MODID = "lightdirector";

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> DIRECTOR_REMOTE =
            ITEMS.register("director_remote", () -> new DirectorRemoteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> LIGHT_DIRECTOR_TAB =
            TABS.register("light_director", () -> CreativeModeTab.builder()
                    .title(Component.literal("Light Director"))
                    .icon(() -> DIRECTOR_REMOTE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(DIRECTOR_REMOTE.get()))
                    .build());

    public LightDirectorMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(this::creativeTab);

        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(LightDirectorController::onLevelTick);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(DIRECTOR_REMOTE.get());
        }
    }

    private void commands(RegisterCommandsEvent event) {
        LightDirectorCommands.register(event.getDispatcher());
    }
}

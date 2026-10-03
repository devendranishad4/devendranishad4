package com.injaa.lightdirector.registry;

import com.injaa.lightdirector.LightDirectorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LightDirectorMod.MODID);
    public static final RegistryObject<CreativeModeTab> LIGHT_DIRECTOR = TABS.register("light_director", () -> CreativeModeTab.builder()
            .title(Component.literal("Light Director"))
            .icon(() -> new ItemStack(ModItems.DIRECTOR_REMOTE.get()))
            .displayItems((params, output) -> output.accept(ModItems.DIRECTOR_REMOTE.get()))
            .build());
    private ModCreativeTabs() {}
}

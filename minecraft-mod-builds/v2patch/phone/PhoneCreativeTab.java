package com.injaa.phonev3.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PhoneCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "phonev3addon");
    public static final RegistryObject<CreativeModeTab> PHONE = TABS.register("advanced_phone", () -> CreativeModeTab.builder()
            .title(Component.literal("Advanced Phone"))
            .icon(() -> new ItemStack(phoneItem()))
            .displayItems((params, output) -> output.accept(phoneItem()))
            .build());

    private static Item phoneItem() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("chhalavaphone", "android_phone"));
        return item == null ? Items.COMPASS : item;
    }
    private PhoneCreativeTab() { }
}

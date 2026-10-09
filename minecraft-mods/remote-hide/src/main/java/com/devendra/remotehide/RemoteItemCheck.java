package com.devendra.remotehide;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RemoteItemCheck {
    private RemoteItemCheck() {}

    public static boolean shouldHide(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;

        String ns = id.getNamespace().toLowerCase();
        String path = id.getPath().toLowerCase();

        if (ns.equals("lightdirector")) return true;
        if (ns.equals("chhorror") && (path.contains("remote") || path.contains("director"))) return true;

        return path.contains("light_director_remote")
                || path.contains("lightdirector_remote")
                || path.contains("horror_director_remote");
    }
}

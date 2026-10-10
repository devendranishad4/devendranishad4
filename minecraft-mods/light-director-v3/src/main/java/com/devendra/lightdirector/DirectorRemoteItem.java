package com.devendra.lightdirector;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DirectorRemoteItem extends Item {
    public DirectorRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                int next = LightDirectorController.cycleRange(serverPlayer);
                player.displayClientMessage(Component.literal("Light Director range: " + next + " blocks")
                        .withStyle(ChatFormatting.AQUA), true);
            } else {
                boolean active = LightDirectorController.toggleFlicker(serverPlayer);
                player.displayClientMessage(Component.literal(active
                        ? "Light Director: flicker ON"
                        : "Light Director: restored / OFF").withStyle(ChatFormatting.YELLOW), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: toggle flicker").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift + right-click: cycle range").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("/lightdirector for blackout, speed and restore").withStyle(ChatFormatting.DARK_GRAY));
    }
}

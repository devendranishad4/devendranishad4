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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

public class DirectorRemoteItem extends Item {
    public DirectorRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                int next = LightDirectorController.cycleRange(serverPlayer);
                player.displayClientMessage(Component.literal("Light Director range: " + next + " blocks")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        } else if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.devendra.lightdirector.client.LightDirectorScreen.open());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: open Director Control Panel").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Shift + right-click: cycle range").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Blackout • Flicker • Restore • Range • Speed").withStyle(ChatFormatting.DARK_GRAY));
    }
}

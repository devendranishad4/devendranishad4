package com.injaa.train31;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Recording-safe controller: right-click prepares/teleports outside; sneak-right-click starts after 20s. */
public class DirectorItem extends Item {
    public DirectorItem(Properties properties){super(properties);}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(player instanceof ServerPlayer p))return InteractionResultHolder.pass(stack);
        if(player.isShiftKeyDown()) StoryDirector.start(p,20); else StationBuilder.prepare(p);
        return InteractionResultHolder.success(stack);
    }
}

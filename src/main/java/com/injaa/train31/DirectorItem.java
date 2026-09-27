package com.injaa.train31;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Recording controller: right-click prepares in place and starts after 20s; sneak-right-click resets. */
public class DirectorItem extends Item {
    public DirectorItem(Properties properties){super(properties);}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(player instanceof ServerPlayer p))return InteractionResultHolder.pass(stack);

        if(player.isShiftKeyDown()) {
            StoryDirector.reset(p);
        } else if(!StoryDirector.isRunning(p)) {
            StationBuilder.prepare(p); // stays exactly where the player is
            if(p.getPersistentData().getBoolean("train31_prepared")) StoryDirector.start(p,20);
        }
        p.getCooldowns().addCooldown(this,8);
        return InteractionResultHolder.success(stack);
    }
}

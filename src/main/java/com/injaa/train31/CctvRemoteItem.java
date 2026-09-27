package com.injaa.train31;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Command-free CCTV controller for recording.
 * Right-click: CAM 1 -> CAM 2 -> CAM 3 -> CAM 4 -> ...
 * Sneak + right-click: return to the player's eyes.
 */
public class CctvRemoteItem extends Item {
    private static final String INDEX_KEY = "train31_remote_cam";

    public CctvRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            StationBuilder.exitCamera(p);
            p.getCooldowns().addCooldown(this, 8);
            return InteractionResultHolder.success(stack);
        }

        StationBuilder.ensurePrepared(p);

        int index = p.getPersistentData().contains(INDEX_KEY)
                ? p.getPersistentData().getInt(INDEX_KEY)
                : -1;
        index = (index + 1) % 4;
        p.getPersistentData().putInt(INDEX_KEY, index);

        StationBuilder.enterCamera(p, index);
        p.serverLevel().playSound(null, p.blockPosition(), Train31Mod.CAMERA_CLICK.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
        p.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.success(stack);
    }
}

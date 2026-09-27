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
 * Right-click cycles: CAM 1 -> CAM 2 -> CAM 3 -> CAM 4 -> EXIT -> CAM 1...
 * Sneak + right-click also exits immediately.
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

        // Keep the old quick-exit gesture, but do not depend on it anymore.
        if (player.isShiftKeyDown()) {
            StationBuilder.exitCamera(p);
            p.getPersistentData().putInt(INDEX_KEY, -1);
            p.getCooldowns().addCooldown(this, 6);
            return InteractionResultHolder.success(stack);
        }

        StationBuilder.ensurePrepared(p);

        int index = p.getPersistentData().contains(INDEX_KEY)
                ? p.getPersistentData().getInt(INDEX_KEY)
                : -1;
        index++;

        // After CAM 4, one normal right-click returns to the player's eyes.
        if (index >= 4) {
            StationBuilder.exitCamera(p);
            p.getPersistentData().putInt(INDEX_KEY, -1);
            p.serverLevel().playSound(null, p.blockPosition(), Train31Mod.CAMERA_CLICK.get(), SoundSource.PLAYERS, 0.45f, 0.82f);
            p.getCooldowns().addCooldown(this, 6);
            return InteractionResultHolder.success(stack);
        }

        p.getPersistentData().putInt(INDEX_KEY, index);
        StationBuilder.enterCamera(p, index);
        p.serverLevel().playSound(null, p.blockPosition(), Train31Mod.CAMERA_CLICK.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
        p.getCooldowns().addCooldown(this, 6);
        return InteractionResultHolder.success(stack);
    }
}

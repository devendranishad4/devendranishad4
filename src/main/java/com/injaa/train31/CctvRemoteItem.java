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
 * Reliable CCTV controller for recording.
 * Right-click cycles: CAM 1 -> CAM 2 -> CAM 3 -> CAM 4 -> EXIT.
 * Sneak + right-click exits immediately.
 */
public class CctvRemoteItem extends Item {
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
            p.getCooldowns().addCooldown(this, 5);
            return InteractionResultHolder.success(stack);
        }

        StationBuilder.ensurePrepared(p);
        if (!p.getPersistentData().getBoolean("train31_prepared")) {
            return InteractionResultHolder.fail(stack);
        }

        // Follow the REAL active camera state instead of an old saved remote index.
        int next = StationBuilder.isCameraActive(p)
                ? StationBuilder.currentCameraIndex(p) + 1
                : 0;

        if (next >= 4) {
            StationBuilder.exitCamera(p);
            p.serverLevel().playSound(null, p.blockPosition(), Train31Mod.CAMERA_CLICK.get(), SoundSource.PLAYERS, 0.45f, 0.82f);
        } else {
            StationBuilder.enterCamera(p, next);
            p.serverLevel().playSound(null, p.blockPosition(), Train31Mod.CAMERA_CLICK.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
        }

        p.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.success(stack);
    }
}

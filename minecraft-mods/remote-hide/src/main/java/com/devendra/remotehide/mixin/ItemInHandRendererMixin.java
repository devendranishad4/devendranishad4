package com.devendra.remotehide.mixin;

import com.devendra.remotehide.RemoteItemCheck;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void remotehide$hideFirstPerson(AbstractClientPlayer player, float partialTicks, float pitch,
                                             InteractionHand hand, float swingProgress, ItemStack stack,
                                             float equippedProgress, PoseStack poseStack,
                                             MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (RemoteItemCheck.shouldHide(stack)) {
            ci.cancel();
        }
    }
}

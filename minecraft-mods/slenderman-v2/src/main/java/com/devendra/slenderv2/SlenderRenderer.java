package com.devendra.slenderv2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SlenderRenderer extends MobRenderer<SlenderEntity, HumanoidModel<SlenderEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/block/black_concrete.png");

    public SlenderRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(SlenderEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SlenderEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.72F, 1.62F, 0.72F);
    }
}

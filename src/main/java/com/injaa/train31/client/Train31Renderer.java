package com.injaa.train31.client;

import com.injaa.train31.Train31Entity;
import com.injaa.train31.Train31Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class Train31Renderer extends EntityRenderer<Train31Entity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(Train31Mod.MODID, "train31_model"), "main");

    private static final ResourceLocation BODY = tex("train_body");
    private static final ResourceLocation ROOF = tex("train_roof");
    private static final ResourceLocation WINDOW = tex("train_window");
    private static final ResourceLocation STRIPE = tex("train_stripe");
    private static final ResourceLocation DARK = tex("train_dark");
    private static final ResourceLocation LIGHT = tex("train_light");

    private final Train31Model model;

    public Train31Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Train31Model(context.bakeLayer(LAYER));
        this.shadowRadius = 0.0F;
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Train31Mod.MODID, "textures/entity/" + name + ".png");
    }

    private static VertexConsumer vc(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
    }

    @Override
    public void render(Train31Entity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0D, 0.08D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // Minecraft entity-model coordinates use +Y downward.
        pose.scale(-1.0F, -1.0F, 1.0F);

        model.setDoorsOpen(entity.doorsOpen());
        int overlay = OverlayTexture.NO_OVERLAY;
        model.renderBody(pose, vc(buffer,BODY), packedLight, overlay);
        model.renderRoof(pose, vc(buffer,ROOF), packedLight, overlay);
        model.renderDark(pose, vc(buffer,DARK), packedLight, overlay);
        model.renderDoorway(pose, vc(buffer,DARK), packedLight, overlay);
        model.renderWindows(pose, vc(buffer,WINDOW), packedLight, overlay);
        model.renderStripe(pose, vc(buffer,STRIPE), packedLight, overlay);
        model.renderDoors(pose, vc(buffer,BODY), packedLight, overlay);
        model.renderLights(pose, vc(buffer,LIGHT), 0xF000F0, overlay);

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Train31Entity entity) {
        return BODY;
    }
}

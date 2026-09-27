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
    private static final ResourceLocation BODY=tex("train_body"),ROOF=tex("train_roof"),WINDOW=tex("train_window"),STRIPE=tex("train_stripe"),DARK=tex("train_dark"),LIGHT=tex("train_light");
    private final Train31Model model;

    public Train31Renderer(EntityRendererProvider.Context context){super(context);model=new Train31Model(context.bakeLayer(LAYER));shadowRadius=0.0F;}
    private static ResourceLocation tex(String name){return new ResourceLocation(Train31Mod.MODID,"textures/entity/"+name+".png");}
    private static VertexConsumer vc(MultiBufferSource buffer,ResourceLocation texture){return buffer.getBuffer(RenderType.entityCutoutNoCull(texture));}

    @Override
    public void render(Train31Entity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffer,int packedLight){
        pose.pushPose();
        pose.translate(0.0D,3.75D,0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F-yaw));
        pose.scale(-1.0F,-1.0F,1.0F);
        model.setDoorsOpen(entity.doorsOpen());
        int o=OverlayTexture.NO_OVERLAY;
        model.renderBody(pose,vc(buffer,BODY),packedLight,o);
        model.renderRoof(pose,vc(buffer,ROOF),packedLight,o);
        model.renderDark(pose,vc(buffer,DARK),packedLight,o);
        model.renderDoorway(pose,vc(buffer,DARK),packedLight,o);
        model.renderInterior(pose,vc(buffer,STRIPE),packedLight,o);
        model.renderPoles(pose,vc(buffer,ROOF),packedLight,o);
        model.renderWindows(pose,vc(buffer,WINDOW),packedLight,o);
        model.renderStripe(pose,vc(buffer,STRIPE),packedLight,o);
        model.renderDoors(pose,vc(buffer,BODY),packedLight,o);
        model.renderLights(pose,vc(buffer,LIGHT),0xF000F0,o);
        pose.popPose();
        super.render(entity,yaw,partialTick,pose,buffer,packedLight);
    }

    @Override public ResourceLocation getTextureLocation(Train31Entity entity){return BODY;}
}

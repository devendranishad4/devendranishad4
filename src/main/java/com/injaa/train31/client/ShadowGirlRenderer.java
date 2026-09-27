package com.injaa.train31.client;

import com.injaa.train31.ShadowGirlEntity;
import com.injaa.train31.Train31Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShadowGirlRenderer extends MobRenderer<ShadowGirlEntity, HumanoidModel<ShadowGirlEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(Train31Mod.MODID, "shadow_girl"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Train31Mod.MODID, "textures/entity/shadow_girl.png");
    private static final ResourceLocation FINAL_TEXTURE = new ResourceLocation(Train31Mod.MODID, "textures/entity/shadow_girl_final.png");

    public ShadowGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.38f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowGirlEntity entity) {
        return entity.isFinalForm() ? FINAL_TEXTURE : TEXTURE;
    }

    @Override
    protected void scale(ShadowGirlEntity entity, PoseStack pose, float partialTick) {
        if(entity.isTall()) pose.scale(1.12F,2.05F,1.12F);
        else if(entity.isFinalForm()) pose.scale(1.08F,1.10F,1.08F);
        else pose.scale(0.98F,1.03F,0.98F);
    }
}

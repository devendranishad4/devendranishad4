package com.injaa.train31.client;

import com.injaa.train31.ShadowGirlEntity;
import com.injaa.train31.Train31Mod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShadowGirlRenderer extends MobRenderer<ShadowGirlEntity, HumanoidModel<ShadowGirlEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(Train31Mod.MODID, "shadow_girl"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Train31Mod.MODID, "textures/entity/shadow_girl.png");

    public ShadowGirlRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.38f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowGirlEntity entity) {
        return TEXTURE;
    }
}

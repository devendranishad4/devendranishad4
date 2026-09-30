package com.injaa.room203.client;
import com.injaa.room203.Visitor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class VisitorRenderer extends HumanoidMobRenderer<Visitor,HumanoidModel<Visitor>> {
 private static final ResourceLocation TEXTURE=new ResourceLocation("room203","textures/entity/visitor.png");
 public VisitorRenderer(EntityRendererProvider.Context c){super(c,new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE)),.35f);}
 @Override public ResourceLocation getTextureLocation(Visitor v){return TEXTURE;}
}

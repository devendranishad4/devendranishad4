package com.injaa.lastelevator.client;

import com.injaa.lastelevator.LastElevator;
import com.injaa.lastelevator.Passenger;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class PassengerRenderer extends MobRenderer<Passenger,PassengerModel> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(new ResourceLocation(LastElevator.ID,"passenger"),"main");
    public PassengerRenderer(EntityRendererProvider.Context c){super(c,new PassengerModel(c.bakeLayer(LAYER)),.5f);}
    @Override public ResourceLocation getTextureLocation(Passenger entity){return new ResourceLocation(LastElevator.ID,"textures/entity/passenger.png");}
    @Mod.EventBusSubscriber(modid=LastElevator.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class Events {
        @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,PassengerModel::layer);}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(LastElevator.PASSENGER.get(),PassengerRenderer::new);}
    }
}

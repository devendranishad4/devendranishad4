package com.injaa.villagedawn.client;

import com.injaa.villagedawn.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientEvents{
    static final ModelLayerLocation LAYER=new ModelLayerLocation(new ResourceLocation(VillageDawn.ID,"caller"),"main");
    static final KeyMapping OPEN=new KeyMapping("key.villagedawn.director",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.villagedawn");
    @Mod.EventBusSubscriber(modid=VillageDawn.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class ModEvents{
        @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,CallerModel::layer);}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(VillageDawn.CALLER.get(),CallerRenderer::new);}
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(OPEN);}
    }
    @Mod.EventBusSubscriber(modid=VillageDawn.ID,value=Dist.CLIENT)
    public static class ForgeEvents{
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END)while(OPEN.consumeClick())if(Minecraft.getInstance().player!=null)Minecraft.getInstance().setScreen(new Panel());}
    }
    public static class CallerRenderer extends MobRenderer<Caller,CallerModel>{
        public CallerRenderer(EntityRendererProvider.Context c){super(c,new CallerModel(c.bakeLayer(LAYER)),.45f);}
        @Override public ResourceLocation getTextureLocation(Caller e){return new ResourceLocation(VillageDawn.ID,"textures/entity/caller.png");}
    }
    /** Command-driven client controls; no visible held remote or custom network channel. */
    public static class Panel extends Screen{
        private static final double[] gains={1,1,1};
        public Panel(){super(Component.literal("Village Dawn Director"));}
        void run(String command){if(minecraft!=null&&minecraft.player!=null)minecraft.player.connection.sendCommand("vd "+command);onClose();}
        @Override protected void init(){
            int x=width/2-155,y=Math.max(38,height/2-95);
            String[] labels={"Start: 20 sec","Start: 15 sec","Start: 10 sec","Pause","Resume","Status","Rehearsal pace","Filming pace","Retry chase"};
            String[] cmds={"auto 20","auto 15","auto 10","pause","resume","status","pace 10","pace 1","retrychase"};
            for(int i=0;i<labels.length;i++){final String cmd=cmds[i];addRenderableWidget(Button.builder(Component.literal(labels[i]),b->run(cmd)).bounds(x+(i%3)*105,y+(i/3)*25,100,22).build());}
            String[] channels={"voice","ambience","scare"};
            for(int i=0;i<3;i++)addRenderableWidget(new MixSlider(x,y+80+i*25,310,22,channels[i],i));
            addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2-50,y+158,100,20).build());
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int x,int y,float dt){renderBackground(g);int top=Math.max(38,height/2-95);g.drawCenteredString(font,title,width/2,top-25,0xead7a3);g.drawCenteredString(font,"Prepare/check first. Rehearsal build.",width/2,top-13,0xffffff);super.render(g,x,y,dt);}
        class MixSlider extends AbstractSliderButton{
            final String channel;final int index;
            MixSlider(int x,int y,int w,int h,String channel,int index){super(x,y,w,h,Component.empty(),gains[index]);this.channel=channel;this.index=index;updateMessage();}
            @Override protected void updateMessage(){setMessage(Component.literal(channel+" volume: "+Math.round(value*100)+"%"));}
            @Override protected void applyValue(){gains[index]=value;if(minecraft!=null&&minecraft.player!=null)minecraft.player.connection.sendCommand("vd mix "+channel+" "+String.format(java.util.Locale.ROOT,"%.2f",value));}
        }
    }
}

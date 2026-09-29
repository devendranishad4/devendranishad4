package com.injaa.lastelevator.client;

import com.injaa.lastelevator.LastElevator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tiny top-right filming HUD: a short objective and relative route arrow. */
public final class DirectorHud {
    private DirectorHud(){}
    private static boolean visible;
    private static String objective="";
    private static BlockPos target=BlockPos.ZERO;

    public static void update(boolean show,String text,BlockPos point){
        visible=show;objective=text;target=point;
    }
    private static final IGuiOverlay OVERLAY=(gui,graphics,partialTick,width,height)->{
        Minecraft mc=Minecraft.getInstance();
        if(!visible||mc.player==null||mc.options.hideGui)return;
        int box=168,x=width-box-8,y=8;
        graphics.fill(x,y,x+box,y+39,0x990B1017);
        graphics.fill(x,y,x+2,y+39,0xFFB78A50);
        graphics.drawString(mc.font,"LAST ELEVATOR",x+8,y+5,0xFFE0B87D,false);
        graphics.drawString(mc.font,objective,x+8,y+16,0xFFF1EFEA,false);
        double dx=target.getX()+.5-mc.player.getX(),dz=target.getZ()+.5-mc.player.getZ();
        double dy=target.getY()-mc.player.getY();
        int metres=(int)Math.round(Math.sqrt(dx*dx+dy*dy+dz*dz));
        String direction;
        if(objective.startsWith("Lift moving"))direction="DOORS OPEN ON ARRIVAL";
        else if(metres<4)direction="HERE — INTERACT";
        else if(Math.hypot(dx,dz)<4&&dy>3)direction="UP  "+metres+"m";
        else if(Math.hypot(dx,dz)<4&&dy< -3)direction="DOWN  "+metres+"m";
        else {
            float desired=(float)(Math.atan2(-dx,dz)*180/Math.PI);
            float turn=Mth.wrapDegrees(desired-mc.player.getYRot());
            direction=(Math.abs(turn)<28?"AHEAD":Math.abs(turn)>150?"BEHIND":turn<0?"LEFT":"RIGHT")
                    +"  "+metres+"m";
        }
        graphics.drawString(mc.font,direction,x+8,y+27,0xFFAFDCCF,false);
    };
    @Mod.EventBusSubscriber(modid=LastElevator.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent e){
            e.registerAboveAll("director_objective",OVERLAY);
        }
    }
    @Mod.EventBusSubscriber(modid=LastElevator.ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    public static final class Logout {
        @SubscribeEvent public static void leave(ClientPlayerNetworkEvent.LoggingOut e){visible=false;}
    }
}

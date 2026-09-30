package com.injaa.room203.client;
import com.injaa.room203.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
public final class RoomHud {
 private static boolean visible,dark;private static String objective="",note="";private static BlockPos target=BlockPos.ZERO;private static int noteTicks;
 public static void update(HudNetwork.Message m){visible=m.visible();dark=m.dark();objective=m.objective();target=m.target();if(!m.note().isEmpty()){note=m.note();noteTicks=m.noteTicks();}}
 private static final IGuiOverlay VIEW=(gui,g,partial,w,h)->{
  var mc=Minecraft.getInstance();if(mc.player==null)return;
  if(dark)g.fill(0,0,w,h,0x4004060A);
  if(!visible||mc.options.hideGui)return;
  int box=178,x=w-box-8,y=8;g.fill(x,y,x+box,y+39,0x990A0D13);
  g.drawString(mc.font,"ROOM 203",x+7,y+5,0xFFCCB69A,false);g.drawString(mc.font,objective,x+7,y+16,0xFFE3E3DE,false);
  double dx=target.getX()+.5-mc.player.getX(),dz=target.getZ()+.5-mc.player.getZ(),dy=target.getY()-mc.player.getY();int dist=(int)Math.round(Math.sqrt(dx*dx+dz*dz+dy*dy));
  float turn=Mth.wrapDegrees((float)(Math.atan2(-dx,dz)*180/Math.PI)-mc.player.getYRot());
  String a=dist<3?"HERE":Math.hypot(dx,dz)<4&&dy>3?"UPSTAIRS":Math.hypot(dx,dz)<4&&dy< -3?"DOWNSTAIRS":Math.abs(turn)<28?"AHEAD":Math.abs(turn)>150?"BEHIND":turn<0?"LEFT":"RIGHT";
  g.drawString(mc.font,a+"  "+dist+"m",x+7,y+27,0xFFADBFB7,false);
  if(noteTicks>0){var lines=mc.font.split(net.minecraft.network.chat.Component.literal(note),box-14);int hh=lines.size()*10+12;g.fill(x,y+44,x+box,y+44+hh,0xDD0A0D13);int yy=y+50;for(var line:lines){g.drawString(mc.font,line,x+7,yy,0xFFD8C7AD,false);yy+=10;}}
 };
 @Mod.EventBusSubscriber(modid=Room203.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
 public static class Register{
  @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent e){e.registerAboveAll("objectives",VIEW);}
  @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Room203.VISITOR.get(),VisitorRenderer::new);}
 }
 @Mod.EventBusSubscriber(modid=Room203.ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
 public static class Events{
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&noteTicks>0&&!Minecraft.getInstance().isPaused())noteTicks--;}
  @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){visible=dark=false;noteTicks=0;note="";}
 }
}

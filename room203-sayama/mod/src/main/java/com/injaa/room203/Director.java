package com.injaa.room203;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Server authority: chapter gates depend on movement/interactions, never timed teleportation. */
public final class Director {
 private static final String KEY="room203_director";
 private static final String[] NAMES={"Arrival","Unpack","Three knocks","The neighbour","Return","Upstairs diary","Last warning","Escape","Street ending","Complete"};
 private static final BlockPos[] IN={MapEdits.STAIR,new BlockPos(667,36,-177),new BlockPos(673,36,-182),MapEdits.DOOR};
 private static final BlockPos[] UP={new BlockPos(673,36,-182),new BlockPos(667,36,-177),new BlockPos(667,36,-173),new BlockPos(667,41,-173),new BlockPos(673,41,-182),MapEdits.UP_CHEST};
 private static final BlockPos[] OUT={new BlockPos(673,41,-182),new BlockPos(667,41,-178),new BlockPos(667,41,-173),new BlockPos(667,36,-173),new BlockPos(667,36,-177),MapEdits.STAIR,MapEdits.EXIT};
 private static CompoundTag state(ServerPlayer p){CompoundTag d=p.getPersistentData();if(!d.contains(KEY))d.put(KEY,new CompoundTag());return d.getCompound(KEY);}
 private static ServerLevel world(ServerPlayer p){return p.getServer().overworld();}
 private static void msg(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[Room 203] "+s));}
 @SubscribeEvent public void commands(RegisterCommandsEvent e){
  var root=Commands.literal("room203").requires(s->s.hasPermission(2));
  root.then(Commands.literal("setup").executes(c->setup(c.getSource().getPlayerOrException())));
  root.then(Commands.literal("auto").executes(c->start(c.getSource().getPlayerOrException(),20)).then(Commands.argument("delay",IntegerArgumentType.integer(10,60)).executes(c->start(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"delay")))));
  root.then(Commands.literal("reset").executes(c->start(c.getSource().getPlayerOrException(),20)));
  root.then(Commands.literal("stop").executes(c->stop(c.getSource().getPlayerOrException(),false)));
  root.then(Commands.literal("restore").executes(c->stop(c.getSource().getPlayerOrException(),true)));
  root.then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();var d=state(p);msg(p,d.getBoolean("running")?"Chapter "+(d.getInt("stage")+1)+": "+NAMES[d.getInt("stage")]+" — "+d.getInt("ticks")/20+"s":"Stopped. /room203 auto starts the episode.");return 1;}));
  root.then(Commands.literal("volume").then(Commands.argument("percent",IntegerArgumentType.integer(0,100)).executes(c->{var p=c.getSource().getPlayerOrException();state(p).putInt("volume",IntegerArgumentType.getInteger(c,"percent"));msg(p,"Story audio: "+state(p).getInt("volume")+"%.");return 1;})));
  root.then(Commands.literal("hud").then(Commands.literal("off").executes(c->{var p=c.getSource().getPlayerOrException();state(p).putBoolean("hideHud",true);hud(p,"",0);return 1;})).then(Commands.literal("on").executes(c->{var p=c.getSource().getPlayerOrException();state(p).putBoolean("hideHud",false);hud(p,"",0);return 1;})));
  e.getDispatcher().register(root);
 }
 private static boolean available(ServerPlayer p){
  if(p.level().dimension()!=Level.OVERWORLD){msg(p,"Use this in the Sayama overworld.");return false;}
  for(var other:p.getServer().getPlayerList().getPlayers())if(other!=p&&state(other).getBoolean("running")){msg(p,"Another player is directing this single-player episode.");return false;}
  return true;
 }
 private static int setup(ServerPlayer p){if(!available(p))return 0;var w=world(p);if(!MapEdits.correctMap(w)){msg(p,"Map check failed. Open the original Sayama-0.3 world; no edits were applied.");return 0;}MapEdits.get(w).install(w);msg(p,"Room 203 is ready. /room203 auto moves you to the street and gives 20 seconds to record.");return 1;}
 private static int start(ServerPlayer p,int delay){
  if(!available(p))return 0;var w=world(p);if(!MapEdits.correctMap(w)){msg(p,"Map check failed. Use the original Sayama v0.3 world.");return 0;}
  var d=state(p);boolean hadEnv=d.contains("oldTime");stopAudio(p);cleanup(p);var edit=MapEdits.get(w);edit.install(w);edit.lighting(w,true);edit.door(w,false);edit.openRoute(w);
  if(!hadEnv){d.putInt("oldMode",p.gameMode.getGameModeForPlayer().getId());d.putBoolean("oldMobSpawning",w.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING));d.putLong("oldTime",w.getDayTime());d.putBoolean("oldDaylight",w.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));d.putBoolean("oldWeather",w.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE));d.putBoolean("oldRain",w.isRaining());d.putBoolean("oldThunder",w.isThundering());d.putInt("oldRainTime",w.getLevelData().getRainTime());d.putInt("oldClearTime",w.getLevelData().getClearWeatherTime());d.putInt("oldThunderTime",w.getLevelData().getThunderTime());}
  p.setGameMode(GameType.ADVENTURE);w.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.getServer());w.setDayTime(18000);w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.getServer());w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,p.getServer());w.setWeatherParameters(0,120000,true,false);
  if(!d.contains("volume"))d.putInt("volume",80);
  d.putBoolean("running",true);d.putBoolean("dark",false);d.putInt("delay",delay*20);d.putInt("stage",0);d.putInt("ticks",0);d.putInt("waypoint",0);d.putInt("ambience",0);d.remove("doorChecked");
  var a=MapEdits.START;w.getChunkAt(a);p.teleportTo(w,a.getX()+.5,a.getY(),a.getZ()+.5,145,0);
  msg(p,"AUTO armed. "+delay+" seconds to close chat and begin recording. Follow the small objective at the right.");hud(p,"",0);return 1;
 }
 private static int stop(ServerPlayer p,boolean restore){
  var d=state(p);stopAudio(p);cleanup(p);d.putBoolean("running",false);d.putBoolean("dark",false);
  var w=world(p);var edit=MapEdits.get(w);if(edit.installed){edit.lighting(w,true);edit.door(w,false);}
  if(d.contains("oldTime")){p.setGameMode(GameType.byId(d.getInt("oldMode")));w.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(d.getBoolean("oldMobSpawning"),p.getServer());w.setDayTime(d.getLong("oldTime"));w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(d.getBoolean("oldDaylight"),p.getServer());w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(d.getBoolean("oldWeather"),p.getServer());w.setWeatherParameters(d.getInt("oldClearTime"),d.getInt("oldRainTime"),d.getBoolean("oldRain"),d.getBoolean("oldThunder"));w.getLevelData().setThunderTime(d.getInt("oldThunderTime"));d.remove("oldTime");}
  if(restore)edit.restore(w);HudNetwork.send(p,false,"",BlockPos.ZERO,"",0,false);msg(p,restore?"Addon edits restored to their original blocks.":"Stopped. /room203 reset restarts the episode.");return 1;
 }
 private static void cleanup(ServerPlayer p){
  var d=state(p);if(d.hasUUID("actor")){Entity v=world(p).getEntity(d.getUUID("actor"));if(v!=null)v.discard();d.remove("actor");}
  // Remove only this player's director actors, including actors loaded after a reconnect.
  world(p).getEntities(Room203.VISITOR.get(),v->v.getTags().contains("room203_"+p.getUUID())).forEach(Entity::discard);
 }
 private static Visitor actor(ServerPlayer p,BlockPos at,boolean chase){cleanup(p);var w=world(p);w.getChunkAt(at);Visitor v=Room203.VISITOR.get().create(w);if(v==null)return null;v.moveTo(at.getX()+.5,at.getY()+.0625,at.getZ()+.5,180,0);v.addTag("room203_"+p.getUUID());v.setSilent(true);v.setInvulnerable(true);if(chase)v.hunt(p);else{v.watch();v.setNoAi(true);double dx=p.getX()-v.getX(),dz=p.getZ()-v.getZ();v.setYRot((float)(Math.atan2(-dx,dz)*180/Math.PI));v.setYHeadRot(v.getYRot());}w.addFreshEntity(v);state(p).putUUID("actor",v.getUUID());return v;}
 private static void audio(ServerPlayer p,String id,BlockPos at,float loud,float pitch){world(p).playSound(null,at,Room203.AUDIO_EVENTS.get(id).get(),SoundSource.AMBIENT,loud*state(p).getInt("volume")/100f,pitch);}
 private static void stopAmbience(ServerPlayer p){for(String id:new String[]{"rain","hall","pursuit"})p.connection.send(new ClientboundStopSoundPacket(new ResourceLocation(Room203.ID,id),SoundSource.AMBIENT));}
 private static void stopAudio(ServerPlayer p){for(String id:Room203.AUDIO_EVENTS.keySet())p.connection.send(new ClientboundStopSoundPacket(new ResourceLocation(Room203.ID,id),SoundSource.AMBIENT));}

 private static boolean near(ServerPlayer p,BlockPos at,double r){return Math.abs(p.getY()-at.getY())<2&&p.position().distanceToSqr(at.getCenter().add(0,-.5,0))<r*r;}
 private static boolean inside(ServerPlayer p){return p.getY()>=35.5&&p.getY()<39.5&&p.getX()<671&&p.getX()>663&&p.getZ()>-204&&p.getZ()< -198;}
 private static void chapter(ServerPlayer p,int stage,String note){stopAmbience(p);var d=state(p);d.putInt("stage",stage);d.putInt("ticks",0);d.putInt("waypoint",0);d.putInt("ambience",0);hud(p,note,360);}
 private static BlockPos route(ServerPlayer p,BlockPos[] a){var d=state(p);int i=Math.min(d.getInt("waypoint"),a.length-1);if(i<a.length-1&&near(p,a[i],2.1)){d.putInt("waypoint",++i);}return a[i];}
 private static void hud(ServerPlayer p,String note,int noteTicks){
  var d=state(p);if(!d.getBoolean("running")){HudNetwork.send(p,false,"",BlockPos.ZERO,note,noteTicks,false);return;}
  int s=d.getInt("stage"),t=d.getInt("ticks")/20;String goal;BlockPos at;
  if(d.getInt("delay")>0){goal="Recording starts: "+((d.getInt("delay")+19)/20)+"s";at=MapEdits.START;}
  else switch(s){
   case 0->{at=route(p,IN);goal=d.getInt("waypoint")==0?"Find the apartment stairs":d.getInt("waypoint")==1?"Climb to the second floor":"Enter Room 203";}
   case 1->{goal="Open your bedside chest";at=MapEdits.CHEST;}
   case 2->{goal=t<60?"Listen. Stay inside 203":"Check your front door";at=t<60?MapEdits.CHEST:MapEdits.DOOR;}
   case 3->{goal="Look down the corridor";at=new BlockPos(673,36,-188);}
   case 4->{goal="Return inside Room 203";at=new BlockPos(669,36,-200);}
   case 5->{at=route(p,UP);goal=d.getInt("waypoint")<4?"Take the stairs to floor 3":"Open the chest in 303";}
   case 6->{goal="Leave Room 303";at=new BlockPos(673,41,-200);}
   case 7->{at=route(p,OUT);goal=d.getInt("waypoint")<5?"RUN down the stairwell":"RUN to the open street";}
   case 8->{goal="Stay outside. Listen.";at=MapEdits.EXIT;}
   default->{goal="Episode complete";at=MapEdits.EXIT;}
  }
  HudNetwork.send(p,!d.getBoolean("hideHud"),goal,at,note,noteTicks,d.getBoolean("dark"));
 }
 @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;var d=state(p);if(!d.getBoolean("running"))return;
  if(p.level().dimension()!=Level.OVERWORLD){stop(p,false);return;}
  int delay=d.getInt("delay");if(delay>0){d.putInt("delay",delay-1);if(delay%20==0)hud(p,"",0);return;}
  int tick=d.getInt("ticks")+1;d.putInt("ticks",tick);int s=d.getInt("stage");var edit=MapEdits.get(world(p));
  int ambient=d.getInt("ambience");if(ambient<=0&&s<9){String id=s==7?"pursuit":s==0||s==8?"rain":"hall";audio(p,id,p.blockPosition(),s==7?.65f:.23f,1);d.putInt("ambience",s==7?640:600);}else d.putInt("ambience",ambient-1);
  if(s==0&&inside(p))chapter(p,1,"You rented Room 203. Your belongings are in the chest.");
  else if(s==2){
   if(tick==400)audio(p,"steps",new BlockPos(669,41,-200),.8f,1);
   if(tick==680)audio(p,"knock",MapEdits.DOOR,.8f,1);
   if(tick==760)audio(p,"steps",new BlockPos(673,36,-188),.6f,.9f);
   if(tick==1000)audio(p,"knock",MapEdits.DOOR,1,1);
   if(tick==1200)hud(p,"Three knocks. Nobody said a word.",160);
  }
  else if(s==3){
   if(tick==20){actor(p,new BlockPos(673,36,-188),false);audio(p,"breath",new BlockPos(673,36,-188),.8f,1);}
   if(tick>=60&&d.hasUUID("actor")){Entity v=world(p).getEntity(d.getUUID("actor"));if(v!=null&&p.hasLineOfSight(v)&&p.getLookAngle().dot(v.position().add(0,1.4,0).subtract(p.getEyePosition()).normalize())>.83){audio(p,"sting",v.blockPosition(),.7f,1);cleanup(p);chapter(p,4,"He was standing outside 201. Then he disappeared.");}}
   if(tick==400){cleanup(p);chapter(p,4,"The corridor is empty. Return to 203.");}
  }
  else if(s==4&&inside(p)){edit.door(world(p),false);edit.lighting(world(p),false);d.putBoolean("dark",true);audio(p,"fault",MapEdits.CHEST,.65f,1);chapter(p,5,"A note under your door: 'The tenant in 303 saw him too. His diary is still upstairs.'");}
  else if(s==6){
   if(tick==100)audio(p,"knock",new BlockPos(671,41,-200),1,.85f);
   if(tick==160){edit.openRoute(world(p));actor(p,new BlockPos(673,41,-188),true);audio(p,"sting",p.blockPosition(),1,1);chapter(p,7,"He is in the corridor. Get down the stairs and outside.");}
  }
  else if(s==7){
   if(tick%40==0&&d.hasUUID("actor")){Entity v=world(p).getEntity(d.getUUID("actor"));if(v instanceof Visitor visitor&&visitor.distanceTo(p)<3)audio(p,"breath",visitor.blockPosition(),.6f,1);}
   if(near(p,MapEdits.EXIT,4)){cleanup(p);d.putBoolean("dark",false);edit.lighting(world(p),true);chapter(p,8,"You made it outside. The footsteps have stopped.");}
  }
  else if(s==8){
   if(tick==300)audio(p,"knock",p.blockPosition(),.75f,1);
   if(tick==460){actor(p,new BlockPos(680,30,-174),false);audio(p,"sting",new BlockPos(680,30,-174),.65f,.9f);hud(p,"Three knocks followed you outside.",180);}
   if(tick==640){cleanup(p);chapter(p,9,"ROOM 203 — END. /room203 reset records another take.");}
  }
  if(tick%20==0)hud(p,"",0);
 }
 @SubscribeEvent public void click(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()!=InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p))return;var d=state(p);if(!d.getBoolean("running")||d.getInt("delay")>0)return;int s=d.getInt("stage");BlockPos at=e.getPos();
  if(s==1&&at.equals(MapEdits.CHEST)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);chapter(p,2,"Lease note: 'Room 201 is empty. Keep your door closed after midnight.'");}
  else if(s==2&&d.getInt("ticks")>=1200&&(at.equals(MapEdits.DOOR)||at.equals(MapEdits.DOOR.above()))){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);MapEdits.get(world(p)).door(world(p),true);chapter(p,3,"");}
  else if(s==5&&at.equals(MapEdits.UP_CHEST)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);chapter(p,6,"Diary: 'He copies the footsteps above you. Three knocks means he knows which room is yours. Leave before he reaches your door.'");}
 }
 @SubscribeEvent public void clone(PlayerEvent.Clone e){var old=e.getOriginal().getPersistentData();if(old.contains(KEY))e.getEntity().getPersistentData().put(KEY,old.getCompound(KEY).copy());}
 @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){if(state(p).getBoolean("running"))hud(p,"Session resumed. Follow the objective.",100);}}
}

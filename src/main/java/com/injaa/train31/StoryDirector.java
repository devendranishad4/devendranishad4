package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 15-minute clock-driven episode: 11:45 PM -> midnight.
 * Major escalation lands roughly every 30 seconds from 11:47 onward.
 * No chat/action-bar narration during recording.
 */
public final class StoryDirector {
    private StoryDirector() {}
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        int tick;
        float fog;
        UUID girl;
        boolean trainSpawned;
        State(int delayTicks){tick=-delayTicks;}
    }

    public static void start(ServerPlayer player,int delaySeconds){
        if(STATES.containsKey(player.getUUID()))return;
        if(!SceneSetup.complete(player)){SceneSetup.status(player);return;}
        StationBuilder.ensurePrepared(player);
        if(!player.getPersistentData().getBoolean("train31_prepared"))return;
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player);
        LightingController.restore(player);
        State s=new State(Math.max(0,delaySeconds)*20);
        STATES.put(player.getUUID(),s);
        sync(player,s,-1,false);
    }

    public static boolean isRunning(ServerPlayer p){return STATES.containsKey(p.getUUID());}
    public static int currentTick(ServerPlayer p){State s=STATES.get(p.getUUID());return s==null?0:s.tick;}
    public static float currentFog(ServerPlayer p){State s=STATES.get(p.getUUID());return s==null?0f:s.fog;}

    public static void tick(ServerPlayer player){
        State s=STATES.get(player.getUUID()); if(s==null)return;
        s.tick++;
        int t=s.tick;
        ServerLevel level=player.serverLevel();
        StationBuilder.RailGeometry g=StationBuilder.geometry(player);

        if(t<0){if(t%20==0)sync(player,s,-1,false);return;}

        // 11:45-11:46:30 — completely normal. Let the audience learn the station.
        if(t==600) play(level,g.rail(),Train31Mod.FLUORESCENT_BUZZ.get(),0.55f,1.0f);
        if(t==1200) play(level,g.rail(),Train31Mod.PA_NORMAL.get(),3.4f,1.0f);
        if(t==1800) play(level,StationBuilder.cctvRoom(player),Train31Mod.CAMERA_CLICK.get(),0.55f,1.0f);

        // 11:47 — CAM 3: impossibly tall silhouette appears far down the selected platform.
        if(t==2400){
            StationBuilder.enterCamera(player,2);
            spawnGirl(level,s,platformPos(g,30),true,false);
        }
        if(t==2640) play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.WHISPER_INJAA.get(),2.8f,0.88f);

        // 11:47:23 — face suddenly fills the camera, then the feed dies.
        if(t==2860){
            spawnGirlAtCameraFace(player,s,2,1.15);
            play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.CCTV_STATIC.get(),2.1f,0.72f);
        }
        if(t==2920){remove(level,s.girl);s.girl=null;StationBuilder.exitCamera(player);}

        // 11:47:30 — first crazy whole-platform flicker burst.
        if(t>=3000 && t<3220 && t%14==0) LightingController.pulse(player,((t/14)&1)==0);
        if(t==3220) LightingController.restore(player);

        // 11:48 — harder flicker + metal impacts from the track.
        if(t==3600) play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),2.1f,0.85f);
        if(t>=3600 && t<3760 && t%10==0) LightingController.pulse(player,((t/10)&1)==0);
        if(t==3760) LightingController.restore(player);

        // 11:48:30 — one light flash reveals her at the end of the platform.
        if(t==4200){LightingController.pulse(player,true);spawnGirl(level,s,platformPos(g,24),false,false);}
        if(t==4240){LightingController.restore(player);remove(level,s.girl);s.girl=null;}

        // 11:49 — the whisper is no longer only inside CCTV.
        if(t==4800) play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.2f,0.82f);

        // 11:49:30 — camera comes back; she is much closer.
        if(t==5400){StationBuilder.enterCamera(player,1);spawnGirl(level,s,platformPos(g,9),false,true);}
        if(t==5560) play(level,StationBuilder.cameraBlockPos(player,1),Train31Mod.CCTV_STATIC.get(),1.8f,0.75f);
        if(t==5620){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:50 — presence behind the player, but gone when the light snaps back.
        if(t==6000){spawnGirlBehindPlayer(level,player,s,7.0,false);play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.4f,0.72f);}
        if(t==6120){remove(level,s.girl);s.girl=null;}

        // 11:50:30 — tunnel begins to answer.
        if(t==6600){play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.0f,0.72f);play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),1.3f,0.65f);}

        // 11:51 — total station blackout. Low visibility, but no giant fog wall.
        if(t==7200){
            play(level,g.rail(),Train31Mod.POWER_DOWN.get(),3.0f,0.92f);
            LightingController.pulse(player,true);
            s.fog=0.28f;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,220,0,false,false));
        }

        // 11:51:30 — voice in the dark, then a camera relay clicks by itself.
        if(t==7800){play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.6f,0.67f);play(level,StationBuilder.cctvRoom(player),Train31Mod.CAMERA_CLICK.get(),1.2f,0.7f);}

        // 11:52 — CCTV forcibly catches her on the platform again.
        if(t==8400){StationBuilder.enterCamera(player,0);spawnGirl(level,s,platformPos(g,14),false,true);}
        if(t==8500) moveGirl(level,s.girl,platformPos(g,6));
        if(t==8580){play(level,StationBuilder.cameraBlockPos(player,0),Train31Mod.CCTV_STATIC.get(),2.0f,0.62f);StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:52:30 — blackout flickers violently for a few seconds and a distant roar is heard.
        if(t>=9000 && t<9160 && t%8==0) LightingController.pulse(player,((t/8)&1)==0);
        if(t==9000) play(level,SceneSetup.tunnel(player),Train31Mod.GIRL_ROAR.get(),1.0f,0.62f);
        if(t==9160) LightingController.pulse(player,true);

        // 11:53 — something starts pacing with the player in the darkness.
        if(t==9600) play(level,behindPlayer(player,5).blockPosition(),Train31Mod.METAL_KNOCKS.get(),1.25f,0.72f);

        // 11:53:30 — first distant train horn from the exact real tunnel marker.
        if(t==10200) play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),2.0f,0.74f);

        // 11:54 — Train 31 now truly spawns inside the marked tunnel and follows that rail to the stop.
        if(t==10800){
            StationBuilder.spawnTrain(player); s.trainSpawned=true;
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),3.2f,0.96f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_ROLL.get(),2.4f,1.0f);
        }
        if(t>=10800 && t<=12000 && s.trainSpawned){
            double p=(t-10800)/1200.0;
            double eased=1.0-Math.pow(1.0-Math.min(1.0,p),3.0);
            StationBuilder.setTrainProgress(player,eased);
        }

        // 11:54:30 — real brake squeal while it is entering the platform.
        if(t==11400) play(level,g.rail(),Train31Mod.TRAIN_BRAKES.get(),2.8f,0.96f);

        // 11:55 — stopped exactly on the manually marked other rail.
        if(t==12000){StationBuilder.setTrainProgress(player,1.0);s.fog=0.18f;}

        // 11:55:30 — door warning and doors open so the player can investigate Train 31.
        if(t==12600){play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.2f,1.0f);StationBuilder.openTrainDoors(player,true);}

        // 11:56 — she is visible deep inside / through the open train doorway.
        if(t==13200){spawnGirl(level,s,trainInteriorPos(player,8.0),false,true);play(level,g.rail(),Train31Mod.WHISPER_INJAA.get(),2.0f,0.73f);}

        // 11:56:30 — doors close and the carriage lights/station lights snap.
        if(t==13800){StationBuilder.openTrainDoors(player,false);play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.0f,0.82f);remove(level,s.girl);s.girl=null;}
        if(t>=13800 && t<13980 && t%10==0) LightingController.pulse(player,((t/10)&1)==0);
        if(t==13980) LightingController.pulse(player,true);

        // 11:57 — she reappears much nearer the carriage/platform.
        if(t==14400) spawnGirl(level,s,trainInteriorPos(player,3.8),false,true);
        if(t==14540){play(level,g.rail(),Train31Mod.CCTV_STATIC.get(),1.2f,0.60f);remove(level,s.girl);s.girl=null;}

        // 11:57:30 — Train 31 PA breaks into the whisper.
        if(t==15000) play(level,g.rail(),Train31Mod.PA_TRAIN31.get(),3.6f,0.90f);
        if(t==15280) play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.8f,0.58f);

        // 11:58 — the station becomes quiet; one door opens again like an invitation.
        if(t==15600){StationBuilder.openTrainDoors(player,true);s.fog=0.34f;}

        // 11:58:30 — final hunt begins. She stalks rather than instantly attacking.
        if(t==16200) spawnGirlBehindPlayer(level,player,s,15.0,true);
        if(t>=16200 && t<16800 && t%80==0) advanceGirlWhenUnseen(player,level,s,2.2);

        // 11:59 — roaring, fast flicker, impossible jumps around the player.
        if(t==16800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),3.8f,0.80f);
        if(t>=16800 && t<17400 && t%12==0) LightingController.pulse(player,((t/12)&1)==0);
        if(t>=16800 && t<17400 && t%100==0) advanceGirlWhenUnseen(player,level,s,3.0);

        // 11:59:30 — fatal ending: final form blocks the route, screams, then rushes the camera.
        if(t==17400){
            remove(level,s.girl); s.girl=null;
            spawnGirlInFront(level,player,s,7.0,true);
            play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),4.8f,0.66f);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,520,0,false,false));
        }
        if(t>=17420 && t<17880 && t%20==0) rushGirl(player,level,s,0.55);
        if(t==17720) play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),3.3f,0.48f);
        if(t==17800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),5.2f,0.58f);

        // Just before midnight: hard blackout -> Minecraft death screen. No graphic animation.
        if(t==17920){
            LightingController.pulse(player,true);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,1,false,false));
            player.setHealth(0.0F);
            endAfterDeath(player,s);
            return;
        }

        if(t%10==0) sync(player,s,-1,false);
    }

    public static void reset(ServerPlayer player){
        State s=STATES.remove(player.getUUID());
        if(s!=null)remove(player.serverLevel(),s.girl);
        LightingController.restore(player);
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Train31Network.ClientState(0,0f,-1,false,false));
    }

    private static void endAfterDeath(ServerPlayer player,State s){
        STATES.remove(player.getUUID());
        remove(player.serverLevel(),s.girl);
        StationBuilder.exitCamera(player);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Train31Network.ClientState(0,0f,-1,false,false));
    }

    public static void skip(ServerPlayer player){State s=STATES.get(player.getUUID());if(s!=null)s.tick+=600;}

    private static void sync(ServerPlayer p,State s,int camera,boolean cctv){
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),new Train31Network.ClientState(Math.max(0,s.tick),s.fog,camera,cctv,true));
    }

    private static void play(ServerLevel l,BlockPos p,SoundEvent sound,float volume,float pitch){l.playSound(null,p,sound,SoundSource.AMBIENT,volume,pitch);}

    private static void spawnGirl(ServerLevel l,State s,BlockPos p,boolean tall,boolean finalForm){
        remove(l,s.girl);
        ShadowGirlEntity girl=Train31Mod.SHADOW_GIRL.get().create(l);if(girl==null)return;
        girl.setPos(p.getX()+0.5,p.getY(),p.getZ()+0.5);girl.setYRot(180f);girl.setYHeadRot(180f);girl.setInvulnerable(true);
        girl.setTall(tall);girl.setFinalForm(finalForm);girl.addTag("train31_girl");l.addFreshEntity(girl);s.girl=girl.getUUID();
    }

    private static void spawnGirlAtCameraFace(ServerPlayer player,State s,int cam,double distance){
        Entity camera=StationBuilder.cameraEntity(player,cam);if(camera==null)return;
        Vec3 look=camera.getLookAngle().normalize();Vec3 pos=camera.position().add(look.scale(distance));
        remove(player.serverLevel(),s.girl);
        ShadowGirlEntity girl=Train31Mod.SHADOW_GIRL.get().create(player.serverLevel());if(girl==null)return;
        girl.setPos(pos.x,camera.getY()-1.55,pos.z);girl.setFinalForm(true);girl.setInvulnerable(true);
        girl.setYRot(camera.getYRot()+180f);girl.setYHeadRot(camera.getYRot()+180f);player.serverLevel().addFreshEntity(girl);s.girl=girl.getUUID();
    }

    private static void spawnGirlBehindPlayer(ServerLevel l,ServerPlayer p,State s,double distance,boolean finalForm){
        Vec3 look=p.getLookAngle();Vec3 flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);flat=flat.normalize();
        Vec3 pos=p.position().subtract(flat.scale(distance));
        spawnGirl(l,s,new BlockPos((int)Math.floor(pos.x),(int)Math.floor(p.getY()),(int)Math.floor(pos.z)),false,finalForm);
    }

    private static void spawnGirlInFront(ServerLevel l,ServerPlayer p,State s,double distance,boolean finalForm){
        Vec3 look=p.getLookAngle();Vec3 flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);flat=flat.normalize();
        Vec3 pos=p.position().add(flat.scale(distance));
        spawnGirl(l,s,new BlockPos((int)Math.floor(pos.x),(int)Math.floor(p.getY()),(int)Math.floor(pos.z)),false,finalForm);
    }

    private static Vec3 behindPlayer(ServerPlayer p,double distance){
        Vec3 look=p.getLookAngle();Vec3 flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);return p.position().subtract(flat.normalize().scale(distance));
    }

    private static BlockPos platformPos(StationBuilder.RailGeometry g,int along){
        int s=g.platformSide(),a=g.tunnelSign();
        return g.axisZ()?g.rail().offset(s*5,1,a*along):g.rail().offset(a*along,1,s*5);
    }

    private static BlockPos trainInteriorPos(ServerPlayer p,double towardTunnelBlocks){
        Vec3 stop=Vec3.atCenterOf(SceneSetup.rail(p));Vec3 tunnel=Vec3.atCenterOf(SceneSetup.tunnel(p));
        Vec3 d=tunnel.subtract(stop);if(d.lengthSqr()<.01)d=new Vec3(0,0,1);d=d.normalize();Vec3 v=stop.add(d.scale(towardTunnelBlocks));
        return new BlockPos((int)Math.floor(v.x),(int)Math.floor(SceneSetup.rail(p).getY()+1),(int)Math.floor(v.z));
    }

    private static void moveGirl(ServerLevel l,UUID id,BlockPos p){Entity e=id==null?null:l.getEntity(id);if(e!=null)e.teleportTo(p.getX()+0.5,p.getY(),p.getZ()+0.5);}
    private static void remove(ServerLevel l,UUID id){Entity e=id==null?null:l.getEntity(id);if(e!=null)e.discard();}

    private static void advanceGirlWhenUnseen(ServerPlayer p,ServerLevel l,State s,double step){
        Entity e=s.girl==null?null:l.getEntity(s.girl);if(e==null)return;
        Vec3 to=e.position().subtract(p.position());double len=to.length();if(len<2.1)return;
        double dot=p.getLookAngle().normalize().dot(to.normalize());
        if(dot<0.58){Vec3 next=e.position().add(p.position().subtract(e.position()).normalize().scale(Math.min(step,len-1.7)));e.teleportTo(next.x,p.getY(),next.z);}
    }

    private static void rushGirl(ServerPlayer p,ServerLevel l,State s,double step){
        Entity e=s.girl==null?null:l.getEntity(s.girl);if(e==null)return;
        Vec3 delta=p.position().subtract(e.position());double len=delta.length();if(len<1.25)return;
        Vec3 next=e.position().add(delta.normalize().scale(Math.min(step,len-1.0)));e.teleportTo(next.x,p.getY(),next.z);
    }
}

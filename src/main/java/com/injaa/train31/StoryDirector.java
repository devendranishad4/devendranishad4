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

/** 15-minute clock-driven episode: 11:45 PM -> midnight, with a major beat every ~20 seconds. */
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
        PhysicalTrainBuilder.restore(player);
        LightingController.restore(player);
        State s=new State(Math.max(0,delaySeconds)*20);
        STATES.put(player.getUUID(),s);
        sync(player,s);
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

        if(t<0){if(t%20==0)sync(player,s);return;}

        // 11:45:20 — faint fluorescent buzz.
        if(t==400) play(level,g.rail(),Train31Mod.FLUORESCENT_BUZZ.get(),0.60f,0.95f);

        // 11:45:40 — distant camera click.
        if(t==800) play(level,StationBuilder.cctvRoom(player),Train31Mod.CAMERA_CLICK.get(),0.65f,0.78f);

        // 11:46:00 — horror begins: first flicker.
        if(t>=1200 && t<1320 && t%18==0) LightingController.pulse(player,((t/18)&1)==0);
        if(t==1200) play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),0.9f,0.84f);
        if(t==1320) LightingController.pulse(player,false);

        // 11:46:20 — first CCTV sighting, far away.
        if(t==1600){
            StationBuilder.enterCamera(player,2);
            spawnGirl(level,s,platformPos(g,28));
            play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.CCTV_STATIC.get(),1.5f,0.80f);
        }
        if(t==1700){remove(level,s.girl);s.girl=null;StationBuilder.exitCamera(player);}

        // 11:46:40 — first whisper and brief distant appearance.
        if(t==2000){
            spawnGirl(level,s,platformPos(g,24));
            play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.2f,1.0f);
        }
        if(t==2100){remove(level,s.girl);s.girl=null;}

        // 11:47:00 — metal impact from the tunnel.
        if(t==2400){
            play(level,SceneSetup.tunnel(player),Train31Mod.METAL_KNOCKS.get(),2.0f,0.72f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),0.9f,0.64f);
        }

        // 11:47:20 — girl on the platform, closer.
        if(t==2800){
            spawnGirl(level,s,platformPos(g,18));
            play(level,player.blockPosition(),Train31Mod.WHISPER_CAN_SEE.get(),2.1f,1.0f);
        }
        if(t==2920){remove(level,s.girl);s.girl=null;}

        // 11:47:40 — stronger light failure and CCTV static.
        if(t>=3200 && t<3340 && t%12==0) LightingController.pulse(player,((t/12)&1)==0);
        if(t==3200) play(level,player.blockPosition(),Train31Mod.CCTV_STATIC.get(),1.45f,0.68f);
        if(t==3340) LightingController.pulse(player,false);

        // 11:48:00 — full physical station blackout. It stays dark.
        if(t==3600){
            play(level,g.rail(),Train31Mod.POWER_DOWN.get(),3.2f,0.90f);
            LightingController.blackout(player);
            s.fog=0.05f;
        }

        // 11:48:20 — girl appears in the darkness.
        if(t==4000){
            spawnGirl(level,s,platformPos(g,14));
            play(level,player.blockPosition(),Train31Mod.WHISPER_HERE.get(),2.2f,1.0f);
        }
        if(t==4100){remove(level,s.girl);s.girl=null;}

        // 11:48:40 — first telekinetic shove.
        if(t==4400){
            telekineticPush(player,0.55,0.12);
            play(level,player.blockPosition(),Train31Mod.METAL_KNOCKS.get(),1.3f,0.58f);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,45,0,false,false));
        }

        // 11:49:00 — opposite-side CCTV sighting.
        if(t==4800){
            StationBuilder.enterCamera(player,0);
            spawnGirl(level,s,platformPos(g,11));
            play(level,StationBuilder.cameraBlockPos(player,0),Train31Mod.CCTV_STATIC.get(),1.9f,0.70f);
        }
        if(t==4900){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:49:20 — whisper behind the player + small push.
        if(t==5200){
            spawnGirlBehindPlayer(level,player,s,8.0);
            play(level,player.blockPosition(),Train31Mod.WHISPER_BEHIND.get(),2.4f,1.0f);
            telekineticPush(player,0.45,0.10);
        }
        if(t==5320){remove(level,s.girl);s.girl=null;}

        // 11:49:40 — bad-memory style distortion: no text, just sound + disorientation.
        if(t==5600){
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,70,0,false,false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0,false,false));
            play(level,player.blockPosition(),Train31Mod.METAL_KNOCKS.get(),1.0f,0.54f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),1.2f,0.60f);
        }

        // 11:50:00 — stalking starts: she advances when unseen.
        if(t==6000){spawnGirl(level,s,platformPos(g,15));play(level,player.blockPosition(),Train31Mod.WHISPER_CAN_SEE.get(),2.1f,1.0f);}
        if(t>=6000 && t<6400 && t%55==0) advanceGirlWhenUnseen(player,level,s,1.35);

        // 11:50:20 — telekinetic pull toward the rail area.
        if(t==6400){
            telekineticToward(player,Vec3.atCenterOf(g.rail()),0.60,0.12);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0,false,false));
            play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),0.55f,1.08f);
        }
        if(t==6480){remove(level,s.girl);s.girl=null;}

        // 11:50:40 — tunnel answers with rumble + whisper.
        if(t==6800){
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.5f,0.68f);
            play(level,player.blockPosition(),Train31Mod.WHISPER_HERE.get(),2.0f,1.0f);
        }

        // 11:51:00 — close CCTV face event.
        if(t==7200){
            StationBuilder.enterCamera(player,1);
            spawnGirlAtCameraFace(player,s,1,3.0);
            play(level,StationBuilder.cameraBlockPos(player,1),Train31Mod.CCTV_STATIC.get(),2.2f,0.66f);
        }
        if(t==7300){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:51:20 — platform apparition + weakness.
        if(t==7600){
            spawnGirl(level,s,platformPos(g,10));
            play(level,player.blockPosition(),Train31Mod.WHISPER_WHY_HERE.get(),2.2f,1.0f);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,70,0,false,false));
        }
        if(t==7700){remove(level,s.girl);s.girl=null;}

        // 11:51:40 — stronger telekinetic hit and brief lift.
        if(t==8000){
            telekineticPush(player,0.85,0.24);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,9,0,false,false));
            play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),0.90f,1.03f);
        }

        // 11:52:00 — she returns and moves only while unseen.
        if(t==8400) spawnGirl(level,s,platformPos(g,12));
        if(t>=8400 && t<8800 && t%48==0) advanceGirlWhenUnseen(player,level,s,1.55);

        // 11:52:20 — sudden whisper from close by.
        if(t==8800){
            play(level,player.blockPosition(),Train31Mod.WHISPER_CANT_LEAVE.get(),2.4f,1.0f);
            play(level,player.blockPosition(),Train31Mod.CCTV_STATIC.get(),1.2f,0.62f);
        }

        // 11:52:40 — girl vanishes; tunnel starts sounding alive.
        if(t==9200){
            remove(level,s.girl);s.girl=null;
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.8f,0.74f);
            play(level,SceneSetup.tunnel(player),Train31Mod.METAL_KNOCKS.get(),1.7f,0.62f);
        }

        // 11:53:00 — Train 31 announcement, still no train visible.
        if(t==9600) play(level,g.rail(),Train31Mod.PA_TRAIN31.get(),3.8f,1.0f);

        // 11:53:20 — horn + "It's coming."
        if(t==10000){
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),2.8f,0.94f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.6f,0.78f);
            play(level,player.blockPosition(),Train31Mod.WHISPER_COMING.get(),2.0f,1.0f);
        }

        // 11:53:40 — Train 31 emerges from the tunnel.
        if(t==10400){
            StationBuilder.spawnTrain(player);s.trainSpawned=true;
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_ROLL.get(),3.0f,1.0f);
        }
        if(t>=10400 && t<=10800 && s.trainSpawned){
            double p=(t-10400)/400.0;
            double eased=1.0-Math.pow(1.0-Math.min(1.0,p),3.0);
            StationBuilder.setTrainProgress(player,eased);
        }

        // 11:54:00 — train stops and becomes physical.
        if(t==10800){
            play(level,g.rail(),Train31Mod.TRAIN_BRAKES.get(),3.2f,0.98f);
            StationBuilder.setTrainProgress(player,1.0);
            StationBuilder.removeTrain(player);s.trainSpawned=false;
            PhysicalTrainBuilder.build(player);
            PhysicalTrainBuilder.setLights(player,true);
            play(level,player.blockPosition(),Train31Mod.WHISPER_DONT_BOARD.get(),2.2f,1.0f);
        }

        // 11:54:20 — doors open.
        if(t==11200){play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.4f,1.0f);PhysicalTrainBuilder.setDoorsOpen(player,true);}

        // 11:54:40 — girl already inside.
        if(t==11600){spawnGirl(level,s,trainInteriorPos(g,8));play(level,g.rail(),Train31Mod.WHISPER_SEE_YOU.get(),2.2f,1.0f);}

        // 11:55:00 — doors shut.
        if(t==12000){PhysicalTrainBuilder.setDoorsOpen(player,false);play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),1.9f,0.88f);remove(level,s.girl);s.girl=null;}

        // 11:55:20 — carriage lights fail.
        if(t==12400){PhysicalTrainBuilder.setLights(player,false);play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),1.3f,0.60f);}
        if(t==12520) PhysicalTrainBuilder.setLights(player,true);

        // 11:55:40 — girl appears deeper inside.
        if(t==12800){spawnGirl(level,s,trainInteriorPos(g,3));play(level,g.rail(),Train31Mod.WHISPER_INJAA.get(),1.9f,1.0f);}
        if(t==12920){remove(level,s.girl);s.girl=null;}

        // 11:56:00 — disorientation inside the carriage.
        if(t==13200){
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,65,0,false,false));
            play(level,g.rail(),Train31Mod.CCTV_STATIC.get(),1.25f,0.56f);
        }

        // 11:56:20 — light telekinetic pull back toward the train center.
        if(t==13600){telekineticToward(player,Vec3.atCenterOf(g.rail()),0.65,0.10);play(level,player.blockPosition(),Train31Mod.WHISPER_CANT_LEAVE.get(),2.3f,1.0f);}

        // 11:56:40 — girl behind the player inside/near the train.
        if(t==14000){spawnGirlBehindPlayer(level,player,s,9.0);play(level,player.blockPosition(),Train31Mod.WHISPER_BEHIND.get(),2.3f,1.0f);}
        if(t==14120){remove(level,s.girl);s.girl=null;}

        // 11:57:00 — doors reopen unexpectedly.
        if(t==14400){PhysicalTrainBuilder.setDoorsOpen(player,true);play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.1f,0.90f);}

        // 11:57:20 — another shove + slow movement.
        if(t==14800){telekineticPush(player,0.75,0.16);player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1,false,false));}

        // 11:57:40 — CCTV static from nowhere, girl appears on platform again.
        if(t==15200){spawnGirl(level,s,platformPos(g,9));play(level,player.blockPosition(),Train31Mod.CCTV_STATIC.get(),2.0f,0.62f);}

        // 11:58:00 — whisper "Run" and stalking accelerates.
        if(t==15600) play(level,player.blockPosition(),Train31Mod.WHISPER_RUN.get(),2.5f,1.0f);
        if(t>=15600 && t<16000 && t%48==0) advanceGirlWhenUnseen(player,level,s,2.15);

        // 11:58:20 — telekinetic hit + short lift.
        if(t==16000){telekineticPush(player,0.90,0.22);player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,8,0,false,false));play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),1.0f,1.02f);}

        // 11:58:40 — she vanishes, then reappears behind you closer.
        if(t==16400){remove(level,s.girl);s.girl=null;spawnGirlBehindPlayer(level,player,s,7.0);play(level,player.blockPosition(),Train31Mod.WHISPER_CAN_SEE.get(),2.2f,1.0f);}

        // 11:59:00 — final pursuit begins.
        if(t==16800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),2.2f,1.0f);
        if(t>=16800 && t<17200 && t%45==0) advanceGirlWhenUnseen(player,level,s,2.8);

        // 11:59:20 — she appears directly ahead.
        if(t==17200){remove(level,s.girl);s.girl=null;spawnGirlInFront(level,player,s,6.0);play(level,player.blockPosition(),Train31Mod.WHISPER_FOUND_YOU.get(),3.2f,1.0f);}
        if(t>=17200 && t<17600 && t%20==0) rushGirl(player,level,s,0.42);

        // 11:59:40 — final whisper and last rush.
        if(t==17600) play(level,player.blockPosition(),Train31Mod.WHISPER_SHOULD_LISTEN.get(),3.0f,1.0f);
        if(t>=17600 && t<18000 && t%16==0) rushGirl(player,level,s,0.58);

        // 12:00 — hard blackout -> vanilla Minecraft death screen. No graphic animation.
        if(t==18000){
            PhysicalTrainBuilder.setLights(player,false);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,1,false,false));
            player.setHealth(0.0F);
            endAfterDeath(player,s);
            return;
        }

        if(t%10==0) sync(player,s);
    }

    public static void reset(ServerPlayer player){
        State s=STATES.remove(player.getUUID());
        if(s!=null)remove(player.serverLevel(),s.girl);
        LightingController.restore(player);
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player);
        PhysicalTrainBuilder.restore(player);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Train31Network.ClientState(0,0f,-1,false,false));
    }

    private static void endAfterDeath(ServerPlayer player,State s){
        STATES.remove(player.getUUID());
        remove(player.serverLevel(),s.girl);
        LightingController.restore(player);
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player);
        PhysicalTrainBuilder.restore(player);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Train31Network.ClientState(0,0f,-1,false,false));
    }

    public static void skip(ServerPlayer player){State s=STATES.get(player.getUUID());if(s!=null)s.tick+=400;}

    /** Periodic story sync preserves the current CCTV feed instead of kicking the player out. */
    private static void sync(ServerPlayer p,State s){
        boolean cctv=StationBuilder.isCameraActive(p);
        int camera=cctv?StationBuilder.currentCameraEntityId(p):-1;
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),new Train31Network.ClientState(Math.max(0,s.tick),s.fog,camera,cctv,true));
    }

    private static void play(ServerLevel l,BlockPos p,SoundEvent sound,float volume,float pitch){l.playSound(null,p,sound,SoundSource.AMBIENT,volume,pitch);}

    private static void telekineticPush(ServerPlayer p,double strength,double up){
        Vec3 look=p.getLookAngle();
        Vec3 flat=new Vec3(look.x,0,look.z);
        if(flat.lengthSqr()<0.01)flat=new Vec3(0,0,1);
        flat=flat.normalize();
        p.push(flat.x*strength,up,flat.z*strength);
    }

    private static void telekineticToward(ServerPlayer p,Vec3 target,double strength,double up){
        Vec3 d=target.subtract(p.position());
        Vec3 flat=new Vec3(d.x,0,d.z);
        if(flat.lengthSqr()<0.01)return;
        flat=flat.normalize();
        p.push(flat.x*strength,up,flat.z*strength);
    }

    private static void spawnGirl(ServerLevel l,State s,BlockPos p){
        remove(l,s.girl);
        ShadowGirlEntity girl=Train31Mod.SHADOW_GIRL.get().create(l);if(girl==null)return;
        girl.setPos(p.getX()+0.5,p.getY(),p.getZ()+0.5);girl.setYRot(180f);girl.setYHeadRot(180f);girl.setInvulnerable(true);
        girl.setTall(false);girl.setFinalForm(true);girl.addTag("train31_girl");l.addFreshEntity(girl);s.girl=girl.getUUID();
    }

    private static void spawnGirlAtCameraFace(ServerPlayer player,State s,int cam,double distance){
        Entity camera=StationBuilder.cameraEntity(player,cam);if(camera==null)return;
        Vec3 look=camera.getLookAngle().normalize();Vec3 pos=camera.position().add(look.scale(distance));
        remove(player.serverLevel(),s.girl);
        ShadowGirlEntity girl=Train31Mod.SHADOW_GIRL.get().create(player.serverLevel());if(girl==null)return;
        girl.setPos(pos.x,camera.getY()-1.55,pos.z);girl.setTall(false);girl.setFinalForm(true);girl.setInvulnerable(true);
        girl.setYRot(camera.getYRot()+180f);girl.setYHeadRot(camera.getYRot()+180f);player.serverLevel().addFreshEntity(girl);s.girl=girl.getUUID();
    }

    private static void spawnGirlBehindPlayer(ServerLevel l,ServerPlayer p,State s,double distance){
        Vec3 look=p.getLookAngle();Vec3 flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);flat=flat.normalize();
        Vec3 pos=p.position().subtract(flat.scale(distance));
        spawnGirl(l,s,new BlockPos((int)Math.floor(pos.x),(int)Math.floor(p.getY()),(int)Math.floor(pos.z)));
    }

    private static void spawnGirlInFront(ServerLevel l,ServerPlayer p,State s,double distance){
        Vec3 look=p.getLookAngle();Vec3 flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);flat=flat.normalize();
        Vec3 pos=p.position().add(flat.scale(distance));
        spawnGirl(l,s,new BlockPos((int)Math.floor(pos.x),(int)Math.floor(p.getY()),(int)Math.floor(pos.z)));
    }

    private static BlockPos platformPos(StationBuilder.RailGeometry g,int along){
        int side=g.platformSide(),a=g.tunnelSign();
        return g.axisZ()?g.rail().offset(side*5,1,a*along):g.rail().offset(a*along,1,side*5);
    }

    private static BlockPos trainInteriorPos(StationBuilder.RailGeometry g,int towardTunnel){
        int a=g.tunnelSign()*towardTunnel;
        return g.axisZ()?g.rail().offset(0,1,a):g.rail().offset(a,1,0);
    }

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

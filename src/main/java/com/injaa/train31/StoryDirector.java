package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/** 15-minute clock-driven episode: 11:45 PM -> midnight, with a major beat every ~30 seconds. */
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

        // 11:45:30 — station still feels normal, just a faint electrical warning.
        if(t==600) play(level,g.rail(),Train31Mod.FLUORESCENT_BUZZ.get(),0.65f,0.96f);

        // 11:46 — horror starts: first light flicker.
        if(t>=1200 && t<1320 && t%20==0) LightingController.pulse(player,((t/20)&1)==0);
        if(t==1320) LightingController.pulse(player,false);
        if(t==1200) play(level,g.rail(),Train31Mod.CAMERA_CLICK.get(),0.9f,0.82f);

        // 11:46:30 — first CCTV disturbance, girl only for a moment.
        if(t==1800){
            StationBuilder.enterCamera(player,2);
            spawnGirl(level,s,platformPos(g,28));
            play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.CCTV_STATIC.get(),1.6f,0.82f);
        }
        if(t==1920){remove(level,s.girl);s.girl=null;StationBuilder.exitCamera(player);}

        // 11:47 — first whisper and distant sighting.
        if(t==2400){
            spawnGirl(level,s,platformPos(g,22));
            play(level,player.blockPosition(),Train31Mod.WHISPER_INJAA.get(),2.2f,1.0f);
        }
        if(t==2520){remove(level,s.girl);s.girl=null;}

        // 11:47:30 — stronger flicker + metal knocks.
        if(t>=3000 && t<3160 && t%14==0) LightingController.pulse(player,((t/14)&1)==0);
        if(t==3000) play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),2.0f,0.82f);
        if(t==3160) LightingController.pulse(player,false);

        // 11:48 — FULL PHYSICAL STATION BLACKOUT. No repeating Minecraft Darkness potion.
        if(t==3600){
            play(level,g.rail(),Train31Mod.POWER_DOWN.get(),3.2f,0.90f);
            LightingController.blackout(player);
            s.fog=0.06f;
            memory(player,"§7The station goes completely silent.");
        }

        // 11:48:30 — girl appears in the dark, closer than before.
        if(t==4200){
            spawnGirl(level,s,platformPos(g,16));
            play(level,player.blockPosition(),Train31Mod.WHISPER_CAN_SEE.get(),2.2f,1.0f);
        }
        if(t==4320){remove(level,s.girl);s.girl=null;}

        // 11:49 — CCTV catches her on the opposite side.
        if(t==4800){
            StationBuilder.enterCamera(player,0);
            spawnGirl(level,s,platformPos(g,12));
            play(level,StationBuilder.cameraBlockPos(player,0),Train31Mod.CCTV_STATIC.get(),1.8f,0.72f);
        }
        if(t==4920){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:49:30 — bad memory / disorientation event.
        if(t==5400){
            memory(player,"§8You have been here before...");
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,80,0,false,false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,0,false,false));
            play(level,player.blockPosition(),Train31Mod.METAL_KNOCKS.get(),1.1f,0.62f);
        }

        // 11:50 — she appears behind you and throws you forward with telekinesis.
        if(t==6000){
            spawnGirlBehindPlayer(level,player,s,8.0);
            play(level,player.blockPosition(),Train31Mod.WHISPER_BEHIND.get(),2.4f,1.0f);
            telekineticPush(player,0.70,0.18);
        }
        if(t==6120){remove(level,s.girl);s.girl=null;}

        // 11:50:30 — tunnel answers back.
        if(t==6600){
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.5f,0.72f);
            play(level,SceneSetup.tunnel(player),Train31Mod.METAL_KNOCKS.get(),1.6f,0.64f);
            memory(player,"§8Something is moving inside the tunnel.");
        }

        // 11:51 — camera cuts to the girl much closer.
        if(t==7200){
            StationBuilder.enterCamera(player,1);
            spawnGirlAtCameraFace(player,s,1,3.0);
            play(level,StationBuilder.cameraBlockPos(player,1),Train31Mod.CCTV_STATIC.get(),2.2f,0.70f);
        }
        if(t==7340){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:51:30 — second bad-memory beat.
        if(t==7800){
            memory(player,"§8The platform remembers your footsteps.");
            play(level,player.blockPosition(),Train31Mod.WHISPER_HERE.get(),2.2f,1.0f);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0,false,false));
        }

        // 11:52 — girl moves closer whenever you are not watching her.
        if(t==8400) spawnGirl(level,s,platformPos(g,13));
        if(t>=8400 && t<9000 && t%70==0) advanceGirlWhenUnseen(player,level,s,1.6);

        // 11:52:30 — second telekinetic attack.
        if(t==9000){
            telekineticPush(player,1.05,0.32);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION,12,0,false,false));
            play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),1.15f,1.08f);
        }
        if(t==9120){remove(level,s.girl);s.girl=null;}

        // 11:53 — clearly visible on the player's platform.
        if(t==9600){
            spawnGirl(level,s,platformPos(g,7));
            play(level,player.blockPosition(),Train31Mod.WHISPER_WHY_HERE.get(),2.3f,1.0f);
            memory(player,"§8You should have left when the lights went out.");
        }
        if(t==9740){remove(level,s.girl);s.girl=null;}

        // 11:53:30 — announcement BEFORE Train 31 is visible.
        if(t==10200){
            play(level,g.rail(),Train31Mod.PA_TRAIN31.get(),3.8f,1.0f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.7f,0.78f);
            play(level,player.blockPosition(),Train31Mod.WHISPER_COMING.get(),2.0f,1.0f);
        }

        // 11:54 — Train 31 starts moving out of the marked tunnel.
        if(t==10800){
            StationBuilder.spawnTrain(player); s.trainSpawned=true;
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),3.3f,0.98f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_ROLL.get(),3.0f,1.0f);
        }
        if(t>=10800 && t<=11400 && s.trainSpawned){
            double p=(t-10800)/600.0;
            double eased=1.0-Math.pow(1.0-Math.min(1.0,p),3.0);
            StationBuilder.setTrainProgress(player,eased);
        }

        // 11:54:30 — train reaches the platform faster, brakes, then becomes physical.
        if(t==11400){
            play(level,g.rail(),Train31Mod.TRAIN_BRAKES.get(),3.2f,0.98f);
            StationBuilder.setTrainProgress(player,1.0);
            StationBuilder.removeTrain(player);
            s.trainSpawned=false;
            PhysicalTrainBuilder.build(player);
            PhysicalTrainBuilder.setLights(player,true);
            play(level,player.blockPosition(),Train31Mod.WHISPER_DONT_BOARD.get(),2.1f,1.0f);
        }

        // 11:55 — physical doors open.
        if(t==12000){
            play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.4f,1.0f);
            PhysicalTrainBuilder.setDoorsOpen(player,true);
        }

        // 11:55:30 — girl is already inside the carriage.
        if(t==12600){
            spawnGirl(level,s,trainInteriorPos(g,8));
            play(level,g.rail(),Train31Mod.WHISPER_SEE_YOU.get(),2.2f,1.0f);
        }

        // 11:56 — doors slam shut and carriage lights fail briefly.
        if(t==13200){
            PhysicalTrainBuilder.setDoorsOpen(player,false);
            PhysicalTrainBuilder.setLights(player,false);
            play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),1.9f,0.88f);
            remove(level,s.girl);s.girl=null;
        }
        if(t==13320) PhysicalTrainBuilder.setLights(player,true);

        // 11:56:30 — third memory and another appearance deeper inside.
        if(t==13800){
            memory(player,"§8You remember a carriage that was never on the map.");
            spawnGirl(level,s,trainInteriorPos(g,3));
            play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),1.4f,0.68f);
        }
        if(t==13940){remove(level,s.girl);s.girl=null;}

        // 11:57 — telekinetic pull toward the train stop area.
        if(t==14400){
            telekineticToward(player,Vec3.atCenterOf(g.rail()),0.95,0.15);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,1,false,false));
            memory(player,"§8Something is pulling you back.");
        }

        // 11:57:30 — doors open again, but the warning changes.
        if(t==15000){
            PhysicalTrainBuilder.setDoorsOpen(player,true);
            play(level,player.blockPosition(),Train31Mod.WHISPER_CANT_LEAVE.get(),2.8f,1.0f);
        }

        // 11:58 — girl behind the player, stalking only while unseen.
        if(t==15600){
            spawnGirlBehindPlayer(level,player,s,14.0);
            play(level,player.blockPosition(),Train31Mod.WHISPER_RUN.get(),2.4f,1.0f);
        }
        if(t>=15600 && t<16200 && t%70==0) advanceGirlWhenUnseen(player,level,s,2.1);

        // 11:58:30 — CCTV static, another shove, she gets closer.
        if(t==16200){
            play(level,player.blockPosition(),Train31Mod.CCTV_STATIC.get(),2.2f,0.66f);
            telekineticPush(player,0.80,0.22);
            memory(player,"§8Don't look away.");
        }
        if(t>=16200 && t<16800 && t%85==0) advanceGirlWhenUnseen(player,level,s,2.5);

        // 11:59 — final pursuit in the dark station.
        if(t==16800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),2.3f,1.0f);
        if(t>=16800 && t<17400 && t%75==0) advanceGirlWhenUnseen(player,level,s,3.0);

        // 11:59:30 — she is directly in front: "I found you."
        if(t==17400){
            remove(level,s.girl);s.girl=null;
            spawnGirlInFront(level,player,s,7.0);
            play(level,player.blockPosition(),Train31Mod.WHISPER_FOUND_YOU.get(),3.4f,1.0f);
            memory(player,"§8There is nowhere else to run.");
        }
        if(t>=17420 && t<17880 && t%20==0) rushGirl(player,level,s,0.55);

        // 11:59:45 — final whispered warning.
        if(t==17700) play(level,player.blockPosition(),Train31Mod.WHISPER_SHOULD_LISTEN.get(),3.0f,1.0f);
        if(t==17800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),3.2f,0.98f);

        // Hard final blackout -> vanilla Minecraft death screen. No graphic animation.
        if(t==17920){
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

    public static void skip(ServerPlayer player){State s=STATES.get(player.getUUID());if(s!=null)s.tick+=600;}

    /** Periodic story sync preserves the current CCTV feed instead of kicking the player out. */
    private static void sync(ServerPlayer p,State s){
        boolean cctv=StationBuilder.isCameraActive(p);
        int camera=cctv?StationBuilder.currentCameraEntityId(p):-1;
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),new Train31Network.ClientState(Math.max(0,s.tick),s.fog,camera,cctv,true));
    }

    private static void play(ServerLevel l,BlockPos p,SoundEvent sound,float volume,float pitch){l.playSound(null,p,sound,SoundSource.AMBIENT,volume,pitch);}

    private static void memory(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}

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

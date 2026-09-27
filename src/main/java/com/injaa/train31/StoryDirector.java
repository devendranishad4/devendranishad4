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

/** 15-minute clock-driven episode: 11:45 PM -> midnight. */
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

        // 11:45-11:46:30 — normal station.
        if(t==600) play(level,g.rail(),Train31Mod.FLUORESCENT_BUZZ.get(),0.55f,1.0f);
        if(t==1200) play(level,g.rail(),Train31Mod.PA_NORMAL.get(),3.2f,1.0f);
        if(t==1800) play(level,StationBuilder.cctvRoom(player),Train31Mod.CAMERA_CLICK.get(),0.55f,1.0f);

        // 11:47 — first CCTV sighting: same girl, no tall shadow form.
        if(t==2400){
            StationBuilder.enterCamera(player,2);
            spawnGirl(level,s,platformPos(g,30));
            play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.WHISPER_INJAA.get(),2.4f,1.0f);
        }
        if(t==2860){
            spawnGirlAtCameraFace(player,s,2,1.35);
            play(level,StationBuilder.cameraBlockPos(player,2),Train31Mod.CCTV_STATIC.get(),2.0f,0.80f);
        }
        if(t==2920){remove(level,s.girl);s.girl=null;StationBuilder.exitCamera(player);}

        // 11:47:30 — flicker.
        if(t>=3000 && t<3220 && t%14==0) LightingController.pulse(player,((t/14)&1)==0);
        if(t==3220) LightingController.restore(player);

        // 11:48 — track impacts.
        if(t==3600) play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),2.0f,0.85f);
        if(t>=3600 && t<3760 && t%10==0) LightingController.pulse(player,((t/10)&1)==0);
        if(t==3760) LightingController.restore(player);

        // 11:48:30 — quick visual reveal.
        if(t==4200){LightingController.pulse(player,true);spawnGirl(level,s,platformPos(g,24));}
        if(t==4240){LightingController.restore(player);remove(level,s.girl);s.girl=null;}

        // 11:49 — "Can you see me?"
        if(t==4800) play(level,player.blockPosition(),Train31Mod.WHISPER_CAN_SEE.get(),2.1f,1.0f);

        // 11:49:30 — second CCTV sighting, closer.
        if(t==5400){StationBuilder.enterCamera(player,1);spawnGirl(level,s,platformPos(g,9));}
        if(t==5560) play(level,StationBuilder.cameraBlockPos(player,1),Train31Mod.CCTV_STATIC.get(),1.7f,0.80f);
        if(t==5620){StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:50 — "Don't turn around."
        if(t==6000){spawnGirlBehindPlayer(level,player,s,7.0);play(level,player.blockPosition(),Train31Mod.WHISPER_BEHIND.get(),2.3f,1.0f);}
        if(t==6120){remove(level,s.girl);s.girl=null;}

        // 11:50:30 — tunnel starts answering.
        if(t==6600){
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.0f,0.72f);
            play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),1.3f,0.66f);
        }

        // 11:51 — station blackout begins and stays dark through the main section.
        if(t==7200){
            play(level,g.rail(),Train31Mod.POWER_DOWN.get(),3.0f,0.92f);
            LightingController.pulse(player,true);
            s.fog=0.34f;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,140,0,false,false));
        }
        if(t>=7200 && t<17400 && t%40==0)
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100,0,false,false));

        // 11:51:30 — "You shouldn't be here."
        if(t==7800){
            play(level,player.blockPosition(),Train31Mod.WHISPER_HERE.get(),2.4f,1.0f);
            play(level,StationBuilder.cctvRoom(player),Train31Mod.CAMERA_CLICK.get(),1.1f,0.72f);
        }

        // 11:52 — CCTV catches her on the platform.
        if(t==8400){StationBuilder.enterCamera(player,0);spawnGirl(level,s,platformPos(g,14));}
        if(t==8500) moveGirl(level,s.girl,platformPos(g,6));
        if(t==8580){play(level,StationBuilder.cameraBlockPos(player,0),Train31Mod.CCTV_STATIC.get(),2.0f,0.70f);StationBuilder.exitCamera(player);remove(level,s.girl);s.girl=null;}

        // 11:52:30 — hard scare beat.
        if(t>=9000 && t<9160 && t%8==0) LightingController.pulse(player,((t/8)&1)==0);
        if(t==9000) play(level,SceneSetup.tunnel(player),Train31Mod.GIRL_ROAR.get(),0.75f,1.0f);
        if(t==9160) LightingController.pulse(player,true);

        // 11:53 — she is actually visible on the player's platform.
        if(t==9600){spawnGirl(level,s,platformPos(g,7));play(level,player.blockPosition(),Train31Mod.WHISPER_WHY_HERE.get(),2.2f,1.0f);}
        if(t==9780){remove(level,s.girl);s.girl=null;}

        // 11:53:20 — announcement BEFORE the train is visible.
        if(t==10000) play(level,g.rail(),Train31Mod.PA_TRAIN31.get(),3.8f,1.0f);

        // 11:53:30 — rumble, horn and "It's coming."
        if(t==10200){
            play(level,SceneSetup.tunnel(player),Train31Mod.TUNNEL_RUMBLE.get(),2.7f,0.80f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),2.5f,0.92f);
            play(level,player.blockPosition(),Train31Mod.WHISPER_COMING.get(),1.9f,1.0f);
        }

        // 11:54 — the larger six-car Train 31 comes from the marked tunnel.
        if(t==10800){
            StationBuilder.spawnTrain(player); s.trainSpawned=true;
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_HORN.get(),3.2f,0.98f);
            play(level,SceneSetup.tunnel(player),Train31Mod.TRAIN_ROLL.get(),2.8f,1.0f);
        }
        if(t>=10800 && t<=12000 && s.trainSpawned){
            double p=(t-10800)/1200.0;
            double eased=1.0-Math.pow(1.0-Math.min(1.0,p),3.0);
            StationBuilder.setTrainProgress(player,eased);
        }

        // 11:54:20 — warning while the train is approaching.
        if(t==11200) play(level,player.blockPosition(),Train31Mod.WHISPER_DONT_BOARD.get(),2.0f,1.0f);
        if(t==11400) play(level,g.rail(),Train31Mod.TRAIN_BRAKES.get(),3.0f,0.98f);

        // 11:55 — cinematic train reaches the stop, then becomes a REAL block train.
        if(t==12000){
            StationBuilder.setTrainProgress(player,1.0);
            StationBuilder.removeTrain(player);
            s.trainSpawned=false;
            PhysicalTrainBuilder.build(player);
            PhysicalTrainBuilder.setLights(player,true);
            s.fog=0.22f;
        }

        // 11:55:30 — physical doors open. Player can walk inside normally.
        if(t==12600){
            play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),2.3f,1.0f);
            PhysicalTrainBuilder.setDoorsOpen(player,true);
        }

        // 11:56 — girl inside the physical carriage: "I can see you."
        if(t==13200){spawnGirl(level,s,trainInteriorPos(g,8));play(level,g.rail(),Train31Mod.WHISPER_SEE_YOU.get(),2.1f,1.0f);}

        // 11:56:30 — doors shut and interior lights glitch.
        if(t==13800){PhysicalTrainBuilder.setDoorsOpen(player,false);play(level,g.rail(),Train31Mod.DOOR_CHIME.get(),1.8f,0.90f);remove(level,s.girl);s.girl=null;}
        if(t>=13800 && t<13980 && t%12==0) PhysicalTrainBuilder.setLights(player,((t/12)&1)==0);
        if(t==13980) PhysicalTrainBuilder.setLights(player,true);

        // 11:57 — same girl appears deeper inside.
        if(t==14400) spawnGirl(level,s,trainInteriorPos(g,3));
        if(t==14540){play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),1.2f,0.72f);remove(level,s.girl);s.girl=null;}

        // 11:57:30 — "You can't leave now."
        if(t==15000) play(level,player.blockPosition(),Train31Mod.WHISPER_CANT_LEAVE.get(),2.7f,1.0f);

        // 11:58 — one chance to enter/leave again.
        if(t==15600){PhysicalTrainBuilder.setDoorsOpen(player,true);PhysicalTrainBuilder.setLights(player,false);s.fog=0.38f;}
        if(t==15660) PhysicalTrainBuilder.setLights(player,true);

        // 11:58:30 — "Injaa... run."
        if(t==16200){spawnGirlBehindPlayer(level,player,s,15.0);play(level,player.blockPosition(),Train31Mod.WHISPER_RUN.get(),2.4f,1.0f);}
        if(t>=16200 && t<16800 && t%80==0) advanceGirlWhenUnseen(player,level,s,2.2);

        // 11:59 — fast flicker / final pursuit.
        if(t==16800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),2.6f,1.0f);
        if(t>=16800 && t<17400 && t%12==0) LightingController.pulse(player,((t/12)&1)==0);
        if(t>=16800 && t<17400 && t%100==0) advanceGirlWhenUnseen(player,level,s,3.0);

        // 11:59:30 — "I found you."
        if(t==17400){
            remove(level,s.girl);s.girl=null;
            spawnGirlInFront(level,player,s,7.0);
            play(level,player.blockPosition(),Train31Mod.WHISPER_FOUND_YOU.get(),3.5f,1.0f);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,520,0,false,false));
        }
        if(t>=17420 && t<17880 && t%20==0) rushGirl(player,level,s,0.55);

        // 11:59:45 — final whispered line.
        if(t==17700) play(level,player.blockPosition(),Train31Mod.WHISPER_SHOULD_LISTEN.get(),3.1f,1.0f);
        if(t==17800) play(level,player.blockPosition(),Train31Mod.GIRL_ROAR.get(),3.4f,0.96f);

        // Hard blackout -> vanilla Minecraft death screen. No graphic animation.
        if(t==17920){
            LightingController.pulse(player,true);
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
        int s=g.platformSide(),a=g.tunnelSign();
        return g.axisZ()?g.rail().offset(s*5,1,a*along):g.rail().offset(a*along,1,s*5);
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

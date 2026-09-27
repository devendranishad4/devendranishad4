package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cinematic, location-aware Train 31 story. No chat/action-bar narration: the scene is told by sound,
 * lighting, live CCTV, fog, the actor and the moving train. The only normal HUD element is the clock.
 */
public final class StoryDirector {
    private StoryDirector() {}
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        int tick;
        float fog;
        UUID girl;
        boolean cctvAuto;
        int cctvUntil;
        double trainOffset = 62.0;
        boolean trainSpawned;
        State(int delayTicks) { tick = -delayTicks; }
    }

    public static void start(ServerPlayer player, int delaySeconds) {
        if (STATES.containsKey(player.getUUID())) return;
        StationBuilder.ensurePrepared(player);
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player.serverLevel(), StationBuilder.geometry(player).rail());
        State s = new State(Math.max(0,delaySeconds)*20);
        STATES.put(player.getUUID(), s);
        sync(player,s,-1,false);
    }

    public static boolean isRunning(ServerPlayer p){ return STATES.containsKey(p.getUUID()); }
    public static int currentTick(ServerPlayer p){ State s=STATES.get(p.getUUID()); return s==null?0:s.tick; }
    public static float currentFog(ServerPlayer p){ State s=STATES.get(p.getUUID()); return s==null?0f:s.fog; }

    public static void tick(ServerPlayer player) {
        State s=STATES.get(player.getUUID()); if(s==null)return;
        s.tick++;
        ServerLevel level=player.serverLevel();
        StationBuilder.RailGeometry g=StationBuilder.geometry(player);
        int t=s.tick;

        if(t<0){ if(t%20==0)sync(player,s,-1,false); return; }

        // 0:00-1:20 — ordinary late-night station. No horror yet.
        if(t==200) LightingController.pulse(player,true);
        if(t==204) LightingController.pulse(player,false);
        if(t==360) LightingController.pulse(player,true);
        if(t==364) LightingController.pulse(player,false);
        if(t==420) play(level,g.rail(),Train31Mod.FLUORESCENT_BUZZ.get(),1.1f,1.0f);
        if(t==1200) play(level,g.rail(),Train31Mod.PA_NORMAL.get(),4.0f,1.0f);

        // 1:20-3:00 — small physical signs that something is wrong.
        if(t==1750 || t==2050) {
            BlockPos p=g.axisZ()?g.rail().offset(4,0,18):g.rail().offset(18,0,4);
            play(level,p,Train31Mod.METAL_KNOCKS.get(),1.7f,0.95f);
        }
        if(t==2200) {
            BlockPos room=StationBuilder.cctvRoom(player);
            play(level,room,Train31Mod.CAMERA_CLICK.get(),1.6f,1.0f);
        }

        // 3:00-4:10 — first sighting exists ONLY on the real CCTV view.
        if(t==3600) {
            spawnGirl(level,s, cameraScenePos(g,0));
            BlockPos room=StationBuilder.cctvRoom(player);
            play(level,room,Train31Mod.CCTV_STATIC.get(),1.0f,1.0f);
        }
        if(t>=3600 && t<3900 && !s.cctvAuto && player.blockPosition().closerThan(StationBuilder.cctvRoom(player),12.0)) {
            StationBuilder.enterCamera(player,2);
            s.cctvAuto=true; s.cctvUntil=t+260;
        }
        if(t==3740) moveGirl(level,s.girl,cameraScenePos(g,1));
        if(t==3840) moveGirl(level,s.girl,cameraScenePos(g,2));
        if(s.cctvAuto && t==s.cctvUntil) {
            play(level,StationBuilder.cctvRoom(player),Train31Mod.CCTV_STATIC.get(),1.6f,0.82f);
            remove(level,s.girl); s.girl=null;
            StationBuilder.exitCamera(player); s.cctvAuto=false;
        }

        // 4:10-6:30 — proper world fog, not a cloud-particle wall. It slowly steals view distance.
        if(t>=5000 && t<7800) {
            s.fog = Math.min(0.92f,(t-5000)/2800f*0.92f);
            if(t%40==0) lowMist(level,g.rail(),g.axisZ(),s.fog);
        }
        if(t==5150) play(level,g.rail(),Train31Mod.TUNNEL_RUMBLE.get(),1.4f,0.82f);
        if(t==5480) play(level,g.rail(),Train31Mod.METAL_KNOCKS.get(),2.0f,0.82f);
        if(t==5750) {
            remove(level,s.girl);
            spawnGirl(level,s,fogGirlPos(g,24));
        }
        if(t>=5850 && t<7400 && t%80==0 && s.girl!=null) advanceGirlWhenUnseen(player,level,s,g);
        if(t==6500) play(level,g.rail(),Train31Mod.PA_TRAIN31.get(),4.5f,0.98f);
        if(t==7000) play(level,tunnelPos(g,42),Train31Mod.TRAIN_HORN.get(),3.2f,0.88f);

        // 6:30-8:00 — fog peaks, figure vanishes, then the tunnel becomes unnaturally quiet.
        if(t>=7800 && t<9300) s.fog=0.94f;
        if(t==7900){remove(level,s.girl);s.girl=null;}
        if(t==8200) LightingController.pulse(player,true);
        if(t==8240) LightingController.pulse(player,false);
        if(t==8600) play(level,tunnelPos(g,48),Train31Mod.TUNNEL_RUMBLE.get(),2.2f,0.68f);

        // 8:00-10:00 — Train 31 actually approaches and stops next to the detected platform/rail.
        if(t==9600){
            s.trainOffset=62.0; StationBuilder.spawnTrain(level,g,s.trainOffset); s.trainSpawned=true;
            play(level,tunnelPos(g,50),Train31Mod.TRAIN_HORN.get(),3.5f,0.76f);
        }
        if(t>=9600 && t<11100 && s.trainSpawned){
            double target=4.2;
            double progress=(t-9600)/1500.0;
            double eased=1.0-Math.pow(1.0-Math.min(1,progress),3);
            double next=62.0+(target-62.0)*eased;
            StationBuilder.setTrainOffset(level,g,s.trainOffset,next); s.trainOffset=next;
            if(t%100==0) play(level,g.rail(),Train31Mod.TUNNEL_RUMBLE.get(),1.1f,0.72f+(float)progress*0.14f);
        }
        if(t==11200){ StationBuilder.openTrainDoors(level,g); s.fog=0.48f; }

        // 10:00-12:15 — no immediate attack. The girl appears where the train windows/cameras frame her.
        if(t==12100) spawnGirl(level,s,platformGirlPos(g,16));
        if(t==12600) moveGirl(level,s.girl,platformGirlPos(g,10));
        if(t==13000 && player.blockPosition().closerThan(StationBuilder.cctvRoom(player),13.0)) {
            StationBuilder.enterCamera(player,1); s.cctvAuto=true; s.cctvUntil=t+220;
        }
        if(t==13100) moveGirl(level,s.girl,platformGirlPos(g,5));
        if(s.cctvAuto && t==s.cctvUntil){ StationBuilder.exitCamera(player); s.cctvAuto=false; }
        if(t==13700){ remove(level,s.girl); s.girl=null; play(level,g.rail(),Train31Mod.CCTV_STATIC.get(),1.4f,0.7f); }

        // 12:15-14:10 — only now does the direct chase begin.
        if(t==14700){ s.fog=0.78f; spawnGirl(level,s,platformGirlPos(g,22)); }
        if(t>=14900 && t<16600 && s.girl!=null){
            Entity e=level.getEntity(s.girl);
            if(e instanceof ShadowGirlEntity girl) girl.getNavigation().moveTo(player,0.88D);
            if(t%140==0) play(level,e!=null?e.blockPosition():g.rail(),Train31Mod.METAL_KNOCKS.get(),0.9f,0.65f);
        }

        // 14:10-15:00 — silence, return, departure. No exposition text.
        if(t==16900){ remove(level,s.girl); s.girl=null; s.fog=0f; LightingController.pulse(player,false); }
        if(t>=17400 && t<18000 && s.trainSpawned){
            double next=s.trainOffset+0.11;
            StationBuilder.setTrainOffset(level,g,s.trainOffset,next);s.trainOffset=next;
        }
        if(t==17500) play(level,g.rail(),Train31Mod.PA_NORMAL.get(),3.5f,0.92f);
        if(t>=18000){ reset(player); return; }

        if(t%10==0) sync(player,s,-1,false);
    }

    public static void reset(ServerPlayer player){
        State s=STATES.remove(player.getUUID());
        if(s!=null){ remove(player.serverLevel(),s.girl); }
        LightingController.restore(player);
        StationBuilder.exitCamera(player);
        StationBuilder.removeTrain(player.serverLevel(),StationBuilder.geometry(player).rail());
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Train31Network.ClientState(0,0f,-1,false,false));
    }

    public static void skip(ServerPlayer player){ State s=STATES.get(player.getUUID()); if(s!=null)s.tick+=1200; }

    private static void sync(ServerPlayer p,State s,int camera,boolean cctv){
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),new Train31Network.ClientState(Math.max(0,s.tick),s.fog,camera,cctv,true));
    }

    private static void play(ServerLevel l,BlockPos p,SoundEvent sound,float volume,float pitch){
        l.playSound(null,p,sound,SoundSource.AMBIENT,volume,pitch);
    }

    private static void lowMist(ServerLevel l,BlockPos rail,boolean axisZ,float amount){
        int count=1+(int)(amount*3);
        for(int i=0;i<count;i++){
            double along=(l.random.nextDouble()-0.5)*42;
            double side=(l.random.nextDouble()-0.5)*7;
            double x=rail.getX()+0.5+(axisZ?side:along), z=rail.getZ()+0.5+(axisZ?along:side);
            l.sendParticles(ParticleTypes.WHITE_ASH,x,rail.getY()+0.35,z,1,0.25,0.08,0.25,0.002);
        }
    }

    private static void spawnGirl(ServerLevel l,State s,BlockPos p){
        remove(l,s.girl);
        ShadowGirlEntity girl=Train31Mod.SHADOW_GIRL.get().create(l); if(girl==null)return;
        girl.setPos(p.getX()+0.5,p.getY(),p.getZ()+0.5);girl.setYRot(180f);girl.setYHeadRot(180f);girl.setInvulnerable(true);
        girl.addTag("train31_girl");l.addFreshEntity(girl);s.girl=girl.getUUID();
    }
    private static void moveGirl(ServerLevel l,UUID id,BlockPos p){Entity e=id==null?null:l.getEntity(id);if(e!=null)e.teleportTo(p.getX()+0.5,p.getY(),p.getZ()+0.5);}
    private static void remove(ServerLevel l,UUID id){Entity e=id==null?null:l.getEntity(id);if(e!=null)e.discard();}

    private static BlockPos cameraScenePos(StationBuilder.RailGeometry g,int stage){int d=stage==0?30:stage==1?18:7;return platformGirlPos(g,d);}
    private static BlockPos platformGirlPos(StationBuilder.RailGeometry g,int along){return g.axisZ()?g.rail().offset(-5,1,along):g.rail().offset(along,1,-5);}
    private static BlockPos fogGirlPos(StationBuilder.RailGeometry g,int along){return g.axisZ()?g.rail().offset(-4,1,along):g.rail().offset(along,1,-4);}
    private static BlockPos tunnelPos(StationBuilder.RailGeometry g,int along){return g.axisZ()?g.rail().offset(0,0,along):g.rail().offset(along,0,0);}

    private static void advanceGirlWhenUnseen(ServerPlayer p,ServerLevel level,State s,StationBuilder.RailGeometry g){
        Entity e=level.getEntity(s.girl);if(e==null)return;
        Vec3 to=e.position().subtract(p.position());double len=to.length();if(len<3)return;
        double dot=p.getLookAngle().normalize().dot(to.normalize());
        if(dot<0.55){
            Vec3 next=e.position().add(p.position().subtract(e.position()).normalize().scale(Math.min(3.2,len-2.5)));
            e.teleportTo(next.x,next.y,next.z);
        }
    }
}

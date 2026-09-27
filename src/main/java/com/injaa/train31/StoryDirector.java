package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.WitherSkeleton;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StoryDirector {
    private StoryDirector() {}
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static class State {
        int tick;
        final BlockPos origin;
        UUID shadow;
        UUID passenger;
        State(int tick, BlockPos origin) { this.tick=tick; this.origin=origin; }
    }

    public static void start(ServerPlayer player, int delaySeconds) {
        // Ignore extra start clicks while a take is already armed/running.
        if (STATES.containsKey(player.getUUID())) return;

        long packed = player.getPersistentData().getLong("train31_origin");
        if (packed == 0L) {
            StationBuilder.build(player);
            packed = player.getPersistentData().getLong("train31_origin");
        }
        BlockPos o = BlockPos.of(packed);
        StationBuilder.removeTrain(player.serverLevel(), o);
        StationBuilder.setRedMode(player.serverLevel(), o, false);
        STATES.put(player.getUUID(), new State(-delaySeconds*20, o));
        player.sendSystemMessage(Component.literal("§c[Train 31: Tokyo Edition] §fAUTO armed — §e" + delaySeconds + "s§f recording delay."));
    }

    public static void tick(ServerPlayer player) {
        State s=STATES.get(player.getUUID());
        if(s==null) return;
        s.tick++;
        ServerLevel level=player.serverLevel();
        int t=s.tick;

        if(t==0) say(player,"§7[11:45 PM] Night shift started. Platform 3 is out of service.");
        if(t==1000){ sound(level,s.origin,SoundEvents.NOTE_BLOCK_BELL.value(),0.6f); say(player,"§eA fluorescent light flickers somewhere down the platform..."); }
        if(t==2000){ sound(level,s.origin,SoundEvents.NOTE_BLOCK_BELL.value(),0.4f); say(player,"§fAnnouncement: §7Last service has ended. Please leave the station."); }

        if(t==3200){ spawnShadow(level,s,"shadow",s.origin.offset(0,0,38)); say(player,"§8Something is standing at the far end of Platform 3."); }
        if(t==4300){ sound(level,s.origin.offset(4,-1,12),SoundEvents.IRON_DOOR_CLOSE,0.55f); say(player,"§7...knock... knock... from below the platform."); }
        if(t==5400){ say(player,"§bCCTV CAM 03: §fMotion detected on Platform 3."); moveShadow(level,s.shadow,s.origin.offset(0,0,25)); }
        if(t==6500){ sound(level,s.origin,SoundEvents.NOTE_BLOCK_BASS.value(),0.5f); say(player,"§7Radio: Don't board it. Whatever happens, don't board Train 31."); }
        if(t==7700){ sound(level,s.origin.offset(5,0,58),SoundEvents.ANVIL_LAND,0.35f); say(player,"§8A distant metallic horn rolls through the tunnel."); }

        if(t==8500){
            for(int i=0;i<10;i++) level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    s.origin.getX()+5.0,
                    s.origin.getY()+1.2,
                    s.origin.getZ()+52+i*1.5,
                    8,0.5,0.35,0.5,0.008);
            say(player,"§7A white headlight appears deep inside the tunnel...");
        }

        if(t==9200){
            removeEntity(level,s.shadow); s.shadow=null;
            StationBuilder.buildTrain(level,s.origin);
            sound(level,s.origin.offset(5,0,14),SoundEvents.PISTON_EXTEND,0.35f);
            say(player,"§cTRAIN 31 §fhas arrived at Platform 3.");
        }
        if(t==10000){ StationBuilder.openTrainDoors(level,s.origin); say(player,"§cThe doors open. §7No one gets off."); }
        if(t==10800){ spawnShadow(level,s,"passenger",s.origin.offset(1,0,18)); say(player,"§8You look away for one second. A passenger is now standing on the platform."); }
        if(t==12000){ moveShadow(level,s.passenger,s.origin.offset(1,0,11)); say(player,"§bCCTV: §fThe passenger moved while you weren't looking."); }
        if(t==13100){ say(player,"§6OLD REPORT — 1998: §fTrain 31 entered with one more passenger than it departed with."); }
        if(t==14100){ StationBuilder.setRedMode(level,s.origin,true); sound(level,s.origin,SoundEvents.IRON_DOOR_CLOSE,0.5f); say(player,"§cEMERGENCY LOCKDOWN. §fStation exits sealed."); }
        if(t==15000){ say(player,"§7Radio: I never told you I escaped."); moveShadow(level,s.passenger,s.origin.offset(1,0,5)); }
        if(t==15800){ say(player,"§cRUN. §fGet back through the station before it reaches you."); sound(level,s.origin,SoundEvents.ENDERMAN_STARE,0.45f); }

        if(t==16900){
            removeEntity(level,s.passenger); s.passenger=null;
            StationBuilder.setRedMode(level,s.origin,false);
            say(player,"§f[1:00 AM] Everything stops.");
        }
        if(t==17400){ say(player,"§8The passenger calmly steps back into Train 31."); }
        if(t==17700){ StationBuilder.removeTrain(level,s.origin); sound(level,s.origin.offset(5,0,14),SoundEvents.PISTON_EXTEND,0.25f); say(player,"§7Train 31 disappears into the tunnel."); }
        if(t==17900){ say(player,"§cAttention please. Train 31 will return tomorrow."); }
        if(t>18100){ STATES.remove(player.getUUID()); }
    }

    public static void reset(ServerPlayer player){
        State s=STATES.remove(player.getUUID());
        BlockPos o = s != null ? s.origin : StationBuilder.TOKYO_ANCHOR;
        if(s!=null){ removeEntity(player.serverLevel(),s.shadow); removeEntity(player.serverLevel(),s.passenger); }
        StationBuilder.removeTrain(player.serverLevel(),o);
        StationBuilder.setRedMode(player.serverLevel(),o,false);
        player.sendSystemMessage(Component.literal("§a[Train 31] Tokyo story reset. Ready for another take."));
    }

    public static void skip(ServerPlayer player){
        State s=STATES.get(player.getUUID());
        if(s!=null){ s.tick+=1200; player.sendSystemMessage(Component.literal("§e[Train 31] Skipped ~60 seconds.")); }
    }

    private static void spawnShadow(ServerLevel level, State s, String kind, BlockPos p){
        WitherSkeleton e= EntityType.WITHER_SKELETON.create(level);
        if(e==null) return;
        e.setNoAi(true);
        e.setSilent(true);
        e.setInvulnerable(true);
        e.setPos(p.getX()+0.5,p.getY(),p.getZ()+0.5);
        level.addFreshEntity(e);
        if(kind.equals("shadow")) s.shadow=e.getUUID(); else s.passenger=e.getUUID();
    }

    private static void moveShadow(ServerLevel level, UUID id, BlockPos p){
        Entity e=id==null?null:level.getEntity(id);
        if(e!=null) e.teleportTo(p.getX()+0.5,p.getY(),p.getZ()+0.5);
    }

    private static void removeEntity(ServerLevel level, UUID id){
        Entity e=id==null?null:level.getEntity(id);
        if(e!=null) e.discard();
    }

    private static void say(ServerPlayer p,String msg){ p.displayClientMessage(Component.literal(msg),true); }
    private static void sound(ServerLevel l,BlockPos p,net.minecraft.sounds.SoundEvent s,float pitch){ l.playSound(null,p,s,SoundSource.AMBIENT,1.2f,pitch); }
}

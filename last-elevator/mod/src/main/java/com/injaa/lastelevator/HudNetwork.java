package com.injaa.lastelevator;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** A compact, client-only objective and waypoint; never writes chat/actionbar. */
public final class HudNetwork {
    private HudNetwork(){}
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LastElevator.ID,"director_hud"),()->VERSION,
            VERSION::equals,VERSION::equals);

    public static void register(){
        CHANNEL.registerMessage(0,Update.class,Update::encode,Update::decode,Update::handle);
    }
    private record Update(boolean visible,String objective,BlockPos target) {
        private static void encode(Update m,FriendlyByteBuf b){
            b.writeBoolean(m.visible);b.writeUtf(m.objective,96);b.writeBlockPos(m.target);
        }
        private static Update decode(FriendlyByteBuf b){
            return new Update(b.readBoolean(),b.readUtf(96),b.readBlockPos());
        }
        private static void handle(Update m,Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    ()->()->com.injaa.lastelevator.client.DirectorHud.update(m.visible,m.objective,m.target)));
            ctx.get().setPacketHandled(true);
        }
    }
    private static void show(ServerPlayer p,String text,int x,int y,int z){
        CHANNEL.sendTo(new Update(true,text,new BlockPos(x,y,z)),
                p.connection.connection,NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void clear(ServerPlayer p){
        CHANNEL.sendTo(new Update(false,"",BlockPos.ZERO),
                p.connection.connection,NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void sync(ServerPlayer p){
        CompoundTag d=p.getPersistentData().getCompound(LastElevator.ID);
        if(d.getInt("towerVersion")<4||!d.getBoolean("running")){clear(p);return;}
        if(d.getInt("delay")>0){show(p,"Recording starts soon",-254,75,107);return;}
        if(d.getBoolean("liftMoving")){show(p,"Lift moving — hold on",
                p.blockPosition().getX(),p.blockPosition().getY(),p.blockPosition().getZ());return;}
        int scene=d.getInt("scene");
        if(scene==0){show(p,"Lift: press button inside",-237,75,100);return;}
        if(scene==1){
            if(d.getBoolean("fuse1"))show(p,"Return to the lift",-237,85,100);
            else show(p,"Open the office drawer",-265,85,94);
            return;
        }
        if(scene==2){show(p,"Find fuse 2 in hotel",-256,100,106);return;}
        if(scene==3){
            boolean entered=d.getBoolean("ruleSpawned");
            String goal=!entered?"Board lift; obey the rule":
                    d.getInt("elapsed")-d.getInt("ruleBell")<45?"Wait for bell to settle":"Face panel; press again";
            show(p,goal,entered?-233:-237,100,100);return;
        }
        if(scene==4){
            if(d.getBoolean("fuse3"))show(p,"RUN back to lift",-237,90,100);
            else if(d.getBoolean("electricalOpen"))show(p,"Take fuse 3",-256,90,106);
            else if(d.getBoolean("breakerKey"))show(p,"Unlock electrical room",-256,90,104);
            else show(p,"Press breaker for key",-251,90,101);
            return;
        }
        if(scene==5){
            if(d.getBoolean("panelRestored"))show(p,"Take the service stairs",-225,85,104);
            else show(p,"Install fuses at panel",-255,85,100);
            return;
        }
        if(scene==6){show(p,"Climb the real stairs",-225,115,103);return;}
        if(scene==7){
            if(d.getBoolean("coldOpen")){show(p,"RUN to emergency exit",-273,115,100);return;}
            if(d.getBoolean("sealOpen"))show(p,"Descend fire stairs",-292,66,100);
            else show(p,"Break fire exit seal",-273,115,100);
            return;
        }
        clear(p);
    }
}

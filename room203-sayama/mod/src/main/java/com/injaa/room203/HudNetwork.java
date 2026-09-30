package com.injaa.room203;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class HudNetwork {
 private static final SimpleChannel C=NetworkRegistry.newSimpleChannel(new ResourceLocation(Room203.ID,"hud"),()->"1","1"::equals,"1"::equals);
 public static void register(){C.registerMessage(0,Message.class,Message::encode,Message::decode,Message::handle);}
 public record Message(boolean visible,String objective,BlockPos target,String note,int noteTicks,boolean dark){
  static void encode(Message m,FriendlyByteBuf b){b.writeBoolean(m.visible);b.writeUtf(m.objective,100);b.writeBlockPos(m.target);b.writeUtf(m.note,500);b.writeInt(m.noteTicks);b.writeBoolean(m.dark);}
  static Message decode(FriendlyByteBuf b){return new Message(b.readBoolean(),b.readUtf(100),b.readBlockPos(),b.readUtf(500),b.readInt(),b.readBoolean());}
  static void handle(Message m,Supplier<NetworkEvent.Context> ctx){ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.injaa.room203.client.RoomHud.update(m)));ctx.get().setPacketHandled(true);}
 }
 public static void send(ServerPlayer p,boolean show,String objective,BlockPos target,String note,int ticks,boolean dark){C.sendTo(new Message(show,objective,target,note,ticks,dark),p.connection.connection,NetworkDirection.PLAY_TO_CLIENT);}
}

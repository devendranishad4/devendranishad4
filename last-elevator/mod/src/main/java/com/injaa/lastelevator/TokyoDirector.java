package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

/** Story locations in one real multi-floor tower of Tokyo Inspired City 1.0.10. */
public final class TokyoDirector {
    private TokyoDirector(){}
    private static final BlockPos HOTEL=new BlockPos(-230,74,100);
    private static final BlockPos UPPER=new BlockPos(-230,99,100);
    private static void set(ServerLevel w,int x,int y,int z,Block b){w.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
    private static void fill(ServerLevel w,int x0,int y0,int z0,int x1,int y1,int z1,Block b){
        for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)set(w,x,y,z,b);
    }
    private static void sign(ServerLevel w,int x,int y,int z,String... lines){
        BlockPos p=new BlockPos(x,y,z);
        w.setBlock(p,Blocks.OAK_SIGN.defaultBlockState(),2);
        if(w.getBlockEntity(p) instanceof SignBlockEntity sign){
            var text=sign.getFrontText();
            for(int i=0;i<Math.min(lines.length,4);i++)text=text.setMessage(i,Component.literal(lines[i]));
            sign.setText(text,true);sign.setChanged();
            w.sendBlockUpdated(p,w.getBlockState(p),w.getBlockState(p),3);
        }
    }
    private static void marker(ServerPlayer p,String name,int x,int y,int z,float yaw){
        CompoundTag data=p.getPersistentData(),d=data.getCompound(LastElevator.ID);
        CompoundTag marks=d.getCompound("marks"),value=new CompoundTag();
        value.putLong("pos",new BlockPos(x,y,z).asLong());value.putFloat("yaw",yaw);
        marks.put(name,value);d.put("marks",marks);data.put(LastElevator.ID,d);
    }
    public static String install(ServerPlayer player){
        ServerLevel w=(ServerLevel)player.level();
        if(!w.getBlockState(HOTEL).is(Blocks.POLISHED_DIORITE)
                ||!w.getBlockState(UPPER).is(Blocks.POLISHED_DIORITE))
            return "This is not the supplied Tokyo Inspired City 1.0.10 world. No blocks changed.";
        CompoundTag d=player.getPersistentData().getCompound(LastElevator.ID);
        if(d.getInt("towerVersion")>=4)return "Tower already installed. Run /le reset, /le setup, /le auto 20.";
        TowerLift.install(w);
        // The original rooms remain on upper floors; dress each corridor according to the story.
        for(int floor:new int[]{84,89,99,114}){
            for(int x=-270;x<=-241;x++){
                for(int z=99;z<=101;z++){
                    if(w.getBlockState(new BlockPos(x,floor,z)).is(Blocks.POLISHED_DIORITE))
                        set(w,x,floor,z,floor==114?Blocks.SMOOTH_QUARTZ:
                                floor==99?Blocks.RED_CONCRETE:
                                floor==89?Blocks.POLISHED_DEEPSLATE:Blocks.POLISHED_BLACKSTONE);
                }
                if(x%8==0)set(w,x,floor+4,100,floor==89?Blocks.REDSTONE_LAMP:Blocks.OCHRE_FROGLIGHT);
            }
        }
        // Readable clues and floor indicators sit next to the actual lifts.
        sign(w,-247,75,101,"NIGHT SHIFT","11:47 PM","Floor 6: panel","3 fuses missing");
        sign(w,-240,75,102,"LIFT","Press button","inside car","");
        sign(w,-255,85,102,"MAINTENANCE","FUSES 0 / 3","Find all three","Return here");
        for(int x:new int[]{-257,-255,-253})set(w,x,88,102,Blocks.POLISHED_BLACKSTONE);
        set(w,-255,84,100,Blocks.IRON_BLOCK);
        set(w,-255,85,100,Blocks.STONE_BUTTON);
        sign(w,-261,85,102,"DON'T RETURN","WITH TWO","PEOPLE","");
        sign(w,-247,100,102,"FLOOR 13","NOT ON THE","DIRECTORY","");
        // Furnished original suite: a second untouched cup is the visual clue.
        set(w,-261,101,108,Blocks.SEA_PICKLE);
        sign(w,-245,100,103,"AFTER THE BELL","DO NOT LOOK","AT THE OTHER","PASSENGER");
        sign(w,-245,90,102,"MAINTENANCE","RADIO / BREAKER","FUSE THREE","");
        set(w,-251,89,101,Blocks.IRON_BLOCK);
        set(w,-251,90,101,Blocks.STONE_BUTTON);
        sign(w,-251,90,102,"BREAKER","KEY RELEASE","PRESS BUTTON","");
        fill(w,-257,90,104,-255,92,104,Blocks.IRON_BARS);
        sign(w,-259,90,102,"ELECTRICAL","ROOM LOCKED","FIND KEY","");
        sign(w,-245,115,102,"0","ONE PASSENGER","MUST REMAIN","");
        sign(w,-258,115,102,"EMPLOYEE","PHOTO FILE","UNASSIGNED","");
        // Visible, reachable fuses. They are spawned as items by /le setup.
        marker(player,"lobby",-255,75,107,0);marker(player,"car",-234,75,101,90);
        marker(player,"office",-252,85,100,90);marker(player,"hotel",-267,100,100,90);
        marker(player,"maintenance",-250,90,100,90);marker(player,"stair",-241,105,100,90);
        marker(player,"zero",-250,115,100,90);marker(player,"street",-292,66,100,90);
        marker(player,"fuse1",-265,85,100,0);marker(player,"fuse2",-256,100,106,0);
        marker(player,"fuse3",-256,90,106,0);
        marker(player,"passenger_rule",-245,100,100,0);
        marker(player,"passenger_maintenance",-268,90,100,0);
        marker(player,"passenger_zero",-259,115,100,0);
        d=player.getPersistentData().getCompound(LastElevator.ID);
        d.putInt("towerVersion",4);d.putInt("carFloor",TowerLift.MIN);
        d.putBoolean("tokyoInstalled",true);d.putBoolean("built",false);
        seal(w,true);
        player.teleportTo(w,-254.5,75,107.5,0,0);
        return "Tower with physical moving lift installed. Run /le check, /le setup, /le auto 20.";
    }
    public static void seal(ServerLevel w,boolean closed){
        fill(w,-273,115,100,-273,117,101,closed?Blocks.IRON_BARS:Blocks.AIR);
    }
    public static void employee(ServerPlayer p){
        String name=p.getGameProfile().getName();
        ServerLevel w=(ServerLevel)p.level();
        sign(w,-258,115,102,"EMPLOYEE","PHOTO FILE",name,"");
        BlockPos head=new BlockPos(-258,116,103);
        set(w,-258,115,103,Blocks.POLISHED_BLACKSTONE);
        w.setBlock(head,Blocks.PLAYER_HEAD.defaultBlockState(),2);
        if(w.getBlockEntity(head) instanceof SkullBlockEntity skull){
            skull.setOwner(p.getGameProfile());skull.setChanged();
            w.sendBlockUpdated(head,w.getBlockState(head),w.getBlockState(head),3);
        }
    }
    public static void operator(ServerPlayer p){
        String name=p.getGameProfile().getName();
        sign((ServerLevel)p.level(),-292,66,102,"TOWER DIRECTORY","NIGHT OPERATOR",name,"");
    }
    public static void restorePanel(ServerPlayer p){
        ServerLevel w=(ServerLevel)p.level();
        for(int x:new int[]{-257,-255,-253})set(w,x,88,102,Blocks.SEA_LANTERN);
        sign(w,-255,85,102,"POWER RESTORED","FUSES 3 / 3","ONE PASSENGER","MUST REMAIN");
    }
    public static void resetPanel(ServerLevel w){
        for(int x:new int[]{-257,-255,-253})set(w,x,88,102,Blocks.POLISHED_BLACKSTONE);
        sign(w,-255,85,102,"MAINTENANCE","FUSES 0 / 3","Find all three","Return here");
    }
    public static void electricalDoor(ServerLevel w,boolean locked){
        fill(w,-257,90,104,-255,92,104,locked?Blocks.IRON_BARS:Blocks.AIR);
    }
}

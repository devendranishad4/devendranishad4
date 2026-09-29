package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Set dressing and story markers inside the supplied Tokyo Inspired City 1.0.10 world. */
public final class TokyoDirector {
    private TokyoDirector(){}
    private static final BlockPos ATRIUM=new BlockPos(-230,77,660);
    private static final BlockPos GLASS_TOWER=new BlockPos(76,85,236);
    private static final BlockPos HOTEL=new BlockPos(-250,74,100);
    private static final BlockPos UPPER_HOTEL=new BlockPos(-271,99,100);

    public static String install(ServerPlayer player){
        ServerLevel world=(ServerLevel)player.level();
        if(!world.getBlockState(ATRIUM).is(Blocks.SPRUCE_PLANKS)
                ||!world.getBlockState(GLASS_TOWER).is(Blocks.BLACK_STAINED_GLASS)
                ||!world.getBlockState(HOTEL).is(Blocks.POLISHED_DIORITE)
                ||!world.getBlockState(UPPER_HOTEL).is(Blocks.POLISHED_DIORITE))
            return "This is not the supplied Tokyo Inspired City 1.0.10 world. No blocks changed.";
        CompoundTag story=player.getPersistentData().getCompound(LastElevator.ID);
        if(story.getBoolean("tokyoInstalled"))return "Tokyo story already installed. Run /le check, /le setup, /le auto 20.";
        // Restore a 20-block-high atrium already present in the Tokyo city map.
        // Its skyscraper walls, windows, roof, street and skyline are original map blocks.
        for(int x=-258;x<=-216;x++)for(int z=654;z<=678;z++){
            BlockPos floor=new BlockPos(x,77,z);
            if(world.getBlockState(floor).is(Blocks.SPRUCE_PLANKS)){
                Block tile=(x/5+z/5)%2==0?Blocks.POLISHED_BLACKSTONE:Blocks.POLISHED_DIORITE;
                if(z>=663&&z<=667)tile=Blocks.POLISHED_DEEPSLATE;
                if(z==665&&x%6==0)tile=Blocks.CHISELED_QUARTZ_BLOCK;
                set(world,floor,tile);
            }
        }
        for(int x:new int[]{-256,-243,-230,-219})for(int z:new int[]{655,677}){
            fill(world,x,78,z,x+1,90,z+1,Blocks.SMOOTH_QUARTZ);
            fill(world,x,78,z,x+1,78,z+1,Blocks.POLISHED_BLACKSTONE);
            fill(world,x,90,z,x+1,90,z+1,Blocks.CUT_COPPER);
            set(world,new BlockPos(x,84,z),Blocks.OCHRE_FROGLIGHT);
        }
        for(int x:new int[]{-250,-235,-221}){
            fill(world,x,93,664,x,95,664,Blocks.CHAIN);
            fill(world,x-1,92,663,x+1,92,665,Blocks.CUT_COPPER);
            set(world,new BlockPos(x,92,664),Blocks.OCHRE_FROGLIGHT);
        }
        // Reception counter with glowing front and furnishings, clear central path.
        fill(world,-253,78,657,-242,79,660,Blocks.POLISHED_BLACKSTONE_BRICKS);
        fill(world,-253,80,660,-242,80,660,Blocks.OCHRE_FROGLIGHT);
        fill(world,-253,81,657,-242,81,659,Blocks.DARK_OAK_PLANKS);
        for(int x:new int[]{-250,-236,-224}){
            fill(world,x,78,672,x+3,78,672,Blocks.DARK_OAK_STAIRS);
            fill(world,x+1,78,675,x+2,78,675,Blocks.DARK_OAK_STAIRS);
            fill(world,x+1,78,673,x+2,78,674,Blocks.BROWN_CARPET);
        }
        for(int x:new int[]{-256,-235})for(int z:new int[]{660,674}){
            fill(world,x,78,z,x+2,79,z+2,Blocks.POLISHED_BLACKSTONE_BRICKS);
            for(int y=80;y<=83;y++)world.setBlock(new BlockPos(x+1,y,z+1),
                    Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
        }
        // Framed lift at the far side of the atrium. Its doorway faces the reception.
        for(int y=78;y<=84;y++){
            for(int z=660;z<=668;z++){
                if(!(z>=662&&z<=665&&y<=81))set(world,new BlockPos(-218,y,z),Blocks.POLISHED_DEEPSLATE);
                set(world,new BlockPos(-211,y,z),Blocks.POLISHED_DEEPSLATE);
            }
            for(int x=-217;x<=-212;x++){
                set(world,new BlockPos(x,y,660),Blocks.IRON_BLOCK);
                set(world,new BlockPos(x,y,668),Blocks.IRON_BLOCK);
            }
        }
        for(int x=-217;x<=-212;x++)for(int z=661;z<=667;z++){
            set(world,new BlockPos(x,77,z),Blocks.POLISHED_BLACKSTONE);
            for(int y=78;y<=82;y++)set(world,new BlockPos(x,y,z),Blocks.AIR);
        }
        for(int x=-216;x<=-213;x++)for(int z=662;z<=666;z++)set(world,new BlockPos(x,84,z),Blocks.SEA_LANTERN);
        set(world,new BlockPos(-212,78,663),Blocks.IRON_BLOCK);
        set(world,new BlockPos(-212,79,663),Blocks.STONE_BUTTON);
        // Glass-walled office remains in another authentic Tokyo skyscraper.
        for(int x=68;x<=83;x++)for(int z=230;z<=239;z++){
            BlockPos floor=new BlockPos(x,85,z);
            if(world.getBlockState(floor).is(Blocks.BLACK_STAINED_GLASS))
                set(world,floor,(x+z)%8==0?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.POLISHED_DIORITE);
        }
        for(int x:new int[]{69,73,79}){
            fill(world,x,86,231,x+2,86,233,Blocks.DARK_OAK_PLANKS);
            set(world,new BlockPos(x+1,87,232),Blocks.LANTERN);
        }
        fill(world,68,86,237,73,87,238,Blocks.POLISHED_DEEPSLATE);
        for(int x:new int[]{69,72})set(world,new BlockPos(x,88,237),Blocks.REDSTONE_LAMP);
        // Retain the Tokyo building's doors, room furniture, facade, and six real floors.
        for(int floor:new int[]{74,79,84,89,94,99,104,109,114}){
            for(int x=-272;x<=-237;x++){
                for(int z=99;z<=101;z++){
                    BlockPos at=new BlockPos(x,floor,z);
                    if(world.getBlockState(at).is(Blocks.POLISHED_DIORITE))
                        set(world,at,(floor>=99&&floor<=104)?Blocks.RED_CONCRETE:Blocks.POLISHED_BLACKSTONE);
                }
                if(x%8==0){
                    BlockPos light=new BlockPos(x,floor+4,100);
                    if(world.isEmptyBlock(light))set(world,light,floor==89?Blocks.REDSTONE_LAMP:Blocks.OCHRE_FROGLIGHT);
                }
            }
        }
        seal(world,true);
        marker(player,"lobby",-235,78,665,270);marker(player,"car",-215,78,663,270);
        marker(player,"office",76,86,236,90);marker(player,"hotel",-269,100,100,270);
        marker(player,"maintenance",-250,90,100,270);marker(player,"stair",-218,110,110,90);
        marker(player,"zero",-244,115,100,270);marker(player,"street",-175,72,665,180);
        marker(player,"fuse1",82,86,236,0);marker(player,"fuse2",-250,100,100,0);
        marker(player,"fuse3",-241,90,100,0);marker(player,"passenger_rule",-258,100,100,0);
        marker(player,"passenger_maintenance",-238,90,100,0);
        marker(player,"passenger_zero",-251,115,100,0);
        story=player.getPersistentData().getCompound(LastElevator.ID);
        story.putBoolean("tokyoInstalled",true);story.putBoolean("built",false);
        player.teleportTo(world,-234.5,78,665.5,270,0);
        return "Tokyo city story installed in the real atrium, glass tower and furnished hotel. Run /le check, /le setup, /le auto 20.";
    }
    public static void seal(ServerLevel world,boolean closed){
        for(int y=115;y<=117;y++)for(int z=99;z<=101;z++)
            set(world,new BlockPos(-234,y,z),closed?Blocks.IRON_BARS:Blocks.AIR);
    }
    private static void fill(ServerLevel w,int x1,int y1,int z1,int x2,int y2,int z2,Block b){
        for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)for(int x=x1;x<=x2;x++)set(w,new BlockPos(x,y,z),b);
    }
    private static void set(ServerLevel w,BlockPos at,Block b){w.setBlock(at,b.defaultBlockState(),2);}
    private static void marker(ServerPlayer p,String name,int x,int y,int z,float yaw){
        CompoundTag data=p.getPersistentData(),story=data.getCompound(LastElevator.ID),marks=story.getCompound("marks"),value=new CompoundTag();
        value.putLong("pos",new BlockPos(x,y,z).asLong());value.putFloat("yaw",yaw);
        marks.put(name,value);story.put("marks",marks);data.put(LastElevator.ID,story);
    }
}

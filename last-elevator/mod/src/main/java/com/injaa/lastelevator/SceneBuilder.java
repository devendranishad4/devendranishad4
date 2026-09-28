package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Places the six independent sets above an empty 72x52 footprint. */
public final class SceneBuilder {
    private SceneBuilder(){}
    private record Placement(String resource,int dx,int dy,int dz){}
    private record Prepared(Placement place,int width,int height,int length,byte[] data,BlockState[] palette){}
    private static final Placement[] LAYOUT={
        new Placement("01_lobby_and_lift",0,0,0),
        new Placement("02_office_and_fuse_panel",0,14,0),
        new Placement("03_impossible_hotel_13",0,24,0),
        new Placement("04_maintenance_chase",0,34,0),
        new Placement("05_floor_zero_and_exit",0,44,0),
        new Placement("06_looping_stairwell",54,0,10)
    };
    public static String build(ServerPlayer p){
        ServerLevel world=(ServerLevel)p.level();BlockPos origin=p.blockPosition();
        List<Prepared> sets=new ArrayList<>();
        try{
            for(Placement place:LAYOUT){
                String path="/data/lastelevator/sets/"+place.resource+".schem";
                try(InputStream stream=SceneBuilder.class.getResourceAsStream(path)){
                    if(stream==null)return "Missing schematic resource: "+place.resource;
                    CompoundTag root=NbtIo.readCompressed(stream);
                    if(root.getInt("Version")!=2)return "Unsupported schematic: "+place.resource;
                    int w=root.getShort("Width"),h=root.getShort("Height"),l=root.getShort("Length");
                    byte[] data=root.getByteArray("BlockData");
                    BlockState[] states=new BlockState[root.getInt("PaletteMax")];
                    CompoundTag palette=root.getCompound("Palette");
                    for(String key:palette.getAllKeys()){
                        int id=palette.getInt(key);
                        Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key.split("\\[")[0]));
                        if(block==null||block==Blocks.AIR&&!key.startsWith("minecraft:air"))return "Unknown block: "+key;
                        BlockState state=block.defaultBlockState();
                        int bracket=key.indexOf('[');
                        if(bracket>=0&&key.endsWith("]")){
                            for(String pair:key.substring(bracket+1,key.length()-1).split(",")){
                                String[] kv=pair.split("=",2);
                                if(kv.length!=2)return "Invalid block property: "+key;
                                boolean found=false;
                                for(Property<?> property:state.getProperties()){
                                    if(property.getName().equals(kv[0])){
                                        state=withProperty(state,property,kv[1]);found=true;break;
                                    }
                                }
                                if(!found)return "Unknown block property: "+key;
                            }
                        }
                        states[id]=state;
                    }
                    if(w<=0||h<=0||l<=0||states.length==0)return "Invalid schematic: "+place.resource;
                    sets.add(new Prepared(place,w,h,l,data,states));
                }
            }
            // Refuse to replace *any* existing city block. Scan every target volume before changing anything.
            for(Prepared s:sets)for(int y=0;y<s.height;y++)for(int z=0;z<s.length;z++)for(int x=0;x<s.width;x++){
                BlockPos at=origin.offset(s.place.dx+x,s.place.dy+y,s.place.dz+z);
                if(!world.isEmptyBlock(at))return "Blocked at "+at.toShortString()+". Stand on an empty plot; nothing was placed.";
            }
            for(Prepared s:sets){
                int cursor=0,index=0,total=s.width*s.height*s.length;
                while(index<total){
                    int shift=0,id=0,part;
                    do{
                        if(cursor>=s.data.length)return "Truncated schematic: "+s.place.resource;
                        part=s.data[cursor++]&255;id|=(part&127)<<shift;shift+=7;
                    }while((part&128)!=0&&shift<35);
                    if(id<0||id>=s.palette.length||s.palette[id]==null)return "Bad palette index: "+s.place.resource;
                    BlockState block=s.palette[id];
                    if(!block.isAir()){
                        int x=index%s.width,z=(index/s.width)%s.length,y=index/(s.width*s.length);
                        world.setBlock(origin.offset(s.place.dx+x,s.place.dy+y,s.place.dz+z),block,2);
                    }
                    index++;
                }
            }
            // Each marker is at a clear arrival position within the selected set.
            marker(p,"lobby",origin.offset(23,1,8));marker(p,"street",origin.offset(22,1,1));
            marker(p,"car",origin.offset(36,1,31));marker(p,"office",origin.offset(20,15,11));
            marker(p,"hotel",origin.offset(10,25,9));marker(p,"maintenance",origin.offset(10,35,10));
            marker(p,"stair",origin.offset(59,1,19));marker(p,"zero",origin.offset(10,45,9));
            marker(p,"fuse1",origin.offset(8,15,16));marker(p,"fuse2",origin.offset(46,25,10));
            marker(p,"fuse3",origin.offset(47,35,3));
            marker(p,"passenger_rule",origin.offset(18,25,9));
            marker(p,"passenger_maintenance",origin.offset(42,35,10));
            marker(p,"passenger_zero",origin.offset(35,45,10));
            CompoundTag story=p.getPersistentData().getCompound(LastElevator.ID);
            story.putBoolean("built",true);story.putLong("builtOrigin",origin.asLong());
            p.teleportTo(world,origin.getX()+23.5,origin.getY()+1,origin.getZ()+8.5,0,0);
            return "Placed all six sets and 14 markers. Run /le check, then /le setup and /le auto 20.";
        }catch(Exception error){return "Build stopped: "+error.getClass().getSimpleName()+": "+error.getMessage();}
    }
    private static <T extends Comparable<T>> BlockState withProperty(BlockState state,Property<T> property,String value){
        return property.getValue(value).map(v->state.setValue(property,v)).orElse(state);
    }
    private static void marker(ServerPlayer p,String name,BlockPos at){
        CompoundTag data=p.getPersistentData(),story=data.getCompound(LastElevator.ID),marks=story.getCompound("marks"),value=new CompoundTag();
        value.putLong("pos",at.asLong());value.putFloat("yaw",0);
        marks.put(name,value);story.put("marks",marks);data.put(LastElevator.ID,story);
    }
}

package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Physical, block-moving car in the supplied Tokyo tower, not a scene teleport. */
public final class TowerLift {
    private TowerLift(){}
    public static final int X0=-235,X1=-232,Z0=99,Z1=102,MIN=74,MAX=114;
    public static final int[] LANDINGS={74,79,84,89,94,99,104,109,114};

    private static void put(ServerLevel w,int x,int y,int z,Block b){w.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
    private static void fill(ServerLevel w,int x0,int y0,int z0,int x1,int y1,int z1,Block b){
        for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)put(w,x,y,z,b);
    }
    public static boolean inside(ServerPlayer p,int floor){
        return p.getX()>X0+.05&&p.getX()<X1+.95&&p.getZ()>Z0+.05&&p.getZ()<Z1+.95
                &&p.getY()>=floor+.9&&p.getY()<floor+3.8;
    }
    public static void install(ServerLevel w){
        // Reuse the original hotel's repeating corridor. Only a 6x6 interior shaft
        // and the new ground-floor lounge are changed; the exterior is retained.
        fill(w,-236,74,98,-231,119,103,Blocks.AIR);
        for(int y=74;y<=119;y++){
            for(int z=98;z<=103;z++){
                put(w,-236,y,z,Blocks.POLISHED_DEEPSLATE);
                put(w,-231,y,z,Blocks.POLISHED_DEEPSLATE);
            }
            for(int x=-235;x<=-232;x++){
                put(w,x,y,98,Blocks.POLISHED_DEEPSLATE);
                put(w,x,y,103,Blocks.POLISHED_DEEPSLATE);
            }
            if(y%5==4){put(w,-231,y,100,Blocks.OCHRE_FROGLIGHT);put(w,-231,y,101,Blocks.OCHRE_FROGLIGHT);}
        }
        // Framed thresholds at every real floor, so the player can see the cabin
        // arrive through doors rather than watch the world switch around them.
        for(int floor:LANDINGS){
            fill(w,-239,floor,99,-237,floor,102,Blocks.POLISHED_BLACKSTONE);
            fill(w,-237,floor+1,99,-237,floor+3,99,Blocks.CUT_COPPER);
            fill(w,-237,floor+1,102,-237,floor+3,102,Blocks.CUT_COPPER);
            fill(w,-237,floor+4,99,-237,floor+4,102,Blocks.OCHRE_FROGLIGHT);
        }
        lounge(w);
        stairs(w);
        escape(w);
        car(w,MIN,false);
        door(w,MIN,false);
    }
    private static void lounge(ServerLevel w){
        // Two-height ground lobby in the heart of the existing high-rise.
        fill(w,-269,75,104,-241,83,113,Blocks.AIR);
        for(int x=-269;x<=-241;x++)for(int z=104;z<=113;z++){
            put(w,x,74,z,(x+z)%8==0?Blocks.CHISELED_QUARTZ_BLOCK:
                    (z==108||z==109?Blocks.POLISHED_DEEPSLATE:Blocks.POLISHED_DIORITE));
        }
        for(int x:new int[]{-268,-257,-245})for(int z:new int[]{105,112}){
            fill(w,x,75,z,x+1,82,z+1,Blocks.SMOOTH_QUARTZ);
            fill(w,x,75,z,x+1,75,z+1,Blocks.POLISHED_BLACKSTONE);
            fill(w,x,82,z,x+1,82,z+1,Blocks.CUT_COPPER);
        }
        for(int x:new int[]{-264,-253,-244}){
            fill(w,x,82,108,x,83,108,Blocks.CHAIN);
            fill(w,x-1,81,107,x+1,81,109,Blocks.CUT_COPPER);
            put(w,x,81,108,Blocks.OCHRE_FROGLIGHT);
        }
        // Reception, dark wood seating and a direct arch into the lift corridor.
        fill(w,-264,75,110,-258,76,111,Blocks.DARK_OAK_PLANKS);
        fill(w,-264,77,111,-258,77,111,Blocks.OCHRE_FROGLIGHT);
        for(int x:new int[]{-252,-247}){
            fill(w,x,75,110,x+2,75,110,Blocks.DARK_OAK_STAIRS);
            fill(w,x+1,75,107,x+1,75,108,Blocks.BROWN_CARPET);
        }
        fill(w,-254,75,103,-251,78,104,Blocks.AIR);
        fill(w,-254,79,103,-251,79,104,Blocks.CUT_COPPER);
        // Lobby direction markers are physical lights rather than chat spam.
        for(int x=-249;x<=-240;x+=3)put(w,x,78,100,Blocks.OCHRE_FROGLIGHT);
    }
    private static int[][] ring(int minX,int maxX,int minZ,int maxZ){
        int[][] r=new int[16][2];int i=0;
        for(int x=minX;x<=maxX;x++)r[i++]=new int[]{x,maxZ};
        for(int z=maxZ-1;z>=minZ;z--)r[i++]=new int[]{maxX,z};
        for(int x=maxX-1;x>=minX;x--)r[i++]=new int[]{x,minZ};
        for(int z=minZ+1;z<maxZ;z++)r[i++]=new int[]{minX,z};
        return r;
    }
    private static void stairs(ServerLevel w){
        // Six distinct flights climb from floor 6 to floor 0. Identical lit
        // landings make the ascent appear to repeat without teleporting.
        fill(w,-226,84,98,-220,119,104,Blocks.POLISHED_DEEPSLATE);
        fill(w,-225,84,99,-221,119,103,Blocks.AIR);
        fill(w,-224,84,100,-222,119,102,Blocks.TINTED_GLASS);
        int[][] path=ring(-225,-221,99,103);
        for(int loop=0;loop<6;loop++)for(int i=0;i<16;i++){
            int y=84+loop*5+(i*5)/16;
            put(w,path[i][0],y,path[i][1],Blocks.POLISHED_BLACKSTONE);
        }
        // Walkway from the office corridor around the lift shaft.
        fill(w,-239,84,105,-225,84,105,Blocks.POLISHED_BLACKSTONE);
        fill(w,-239,85,105,-225,87,105,Blocks.AIR);
        fill(w,-239,85,102,-239,87,105,Blocks.AIR);
        fill(w,-225,85,104,-225,87,104,Blocks.AIR);
        // The last flight rejoins the real top-floor corridor.
        fill(w,-239,114,105,-225,114,105,Blocks.SMOOTH_QUARTZ);
        fill(w,-239,115,105,-225,117,105,Blocks.AIR);
        fill(w,-239,115,102,-239,117,105,Blocks.AIR);
        fill(w,-225,115,104,-225,117,104,Blocks.AIR);
        for(int y:new int[]{89,99,104,109}){
            put(w,-225,y+2,104,Blocks.OCHRE_FROGLIGHT);
            put(w,-224,y+2,104,Blocks.OAK_SIGN);
        }
    }
    private static void escape(ServerLevel w){
        // Independent fire staircase descends eight real floors to street level.
        fill(w,-280,74,98,-274,119,104,Blocks.POLISHED_DEEPSLATE);
        fill(w,-279,74,99,-275,119,103,Blocks.AIR);
        fill(w,-278,74,100,-276,119,102,Blocks.TINTED_GLASS);
        int[][] path=ring(-279,-275,99,103);
        for(int loop=0;loop<8;loop++)for(int i=0;i<16;i++){
            int y=74+loop*5+(i*5)/16;
            put(w,path[i][0],y,path[i][1],Blocks.POLISHED_BLACKSTONE);
        }
        // Top entrance opens after the emergency seal is broken.
        fill(w,-274,114,100,-272,114,101,Blocks.SMOOTH_QUARTZ);
        fill(w,-274,115,100,-272,117,101,Blocks.AIR);
        // At ground level a short outdoor descent reaches existing street height.
        fill(w,-282,74,100,-280,74,101,Blocks.POLISHED_BLACKSTONE);
        fill(w,-282,75,100,-280,77,101,Blocks.AIR);
        for(int x=-283;x>=-291;x--){
            int floor=74-(Math.abs(x+282));
            fill(w,x,floor,100,x,floor,101,Blocks.POLISHED_BLACKSTONE);
            fill(w,x,floor+1,100,x,floor+3,101,Blocks.AIR);
        }
        fill(w,-294,65,99,-291,65,102,Blocks.SMOOTH_STONE);
    }
    private static void car(ServerLevel w,int floor,boolean closed){
        for(int x=X0;x<=X1;x++)for(int z=Z0;z<=Z1;z++){
            put(w,x,floor,z,(x+z)%4==0?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.POLISHED_BLACKSTONE);
            put(w,x,floor+4,z,Blocks.CUT_COPPER);
        }
        for(int y=floor+1;y<=floor+3;y++)for(int z=Z0;z<=Z1;z++){
            put(w,X1,y,z,z==100||z==101?Blocks.TINTED_GLASS:Blocks.CUT_COPPER);
            put(w,X0,y,z,(z==100||z==101)&&!closed?Blocks.AIR:Blocks.CUT_COPPER);
        }
        for(int y=floor+1;y<=floor+3;y++)for(int x=X0+1;x<X1;x++){
            put(w,x,y,Z0,Blocks.CUT_COPPER);
            put(w,x,y,Z1,Blocks.CUT_COPPER);
        }
        put(w,-234,floor+4,100,Blocks.SEA_LANTERN);
        put(w,-233,floor+4,101,Blocks.SEA_LANTERN);
        // Button is mounted on its own floor tile and moves with the car.
        put(w,-233,floor,100,Blocks.IRON_BLOCK);
        put(w,-233,floor+1,100,Blocks.STONE_BUTTON);
    }
    private static void door(ServerLevel w,int floor,boolean closed){
        for(int y=floor+1;y<=floor+3;y++)for(int z=100;z<=101;z++){
            put(w,-236,y,z,closed?Blocks.IRON_BLOCK:Blocks.AIR);
            put(w,X0,y,z,closed?Blocks.IRON_BLOCK:Blocks.AIR);
        }
    }
    public static void begin(ServerPlayer p,CompoundTag d,int target,int targetScene){
        int floor=d.getInt("carFloor");
        if(!inside(p,floor))return;
        door((ServerLevel)p.level(),floor,true);
        d.putBoolean("liftMoving",true);d.putInt("liftTicks",0);
        d.putInt("liftTarget",target);d.putInt("liftScene",targetScene);
        p.setDeltaMovement(0,0,0);
    }
    /** Returns the arrived story scene, or -1 while the cabin is moving. */
    public static int tick(ServerPlayer p,CompoundTag d){
        if(!d.getBoolean("liftMoving"))return -1;
        ServerLevel w=(ServerLevel)p.level();int ticks=d.getInt("liftTicks")+1;
        d.putInt("liftTicks",ticks);
        if(ticks<=20||ticks%5!=0)return -1;
        int floor=d.getInt("carFloor"),target=d.getInt("liftTarget");
        if(floor!=target){
            // Adjacent cabin volumes overlap; clear the old one before drawing the new.
            fill(w,X0,floor,Z0,X1,floor+4,Z1,Blocks.AIR);
            int next=floor+(target>floor?1:-1);
            car(w,next,true);
            p.teleportTo(w,p.getX(),p.getY()+(next-floor),p.getZ(),p.getYRot(),p.getXRot());
            p.setDeltaMovement(0,0,0);p.fallDistance=0;
            d.putInt("carFloor",next);
            return -1;
        }
        if(ticks<45+Math.abs(target-MIN)*5)return -1;
        door(w,target,false);
        d.putBoolean("liftMoving",false);
        return d.getInt("liftScene");
    }
    public static void reset(ServerPlayer p,CompoundTag d){
        ServerLevel w=(ServerLevel)p.level();int floor=d.getInt("carFloor");
        fill(w,X0,floor,Z0,X1,floor+4,Z1,Blocks.AIR);
        car(w,MIN,false);door(w,MIN,false);
        d.putInt("carFloor",MIN);d.putBoolean("liftMoving",false);
    }
    public static void position(ServerLevel w,CompoundTag d,int target){
        int old=d.getInt("carFloor");
        fill(w,X0,old,Z0,X1,old+4,Z1,Blocks.AIR);
        door(w,old,true);
        car(w,target,false);door(w,target,false);
        d.putInt("carFloor",target);d.putBoolean("liftMoving",false);
    }
}

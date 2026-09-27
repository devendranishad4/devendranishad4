package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/** Map-aware scene setup for the real Tokyo Inspired City subway. */
public final class StationBuilder {
    private StationBuilder() {}
    public static final BlockPos TOKYO_ANCHOR = new BlockPos(-92, 61, 213);
    private static final String CAMERA_TAG = "train31_camera";
    private static final String TRAIN_TAG = "train31_train";
    private static final String DOOR_TAG = "train31_door";
    private static final Map<UUID, List<UUID>> CAMERAS = new HashMap<>();

    public record RailGeometry(BlockPos rail, boolean axisZ) {}

    public static void prepare(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        cleanupCameras(level, player);
        removeTrain(level, TOKYO_ANCHOR);

        RailGeometry rail = findRail(level, TOKYO_ANCHOR);
        BlockPos outside = findSurfaceStart(level, TOKYO_ANCHOR);
        BlockPos room = findSecurityRoom(level, rail.rail());

        player.getPersistentData().putLong("train31_rail", rail.rail().asLong());
        player.getPersistentData().putBoolean("train31_axis_z", rail.axisZ());
        player.getPersistentData().putLong("train31_outside", outside.asLong());
        player.getPersistentData().putLong("train31_cctv", room.asLong());
        player.getPersistentData().putBoolean("train31_prepared", true);

        buildMonitorBank(level, room);
        spawnCameras(level, player, rail);
        teleportOutside(player);
    }

    public static void ensurePrepared(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean("train31_prepared") || !CAMERAS.containsKey(player.getUUID())) prepare(player);
    }

    public static void teleportOutside(ServerPlayer player) {
        BlockPos p = BlockPos.of(player.getPersistentData().getLong("train31_outside"));
        if (p.equals(BlockPos.ZERO)) p = findSurfaceStart(player.serverLevel(), TOKYO_ANCHOR);
        player.teleportTo(player.serverLevel(), p.getX()+0.5, p.getY()+0.1, p.getZ()+0.5, 0f, 8f);
    }

    public static RailGeometry geometry(ServerPlayer player) {
        long packed = player.getPersistentData().getLong("train31_rail");
        RailGeometry detected = packed == 0 ? findRail(player.serverLevel(), TOKYO_ANCHOR) : null;
        BlockPos rail = packed == 0 ? detected.rail() : BlockPos.of(packed);
        boolean axisZ = packed == 0 ? detected.axisZ() : player.getPersistentData().getBoolean("train31_axis_z");
        return new RailGeometry(rail, axisZ);
    }

    public static BlockPos cctvRoom(ServerPlayer player) {
        long packed = player.getPersistentData().getLong("train31_cctv");
        return packed == 0 ? TOKYO_ANCHOR.offset(8,1,0) : BlockPos.of(packed);
    }

    public static boolean isMonitorClick(ServerPlayer player, BlockPos clicked) {
        if (!player.getPersistentData().getBoolean("train31_prepared")) return false;
        BlockPos c = cctvRoom(player);
        return Math.abs(clicked.getX()-c.getX()) <= 5 && Math.abs(clicked.getY()-c.getY()) <= 4 && Math.abs(clicked.getZ()-c.getZ()) <= 5;
    }

    public static void cycleCamera(ServerPlayer player) {
        ensurePrepared(player);
        int index = player.getPersistentData().getInt("train31_cam_index");
        index = (index + 1) % 4;
        player.getPersistentData().putInt("train31_cam_index", index);
        enterCamera(player, index);
    }

    public static void enterCamera(ServerPlayer player, int index) {
        ensurePrepared(player);
        List<UUID> list = CAMERAS.get(player.getUUID());
        if (list == null || list.isEmpty()) return;
        index = Math.floorMod(index, list.size());
        Entity e = player.serverLevel().getEntity(list.get(index));
        if (e == null) return;
        int storyTick = StoryDirector.currentTick(player);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0,storyTick), StoryDirector.currentFog(player), e.getId(), true, StoryDirector.isRunning(player)));
    }

    public static void exitCamera(ServerPlayer player) {
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0,StoryDirector.currentTick(player)), StoryDirector.currentFog(player), -1, false, StoryDirector.isRunning(player)));
    }

    private static void spawnCameras(ServerLevel level, ServerPlayer player, RailGeometry g) {
        List<UUID> ids = new ArrayList<>();
        BlockPos r = g.rail();
        if (g.axisZ()) {
            ids.add(spawnCamera(level, r.offset(-5,3,-18), 0f, 8f));
            ids.add(spawnCamera(level, r.offset(5,3,12), 180f, 10f));
            ids.add(spawnCamera(level, r.offset(-6,3,28), 180f, 8f));
            ids.add(spawnCamera(level, r.offset(0,3,48), 180f, 5f));
        } else {
            ids.add(spawnCamera(level, r.offset(-18,3,-5), -90f, 8f));
            ids.add(spawnCamera(level, r.offset(12,3,5), 90f, 10f));
            ids.add(spawnCamera(level, r.offset(28,3,-6), 90f, 8f));
            ids.add(spawnCamera(level, r.offset(48,3,0), 90f, 5f));
        }
        CAMERAS.put(player.getUUID(), ids);
    }

    private static UUID spawnCamera(ServerLevel level, BlockPos p, float yaw, float pitch) {
        ArmorStand a = EntityType.ARMOR_STAND.create(level);
        if (a == null) return UUID.randomUUID();
        a.setInvisible(true);
        a.setNoGravity(true);
        a.setInvulnerable(true);
        a.setSilent(true);
        a.setPos(p.getX()+0.5,p.getY()+0.2,p.getZ()+0.5);
        a.setYRot(yaw); a.setXRot(pitch); a.setYHeadRot(yaw);
        a.addTag(CAMERA_TAG);
        level.addFreshEntity(a);
        return a.getUUID();
    }

    private static void cleanupCameras(ServerLevel level, ServerPlayer player) {
        List<UUID> ids = CAMERAS.remove(player.getUUID());
        if (ids != null) for (UUID id : ids) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
        AABB box = new AABB(TOKYO_ANCHOR).inflate(90);
        for (ArmorStand a : level.getEntitiesOfClass(ArmorStand.class, box, e -> e.getTags().contains(CAMERA_TAG))) a.discard();
    }

    private static RailGeometry findRail(ServerLevel level, BlockPos anchor) {
        BlockPos best = null; double bestD = Double.MAX_VALUE;
        for (int x=-30;x<=30;x++) for (int z=-30;z<=30;z++) for (int y=-5;y<=5;y++) {
            BlockPos p=anchor.offset(x,y,z);
            if (!level.getBlockState(p).is(BlockTags.RAILS)) continue;
            double d=p.distSqr(anchor);
            if(d<bestD){bestD=d;best=p;}
        }
        if(best==null) return new RailGeometry(anchor.offset(5,-1,0), true);
        int zNeighbors=(isRail(level,best.north())?1:0)+(isRail(level,best.south())?1:0);
        int xNeighbors=(isRail(level,best.east())?1:0)+(isRail(level,best.west())?1:0);
        return new RailGeometry(best,zNeighbors>=xNeighbors);
    }

    private static boolean isRail(ServerLevel l, BlockPos p){return l.getBlockState(p).is(BlockTags.RAILS);}

    private static BlockPos findSurfaceStart(ServerLevel level, BlockPos anchor) {
        BlockPos best=null; double bestScore=-1e9;
        for(int x=-48;x<=48;x+=2) for(int z=-48;z<=48;z+=2){
            int wx=anchor.getX()+x, wz=anchor.getZ()+z;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,wx,wz);
            BlockPos p=new BlockPos(wx,y,wz);
            if(y<anchor.getY()+5 || !level.canSeeSky(p)) continue;
            int stairs=nearbyStairs(level,p);
            BlockState ground=level.getBlockState(p.below());
            double pathBonus=(ground.is(Blocks.STONE)||ground.is(Blocks.SMOOTH_STONE)||ground.is(Blocks.GRAY_CONCRETE)||ground.is(Blocks.LIGHT_GRAY_CONCRETE))?4:0;
            double score=stairs*14.0+pathBonus-Math.sqrt(x*x+z*z)*0.12;
            if(score>bestScore){bestScore=score;best=p;}
        }
        if(best!=null) return best;
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,anchor.getX(),anchor.getZ());
        return new BlockPos(anchor.getX(),y,anchor.getZ());
    }

    private static int nearbyStairs(ServerLevel level,BlockPos p){
        int n=0;
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) for(int y=-2;y<=2;y++)
            if(level.getBlockState(p.offset(x,y,z)).getBlock() instanceof StairBlock)n++;
        return n;
    }

    private static BlockPos findSecurityRoom(ServerLevel level, BlockPos rail) {
        BlockPos best=null; int bestScore=-1;
        for(int x=-34;x<=34;x+=2) for(int z=-34;z<=34;z+=2) for(int y=0;y<=9;y++){
            BlockPos p=TOKYO_ANCHOR.offset(x,y,z);
            if(!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()||level.getBlockState(p.below()).isAir())continue;
            int east=wallDistance(level,p,1,0), west=wallDistance(level,p,-1,0), south=wallDistance(level,p,0,1), north=wallDistance(level,p,0,-1);
            if(east<0||west<0||south<0||north<0)continue;
            int width=east+west, depth=north+south;
            if(width<4||depth<4||width>13||depth>13)continue;
            int ceiling=ceilingDistance(level,p);
            if(ceiling<3||ceiling>6)continue;
            double rd=Math.sqrt(p.distSqr(rail));
            int score=100-(int)Math.abs(width-8)*3-(int)Math.abs(depth-7)*3-(int)Math.abs(rd-18);
            if(score>bestScore){bestScore=score;best=p;}
        }
        return best!=null?best:TOKYO_ANCHOR.offset(10,1,8);
    }

    private static int wallDistance(ServerLevel l,BlockPos p,int dx,int dz){
        for(int i=2;i<=7;i++) if(!l.getBlockState(p.offset(dx*i,1,dz*i)).isAir()) return i;
        return -1;
    }
    private static int ceilingDistance(ServerLevel l,BlockPos p){
        for(int i=2;i<=7;i++) if(!l.getBlockState(p.above(i)).isAir())return i;
        return -1;
    }

    private static void buildMonitorBank(ServerLevel level, BlockPos c) {
        // Compact four-monitor console placed inside an existing room; no fake off-map room.
        for(int x=-4;x<=4;x++) set(level,c.offset(x,0,2),Blocks.DARK_OAK_SLAB.defaultBlockState());
        for(int x=-4;x<=4;x++) for(int y=1;y<=3;y++) set(level,c.offset(x,y,3),Blocks.BLACK_CONCRETE.defaultBlockState());
        for(int m=0;m<4;m++){
            int x=-3+m*2;
            set(level,c.offset(x,2,2),Blocks.TINTED_GLASS.defaultBlockState());
            set(level,c.offset(x,3,2),Blocks.GRAY_STAINED_GLASS.defaultBlockState());
            set(level,c.offset(x,1,2),Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        }
        set(level,c.offset(-4,4,3),Blocks.REDSTONE_LAMP.defaultBlockState());
        set(level,c.offset(4,4,3),Blocks.REDSTONE_LAMP.defaultBlockState());
    }

    public static void spawnTrain(ServerLevel level, RailGeometry g, double offset) {
        removeTrain(level,g.rail());
        double yaw=g.axisZ()?0.0:90.0;
        for(int coach=0;coach<3;coach++){
            double along=offset+coach*8.6;
            double x=g.rail().getX()+0.5+(g.axisZ()?0:along);
            double z=g.rail().getZ()+0.5+(g.axisZ()?along:0);
            double y=g.rail().getY()+1.65;
            display(level,x,y,z,Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),3.65f,2.85f,8.0f,yaw,TRAIN_TAG);
            display(level,x,y+1.48,z,Blocks.SMOOTH_STONE.defaultBlockState(),3.82f,0.18f,8.12f,yaw,TRAIN_TAG);
            display(level,x,y+0.45,z,Blocks.BLACK_STAINED_GLASS.defaultBlockState(),3.76f,1.12f,6.2f,yaw,TRAIN_TAG);
            display(level,x,y-0.55,z,Blocks.RED_CONCRETE.defaultBlockState(),3.78f,0.22f,7.9f,yaw,TRAIN_TAG);
            display(level,x,y+1.05,z,Blocks.SEA_LANTERN.defaultBlockState(),2.7f,0.06f,6.4f,yaw,TRAIN_TAG);
            display(level,x,y,z,Blocks.IRON_BLOCK.defaultBlockState(),3.86f,2.42f,1.35f,yaw,TRAIN_TAG,DOOR_TAG);
        }
        double fx=g.rail().getX()+0.5+(g.axisZ()?0:offset-4.15);
        double fz=g.rail().getZ()+0.5+(g.axisZ()?offset-4.15:0);
        double fy=g.rail().getY()+1.65;
        display(level,fx,fy,fz,Blocks.DEEPSLATE_TILES.defaultBlockState(),3.66f,2.86f,0.24f,yaw,TRAIN_TAG);
        display(level,fx,fy-0.35,fz,Blocks.SEA_LANTERN.defaultBlockState(),2.25f,0.4f,0.28f,yaw,TRAIN_TAG);
    }

    public static void setTrainOffset(ServerLevel level, RailGeometry g, double oldOffset, double newOffset) {
        double delta=newOffset-oldOffset;
        AABB box=new AABB(g.rail()).inflate(110);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(TRAIN_TAG))){
            if(g.axisZ())d.teleportTo(d.getX(),d.getY(),d.getZ()+delta); else d.teleportTo(d.getX()+delta,d.getY(),d.getZ());
        }
    }

    public static void openTrainDoors(ServerLevel level, RailGeometry g) {
        AABB box=new AABB(g.rail()).inflate(100);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(DOOR_TAG)))d.discard();
    }

    public static void removeTrain(ServerLevel level, BlockPos around) {
        AABB box=new AABB(around).inflate(120);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(TRAIN_TAG)))d.discard();
    }

    private static void display(ServerLevel level,double x,double y,double z,BlockState state,float sx,float sy,float sz,double yawDeg,String...tags){
        Display.BlockDisplay d=EntityType.BLOCK_DISPLAY.create(level); if(d==null)return;
        double rad=Math.toRadians(yawDeg); float qy=(float)Math.sin(rad/2.0), qw=(float)Math.cos(rad/2.0);
        CompoundTag nbt=new CompoundTag(); nbt.put("block_state",NbtUtils.writeBlockState(state));
        CompoundTag tr=new CompoundTag(); tr.put("translation",floats(-sx/2,-sy/2,-sz/2)); tr.put("scale",floats(sx,sy,sz));
        tr.put("left_rotation",floats(0,qy,0,qw)); tr.put("right_rotation",floats(0,0,0,1)); nbt.put("transformation",tr);
        nbt.putFloat("view_range",2.5f); nbt.putFloat("shadow_radius",0f); d.load(nbt); d.setPos(x,y,z);
        for(String t:tags)d.addTag(t); level.addFreshEntity(d);
    }
    private static ListTag floats(float...v){ListTag l=new ListTag();for(float f:v)l.add(FloatTag.valueOf(f));return l;}
    private static void set(ServerLevel l,BlockPos p,BlockState s){l.setBlock(p,s,3);}
}

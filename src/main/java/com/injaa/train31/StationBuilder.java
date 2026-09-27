package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * Exact-map scene setup for the user's Tokyo Inspired City station.
 * The creator marks START, CCTV and PLATFORM once; this class never guesses a roof/room/platform after that.
 */
public final class StationBuilder {
    private StationBuilder() {}

    private static final String CAMERA_TAG = "train31_camera";
    private static final String CAMERA_PROP_TAG = "train31_camera_prop";
    private static final String TRAIN_TAG = "train31_train";
    private static final String DOOR_TAG = "train31_door";
    private static final Map<UUID, List<UUID>> CAMERAS = new HashMap<>();

    /** platformSide is -1/+1 on the axis perpendicular to the rail, pointing toward the marked player platform. */
    public record RailGeometry(BlockPos rail, boolean axisZ, int platformSide) {}

    public static void prepare(ServerPlayer player) {
        if (!SceneSetup.complete(player)) {
            SceneSetup.status(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        cleanupCameras(level, player);

        BlockPos platform = SceneSetup.platform(player);
        RailGeometry rail = findRail(level, platform);
        removeTrain(level, rail.rail());

        BlockPos outside = SceneSetup.start(player);
        BlockPos room = SceneSetup.cctv(player);

        player.getPersistentData().putLong("train31_rail", rail.rail().asLong());
        player.getPersistentData().putBoolean("train31_axis_z", rail.axisZ());
        player.getPersistentData().putInt("train31_platform_side", rail.platformSide());
        player.getPersistentData().putLong("train31_outside", outside.asLong());
        player.getPersistentData().putLong("train31_cctv", room.asLong());
        player.getPersistentData().putBoolean("train31_prepared", true);

        buildMonitorBank(level, room, SceneSetup.cctvFacing(player));
        spawnCameras(level, player, rail);
        teleportOutside(player);
    }

    public static void ensurePrepared(ServerPlayer player) {
        if (!SceneSetup.complete(player)) return;
        if (!player.getPersistentData().getBoolean("train31_prepared") || !CAMERAS.containsKey(player.getUUID())) prepare(player);
    }

    public static void teleportOutside(ServerPlayer player) {
        if (!SceneSetup.complete(player)) return;
        BlockPos p = SceneSetup.start(player);
        player.teleportTo(player.serverLevel(), p.getX()+0.5, p.getY()+0.05, p.getZ()+0.5, SceneSetup.startYaw(player), 5f);
    }

    public static RailGeometry geometry(ServerPlayer player) {
        long packed = player.getPersistentData().getLong("train31_rail");
        if (packed == 0 && SceneSetup.complete(player)) return findRail(player.serverLevel(), SceneSetup.platform(player));
        BlockPos rail = packed == 0 ? player.blockPosition() : BlockPos.of(packed);
        boolean axisZ = player.getPersistentData().getBoolean("train31_axis_z");
        int side = player.getPersistentData().getInt("train31_platform_side");
        if (side == 0) side = -1;
        return new RailGeometry(rail, axisZ, side);
    }

    public static BlockPos cctvRoom(ServerPlayer player) {
        if (SceneSetup.complete(player)) return SceneSetup.cctv(player);
        long packed = player.getPersistentData().getLong("train31_cctv");
        return packed == 0 ? player.blockPosition() : BlockPos.of(packed);
    }

    public static boolean isMonitorClick(ServerPlayer player, BlockPos clicked) {
        if (!player.getPersistentData().getBoolean("train31_prepared")) return false;
        BlockPos c = cctvRoom(player);
        return Math.abs(clicked.getX()-c.getX()) <= 6 && Math.abs(clicked.getY()-c.getY()) <= 5 && Math.abs(clicked.getZ()-c.getZ()) <= 6;
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
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0, StoryDirector.currentTick(player)), StoryDirector.currentFog(player), e.getId(), true, StoryDirector.isRunning(player)));
    }

    public static void exitCamera(ServerPlayer player) {
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0, StoryDirector.currentTick(player)), StoryDirector.currentFog(player), -1, false, StoryDirector.isRunning(player)));
    }

    private static void spawnCameras(ServerLevel level, ServerPlayer player, RailGeometry g) {
        List<UUID> ids = new ArrayList<>();
        BlockPos r = g.rail();
        int s = g.platformSide();

        if (g.axisZ()) {
            ids.add(spawnCamera(level, r.offset(s*5,3,-18), 0f, 8f));
            ids.add(spawnCamera(level, r.offset(s*5,3,12), 180f, 9f));
            ids.add(spawnCamera(level, r.offset(-s*5,3,28), 180f, 8f));
            ids.add(spawnCamera(level, r.offset(0,3,48), 180f, 4f));
        } else {
            ids.add(spawnCamera(level, r.offset(-18,3,s*5), -90f, 8f));
            ids.add(spawnCamera(level, r.offset(12,3,s*5), 90f, 9f));
            ids.add(spawnCamera(level, r.offset(28,3,-s*5), 90f, 8f));
            ids.add(spawnCamera(level, r.offset(48,3,0), 90f, 4f));
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
        a.setPos(p.getX()+0.5, p.getY()+0.2, p.getZ()+0.5);
        a.setYRot(yaw); a.setXRot(pitch); a.setYHeadRot(yaw);
        a.addTag(CAMERA_TAG);
        level.addFreshEntity(a);

        // Small physical camera housing so the cameras really exist in the station.
        display(level, p.getX()+0.5, p.getY()+0.15, p.getZ()+0.5,
                Blocks.BLACK_CONCRETE.defaultBlockState(), 0.48f,0.34f,0.70f,yaw,CAMERA_PROP_TAG);
        display(level, p.getX()+0.5, p.getY()+0.15, p.getZ()+0.5,
                Blocks.OBSERVER.defaultBlockState(), 0.20f,0.20f,0.22f,yaw,CAMERA_PROP_TAG);
        return a.getUUID();
    }

    private static void cleanupCameras(ServerLevel level, ServerPlayer player) {
        List<UUID> ids = CAMERAS.remove(player.getUUID());
        if (ids != null) for (UUID id : ids) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
        BlockPos center = SceneSetup.complete(player) ? SceneSetup.platform(player) : player.blockPosition();
        AABB box = new AABB(center).inflate(140);
        for (ArmorStand a : level.getEntitiesOfClass(ArmorStand.class, box, e -> e.getTags().contains(CAMERA_TAG))) a.discard();
        for (Display.BlockDisplay d : level.getEntitiesOfClass(Display.BlockDisplay.class, box, e -> e.getTags().contains(CAMERA_PROP_TAG))) d.discard();
    }

    private static RailGeometry findRail(ServerLevel level, BlockPos platform) {
        BlockPos best = null; double bestD = Double.MAX_VALUE;
        for (int x=-16;x<=16;x++) for (int z=-16;z<=16;z++) for (int y=-4;y<=4;y++) {
            BlockPos p = platform.offset(x,y,z);
            if (!level.getBlockState(p).is(BlockTags.RAILS)) continue;
            double d = p.distSqr(platform);
            if (d < bestD) { bestD=d; best=p; }
        }
        if (best == null) best = platform.offset(0,-1,0);

        int zNeighbors=(isRail(level,best.north())?1:0)+(isRail(level,best.south())?1:0);
        int xNeighbors=(isRail(level,best.east())?1:0)+(isRail(level,best.west())?1:0);
        boolean axisZ = zNeighbors >= xNeighbors;
        int side = axisZ ? Integer.compare(platform.getX(), best.getX()) : Integer.compare(platform.getZ(), best.getZ());
        if (side == 0) side = -1;
        return new RailGeometry(best, axisZ, side);
    }

    private static boolean isRail(ServerLevel l, BlockPos p) { return l.getBlockState(p).is(BlockTags.RAILS); }

    private static BlockPos local(BlockPos origin, Direction forward, int right, int up, int ahead) {
        Direction r = forward.getClockWise();
        return origin.offset(r.getStepX()*right + forward.getStepX()*ahead, up,
                r.getStepZ()*right + forward.getStepZ()*ahead);
    }

    private static void buildMonitorBank(ServerLevel level, BlockPos c, Direction facing) {
        // Existing room stays in place. We add only a compact desk + four screens in the direction the creator faced.
        for (int x=-4;x<=4;x++) set(level, local(c,facing,x,0,1), Blocks.DARK_OAK_SLAB.defaultBlockState());
        for (int x=-4;x<=4;x++) for (int y=1;y<=3;y++) set(level, local(c,facing,x,y,3), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int m=0;m<4;m++) {
            int x=-3+m*2;
            set(level, local(c,facing,x,2,2), Blocks.TINTED_GLASS.defaultBlockState());
            set(level, local(c,facing,x,3,2), Blocks.GRAY_STAINED_GLASS.defaultBlockState());
            set(level, local(c,facing,x,1,2), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        }
        set(level, local(c,facing,-4,4,3), Blocks.REDSTONE_LAMP.defaultBlockState());
        set(level, local(c,facing,4,4,3), Blocks.REDSTONE_LAMP.defaultBlockState());
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
        AABB box=new AABB(g.rail()).inflate(120);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(TRAIN_TAG))){
            if(g.axisZ()) d.teleportTo(d.getX(),d.getY(),d.getZ()+delta);
            else d.teleportTo(d.getX()+delta,d.getY(),d.getZ());
        }
    }

    public static void openTrainDoors(ServerLevel level, RailGeometry g) {
        AABB box=new AABB(g.rail()).inflate(110);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(DOOR_TAG))) d.discard();
    }

    public static void removeTrain(ServerLevel level, BlockPos around) {
        AABB box=new AABB(around).inflate(140);
        for(Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(TRAIN_TAG))) d.discard();
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

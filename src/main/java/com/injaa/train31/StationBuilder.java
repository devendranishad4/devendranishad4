package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/** Exact-map scene setup. Every important point is creator-marked; no map guessing. */
public final class StationBuilder {
    private StationBuilder() {}

    private static final String CAMERA_TAG = "train31_camera";
    private static final String CAMERA_PROP_TAG = "train31_camera_prop";
    private static final String TRAIN_TAG = "train31_train_entity";
    private static final String CCTV_ACTIVE = "train31_cctv_active";
    private static final String CCTV_CURRENT = "train31_cctv_current";
    private static final Map<UUID, List<UUID>> CAMERAS = new HashMap<>();
    private static final Map<UUID, UUID> TRAINS = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> MAP_TRAIN_LIGHTS = new HashMap<>();

    public record RailGeometry(BlockPos rail, boolean axisZ, int platformSide, int tunnelSign) {}

    public static void prepare(ServerPlayer player) {
        if (!SceneSetup.complete(player)) {
            SceneSetup.status(player);
            return;
        }
        if (!SceneSetup.valid(player)) return;

        ServerLevel level = player.serverLevel();
        cleanupCameras(level, player);
        removeTrain(player);
        setMapTrainLights(player,false);
        exitCamera(player);

        RailGeometry rail = geometryFromMarkers(player);
        player.getPersistentData().putLong("train31_rail", rail.rail().asLong());
        player.getPersistentData().putBoolean("train31_axis_z", rail.axisZ());
        player.getPersistentData().putInt("train31_platform_side", rail.platformSide());
        player.getPersistentData().putInt("train31_tunnel_sign", rail.tunnelSign());
        player.getPersistentData().putLong("train31_outside", SceneSetup.start(player).asLong());
        player.getPersistentData().putLong("train31_cctv", SceneSetup.cctv(player).asLong());
        player.getPersistentData().putBoolean("train31_prepared", true);

        buildMonitorBank(level, SceneSetup.cctv(player), SceneSetup.cctvFacing(player));
        spawnCameras(level, player);
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

    private static RailGeometry geometryFromMarkers(ServerPlayer player) {
        BlockPos rail = SceneSetup.rail(player);
        BlockPos platform = SceneSetup.platform(player);
        BlockPos tunnel = SceneSetup.tunnel(player);
        int dx = tunnel.getX() - rail.getX();
        int dz = tunnel.getZ() - rail.getZ();
        boolean axisZ = Math.abs(dz) >= Math.abs(dx);
        int side = axisZ ? Integer.compare(platform.getX(), rail.getX()) : Integer.compare(platform.getZ(), rail.getZ());
        if (side == 0) side = -1;
        int tunnelSign = axisZ ? Integer.compare(tunnel.getZ(), rail.getZ()) : Integer.compare(tunnel.getX(), rail.getX());
        if (tunnelSign == 0) tunnelSign = 1;
        return new RailGeometry(rail, axisZ, side, tunnelSign);
    }

    public static RailGeometry geometry(ServerPlayer player) {
        if (SceneSetup.complete(player)) return geometryFromMarkers(player);
        long packed = player.getPersistentData().getLong("train31_rail");
        BlockPos rail = packed == 0 ? player.blockPosition() : BlockPos.of(packed);
        boolean axisZ = player.getPersistentData().getBoolean("train31_axis_z");
        int side = player.getPersistentData().getInt("train31_platform_side");
        int tunnelSign = player.getPersistentData().getInt("train31_tunnel_sign");
        if (side == 0) side = -1;
        if (tunnelSign == 0) tunnelSign = 1;
        return new RailGeometry(rail, axisZ, side, tunnelSign);
    }

    public static BlockPos cctvRoom(ServerPlayer player) {
        return SceneSetup.complete(player) ? SceneSetup.cctv(player) : player.blockPosition();
    }

    public static boolean isMonitorClick(ServerPlayer player, BlockPos clicked) {
        if (!player.getPersistentData().getBoolean("train31_prepared")) return false;
        BlockPos c = cctvRoom(player);
        return Math.abs(clicked.getX()-c.getX()) <= 7 && Math.abs(clicked.getY()-c.getY()) <= 5 && Math.abs(clicked.getZ()-c.getZ()) <= 7;
    }

    public static void cycleCamera(ServerPlayer player) {
        ensurePrepared(player);
        int index = isCameraActive(player) ? currentCameraIndex(player)+1 : 0;
        index = Math.floorMod(index,4);
        enterCamera(player,index);
    }

    public static Entity cameraEntity(ServerPlayer player, int index) {
        List<UUID> list = CAMERAS.get(player.getUUID());
        if (list == null || list.isEmpty()) return null;
        index = Math.floorMod(index, list.size());
        return player.serverLevel().getEntity(list.get(index));
    }

    public static BlockPos cameraBlockPos(ServerPlayer player, int index) {
        Entity e = cameraEntity(player,index);
        return e == null ? SceneSetup.rail(player) : e.blockPosition();
    }

    public static void enterCamera(ServerPlayer player, int index) {
        ensurePrepared(player);
        Entity e = cameraEntity(player,index);
        if (e == null) return;
        int safeIndex=Math.floorMod(index,4);
        player.getPersistentData().putBoolean(CCTV_ACTIVE,true);
        player.getPersistentData().putInt(CCTV_CURRENT,safeIndex);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0, StoryDirector.currentTick(player)), StoryDirector.currentFog(player), e.getId(), true, StoryDirector.isRunning(player)));
    }

    public static void exitCamera(ServerPlayer player) {
        player.getPersistentData().putBoolean(CCTV_ACTIVE,false);
        player.getPersistentData().putInt(CCTV_CURRENT,-1);
        Train31Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Train31Network.ClientState(Math.max(0, StoryDirector.currentTick(player)), StoryDirector.currentFog(player), -1, false, StoryDirector.isRunning(player)));
    }

    public static boolean isCameraActive(ServerPlayer player){
        return player.getPersistentData().getBoolean(CCTV_ACTIVE);
    }

    public static int currentCameraIndex(ServerPlayer player){
        return player.getPersistentData().getInt(CCTV_CURRENT);
    }

    public static int currentCameraEntityId(ServerPlayer player){
        if(!isCameraActive(player))return -1;
        Entity e=cameraEntity(player,currentCameraIndex(player));
        return e==null?-1:e.getId();
    }

    private static void spawnCameras(ServerLevel level, ServerPlayer player) {
        List<UUID> ids = new ArrayList<>();
        for (int i=1;i<=4;i++) {
            BlockPos p = SceneSetup.cameraPos(player,i);
            ids.add(spawnCameraExact(level,p,SceneSetup.cameraYaw(player,i),SceneSetup.cameraPitch(player,i)));
        }
        CAMERAS.put(player.getUUID(), ids);
    }

    private static UUID spawnCameraExact(ServerLevel level, BlockPos p, float yaw, float pitch) {
        ArmorStand a = EntityType.ARMOR_STAND.create(level);
        if (a == null) return UUID.randomUUID();
        double x=p.getX()+0.5, y=p.getY()+1.72, z=p.getZ()+0.5;
        a.setInvisible(true); a.setNoGravity(true); a.setInvulnerable(true); a.setSilent(true);
        a.setPos(x,y,z); a.setYRot(yaw); a.setXRot(pitch); a.setYHeadRot(yaw);
        a.addTag(CAMERA_TAG); level.addFreshEntity(a);
        display(level,x,y-0.08,z,Blocks.BLACK_CONCRETE.defaultBlockState(),0.48f,0.34f,0.70f,yaw,CAMERA_PROP_TAG);
        display(level,x,y-0.08,z,Blocks.OBSERVER.defaultBlockState(),0.20f,0.20f,0.22f,yaw,CAMERA_PROP_TAG);
        return a.getUUID();
    }

    private static void cleanupCameras(ServerLevel level, ServerPlayer player) {
        List<UUID> ids = CAMERAS.remove(player.getUUID());
        if (ids != null) for (UUID id : ids) { Entity e=level.getEntity(id); if(e!=null)e.discard(); }
        BlockPos center=SceneSetup.complete(player)?SceneSetup.platform(player):player.blockPosition();
        AABB box=new AABB(center).inflate(220);
        for (ArmorStand a:level.getEntitiesOfClass(ArmorStand.class,box,e->e.getTags().contains(CAMERA_TAG))) a.discard();
        for (Display.BlockDisplay d:level.getEntitiesOfClass(Display.BlockDisplay.class,box,e->e.getTags().contains(CAMERA_PROP_TAG))) d.discard();
    }

    private static BlockPos local(BlockPos origin, Direction forward, int right, int up, int ahead) {
        Direction r=forward.getClockWise();
        return origin.offset(r.getStepX()*right+forward.getStepX()*ahead,up,r.getStepZ()*right+forward.getStepZ()*ahead);
    }

    private static void buildMonitorBank(ServerLevel level, BlockPos c, Direction facing) {
        for(int x=-4;x<=4;x++) set(level,local(c,facing,x,0,1),Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
        for(int x=-4;x<=4;x++) for(int y=1;y<=4;y++) set(level,local(c,facing,x,y,3),Blocks.BLACK_CONCRETE.defaultBlockState());
        for(int m=0;m<4;m++){
            int x=-3+m*2;
            set(level,local(c,facing,x,2,2),Blocks.TINTED_GLASS.defaultBlockState());
            set(level,local(c,facing,x,3,2),Blocks.BLACK_STAINED_GLASS.defaultBlockState());
            set(level,local(c,facing,x,1,2),Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            set(level,local(c,facing,x,4,2),Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }
        set(level,local(c,facing,-4,4,3),Blocks.REDSTONE_LAMP.defaultBlockState());
        set(level,local(c,facing,4,4,3),Blocks.REDSTONE_LAMP.defaultBlockState());
    }

    /** Adds invisible vanilla light blocks only into air inside the creator-selected physical map train. */
    public static void setMapTrainLights(ServerPlayer player, boolean on){
        ServerLevel level=player.serverLevel();
        if(!on){
            List<BlockPos> old=MAP_TRAIN_LIGHTS.remove(player.getUUID());
            if(old!=null) for(BlockPos p:old) if(level.getBlockState(p).is(Blocks.LIGHT)) level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            return;
        }
        if(MAP_TRAIN_LIGHTS.containsKey(player.getUUID()))return;
        if(!SceneSetup.complete(player))return;

        BlockPos center=SceneSetup.mapTrain(player);
        Direction forward=Direction.fromYRot(SceneSetup.mapTrainYaw(player));
        if(!forward.getAxis().isHorizontal())forward=Direction.NORTH;
        List<BlockPos> placed=new ArrayList<>();

        for(int a=-24;a<=24;a+=3){
            BlockPos base=center.relative(forward,a);
            BlockPos p=base.above(2);
            if(!level.getBlockState(p).isAir())p=base.above(1);
            if(level.getBlockState(p).isAir()){
                level.setBlock(p,Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,12),3);
                placed.add(p.immutable());
            }
        }
        MAP_TRAIN_LIGHTS.put(player.getUUID(),placed);
    }

    public static BlockPos mapTrainSoundPos(ServerPlayer player){
        return SceneSetup.mapTrain(player);
    }

    public static BlockPos mapTrainInteriorPos(ServerPlayer player,double forwardBlocks){
        Direction forward=Direction.fromYRot(SceneSetup.mapTrainYaw(player));
        if(!forward.getAxis().isHorizontal())forward=Direction.NORTH;
        return SceneSetup.mapTrain(player).relative(forward,(int)Math.round(forwardBlocks));
    }

    /** Legacy custom train helpers remain only so old spawned entities can be cleaned up. */
    public static Train31Entity spawnTrain(ServerPlayer player) {
        removeTrain(player);
        ServerLevel level=player.serverLevel();
        BlockPos tunnel=SceneSetup.tunnel(player);
        BlockPos rail=SceneSetup.rail(player);
        Train31Entity train=Train31Mod.TRAIN.get().create(level);
        if(train==null)return null;
        Vec3 d=Vec3.atCenterOf(rail).subtract(Vec3.atCenterOf(tunnel));
        float yaw=(float)(Mth.atan2(-d.x,d.z)*(180.0/Math.PI));
        train.setYRot(yaw); train.setXRot(0); train.setPos(tunnel.getX()+0.5,tunnel.getY()+0.08,tunnel.getZ()+0.5);
        train.addTag(TRAIN_TAG); level.addFreshEntity(train);
        TRAINS.put(player.getUUID(),train.getUUID());
        return train;
    }

    public static Train31Entity train(ServerPlayer player) {
        UUID id=TRAINS.get(player.getUUID());
        Entity e=id==null?null:player.serverLevel().getEntity(id);
        return e instanceof Train31Entity t?t:null;
    }

    public static void setTrainProgress(ServerPlayer player, double progress) {
        Train31Entity train=train(player); if(train==null)return;
        BlockPos tunnel=SceneSetup.tunnel(player), rail=SceneSetup.rail(player);
        progress=Mth.clamp(progress,0.0,1.0);
        double x=Mth.lerp(progress,tunnel.getX()+0.5,rail.getX()+0.5);
        double y=Mth.lerp(progress,tunnel.getY()+0.08,rail.getY()+0.08);
        double z=Mth.lerp(progress,tunnel.getZ()+0.5,rail.getZ()+0.5);
        train.teleportTo(x,y,z);
    }

    public static void departTrain(ServerPlayer player, double distance) {
        Train31Entity train=train(player); if(train==null)return;
        Vec3 tunnel=Vec3.atCenterOf(SceneSetup.tunnel(player));
        Vec3 stop=Vec3.atCenterOf(SceneSetup.rail(player));
        Vec3 forward=stop.subtract(tunnel); if(forward.lengthSqr()<0.001)return;
        forward=forward.normalize();
        Vec3 p=stop.add(forward.scale(distance));
        train.teleportTo(p.x,SceneSetup.rail(player).getY()+0.08,p.z);
    }

    public static void openTrainDoors(ServerPlayer player, boolean open) {
        Train31Entity t=train(player); if(t!=null)t.setDoorsOpen(open);
    }

    public static void removeTrain(ServerPlayer player) {
        ServerLevel level=player.serverLevel();
        UUID id=TRAINS.remove(player.getUUID());
        Entity direct=id==null?null:level.getEntity(id); if(direct!=null)direct.discard();
        BlockPos center=SceneSetup.complete(player)?SceneSetup.rail(player):player.blockPosition();
        AABB box=new AABB(center).inflate(260);
        for(Train31Entity t:level.getEntitiesOfClass(Train31Entity.class,box,e->e.getTags().contains(TRAIN_TAG)))t.discard();
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

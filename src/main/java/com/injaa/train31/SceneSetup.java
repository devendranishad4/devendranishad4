package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Stores scene positions and can now create the entire camera setup from one platform anchor. */
public final class SceneSetup {
    private SceneSetup() {}

    private static final String START = "train31_manual_start";
    private static final String START_YAW = "train31_manual_start_yaw";
    private static final String CCTV = "train31_manual_cctv";
    private static final String CCTV_FACE = "train31_manual_cctv_face";
    private static final String PLATFORM = "train31_manual_platform";
    private static final String RAIL = "train31_manual_rail";
    private static final String TUNNEL = "train31_manual_tunnel";
    private static final String MAP_TRAIN = "train31_manual_map_train"; // legacy/optional
    private static final String MAP_TRAIN_YAW = "train31_manual_map_train_yaw";

    private static String camPosKey(int i){ return "train31_manual_cam" + i + "_pos"; }
    private static String camYawKey(int i){ return "train31_manual_cam" + i + "_yaw"; }
    private static String camPitchKey(int i){ return "train31_manual_cam" + i + "_pitch"; }

    public static void markStart(ServerPlayer p) {
        p.getPersistentData().putLong(START, p.blockPosition().asLong());
        p.getPersistentData().putFloat(START_YAW, p.getYRot());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aTrain 31 START saved at " + coord(p.blockPosition())));
    }

    public static void markCctv(ServerPlayer p) {
        p.getPersistentData().putLong(CCTV, p.blockPosition().asLong());
        p.getPersistentData().putString(CCTV_FACE, p.getDirection().getName());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aCCTV reference saved at " + coord(p.blockPosition())));
    }

    public static void markPlatform(ServerPlayer p) {
        p.getPersistentData().putLong(PLATFORM, p.blockPosition().asLong());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aPLATFORM saved at " + coord(p.blockPosition())));
    }

    public static void markRail(ServerPlayer p) {
        p.getPersistentData().putLong(RAIL, p.blockPosition().asLong());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aTRAIN STOP RAIL saved at " + coord(p.blockPosition())));
    }

    public static void markTunnel(ServerPlayer p) {
        p.getPersistentData().putLong(TUNNEL, p.blockPosition().asLong());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aTUNNEL APPROACH saved at " + coord(p.blockPosition())));
    }

    /** Legacy option retained for old worlds. The new custom physical train does not require this marker. */
    public static void markMapTrain(ServerPlayer p) {
        p.getPersistentData().putLong(MAP_TRAIN, p.blockPosition().asLong());
        p.getPersistentData().putFloat(MAP_TRAIN_YAW, p.getYRot());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aLegacy map train marker saved at " + coord(p.blockPosition())));
    }

    /** Stand where the camera should be, look exactly where it should look, then save. */
    public static void markCamera(ServerPlayer p, int index) {
        if(index < 1 || index > 4) return;
        p.getPersistentData().putLong(camPosKey(index), p.blockPosition().asLong());
        p.getPersistentData().putFloat(camYawKey(index), p.getYRot());
        p.getPersistentData().putFloat(camPitchKey(index), p.getXRot());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aCAM " + index + " saved at " + coord(p.blockPosition())));
    }

    /**
     * ONE-COMMAND SETUP.
     * Stand on the platform at the point where Train 31 should stop and FACE DOWN THE TRACK toward the tunnel.
     * The code detects which side of the platform drops down toward the track, then creates rail/tunnel/CCTV/cameras itself.
     */
    public static void autoSetup(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos platform = p.blockPosition();
        Direction forward = p.getDirection();
        Direction right = forward.getClockWise();
        Direction left = forward.getCounterClockWise();

        int rightFloor = surfaceY(level, platform.relative(right,5), platform.getY());
        int leftFloor = surfaceY(level, platform.relative(left,5), platform.getY());
        Direction trackSide = rightFloor <= leftFloor ? right : left;
        int trackFloor = Math.min(rightFloor, leftFloor);

        BlockPos railXZ = platform.relative(trackSide,5);
        BlockPos rail = new BlockPos(railXZ.getX(), trackFloor + 1, railXZ.getZ());
        BlockPos tunnel = rail.relative(forward,42);
        BlockPos start = platform.relative(forward.getOpposite(),14);
        BlockPos cctv = platform.relative(forward.getOpposite(),8).relative(trackSide.getOpposite(),5);

        p.getPersistentData().putLong(START,start.asLong());
        p.getPersistentData().putFloat(START_YAW,p.getYRot());
        p.getPersistentData().putLong(CCTV,cctv.asLong());
        p.getPersistentData().putString(CCTV_FACE,forward.getName());
        p.getPersistentData().putLong(PLATFORM,platform.asLong());
        p.getPersistentData().putLong(RAIL,rail.asLong());
        p.getPersistentData().putLong(TUNNEL,tunnel.asLong());

        BlockPos cam1 = findCameraAir(level, platform.relative(forward.getOpposite(),7).relative(trackSide.getOpposite(),2).above(3));
        BlockPos cam2 = findCameraAir(level, platform.relative(forward,17).relative(trackSide.getOpposite(),2).above(3));
        BlockPos cam3 = findCameraAir(level, platform.relative(forward.getOpposite(),18).relative(trackSide.getOpposite(),2).above(3));
        BlockPos cam4 = findCameraAir(level, platform.relative(forward,4).relative(trackSide,1).above(4));

        saveAutoCamera(p,1,cam1,platform.above(1));
        saveAutoCamera(p,2,cam2,rail.relative(forward,6).above(1));
        saveAutoCamera(p,3,cam3,platform.relative(forward,5).above(1));
        saveAutoCamera(p,4,cam4,rail.above(1));

        dirty(p);
        p.sendSystemMessage(Component.literal("§a§lTRAIN 31 AUTO SETUP COMPLETE"));
        p.sendSystemMessage(Component.literal("§7Platform/rail/tunnel + all 4 CCTV cameras were placed automatically. Track side detected: §f"+trackSide.getName()));
    }

    private static void saveAutoCamera(ServerPlayer p,int index,BlockPos camera,BlockPos target){
        Vec3 from=Vec3.atCenterOf(camera).add(0,1.72,0);
        Vec3 to=Vec3.atCenterOf(target);
        Vec3 d=to.subtract(from);
        double flat=Math.sqrt(d.x*d.x+d.z*d.z);
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.max(0.001,flat)));
        p.getPersistentData().putLong(camPosKey(index),camera.asLong());
        p.getPersistentData().putFloat(camYawKey(index),yaw);
        p.getPersistentData().putFloat(camPitchKey(index),pitch);
    }

    private static BlockPos findCameraAir(ServerLevel level,BlockPos base){
        BlockPos p=base;
        for(int i=0;i<5;i++){
            if(level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
            p=p.above();
        }
        return p;
    }

    private static int surfaceY(ServerLevel level,BlockPos pos,int aroundY){
        for(int y=aroundY+1;y>=aroundY-5;y--){
            BlockPos p=new BlockPos(pos.getX(),y,pos.getZ());
            if(level.getBlockState(p).isCollisionShapeFullBlock(level,p)) return y;
        }
        return aroundY-1;
    }

    public static boolean hasCamera(ServerPlayer p, int index) {
        return index >= 1 && index <= 4 && p.getPersistentData().contains(camPosKey(index));
    }

    public static boolean complete(ServerPlayer p) {
        return p.getPersistentData().contains(START)
                && p.getPersistentData().contains(CCTV)
                && p.getPersistentData().contains(PLATFORM)
                && p.getPersistentData().contains(RAIL)
                && p.getPersistentData().contains(TUNNEL)
                && hasCamera(p,1) && hasCamera(p,2) && hasCamera(p,3) && hasCamera(p,4);
    }

    public static boolean valid(ServerPlayer p) {
        if(!complete(p)) return false;
        boolean ok=true;
        BlockPos st=start(p), ct=cctv(p), pl=platform(p), ra=rail(p), tu=tunnel(p);

        if(d2(st,pl) < 100){ p.sendSystemMessage(Component.literal("§cSetup error: START is too close to PLATFORM.")); ok=false; }
        if(Math.abs(pl.getY()-ra.getY()) > 5 || d2(pl,ra) > 500){ p.sendSystemMessage(Component.literal("§cSetup error: RAIL must be beside PLATFORM.")); ok=false; }
        if(Math.abs(tu.getY()-ra.getY()) > 5 || d2(tu,ra) < 100){ p.sendSystemMessage(Component.literal("§cSetup error: TUNNEL must be farther down the same track.")); ok=false; }
        if(d2(ct,pl) > 14400){ p.sendSystemMessage(Component.literal("§cSetup error: CCTV reference is too far from the platform.")); ok=false; }
        for(int i=1;i<=4;i++){
            BlockPos c=cameraPos(p,i);
            if(d2(c,pl) > 19600 || c.getY() > pl.getY()+32){
                p.sendSystemMessage(Component.literal("§cSetup error: CAM "+i+" is outside/far from the station.")); ok=false;
            }
        }
        if(!ok) dirty(p);
        return ok;
    }

    public static BlockPos start(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(START)); }
    public static BlockPos cctv(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(CCTV)); }
    public static BlockPos platform(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(PLATFORM)); }
    public static BlockPos rail(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(RAIL)); }
    public static BlockPos tunnel(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(TUNNEL)); }
    public static BlockPos mapTrain(ServerPlayer p) { return p.getPersistentData().contains(MAP_TRAIN) ? BlockPos.of(p.getPersistentData().getLong(MAP_TRAIN)) : rail(p); }
    public static float mapTrainYaw(ServerPlayer p) { return p.getPersistentData().getFloat(MAP_TRAIN_YAW); }
    public static float startYaw(ServerPlayer p) { return p.getPersistentData().getFloat(START_YAW); }

    public static BlockPos cameraPos(ServerPlayer p, int index) { return BlockPos.of(p.getPersistentData().getLong(camPosKey(index))); }
    public static float cameraYaw(ServerPlayer p, int index) { return p.getPersistentData().getFloat(camYawKey(index)); }
    public static float cameraPitch(ServerPlayer p, int index) { return p.getPersistentData().getFloat(camPitchKey(index)); }

    public static Direction cctvFacing(ServerPlayer p) {
        Direction d = Direction.byName(p.getPersistentData().getString(CCTV_FACE));
        return d != null && d.getAxis().isHorizontal() ? d : Direction.SOUTH;
    }

    public static void status(ServerPlayer p) {
        String a = p.getPersistentData().contains(START) ? "§aSET" : "§cMISSING";
        String b = p.getPersistentData().contains(CCTV) ? "§aSET" : "§cMISSING";
        String c = p.getPersistentData().contains(PLATFORM) ? "§aSET" : "§cMISSING";
        String d = p.getPersistentData().contains(RAIL) ? "§aSET" : "§cMISSING";
        String e = p.getPersistentData().contains(TUNNEL) ? "§aSET" : "§cMISSING";
        p.sendSystemMessage(Component.literal("§eTrain 31 setup §7| §fSTART: "+a+" §7| §fCCTV: "+b+" §7| §fPLATFORM: "+c+" §7| §fRAIL: "+d+" §7| §fTUNNEL: "+e));
        p.sendSystemMessage(Component.literal("§fCAM1: "+(hasCamera(p,1)?"§aSET":"§cMISSING")+" §7| §fCAM2: "+(hasCamera(p,2)?"§aSET":"§cMISSING")+" §7| §fCAM3: "+(hasCamera(p,3)?"§aSET":"§cMISSING")+" §7| §fCAM4: "+(hasCamera(p,4)?"§aSET":"§cMISSING")));
        if(!complete(p)) p.sendSystemMessage(Component.literal("§7Easy setup: stand on platform, face down the track toward the tunnel, then run §f/train31 auto"));
        else valid(p);
    }

    public static void clear(ServerPlayer p) {
        p.getPersistentData().remove(START); p.getPersistentData().remove(START_YAW);
        p.getPersistentData().remove(CCTV); p.getPersistentData().remove(CCTV_FACE);
        p.getPersistentData().remove(PLATFORM); p.getPersistentData().remove(RAIL); p.getPersistentData().remove(TUNNEL);
        p.getPersistentData().remove(MAP_TRAIN); p.getPersistentData().remove(MAP_TRAIN_YAW);
        for(int i=1;i<=4;i++){
            p.getPersistentData().remove(camPosKey(i)); p.getPersistentData().remove(camYawKey(i)); p.getPersistentData().remove(camPitchKey(i));
        }
        p.getPersistentData().remove("train31_prepared");
        p.getPersistentData().remove("train31_remote_cam");
        p.getPersistentData().remove("train31_cctv_active");
        p.getPersistentData().remove("train31_cctv_current");
        p.sendSystemMessage(Component.literal("§eTrain 31 setup cleared."));
    }

    private static void dirty(ServerPlayer p){p.getPersistentData().remove("train31_prepared");}
    private static long d2(BlockPos a, BlockPos b){
        long dx=(long)a.getX()-b.getX(), dy=(long)a.getY()-b.getY(), dz=(long)a.getZ()-b.getZ();
        return dx*dx+dy*dy+dz*dz;
    }
    private static String coord(BlockPos p){ return "["+p.getX()+", "+p.getY()+", "+p.getZ()+"]"; }
}

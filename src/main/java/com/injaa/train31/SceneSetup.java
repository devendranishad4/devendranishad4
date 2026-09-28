package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Stores scene positions and can create the entire camera setup from one platform anchor. */
public final class SceneSetup {
    private SceneSetup() {}

    private static final String START = "train31_manual_start";
    private static final String START_YAW = "train31_manual_start_yaw";
    private static final String CCTV = "train31_manual_cctv";
    private static final String CCTV_FACE = "train31_manual_cctv_face";
    private static final String PLATFORM = "train31_manual_platform";
    private static final String RAIL = "train31_manual_rail";
    private static final String TUNNEL = "train31_manual_tunnel";
    private static final String MAP_TRAIN = "train31_manual_map_train";
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

    public static void markMapTrain(ServerPlayer p) {
        p.getPersistentData().putLong(MAP_TRAIN, p.blockPosition().asLong());
        p.getPersistentData().putFloat(MAP_TRAIN_YAW, p.getYRot());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aLegacy map train marker saved at " + coord(p.blockPosition())));
    }

    public static void markCamera(ServerPlayer p, int index) {
        if(index < 1 || index > 4) return;
        p.getPersistentData().putLong(camPosKey(index), p.blockPosition().asLong());
        p.getPersistentData().putFloat(camYawKey(index), p.getYRot());
        p.getPersistentData().putFloat(camPitchKey(index), p.getXRot());
        dirty(p);
        p.sendSystemMessage(Component.literal("§aCAM " + index + " saved at " + coord(p.blockPosition())));
    }

    /**
     * Preset for the Tokyo-inspired subway map used by this episode.
     * It puts the player on the tested central platform, facing the tunnel, then runs the normal smart auto setup.
     */
    public static void tokyoPreset(ServerPlayer p) {
        clear(p);
        p.teleportTo(p.serverLevel(), -74.5, 62.05, 236.5, 90.0f, 0.0f);
        autoSetup(p);
        p.sendSystemMessage(Component.literal("§bTokyo subway preset loaded. §fYou are at the recommended Train 31 platform anchor."));
    }

    /**
     * ONE-COMMAND SETUP.
     * Stand in the middle of the platform and FACE DOWN THE TRACK toward the tunnel.
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
        BlockPos tunnel = rail.relative(forward,48);
        BlockPos start = platform.relative(forward.getOpposite(),12);
        BlockPos cctv = platform.relative(forward.getOpposite(),6).relative(trackSide.getOpposite(),2);

        p.getPersistentData().putLong(START,start.asLong());
        p.getPersistentData().putFloat(START_YAW,p.getYRot());
        p.getPersistentData().putLong(CCTV,cctv.asLong());
        p.getPersistentData().putString(CCTV_FACE,forward.getName());
        p.getPersistentData().putLong(PLATFORM,platform.asLong());
        p.getPersistentData().putLong(RAIL,rail.asLong());
        p.getPersistentData().putLong(TUNNEL,tunnel.asLong());

        // Cameras now sit toward the track instead of against the rear wall/ceiling.
        BlockPos cam1Preferred = platform.relative(forward.getOpposite(),18).relative(trackSide,1);
        BlockPos cam2Preferred = platform.relative(forward,20).relative(trackSide,1);
        BlockPos oppositeBase = new BlockPos(rail.getX(), platform.getY(), rail.getZ()).relative(trackSide,5).relative(forward.getOpposite(),7);
        BlockPos cam4Preferred = platform.relative(forward.getOpposite(),4).relative(trackSide,1);

        BlockPos cam1 = findCameraAir(level, cam1Preferred);
        BlockPos cam2 = findCameraAir(level, cam2Preferred);
        BlockPos cam3 = findCameraAir(level, oppositeBase);
        BlockPos cam4 = findCameraAir(level, cam4Preferred);

        // 1 wide platform, 2 tunnel approach, 3 opposite-platform master, 4 closer stop-zone diagonal.
        saveAutoCamera(p,1,cam1,platform.relative(forward,7).above(1));
        saveAutoCamera(p,2,cam2,tunnel.relative(forward.getOpposite(),5).above(1));
        saveAutoCamera(p,3,cam3,rail.relative(forward,2).above(1));
        saveAutoCamera(p,4,cam4,rail.relative(forward,7).above(1));

        dirty(p);
        p.sendSystemMessage(Component.literal("§a§lTRAIN 31 AUTO SETUP COMPLETE"));
        p.sendSystemMessage(Component.literal("§7Wide / tunnel / opposite-platform / stop-zone CCTV angles generated automatically."));
        p.sendSystemMessage(Component.literal("§8CAM1 "+coord(cam1)+" | CAM2 "+coord(cam2)+" | CAM3 "+coord(cam3)+" | CAM4 "+coord(cam4)));
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

    /** Search nearby air too, so a preferred camera point inside a pillar/wall is moved into a usable open spot. */
    private static BlockPos findCameraAir(ServerLevel level,BlockPos base){
        for(int radius=0; radius<=5; radius++){
            for(int dx=-radius; dx<=radius; dx++){
                for(int dz=-radius; dz<=radius; dz++){
                    if(radius>0 && Math.abs(dx)!=radius && Math.abs(dz)!=radius) continue;
                    for(int dy=0; dy<=3; dy++){
                        BlockPos p=base.offset(dx,dy,dz);
                        if(level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
                    }
                }
            }
        }
        return base.above(2);
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
        if(!complete(p)) p.sendSystemMessage(Component.literal("§7Easy setup: stand on platform, face toward the tunnel, then run §f/train31 auto"));
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

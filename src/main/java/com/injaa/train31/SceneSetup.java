package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Stores exact creator-marked positions in the Tokyo station. */
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
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: START saved outside at " + coord(p.blockPosition())));
    }

    public static void markCctv(ServerPlayer p) {
        p.getPersistentData().putLong(CCTV, p.blockPosition().asLong());
        p.getPersistentData().putString(CCTV_FACE, p.getDirection().getName());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aCCTV ROOM saved at " + coord(p.blockPosition()) + ". Face the monitor wall when saving."));
    }

    public static void markPlatform(ServerPlayer p) {
        p.getPersistentData().putLong(PLATFORM, p.blockPosition().asLong());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aPLATFORM saved at " + coord(p.blockPosition())));
    }

    public static void markRail(ServerPlayer p) {
        p.getPersistentData().putLong(RAIL, p.blockPosition().asLong());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aTRACK REFERENCE saved at " + coord(p.blockPosition())));
    }

    public static void markTunnel(ServerPlayer p) {
        p.getPersistentData().putLong(TUNNEL, p.blockPosition().asLong());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aTUNNEL SOUND DIRECTION saved at " + coord(p.blockPosition())));
    }

    /** Stand inside the EXISTING block-built subway train and face along the carriage before saving. */
    public static void markMapTrain(ServerPlayer p) {
        p.getPersistentData().putLong(MAP_TRAIN, p.blockPosition().asLong());
        p.getPersistentData().putFloat(MAP_TRAIN_YAW, p.getYRot());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aEXISTING MAP TRAIN selected at " + coord(p.blockPosition()) + ". This physical train is now Train 31."));
    }

    /** Stand where the camera should be, look exactly where it should look, then save. */
    public static void markCamera(ServerPlayer p, int index) {
        if(index < 1 || index > 4) return;
        p.getPersistentData().putLong(camPosKey(index), p.blockPosition().asLong());
        p.getPersistentData().putFloat(camYawKey(index), p.getYRot());
        p.getPersistentData().putFloat(camPitchKey(index), p.getXRot());
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§aCAM " + index + " saved at " + coord(p.blockPosition()) + " using your current view direction."));
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
                && p.getPersistentData().contains(MAP_TRAIN)
                && hasCamera(p,1) && hasCamera(p,2) && hasCamera(p,3) && hasCamera(p,4);
    }

    /** Reject markers accidentally saved outside or far from the station scene. */
    public static boolean valid(ServerPlayer p) {
        if(!complete(p)) return false;
        boolean ok=true;
        BlockPos st=start(p), ct=cctv(p), pl=platform(p), ra=rail(p), tu=tunnel(p), mt=mapTrain(p);

        if(d2(st,pl) < 100){
            p.sendSystemMessage(Component.literal("§cSetup error: PLATFORM is too close to START. PLATFORM must be inside/down in the subway.")); ok=false;
        }
        if(Math.abs(pl.getY()-ra.getY()) > 4 || d2(pl,ra) > 400){
            p.sendSystemMessage(Component.literal("§cSetup error: RAIL must be beside the PLATFORM on the subway track.")); ok=false;
        }
        if(Math.abs(tu.getY()-ra.getY()) > 5 || d2(tu,ra) < 100){
            p.sendSystemMessage(Component.literal("§cSetup error: TUNNEL must be farther down the same track direction.")); ok=false;
        }
        if(d2(st,ra) < 100 || d2(st,tu) < 100){
            p.sendSystemMessage(Component.literal("§cSetup error: RAIL/TUNNEL are still near the outside START.")); ok=false;
        }
        if(Math.abs(mt.getY()-pl.getY()) > 6 || d2(mt,pl) > 2500){
            p.sendSystemMessage(Component.literal("§cSetup error: MAPTRAIN must be inside the existing subway train beside this platform.")); ok=false;
        }
        if(d2(ct,pl) > 14400){
            p.sendSystemMessage(Component.literal("§cSetup error: CCTV ROOM looks too far from the subway platform.")); ok=false;
        }
        for(int i=1;i<=4;i++){
            BlockPos c=cameraPos(p,i);
            if(d2(c,pl) > 19600 || c.getY() > pl.getY()+24){
                p.sendSystemMessage(Component.literal("§cSetup error: CAM " + i + " looks outside/far from the subway. Save it inside the station.")); ok=false;
            }
        }

        if(!ok) p.getPersistentData().remove("train31_prepared");
        return ok;
    }

    public static BlockPos start(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(START)); }
    public static BlockPos cctv(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(CCTV)); }
    public static BlockPos platform(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(PLATFORM)); }
    public static BlockPos rail(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(RAIL)); }
    public static BlockPos tunnel(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(TUNNEL)); }
    public static BlockPos mapTrain(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(MAP_TRAIN)); }
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
        String f = p.getPersistentData().contains(MAP_TRAIN) ? "§aSET" : "§cMISSING";
        p.sendSystemMessage(Component.literal("§eTrain 31 setup §7| §fSTART: " + a + " §7| §fCCTV: " + b + " §7| §fPLATFORM: " + c + " §7| §fRAIL: " + d + " §7| §fTUNNEL: " + e + " §7| §fMAPTRAIN: " + f));
        p.sendSystemMessage(Component.literal("§fCAM1: " + (hasCamera(p,1)?"§aSET":"§cMISSING") + " §7| §fCAM2: " + (hasCamera(p,2)?"§aSET":"§cMISSING") + " §7| §fCAM3: " + (hasCamera(p,3)?"§aSET":"§cMISSING") + " §7| §fCAM4: " + (hasCamera(p,4)?"§aSET":"§cMISSING")));

        if(p.getPersistentData().contains(START)) p.sendSystemMessage(Component.literal("§7START " + coord(start(p))));
        if(p.getPersistentData().contains(CCTV)) p.sendSystemMessage(Component.literal("§7CCTV " + coord(cctv(p))));
        if(p.getPersistentData().contains(PLATFORM)) p.sendSystemMessage(Component.literal("§7PLATFORM " + coord(platform(p))));
        if(p.getPersistentData().contains(RAIL)) p.sendSystemMessage(Component.literal("§7RAIL " + coord(rail(p))));
        if(p.getPersistentData().contains(TUNNEL)) p.sendSystemMessage(Component.literal("§7TUNNEL " + coord(tunnel(p))));
        if(p.getPersistentData().contains(MAP_TRAIN)) p.sendSystemMessage(Component.literal("§7MAPTRAIN " + coord(mapTrain(p))));
        for(int i=1;i<=4;i++) if(hasCamera(p,i)) p.sendSystemMessage(Component.literal("§7CAM"+i+" "+coord(cameraPos(p,i))));

        if (!complete(p)) p.sendSystemMessage(Component.literal("§7One-time setup: set start | cctv | platform | rail | tunnel | maptrain | cam1 | cam2 | cam3 | cam4"));
        else valid(p);
    }

    public static void clear(ServerPlayer p) {
        p.getPersistentData().remove(START);
        p.getPersistentData().remove(START_YAW);
        p.getPersistentData().remove(CCTV);
        p.getPersistentData().remove(CCTV_FACE);
        p.getPersistentData().remove(PLATFORM);
        p.getPersistentData().remove(RAIL);
        p.getPersistentData().remove(TUNNEL);
        p.getPersistentData().remove(MAP_TRAIN);
        p.getPersistentData().remove(MAP_TRAIN_YAW);
        for(int i=1;i<=4;i++){
            p.getPersistentData().remove(camPosKey(i));
            p.getPersistentData().remove(camYawKey(i));
            p.getPersistentData().remove(camPitchKey(i));
        }
        p.getPersistentData().remove("train31_prepared");
        p.getPersistentData().remove("train31_remote_cam");
        p.getPersistentData().remove("train31_cctv_active");
        p.getPersistentData().remove("train31_cctv_current");
        p.sendSystemMessage(Component.literal("§eTrain 31 exact-map setup cleared."));
    }

    private static long d2(BlockPos a, BlockPos b){
        long dx=(long)a.getX()-b.getX(), dy=(long)a.getY()-b.getY(), dz=(long)a.getZ()-b.getZ();
        return dx*dx+dy*dy+dz*dz;
    }
    private static String coord(BlockPos p){ return "["+p.getX()+", "+p.getY()+", "+p.getZ()+"]"; }
}

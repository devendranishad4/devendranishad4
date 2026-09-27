package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Stores the exact creator-marked positions in the Tokyo station. */
public final class SceneSetup {
    private SceneSetup() {}

    private static final String START = "train31_manual_start";
    private static final String START_YAW = "train31_manual_start_yaw";
    private static final String CCTV = "train31_manual_cctv";
    private static final String CCTV_FACE = "train31_manual_cctv_face";
    private static final String PLATFORM = "train31_manual_platform";
    private static final String RAIL = "train31_manual_rail";
    private static final String TUNNEL = "train31_manual_tunnel";

    private static String camPosKey(int i){ return "train31_manual_cam" + i + "_pos"; }
    private static String camYawKey(int i){ return "train31_manual_cam" + i + "_yaw"; }
    private static String camPitchKey(int i){ return "train31_manual_cam" + i + "_pitch"; }

    public static void markStart(ServerPlayer p) {
        p.getPersistentData().putLong(START, p.blockPosition().asLong());
        p.getPersistentData().putFloat(START_YAW, p.getYRot());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: START saved here."));
    }

    public static void markCctv(ServerPlayer p) {
        p.getPersistentData().putLong(CCTV, p.blockPosition().asLong());
        p.getPersistentData().putString(CCTV_FACE, p.getDirection().getName());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: CCTV ROOM saved. Stand inside the real room and face the wall where the monitor showcase should appear."));
    }

    public static void markPlatform(ServerPlayer p) {
        p.getPersistentData().putLong(PLATFORM, p.blockPosition().asLong());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: PLATFORM position saved."));
    }

    /** Stand on the exact track/rail where the train should stop. */
    public static void markRail(ServerPlayer p) {
        p.getPersistentData().putLong(RAIL, p.blockPosition().asLong());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: TRAIN RAIL / STOP CENTER saved."));
    }

    /** Stand inside the real tunnel on the same rail, where the train must come from. */
    public static void markTunnel(ServerPlayer p) {
        p.getPersistentData().putLong(TUNNEL, p.blockPosition().asLong());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: TUNNEL APPROACH saved."));
    }

    /** Stand below the exact camera mounting spot and look exactly where that camera should look. */
    public static void markCamera(ServerPlayer p, int index) {
        if(index < 1 || index > 4) return;
        p.getPersistentData().putLong(camPosKey(index), p.blockPosition().asLong());
        p.getPersistentData().putFloat(camYawKey(index), p.getYRot());
        p.getPersistentData().putFloat(camPitchKey(index), p.getXRot());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: CAM " + index + " saved. The camera will mount above this spot and use your current view direction."));
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

    public static BlockPos start(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(START)); }
    public static BlockPos cctv(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(CCTV)); }
    public static BlockPos platform(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(PLATFORM)); }
    public static BlockPos rail(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(RAIL)); }
    public static BlockPos tunnel(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(TUNNEL)); }
    public static float startYaw(ServerPlayer p) { return p.getPersistentData().getFloat(START_YAW); }

    public static BlockPos cameraPos(ServerPlayer p, int index) {
        return BlockPos.of(p.getPersistentData().getLong(camPosKey(index)));
    }
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
        p.sendSystemMessage(Component.literal("§eTrain 31 setup §7| §fSTART: " + a + " §7| §fCCTV: " + b + " §7| §fPLATFORM: " + c + " §7| §fRAIL: " + d + " §7| §fTUNNEL: " + e));
        String cams = "§fCAM1: " + (hasCamera(p,1)?"§aSET":"§cMISSING")
                + " §7| §fCAM2: " + (hasCamera(p,2)?"§aSET":"§cMISSING")
                + " §7| §fCAM3: " + (hasCamera(p,3)?"§aSET":"§cMISSING")
                + " §7| §fCAM4: " + (hasCamera(p,4)?"§aSET":"§cMISSING");
        p.sendSystemMessage(Component.literal(cams));
        if (!complete(p)) p.sendSystemMessage(Component.literal("§7One-time setup: set start | cctv | platform | rail | tunnel | cam1 | cam2 | cam3 | cam4"));
    }

    public static void clear(ServerPlayer p) {
        p.getPersistentData().remove(START);
        p.getPersistentData().remove(START_YAW);
        p.getPersistentData().remove(CCTV);
        p.getPersistentData().remove(CCTV_FACE);
        p.getPersistentData().remove(PLATFORM);
        p.getPersistentData().remove(RAIL);
        p.getPersistentData().remove(TUNNEL);
        for(int i=1;i<=4;i++){
            p.getPersistentData().remove(camPosKey(i));
            p.getPersistentData().remove(camYawKey(i));
            p.getPersistentData().remove(camPitchKey(i));
        }
        p.getPersistentData().remove("train31_prepared");
        p.getPersistentData().remove("train31_remote_cam");
        p.sendSystemMessage(Component.literal("§eTrain 31 exact-map setup cleared."));
    }
}

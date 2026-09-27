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

    public static void markStart(ServerPlayer p) {
        p.getPersistentData().putLong(START, p.blockPosition().asLong());
        p.getPersistentData().putFloat(START_YAW, p.getYRot());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: START saved here."));
    }

    public static void markCctv(ServerPlayer p) {
        p.getPersistentData().putLong(CCTV, p.blockPosition().asLong());
        p.getPersistentData().putString(CCTV_FACE, p.getDirection().getName());
        p.sendSystemMessage(Component.literal("§aTrain 31 setup: CCTV ROOM saved. Face the monitor wall when saving."));
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

    public static boolean complete(ServerPlayer p) {
        return p.getPersistentData().contains(START)
                && p.getPersistentData().contains(CCTV)
                && p.getPersistentData().contains(PLATFORM)
                && p.getPersistentData().contains(RAIL)
                && p.getPersistentData().contains(TUNNEL);
    }

    public static BlockPos start(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(START)); }
    public static BlockPos cctv(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(CCTV)); }
    public static BlockPos platform(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(PLATFORM)); }
    public static BlockPos rail(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(RAIL)); }
    public static BlockPos tunnel(ServerPlayer p) { return BlockPos.of(p.getPersistentData().getLong(TUNNEL)); }
    public static float startYaw(ServerPlayer p) { return p.getPersistentData().getFloat(START_YAW); }

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
        if (!complete(p)) p.sendSystemMessage(Component.literal("§7One-time setup: /train31 set start | cctv | platform | rail | tunnel"));
    }

    public static void clear(ServerPlayer p) {
        p.getPersistentData().remove(START);
        p.getPersistentData().remove(START_YAW);
        p.getPersistentData().remove(CCTV);
        p.getPersistentData().remove(CCTV_FACE);
        p.getPersistentData().remove(PLATFORM);
        p.getPersistentData().remove(RAIL);
        p.getPersistentData().remove(TUNNEL);
        p.getPersistentData().remove("train31_prepared");
        p.sendSystemMessage(Component.literal("§eTrain 31 exact-map setup cleared."));
    }
}

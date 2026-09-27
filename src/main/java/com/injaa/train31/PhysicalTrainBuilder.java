package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Converts the arriving cinematic entity into a real block-built train once it stops.
 * The player can touch it, walk through the doors and enter the interior normally.
 */
public final class PhysicalTrainBuilder {
    private PhysicalTrainBuilder() {}

    private static final Map<UUID, LinkedHashMap<BlockPos, BlockState>> SAVED = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> DOORS = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> LIGHTS = new HashMap<>();

    private static final int HALF_LENGTH = 33; // about 67 blocks total
    private static final int HALF_WIDTH = 2;   // five blocks wide
    private static final int HEIGHT = 5;

    public static void build(ServerPlayer player) {
        restore(player);
        ServerLevel level = player.serverLevel();
        StationBuilder.RailGeometry g = StationBuilder.geometry(player);
        BlockPos center = g.rail();
        int platformWall = g.platformSide() >= 0 ? HALF_WIDTH : -HALF_WIDTH;
        int platformSign = platformWall > 0 ? 1 : -1;

        LinkedHashMap<BlockPos, BlockState> saved = new LinkedHashMap<>();
        List<BlockPos> doors = new ArrayList<>();
        List<BlockPos> lights = new ArrayList<>();
        SAVED.put(player.getUUID(), saved);
        DOORS.put(player.getUUID(), doors);
        LIGHTS.put(player.getUUID(), lights);

        Set<Integer> joints = Set.of(-22, -11, 0, 11, 22);
        Set<Integer> doorCenters = Set.of(-27, -16, -5, 5, 16, 27);

        for (int along = -HALF_LENGTH; along <= HALF_LENGTH; along++) {
            boolean end = Math.abs(along) == HALF_LENGTH;
            boolean joint = joints.contains(along);

            for (int side = -HALF_WIDTH; side <= HALF_WIDTH; side++) {
                put(level, saved, pos(center, g.axisZ(), side, 0, along), Blocks.SMOOTH_STONE.defaultBlockState());
                put(level, saved, pos(center, g.axisZ(), side, HEIGHT, along), joint ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }

            if (!end) {
                for (int side = -1; side <= 1; side++) {
                    for (int y = 1; y < HEIGHT; y++) put(level, saved, pos(center, g.axisZ(), side, y, along), Blocks.AIR.defaultBlockState());
                }
            }

            for (int wall : new int[]{-HALF_WIDTH, HALF_WIDTH}) {
                for (int y = 1; y < HEIGHT; y++) {
                    BlockState state;
                    if (end) state = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                    else if (joint) state = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    else if (y == 1) state = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                    else if (y == 2) state = Blocks.CYAN_CONCRETE.defaultBlockState();
                    else if (y == 3) state = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                    else state = Blocks.WHITE_CONCRETE.defaultBlockState();
                    put(level, saved, pos(center, g.axisZ(), wall, y, along), state);
                }
            }

            if (end) {
                for (int side = -1; side <= 1; side++) {
                    put(level, saved, pos(center, g.axisZ(), side, 1, along), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                    put(level, saved, pos(center, g.axisZ(), side, 2, along), Blocks.CYAN_CONCRETE.defaultBlockState());
                    put(level, saved, pos(center, g.axisZ(), side, 3, along), Blocks.BLACK_STAINED_GLASS.defaultBlockState());
                    put(level, saved, pos(center, g.axisZ(), side, 4, along), Blocks.WHITE_CONCRETE.defaultBlockState());
                }
            }

            if (Math.floorMod(along + HALF_LENGTH, 4) == 2 && !joint && !end) {
                BlockPos light = pos(center, g.axisZ(), 0, 4, along);
                put(level, saved, light, Blocks.SEA_LANTERN.defaultBlockState());
                lights.add(light.immutable());
            }
        }

        // Six two-block-wide doors on the platform side. They start closed.
        for (int c : doorCenters) {
            for (int a = c; a <= c + 1; a++) {
                for (int y = 1; y <= 3; y++) {
                    BlockPos d = pos(center, g.axisZ(), platformWall, y, a);
                    put(level, saved, d, y == 2 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                    doors.add(d.immutable());
                }
                // Two-block boarding threshold toward the platform so entering is easy instead of a long jump.
                put(level, saved, pos(center, g.axisZ(), platformWall + platformSign, 0, a), Blocks.POLISHED_ANDESITE.defaultBlockState());
                put(level, saved, pos(center, g.axisZ(), platformWall + platformSign*2, 0, a), Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }

        for (int a = -28; a <= 28; a += 7) {
            if (joints.contains(a)) continue;
            for (int y = 1; y <= 3; y++) put(level, saved, pos(center, g.axisZ(), 0, y, a), Blocks.IRON_BARS.defaultBlockState());
        }
    }

    public static boolean exists(ServerPlayer player) { return SAVED.containsKey(player.getUUID()); }

    public static void setDoorsOpen(ServerPlayer player, boolean open) {
        LinkedHashMap<BlockPos, BlockState> saved = SAVED.get(player.getUUID());
        List<BlockPos> doors = DOORS.get(player.getUUID());
        if (saved == null || doors == null) return;
        ServerLevel level = player.serverLevel();
        for (BlockPos p : doors) {
            BlockState closed = (p.getY() - StationBuilder.geometry(player).rail().getY()) == 2
                    ? Blocks.IRON_BLOCK.defaultBlockState()
                    : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            level.setBlock(p, open ? Blocks.AIR.defaultBlockState() : closed, 3);
        }
    }

    public static void setLights(ServerPlayer player, boolean on) {
        List<BlockPos> lights = LIGHTS.get(player.getUUID());
        if (lights == null) return;
        ServerLevel level = player.serverLevel();
        for (BlockPos p : lights) level.setBlock(p, on ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState(), 3);
    }

    public static void restore(ServerPlayer player) {
        LinkedHashMap<BlockPos, BlockState> saved = SAVED.remove(player.getUUID());
        DOORS.remove(player.getUUID()); LIGHTS.remove(player.getUUID());
        if (saved == null) return;
        ServerLevel level = player.serverLevel();
        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(saved.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<BlockPos, BlockState> e : entries) level.setBlock(e.getKey(), e.getValue(), 3);
    }

    private static BlockPos pos(BlockPos center, boolean axisZ, int side, int y, int along) {
        return axisZ ? center.offset(side, y, along) : center.offset(along, y, side);
    }

    private static void put(ServerLevel level, LinkedHashMap<BlockPos, BlockState> saved, BlockPos p, BlockState state) {
        BlockPos key = p.immutable();
        saved.putIfAbsent(key, level.getBlockState(key));
        level.setBlock(key, state, 3);
    }
}

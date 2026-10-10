package com.devendra.lightdirector;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public final class LightDirectorController {
    private LightDirectorController() {}

    private static final int[] RANGES = {100, 250, 500, 1000, 1500, 2500};
    private static final double[] SPEEDS = {0.2D, 0.35D, 0.5D, 0.75D, 1.0D, 1.2D, 1.25D, 1.5D, 2.0D, 3.0D};
    private static final String RANGE_KEY = "LightDirectorRange";
    private static final String SPEED_KEY = "LightDirectorSpeed";

    private enum Mode { IDLE, BLACKOUT, FLICKER, RESTORING }

    private static final Map<net.minecraft.resources.ResourceKey<Level>, State> STATES = new HashMap<>();

    private static final class State {
        Mode mode = Mode.IDLE;
        UUID owner;
        int originX;
        int originZ;
        int range;
        int minCX, maxCX, minCZ, maxCZ;
        int cx, cz;
        int sectionIndex;
        boolean scanning;
        int ticker;
        final LinkedHashMap<BlockPos, BlockState> lights = new LinkedHashMap<>();
        final HashSet<BlockPos> toggledOff = new HashSet<>();
        final ArrayDeque<BlockPos> restoreQueue = new ArrayDeque<>();
        final Random random = new Random();
    }

    private static State state(ServerLevel level) {
        return STATES.computeIfAbsent(level.dimension(), k -> new State());
    }

    public static int getRange(ServerPlayer player) {
        int v = player.getPersistentData().getInt(RANGE_KEY);
        if (v <= 0) {
            v = 1000;
            player.getPersistentData().putInt(RANGE_KEY, v);
        }
        return snapRange(v);
    }

    public static int setRange(ServerPlayer player, int requested) {
        int v = snapRange(requested);
        player.getPersistentData().putInt(RANGE_KEY, v);
        return v;
    }

    public static int cycleRange(ServerPlayer player) {
        int current = getRange(player);
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == current) {
                int next = RANGES[(i + 1) % RANGES.length];
                player.getPersistentData().putInt(RANGE_KEY, next);
                return next;
            }
        }
        player.getPersistentData().putInt(RANGE_KEY, RANGES[0]);
        return RANGES[0];
    }

    private static int snapRange(int requested) {
        int best = RANGES[0];
        int diff = Math.abs(requested - best);
        for (int r : RANGES) {
            int d = Math.abs(requested - r);
            if (d < diff) {
                best = r;
                diff = d;
            }
        }
        return best;
    }

    public static double getSpeed(ServerPlayer player) {
        double v = player.getPersistentData().getDouble(SPEED_KEY);
        if (v <= 0.0D) {
            v = 1.0D;
            player.getPersistentData().putDouble(SPEED_KEY, v);
        }
        return snapSpeed(v);
    }

    public static double setSpeed(ServerPlayer player, double requested) {
        double v = snapSpeed(requested);
        player.getPersistentData().putDouble(SPEED_KEY, v);
        return v;
    }

    public static double cycleSpeed(ServerPlayer player) {
        double current = getSpeed(player);
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - current) < 0.0001D) {
                double next = SPEEDS[(i + 1) % SPEEDS.length];
                player.getPersistentData().putDouble(SPEED_KEY, next);
                return next;
            }
        }
        player.getPersistentData().putDouble(SPEED_KEY, 1.0D);
        return 1.0D;
    }

    private static double snapSpeed(double requested) {
        double best = SPEEDS[0];
        double diff = Math.abs(requested - best);
        for (double s : SPEEDS) {
            double d = Math.abs(requested - s);
            if (d < diff) {
                best = s;
                diff = d;
            }
        }
        return best;
    }

    public static void startBlackout(ServerPlayer player) {
        begin(player, Mode.BLACKOUT);
    }

    public static void startFlicker(ServerPlayer player) {
        begin(player, Mode.FLICKER);
    }

    public static boolean toggleFlicker(ServerPlayer player) {
        State s = state(player.serverLevel());
        if (s.mode == Mode.FLICKER) {
            restore(player.serverLevel());
            return false;
        }
        startFlicker(player);
        return true;
    }

    private static void begin(ServerPlayer player, Mode mode) {
        ServerLevel level = player.serverLevel();
        State s = state(level);
        restoreImmediate(level, s);

        s.mode = mode;
        s.owner = player.getUUID();
        s.originX = player.getBlockX();
        s.originZ = player.getBlockZ();
        s.range = getRange(player);
        int chunkRadius = (s.range + 15) >> 4;
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        s.minCX = pcx - chunkRadius;
        s.maxCX = pcx + chunkRadius;
        s.minCZ = pcz - chunkRadius;
        s.maxCZ = pcz + chunkRadius;
        s.cx = s.minCX;
        s.cz = s.minCZ;
        s.sectionIndex = 0;
        s.scanning = true;
        s.ticker = 0;
        s.lights.clear();
        s.toggledOff.clear();
        s.restoreQueue.clear();
    }

    public static void restore(ServerLevel level) {
        State s = state(level);
        s.mode = Mode.RESTORING;
        s.scanning = false;
        s.restoreQueue.clear();
        s.restoreQueue.addAll(s.lights.keySet());
    }

    public static String status(ServerLevel level) {
        State s = state(level);
        return s.mode.name().toLowerCase(Locale.ROOT) +
                ", cached lights " + s.lights.size() +
                (s.scanning ? ", scanning" : "");
    }

    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        State s = STATES.get(level.dimension());
        if (s == null || s.mode == Mode.IDLE) return;

        if (s.mode == Mode.RESTORING) {
            restoreTick(level, s);
            return;
        }

        if (s.scanning) {
            scanTick(level, s);
        }

        if (s.mode == Mode.FLICKER && !s.lights.isEmpty()) {
            flickerTick(level, s);
        }
    }

    private static void scanTick(ServerLevel level, State s) {
        int sectionBudget = 12;
        int coordinateBudget = 256;

        while (coordinateBudget-- > 0 && sectionBudget > 0 && s.scanning) {
            if (s.cx > s.maxCX) {
                s.scanning = false;
                break;
            }

            if (!withinRange(s, s.cx, s.cz) || !level.hasChunk(s.cx, s.cz)) {
                advanceChunk(s);
                continue;
            }

            LevelChunk chunk = level.getChunk(s.cx, s.cz);
            LevelChunkSection[] sections = chunk.getSections();
            if (s.sectionIndex >= sections.length) {
                advanceChunk(s);
                continue;
            }

            LevelChunkSection section = sections[s.sectionIndex];
            int baseY = level.getMinBuildHeight() + s.sectionIndex * 16;
            scanSection(level, s, chunk, section, baseY);
            s.sectionIndex++;
            sectionBudget--;

            if (s.sectionIndex >= sections.length) {
                advanceChunk(s);
            }
        }
    }

    private static boolean withinRange(State s, int chunkX, int chunkZ) {
        double x = chunkX * 16.0D + 8.0D - s.originX;
        double z = chunkZ * 16.0D + 8.0D - s.originZ;
        double extra = 12.0D;
        double r = s.range + extra;
        return x * x + z * z <= r * r;
    }

    private static void advanceChunk(State s) {
        s.sectionIndex = 0;
        s.cz++;
        if (s.cz > s.maxCZ) {
            s.cz = s.minCZ;
            s.cx++;
        }
    }

    private static void scanSection(ServerLevel level, State s, LevelChunk chunk, LevelChunkSection section, int baseY) {
        if (section.hasOnlyAir()) return;
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState bs = section.getBlockState(x, y, z);
                    if (!isDirectorLight(bs)) continue;

                    int wx = minX + x;
                    int wy = baseY + y;
                    int wz = minZ + z;
                    long dx = (long) wx - s.originX;
                    long dz = (long) wz - s.originZ;
                    if (dx * dx + dz * dz > (long)s.range * s.range) continue;

                    BlockPos pos = new BlockPos(wx, wy, wz);
                    s.lights.putIfAbsent(pos.immutable(), bs);

                    if (s.mode == Mode.BLACKOUT) {
                        setOff(level, s, pos, bs);
                    }
                }
            }
        }
    }

    private static boolean isDirectorLight(BlockState state) {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        if (state.getBlock() instanceof FireBlock) return false;
        return state.getLightEmission() >= 7;
    }

    private static BlockState offState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.LIT) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.LIT))) {
            return state.setValue(BlockStateProperties.LIT, false);
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    private static void setOff(ServerLevel level, State s, BlockPos pos, BlockState original) {
        if (s.toggledOff.add(pos.immutable())) {
            level.setBlock(pos, offState(original), 3);
        }
    }

    private static void flickerTick(ServerLevel level, State s) {
        s.ticker++;
        net.minecraft.world.entity.player.Player owner = s.owner == null ? null : level.getPlayerByUUID(s.owner);
        double speed = owner instanceof ServerPlayer sp ? getSpeed(sp) : 1.0D;
        int interval = Math.max(1, (int)Math.round(4.0D / speed));
        if (s.ticker % interval != 0) return;

        int count = Math.min(48, Math.max(4, s.lights.size() / 20));
        List<BlockPos> positions = new ArrayList<>(s.lights.keySet());
        for (int i = 0; i < count && !positions.isEmpty(); i++) {
            BlockPos pos = positions.get(s.random.nextInt(positions.size()));
            BlockState original = s.lights.get(pos);
            if (original == null) continue;

            if (s.toggledOff.remove(pos)) {
                level.setBlock(pos, original, 3);
            } else {
                s.toggledOff.add(pos);
                level.setBlock(pos, offState(original), 3);
            }
        }
    }

    private static void restoreTick(ServerLevel level, State s) {
        int budget = 512;
        while (budget-- > 0 && !s.restoreQueue.isEmpty()) {
            BlockPos pos = s.restoreQueue.pollFirst();
            BlockState original = s.lights.get(pos);
            if (original != null) level.setBlock(pos, original, 3);
        }
        if (s.restoreQueue.isEmpty()) {
            s.lights.clear();
            s.toggledOff.clear();
            s.mode = Mode.IDLE;
            s.owner = null;
        }
    }

    private static void restoreImmediate(ServerLevel level, State s) {
        if (s.lights.isEmpty()) return;
        for (Map.Entry<BlockPos, BlockState> e : s.lights.entrySet()) {
            if (s.toggledOff.contains(e.getKey()) || s.mode == Mode.BLACKOUT || s.mode == Mode.RESTORING) {
                level.setBlock(e.getKey(), e.getValue(), 3);
            }
        }
        s.lights.clear();
        s.toggledOff.clear();
        s.restoreQueue.clear();
        s.mode = Mode.IDLE;
        s.scanning = false;
    }
}

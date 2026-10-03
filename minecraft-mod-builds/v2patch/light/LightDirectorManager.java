package com.injaa.lightdirector.server;

import com.injaa.lightdirector.network.ControlPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LightDirectorManager {
    public static final LightDirectorManager INSTANCE = new LightDirectorManager();
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void schedule(ServerPlayer p, ControlPacket.Mode mode, int delay, int range) {
        Session s = sessions.computeIfAbsent(p.getUUID(), u -> new Session());
        s.playerId = p.getUUID();
        s.range = range;
        if (mode == ControlPacket.Mode.CANCEL) {
            s.pending = null;
            s.delayTicks = 0;
            return;
        }
        if (mode == ControlPacket.Mode.NORMAL) {
            s.pending = mode;
            s.delayTicks = delay * 20;
            if (delay == 0) start(p, s, mode);
            return;
        }
        prepareScan(p, s);
        s.pending = mode;
        s.delayTicks = delay * 20;
        if (delay == 0) start(p, s, mode);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (Session s : sessions.values()) {
            ServerPlayer p = findPlayer(s.playerId);
            if (p == null) continue;
            scanIncremental(p, s, 1);
            if (s.delayTicks > 0 && --s.delayTicks == 0 && s.pending != null) start(p, s, s.pending);
            if (s.mode == ControlPacket.Mode.FLICKER || s.mode == ControlPacket.Mode.FLICKER_BLACKOUT) {
                if (--s.flickerCooldown <= 0) {
                    s.flickerCooldown = 4 + random.nextInt(8);
                    s.flickerOn = !s.flickerOn;
                    if (s.flickerOn) applyOn(p.serverLevel(), s); else applyOff(p.serverLevel(), s);
                }
                if (s.mode == ControlPacket.Mode.FLICKER_BLACKOUT && --s.toBlackoutTicks <= 0) {
                    applyOff(p.serverLevel(), s);
                    s.mode = ControlPacket.Mode.BLACKOUT;
                }
            }
        }
    }

    private ServerPlayer findPlayer(UUID id) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.getUUID().equals(id)) return p;
        return null;
    }

    private void start(ServerPlayer p, Session s, ControlPacket.Mode mode) {
        s.pending = null;
        s.mode = mode;
        switch (mode) {
            case FLICKER -> { s.flickerCooldown = 1; s.flickerOn = true; }
            case FLICKER_BLACKOUT -> { s.flickerCooldown = 1; s.flickerOn = true; s.toBlackoutTicks = 100; }
            case BLACKOUT -> applyOff(p.serverLevel(), s);
            case ALL_ON -> applyOn(p.serverLevel(), s);
            case NORMAL -> { restore(p.serverLevel(), s); s.mode = ControlPacket.Mode.NORMAL; s.original.clear(); s.chunksToScan.clear(); }
            default -> { }
        }
    }

    private void prepareScan(ServerPlayer p, Session s) {
        s.original.clear();
        s.chunksToScan.clear();
        int cr = (s.range + 15) / 16;
        int pcx = p.chunkPosition().x, pcz = p.chunkPosition().z;
        List<long[]> ordered = new ArrayList<>();
        for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
            for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                if (!p.serverLevel().hasChunk(cx, cz)) continue;
                long d = (long)(cx - pcx) * (cx - pcx) + (long)(cz - pcz) * (cz - pcz);
                ordered.add(new long[]{d, pack(cx, cz)});
            }
        }
        ordered.sort(Comparator.comparingLong(a -> a[0]));
        for (long[] a : ordered) s.chunksToScan.addLast(a[1]);
    }

    private static long pack(int x, int z) { return (((long)x) << 32) ^ (z & 0xffffffffL); }

    private void scanIncremental(ServerPlayer p, Session s, int chunkBudget) {
        if (s.chunksToScan.isEmpty()) return;
        ServerLevel level = p.serverLevel();
        for (int n = 0; n < chunkBudget && !s.chunksToScan.isEmpty(); n++) {
            long key = s.chunksToScan.removeFirst();
            int cx = (int)(key >> 32), cz = (int)key;
            if (!level.hasChunk(cx, cz)) continue;
            int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
            int minX = cx << 4, minZ = cz << 4;
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (pos.distSqr(p.blockPosition()) > (double)s.range * s.range) continue;
                        BlockState st = level.getBlockState(pos);
                        if (!isLightCandidate(st, level, pos)) continue;
                        if (level.getBlockEntity(pos) != null && !st.hasProperty(BlockStateProperties.LIT) && !st.hasProperty(BlockStateProperties.POWERED)) continue;
                        if (s.original.putIfAbsent(pos.immutable(), st) == null) applyCurrentModeToNewLight(level, s, pos);
                    }
                }
            }
        }
    }

    private boolean isLightCandidate(BlockState s, ServerLevel level, BlockPos p) {
        return s.getLightEmission(level, p) > 0 || s.hasProperty(BlockStateProperties.LIT) || s.hasProperty(BlockStateProperties.POWERED);
    }

    private void applyCurrentModeToNewLight(ServerLevel level, Session s, BlockPos pos) {
        if (s.mode == ControlPacket.Mode.BLACKOUT || (s.mode == ControlPacket.Mode.FLICKER && !s.flickerOn) || (s.mode == ControlPacket.Mode.FLICKER_BLACKOUT && !s.flickerOn)) {
            setOff(level, pos, s.original.get(pos));
        } else if (s.mode == ControlPacket.Mode.ALL_ON || s.mode == ControlPacket.Mode.FLICKER || s.mode == ControlPacket.Mode.FLICKER_BLACKOUT) {
            setOn(level, pos, s.original.get(pos));
        }
    }

    private void applyOff(ServerLevel level, Session s) { s.original.forEach((p, orig) -> setOff(level, p, orig)); }
    private void applyOn(ServerLevel level, Session s) { s.original.forEach((p, orig) -> setOn(level, p, orig)); }

    private void setOff(ServerLevel level, BlockPos p, BlockState orig) {
        BlockState cur = level.getBlockState(p);
        if (cur.hasProperty(BlockStateProperties.LIT)) {
            level.setBlock(p, cur.setValue(BlockStateProperties.LIT, false), 2);
        } else if (cur.hasProperty(BlockStateProperties.POWERED)) {
            level.setBlock(p, cur.setValue(BlockStateProperties.POWERED, false), 2);
        } else if (cur.getLightEmission(level, p) > 0 && level.getBlockEntity(p) == null) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private void setOn(ServerLevel level, BlockPos p, BlockState orig) {
        if (orig == null) return;
        BlockState on = orig;
        if (on.hasProperty(BlockStateProperties.LIT)) on = on.setValue(BlockStateProperties.LIT, true);
        else if (on.hasProperty(BlockStateProperties.POWERED)) on = on.setValue(BlockStateProperties.POWERED, true);
        level.setBlock(p, on, 2);
    }

    private void restore(ServerLevel level, Session s) { s.original.forEach((p, st) -> level.setBlock(p, st, 2)); }

    private static class Session {
        UUID playerId;
        int range = 1000, delayTicks, flickerCooldown, toBlackoutTicks;
        boolean flickerOn = true;
        ControlPacket.Mode pending, mode = ControlPacket.Mode.NORMAL;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final ArrayDeque<Long> chunksToScan = new ArrayDeque<>();
    }
}

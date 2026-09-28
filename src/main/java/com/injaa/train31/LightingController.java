package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;

/**
 * Real station-light controller. It snapshots the station lights once, so flickers do not repeatedly scan
 * the whole map. At 11:48 the saved lights can stay physically OFF until reset/end.
 */
public final class LightingController {
    private LightingController() {}

    private record Saved(BlockPos pos, BlockState state) {}
    private static final class SceneLights {
        final List<Saved> lights;
        boolean off;
        SceneLights(List<Saved> lights){this.lights=lights;}
    }

    private static final Map<UUID, SceneLights> SCENES = new HashMap<>();

    public static void pulse(ServerPlayer player, boolean off) {
        SceneLights scene = SCENES.computeIfAbsent(player.getUUID(), id -> capture(player));
        if (scene.off == off) return;
        ServerLevel level = player.serverLevel();
        if (off) {
            for (Saved s : scene.lights) {
                BlockState current = level.getBlockState(s.pos());
                BlockState original = s.state();
                if (original.hasProperty(BlockStateProperties.LIT)) {
                    level.setBlock(s.pos(), original.setValue(BlockStateProperties.LIT, false), 2);
                } else if (current.getLightEmission(level, s.pos()) > 0 || original.getLightEmission(level, s.pos()) > 0) {
                    level.setBlock(s.pos(), Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
                }
            }
        } else {
            for (Saved s : scene.lights) level.setBlock(s.pos(), s.state(), 2);
        }
        scene.off = off;
    }

    /** Keep all captured station lights physically off. */
    public static void blackout(ServerPlayer player) {
        pulse(player, true);
    }

    private static SceneLights capture(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        StationBuilder.RailGeometry g = StationBuilder.geometry(player);
        BlockPos r = g.rail();
        List<Saved> list = new ArrayList<>();

        // Larger than the old scan: full platform, opposite platform edge, ceiling and tunnel mouth.
        int along = 90, side = 26;
        outer:
        for (int a=-along; a<=along; a++) {
            for (int s=-side; s<=side; s++) {
                for (int y=-1; y<=15; y++) {
                    BlockPos p = g.axisZ() ? r.offset(s,y,a) : r.offset(a,y,s);
                    BlockState st = level.getBlockState(p);
                    if (st.getLightEmission(level,p) < 5) continue;
                    list.add(new Saved(p.immutable(), st));
                    if (list.size() >= 1200) break outer;
                }
            }
        }
        return new SceneLights(list);
    }

    public static void restore(ServerPlayer player) {
        SceneLights scene = SCENES.remove(player.getUUID());
        if (scene == null) return;
        ServerLevel level = player.serverLevel();
        for (Saved s : scene.lights) level.setBlock(s.pos(), s.state(), 2);
    }
}

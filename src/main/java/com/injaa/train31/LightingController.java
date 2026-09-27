package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;

/** Temporarily dims real light-emitting blocks around the detected platform and restores them exactly. */
public final class LightingController {
    private LightingController() {}
    private record Saved(BlockPos pos, BlockState state) {}
    private static final Map<UUID,List<Saved>> SAVED = new HashMap<>();

    public static void pulse(ServerPlayer player, boolean off) {
        if (off) darken(player); else restore(player);
    }

    private static void darken(ServerPlayer player) {
        if (SAVED.containsKey(player.getUUID())) return;
        ServerLevel level=player.serverLevel();
        StationBuilder.RailGeometry g=StationBuilder.geometry(player);
        BlockPos r=g.rail();
        List<Saved> list=new ArrayList<>();
        int along=34, side=10;
        for(int a=-along;a<=along;a++) for(int s=-side;s<=side;s++) for(int y=1;y<=7;y++) {
            BlockPos p=g.axisZ()?r.offset(s,y,a):r.offset(a,y,s);
            BlockState st=level.getBlockState(p);
            if(st.getLightEmission(level,p)<9) continue;
            list.add(new Saved(p.immutable(),st));
            if(st.getBlock() instanceof RedStoneLampBlock && st.hasProperty(BlockStateProperties.LIT))
                level.setBlock(p,st.setValue(BlockStateProperties.LIT,false),2);
            else
                level.setBlock(p,Blocks.GRAY_STAINED_GLASS.defaultBlockState(),2);
            if(list.size()>=72) break;
        }
        SAVED.put(player.getUUID(),list);
    }

    public static void restore(ServerPlayer player) {
        List<Saved> list=SAVED.remove(player.getUUID()); if(list==null)return;
        ServerLevel level=player.serverLevel();
        for(Saved s:list) level.setBlock(s.pos(),s.state(),2);
    }
}

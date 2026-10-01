package com.injaa.villagedawn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Stored in the current world/dimension so stopping, logging out or dying cannot lose reset data. */
public class StoryData extends SavedData {
    public CompoundTag data=new CompoundTag();
    public static StoryData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(StoryData::load,StoryData::new,"villagedawn_director");}
    public static StoryData load(CompoundTag n){StoryData d=new StoryData();d.data=n.getCompound("Story");return d;}
    @Override public CompoundTag save(CompoundTag n){n.put("Story",data);return n;}
}

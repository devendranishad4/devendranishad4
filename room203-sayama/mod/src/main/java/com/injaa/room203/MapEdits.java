package com.injaa.room203;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.saveddata.SavedData;

/** Changes only original props we introduce; retains original states and block entity data. */
public final class MapEdits extends SavedData {
 public static final BlockPos DOOR=new BlockPos(671,36,-200),CHEST=new BlockPos(667,36,-202),UP_CHEST=new BlockPos(667,41,-202);
 public static final BlockPos START=new BlockPos(680,30,-166),EXIT=new BlockPos(682,30,-150),STAIR=new BlockPos(671,30,-181);
 public static final BlockPos[] LIGHTS={new BlockPos(668,38,-200),new BlockPos(673,38,-194),new BlockPos(673,43,-194)};
 private CompoundTag original=new CompoundTag();
 public boolean installed;
 public static MapEdits get(ServerLevel w){return w.getDataStorage().computeIfAbsent(MapEdits::load,MapEdits::new,"room203_edits");}
 public static MapEdits load(CompoundTag t){MapEdits s=new MapEdits();s.original=t.getCompound("original");s.installed=t.getBoolean("installed");return s;}
 @Override public CompoundTag save(CompoundTag t){t.put("original",original);t.putBoolean("installed",installed);return t;}
 public static boolean correctMap(ServerLevel w){w.getChunkAt(DOOR);return w.getBlockState(DOOR).is(Blocks.OAK_DOOR)&&w.getBlockState(CHEST).is(Blocks.CHEST)&&w.getBlockState(UP_CHEST).is(Blocks.CHEST)&&w.getBlockState(new BlockPos(672,37,-201)).getBlock() instanceof SignBlock;}
 public void remember(ServerLevel w,BlockPos p){
  String key=Long.toString(p.asLong());if(original.contains(key))return;
  CompoundTag t=new CompoundTag();t.put("state",NbtUtils.writeBlockState(w.getBlockState(p)));
  var be=w.getBlockEntity(p);if(be!=null)t.put("entity",be.saveWithFullMetadata());original.put(key,t);setDirty();
 }
 public void change(ServerLevel w,BlockPos p,BlockState b){remember(w,p);w.setBlock(p,b,2);setDirty();}
 public void install(ServerLevel w){
  if(installed)return;
  for(BlockPos p:LIGHTS)change(w,p,Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL,15));
  // Add a low bedside light; retain the detailed original furniture and facade.
  change(w,new BlockPos(665,36,-201),Blocks.LANTERN.defaultBlockState());
  remember(w,DOOR);remember(w,DOOR.above());
  installed=true;setDirty();
 }
 public void openRoute(ServerLevel w){
  for(int y:new int[]{36,37}){BlockPos p=new BlockPos(667,y,-174);BlockState s=w.getBlockState(p);if(s.hasProperty(BlockStateProperties.OPEN))change(w,p,s.setValue(BlockStateProperties.OPEN,true));}
 }
 public void lighting(ServerLevel w,boolean on){for(BlockPos p:LIGHTS)change(w,p,Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL,on?15:0));
  change(w,new BlockPos(665,36,-201),on?Blocks.LANTERN.defaultBlockState():Blocks.AIR.defaultBlockState());}
 public void door(ServerLevel w,boolean open){for(BlockPos p:new BlockPos[]{DOOR,DOOR.above()}){BlockState s=w.getBlockState(p);if(s.hasProperty(BlockStateProperties.OPEN))change(w,p,s.setValue(BlockStateProperties.OPEN,open));}}
 public void restore(ServerLevel w){
  for(String key:original.getAllKeys()){
   BlockPos p=BlockPos.of(Long.parseLong(key));CompoundTag t=original.getCompound(key);
   w.setBlock(p,NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),t.getCompound("state")),2);
   if(t.contains("entity")){var be=w.getBlockEntity(p);if(be!=null){be.load(t.getCompound("entity"));be.setChanged();}}
  }
  original=new CompoundTag();installed=false;setDirty();
 }
 public int snapshotCount(){return original.size();}
}

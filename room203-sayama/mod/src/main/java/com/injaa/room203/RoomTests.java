package com.injaa.room203;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
@GameTestHolder(Room203.ID)
public final class RoomTests {
 @GameTest(template="empty",timeoutTicks=200)
 public static void reversibleEditsAndActor(GameTestHelper h){
  var w=h.getLevel();var edit=MapEdits.get(w);edit.restore(w);
  h.assertTrue(!MapEdits.correctMap(w),"Unrelated world must fail fingerprint");
  var door=Blocks.OAK_DOOR.defaultBlockState();w.setBlock(MapEdits.DOOR,door,2);w.setBlock(MapEdits.DOOR.above(),door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),2);
  w.setBlock(MapEdits.CHEST,Blocks.CHEST.defaultBlockState(),2);w.setBlock(MapEdits.UP_CHEST,Blocks.CHEST.defaultBlockState(),2);w.setBlock(new BlockPos(672,37,-201),Blocks.OAK_WALL_SIGN.defaultBlockState(),2);
  var chest=(ChestBlockEntity)w.getBlockEntity(MapEdits.CHEST);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
  h.assertTrue(MapEdits.correctMap(w),"Expected apartment fingerprint accepted");
  var marker=new BlockPos(665,36,-201);w.setBlock(marker,Blocks.STONE.defaultBlockState(),2);
  edit.install(w);edit.install(w);h.assertTrue(edit.snapshotCount()==6,"Install is idempotent and captures exactly introduced changes");
  edit.lighting(w,false);edit.door(w,true);h.assertTrue(w.getBlockState(MapEdits.DOOR).getValue(BlockStateProperties.OPEN),"Story opens original door");
  edit.restore(w);h.assertTrue(w.getBlockState(marker).is(Blocks.STONE),"Original bedside block restored");h.assertTrue(!w.getBlockState(MapEdits.DOOR).getValue(BlockStateProperties.OPEN),"Original door state restored");
  h.assertTrue(((ChestBlockEntity)w.getBlockEntity(MapEdits.CHEST)).getItem(0).getCount()==7,"Furniture chest contents preserved");
  h.assertTrue(w.getEntitiesOfClass(ItemEntity.class,new AABB(660,25,-210,690,50,-140)).isEmpty(),"Editing creates no dropped items");
  Visitor v=Room203.VISITOR.get().create(w);h.assertTrue(v!=null,"Actor entity registered");h.assertTrue(!v.doHurtTarget(h.makeMockPlayer()),"Actor never damages recording player");v.discard();
  h.assertTrue(Room203.AUDIO_EVENTS.size()==8&&Room203.AUDIO_EVENTS.values().stream().allMatch(x->x.isPresent()),"All story sounds registered");h.succeed();
 }
}

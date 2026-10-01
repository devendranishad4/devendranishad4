package com.injaa.villagedawn;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.*;

@GameTestHolder(VillageDawn.ID)
@PrefixGameTestTemplate(false)
public class DirectorGameTests{
    @GameTest(template="empty",timeoutTicks=100)
    public static void restoresChestContentsAndLimitsEdits(GameTestHelper h){
        var l=h.getLevel();BlockPos a=h.absolutePos(new BlockPos(2,2,2)),untouched=a.offset(2,0,0);
        l.setBlock(a,Blocks.CHEST.defaultBlockState(),3);l.setBlock(untouched,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        ((ChestBlockEntity)l.getBlockEntity(a)).setItem(7,new ItemStack(Items.EMERALD,9));
        Director.setRemembered(l,a,Blocks.BARREL.defaultBlockState());
        Director.setRemembered(l,a,Blocks.BELL.defaultBlockState());
        if(Director.state(l).getList("changes",10).size()!=1)throw new AssertionError("Repeated write lost first snapshot");
        Director.restore(l);
        if(!l.getBlockState(a).is(Blocks.CHEST))throw new AssertionError("Original chest not restored");
        var stack=((ChestBlockEntity)l.getBlockEntity(a)).getItem(7);
        if(!stack.is(Items.EMERALD)||stack.getCount()!=9)throw new AssertionError("Original contents lost");
        if(!l.getBlockState(untouched).is(Blocks.DIAMOND_BLOCK))throw new AssertionError("Unrelated block changed");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void safeMarkerRejectsBlockedHeadroom(GameTestHelper h){
        var l=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(5,2,5));
        l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(p,Blocks.AIR.defaultBlockState(),3);l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),3);
        if(!Director.safe(l,p))throw new AssertionError("Clear marker rejected");
        l.setBlock(p.above(),Blocks.STONE.defaultBlockState(),3);
        if(Director.safe(l,p))throw new AssertionError("Blocked marker accepted");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void callerIsRegisteredAndGroundBound(GameTestHelper h){
        var c=VillageDawn.CALLER.get().create(h.getLevel());
        if(c==null||c.isNoGravity()||!c.isNoAi())throw new AssertionError("Caller initial behaviour unsafe");
        if(!(c.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation))throw new AssertionError("Caller is not using ground navigation");
        if(c.doHurtTarget(c))throw new AssertionError("Caller damaged target in recording mode");
        h.succeed();
    }
}

package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;

/** Runs in a real Forge game-test server, rather than only compiling Java. */
@GameTestHolder(LastElevator.ID)
@PrefixGameTestTemplate(false)
public final class TowerGameTests {
    private TowerGameTests(){}

    @GameTest(template="empty",timeoutTicks=200)
    public static void cabinAndShutters(GameTestHelper helper){
        var w=helper.getLevel();
        TowerLift.install(w);
        if(!w.getBlockState(new BlockPos(-233,75,100)).is(Blocks.STONE_BUTTON))
            throw new AssertionError("Lobby cabin button missing");
        TowerLift.door(w,74,true);
        if(!w.getBlockState(new BlockPos(-236,75,100)).is(Blocks.IRON_BLOCK))
            throw new AssertionError("Lobby shutter did not close");
        CompoundTag state=new CompoundTag();state.putInt("carFloor",74);
        TowerLift.position(w,state,99);
        if(!w.getBlockState(new BlockPos(-233,100,100)).is(Blocks.STONE_BUTTON)
                ||!w.getBlockState(new BlockPos(-233,75,100)).isAir())
            throw new AssertionError("Cabin did not change floor");
        if(!w.getBlockState(new BlockPos(-236,100,100)).isAir())
            throw new AssertionError("Hotel shutter did not open");
        helper.succeed();
    }
}

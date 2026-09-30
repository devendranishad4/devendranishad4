package com.injaa.room203;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class Visitor extends Monster {
 private boolean hunting;
 public Visitor(EntityType<? extends Monster> t,Level w){super(t,w);setPersistenceRequired();setMaxUpStep(1f);if(getNavigation() instanceof GroundPathNavigation nav)nav.setCanOpenDoors(true);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this,true));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.08,true){@Override public boolean canUse(){return hunting&&super.canUse();}@Override public boolean canContinueToUse(){return hunting&&super.canContinueToUse();}});}
 public void hunt(LivingEntity p){hunting=true;setNoAi(false);setTarget(p);}
 public void watch(){hunting=false;setTarget(null);getNavigation().stop();}
 @Override public void tick(){super.tick();if(!hunting&&getTarget()!=null)getLookControl().setLookAt(getTarget(),30,30);}
 @Override public boolean doHurtTarget(Entity p){return false;}
 
 @Override protected boolean shouldDespawnInPeaceful(){return false;}
 @Override public boolean removeWhenFarAway(double d){return false;}
 @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("Room203Hunting",hunting);}
 @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);hunting=t.getBoolean("Room203Hunting");}
}

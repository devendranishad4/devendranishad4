package com.injaa.villagedawn;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;

/** Only the story owner is pursued. No natural spawning, teleporting or lethal hits. */
public class Caller extends Monster {
    private UUID owner;
    private boolean pursuing;
    private int breathCooldown;
    public Caller(EntityType<? extends Monster> t,Level l){super(t,l);setPersistenceRequired();setNoAi(true);if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g)g.setCanOpenDoors(true);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this,true));}
    public void direct(UUID owner,boolean chase){this.owner=owner;this.pursuing=chase;setNoAi(!chase);if(!chase)getNavigation().stop();}
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e){return false;}
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide||!pursuing||owner==null)return;
        Player p=((ServerLevel)level()).getPlayerByUUID(owner);
        if(p==null||p.isSpectator()||!p.isAlive()){getNavigation().stop();return;}
        double d=distanceToSqr(p);
        if(d<2.5)getNavigation().stop();
        else if(tickCount%10==0)getNavigation().moveTo(p,1.12);
        if(d<64&&--breathCooldown<=0){level().playSound(null,blockPosition(),VillageDawn.BREATH.get(),SoundSource.HOSTILE,.55f,1);breathCooldown=100;}
    }
    @Override public void addAdditionalSaveData(CompoundTag d){super.addAdditionalSaveData(d);if(owner!=null)d.putUUID("DirectorOwner",owner);d.putBoolean("Pursuing",pursuing);}
    @Override public void readAdditionalSaveData(CompoundTag d){super.readAdditionalSaveData(d);owner=d.hasUUID("DirectorOwner")?d.getUUID("DirectorOwner"):null;pursuing=d.getBoolean("Pursuing");setNoAi(!pursuing);}
}

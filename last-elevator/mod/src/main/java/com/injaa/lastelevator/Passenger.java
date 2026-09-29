package com.injaa.lastelevator;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Recording-only pursuer: follows the player but never damages them. */
public class Passenger extends Monster {
    public Passenger(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,true));
        goalSelector.addGoal(3,new LookAtPlayerGoal(this,Player.class,18));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Filming must not end in an accidental death; closeness is the scare.
        return false;
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide) return;
        Player player=level().getNearestPlayer(this,35);
        if(player==null||player.isSpectator())return;
        double d=distanceToSqr(player);
        if(d<3.25) getNavigation().stop();
        else if(player.isCreative()&&d<625&&tickCount%20==0) getNavigation().moveTo(player,1.14);
    }
}

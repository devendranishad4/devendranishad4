package com.injaa.train31;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class ShadowGirlEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TALL = SynchedEntityData.defineId(ShadowGirlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FINAL_FORM = SynchedEntityData.defineId(ShadowGirlEntity.class, EntityDataSerializers.BOOLEAN);

    public ShadowGirlEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setSilent(true);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TALL,false);
        this.entityData.define(FINAL_FORM,false);
    }

    public void setTall(boolean value){this.entityData.set(TALL,value);}
    public boolean isTall(){return this.entityData.get(TALL);}
    public void setFinalForm(boolean value){this.entityData.set(FINAL_FORM,value);}
    public boolean isFinalForm(){return this.entityData.get(FINAL_FORM);}

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // StoryDirector controls every movement so the actor never breaks scene timing.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }
}

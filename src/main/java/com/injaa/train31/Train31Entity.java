package com.injaa.train31;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Lightweight cinematic commuter train entity. The server only moves one entity;
 * the detailed Tokyo-style body is rendered client-side.
 */
public class Train31Entity extends Entity {
    private static final EntityDataAccessor<Boolean> DOORS_OPEN =
            SynchedEntityData.defineId(Train31Entity.class, EntityDataSerializers.BOOLEAN);

    public Train31Entity(EntityType<? extends Train31Entity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DOORS_OPEN, false);
    }

    public void setDoorsOpen(boolean open) { this.entityData.set(DOORS_OPEN, open); }
    public boolean doorsOpen() { return this.entityData.get(DOORS_OPEN); }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("DoorsOpen")) setDoorsOpen(tag.getBoolean("DoorsOpen"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("DoorsOpen", doorsOpen());
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }
}

package com.devendra.slenderv2;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class SlenderEntity extends Monster {
    public static final int MODE_STALK = 0;
    public static final int MODE_HUNT = 1;
    public static final int MODE_STOP = 2;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(SlenderEntity.class, EntityDataSerializers.INT);

    private int stareTicks;
    private int teleportCooldown;
    private int attackCooldown;

    public SlenderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODE, MODE_STALK);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.50D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 80.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    public void setMode(int mode) {
        this.entityData.set(MODE, mode);
        if (mode != MODE_HUNT) this.setTarget(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (teleportCooldown > 0) teleportCooldown--;
        if (attackCooldown > 0) attackCooldown--;

        Player player = this.level().getNearestPlayer(this, 96.0D);
        if (player == null || player.isSpectator()) {
            this.setTarget(null);
            return;
        }

        int mode = getMode();
        double distance = this.distanceTo(player);

        if (mode == MODE_STOP) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.lookAt(player, 30.0F, 30.0F);
            return;
        }

        boolean looking = isPlayerLookingAtMe(player);
        if (looking && distance < 42.0D) {
            stareTicks++;
            if (stareTicks % 18 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 14, 0, false, false));
                player.playSound(SoundEvents.ENDERMAN_STARE, 0.20F, 0.55F);
            }
            if (stareTicks >= 34 && teleportCooldown <= 0) {
                teleportBehind(player);
                stareTicks = 0;
                teleportCooldown = 70;
            }
        } else {
            stareTicks = Math.max(0, stareTicks - 2);
        }

        if (mode == MODE_STALK) {
            this.setTarget(null);
            if (distance < 9.0D && teleportCooldown <= 0) {
                teleportBehind(player);
                teleportCooldown = 85;
            } else if (distance > 48.0D) {
                this.getNavigation().moveTo(player, 0.72D);
            } else {
                this.getNavigation().stop();
                this.lookAt(player, 35.0F, 35.0F);
            }
        } else if (mode == MODE_HUNT) {
            this.setTarget(player);
            if (distance > 2.3D) {
                this.getNavigation().moveTo(player, 1.26D);
            } else if (attackCooldown <= 0) {
                this.doHurtTarget(player);
                attackCooldown = 20;
            }
        }
    }

    private boolean isPlayerLookingAtMe(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        return view.dot(toMe) > 0.93D && player.hasLineOfSight(this);
    }

    public boolean teleportBehind(Player player) {
        if (!(this.level() instanceof ServerLevel server)) return false;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();
        Vec3 right = new Vec3(-horizontal.z, 0.0D, horizontal.x);

        double backDistance = 11.0D + this.random.nextInt(9);
        double side = (this.random.nextDouble() - 0.5D) * 8.0D;
        Vec3 target = player.position().subtract(horizontal.scale(backDistance)).add(right.scale(side));

        int x = (int)Math.floor(target.x);
        int z = (int)Math.floor(target.z);
        int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        BlockPos pos = new BlockPos(x, y, z);

        if (!server.getWorldBorder().isWithinBounds(pos)) return false;

        this.teleportTo(x + 0.5D, y, z + 0.5D);
        this.getNavigation().stop();
        this.lookAt(player, 180.0F, 180.0F);
        server.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 0.45F, 0.48F);
        return true;
    }

    public void scare(Player player) {
        if (teleportBehind(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 22, 0, false, false));
            player.playSound(SoundEvents.ENDERMAN_STARE, 0.35F, 0.48F);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0));
            living.knockback(0.9D, this.getX() - living.getX(), this.getZ() - living.getZ());
        }
        return hit;
    }
}

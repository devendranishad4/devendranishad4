package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Physical ghost powers used by the Train 31 story.
 * Keeps the scares gameplay-first: real knockback, short knockdowns and directional shoves.
 */
public final class GhostPowerController {
    private GhostPowerController() {}

    private static final String KNOCKDOWN_TICKS = "train31_ghost_knockdown_ticks";
    private static final String LAST_MICRO_BEAT = "train31_ghost_last_micro_beat";
    private static final String ENTRANCE_SEALED = "train31_entrance_sealed";

    public static void tick(ServerPlayer p) {
        tickKnockdown(p);

        if (!StoryDirector.isRunning(p)) {
            p.getPersistentData().remove(LAST_MICRO_BEAT);
            restoreEntrance(p);
            return;
        }

        int t = StoryDirector.currentTick(p);

        // The generated Train 31 is fully disabled during the episode.
        // StoryDirector may still call its old spawn/build methods, but this immediately
        // removes/restores them on the same server tick. Only the map's existing train remains.
        if (t >= 10400) {
            StationBuilder.removeTrain(p);
            if (PhysicalTrainBuilder.exists(p)) PhysicalTrainBuilder.restore(p);
        }

        // 11:47 PM — seal the route back toward the entrance early,
        // so once the horror properly begins the player cannot simply escape upstairs.
        if (t >= 2400 && !p.getPersistentData().getBoolean(ENTRANCE_SEALED)) {
            sealEntrance(p);
            playAt(p, SceneSetup.start(p), Train31Mod.METAL_KNOCKS.get(), 2.2f, 0.62f);
            playAt(p, SceneSetup.start(p), Train31Mod.HORROR_HIT.get(), 1.15f, 0.88f);
        }

        // After 11:48, fill the gaps between the existing 20-second story beats.
        // Every 10 seconds there is a sound/power beat, but repeated "hello/behind you"
        // voice spam is intentionally avoided here. Dialogue stays for the major scenes.
        if (t < 3800 || t >= 17600 || t % 400 != 200) return;

        int beat = t / 200;
        if (p.getPersistentData().getInt(LAST_MICRO_BEAT) == beat) return;
        p.getPersistentData().putInt(LAST_MICRO_BEAT, beat);
        microBeat(p, beat);
    }

    private static void tickKnockdown(ServerPlayer p) {
        int left = p.getPersistentData().getInt(KNOCKDOWN_TICKS);
        if (left <= 0) return;

        p.setSprinting(false);
        p.setForcedPose(Pose.SWIMMING);
        p.getPersistentData().putInt(KNOCKDOWN_TICKS, left - 1);

        if (left == 1) {
            p.setForcedPose(null);
            p.getPersistentData().remove(KNOCKDOWN_TICKS);
        }
    }

    /** Pushes the player in the direction they are facing, as if the ghost hit them from behind. */
    public static void pushFromBehind(ServerPlayer p, double strength, boolean knockDown) {
        Vec3 look = flatLook(p);
        applyPush(p, look, strength, knockDown);
    }

    /** Sudden sideways shove. Useful near the platform edge because the player can genuinely fall. */
    public static void pushSideways(ServerPlayer p, double strength, boolean right, boolean knockDown) {
        Vec3 look = flatLook(p);
        Vec3 side = right ? new Vec3(-look.z, 0, look.x) : new Vec3(look.z, 0, -look.x);
        applyPush(p, side, strength, knockDown);
    }

    /** Pulls the player toward a world position without teleporting them. */
    public static void pullToward(ServerPlayer p, Vec3 target, double strength, boolean knockDown) {
        Vec3 delta = target.subtract(p.position());
        Vec3 flat = new Vec3(delta.x, 0, delta.z);
        if (flat.lengthSqr() < 0.01) return;
        applyPush(p, flat.normalize(), strength, knockDown);
    }

    private static void applyPush(ServerPlayer p, Vec3 direction, double strength, boolean knockDown) {
        Vec3 d = direction.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : direction.normalize();
        double up = knockDown ? 0.34 : 0.16;

        p.setDeltaMovement(p.getDeltaMovement().add(d.x * strength, up, d.z * strength));
        p.hurtMarked = true;
        p.fallDistance = 0.0F;

        if (knockDown) {
            p.getPersistentData().putInt(KNOCKDOWN_TICKS, 26);
            p.setForcedPose(Pose.SWIMMING);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 32, 3, false, false));
        }
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** Additional 10-second scare between the larger scripted moments. */
    private static void microBeat(ServerPlayer p, int beat) {
        int phase = Math.floorMod(beat, 8);
        switch (phase) {
            case 0 -> {
                play(p, Train31Mod.FEMALE_BREATH.get(), 1.25f, 0.94f);
                pushFromBehind(p, 0.30, false);
            }
            case 1 -> {
                play(p, Train31Mod.HORROR_SWELL.get(), 1.35f, 0.86f);
                play(p, Train31Mod.FEMALE_BREATH.get(), 1.05f, 0.82f);
            }
            case 2 -> {
                play(p, Train31Mod.HORROR_HIT.get(), 1.45f, 0.92f);
                pushSideways(p, 0.95, true, true);
            }
            case 3 -> {
                play(p, Train31Mod.CCTV_STATIC.get(), 1.05f, 0.70f);
                play(p, Train31Mod.HORROR_HIT.get(), 1.05f, 1.06f);
            }
            case 4 -> {
                play(p, Train31Mod.FEMALE_BREATH.get(), 1.45f, 0.88f);
                pushSideways(p, 0.42, false, false);
            }
            case 5 -> {
                play(p, Train31Mod.METAL_KNOCKS.get(), 1.30f, 0.58f);
                pullToward(p, Vec3.atCenterOf(SceneSetup.rail(p)), 0.52, false);
            }
            case 6 -> {
                play(p, Train31Mod.HORROR_HIT.get(), 1.20f, 1.04f);
                pushFromBehind(p, 0.72, true);
            }
            default -> {
                play(p, Train31Mod.HORROR_SWELL.get(), 1.10f, 0.74f);
                play(p, Train31Mod.GIRL_ROAR.get(), 0.42f, 1.13f);
            }
        }
    }

    private static void sealEntrance(ServerPlayer p) {
        BlockPos start = SceneSetup.start(p);
        BlockPos platform = SceneSetup.platform(p);
        int dx = platform.getX() - start.getX();
        int dz = platform.getZ() - start.getZ();
        boolean travelAlongX = Math.abs(dx) >= Math.abs(dz);

        // Five blocks wide and three blocks high. Only air is replaced, so the map
        // itself is not damaged. Barrier blocks make it feel like the exit has locked.
        for (int side = -2; side <= 2; side++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = travelAlongX ? start.offset(0, y, side) : start.offset(side, y, 0);
                if (p.serverLevel().getBlockState(pos).isAir()) {
                    p.serverLevel().setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                }
            }
        }
        p.getPersistentData().putBoolean(ENTRANCE_SEALED, true);
    }

    private static void restoreEntrance(ServerPlayer p) {
        if (!p.getPersistentData().getBoolean(ENTRANCE_SEALED)) return;
        if (!SceneSetup.complete(p)) {
            p.getPersistentData().remove(ENTRANCE_SEALED);
            return;
        }

        BlockPos start = SceneSetup.start(p);
        BlockPos platform = SceneSetup.platform(p);
        int dx = platform.getX() - start.getX();
        int dz = platform.getZ() - start.getZ();
        boolean travelAlongX = Math.abs(dx) >= Math.abs(dz);

        for (int side = -2; side <= 2; side++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = travelAlongX ? start.offset(0, y, side) : start.offset(side, y, 0);
                if (p.serverLevel().getBlockState(pos).is(Blocks.BARRIER)) {
                    p.serverLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
        p.getPersistentData().remove(ENTRANCE_SEALED);
    }

    private static void play(ServerPlayer p, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        p.serverLevel().playSound(null, p.blockPosition(), sound, SoundSource.AMBIENT, volume, pitch);
    }

    private static void playAt(ServerPlayer p, BlockPos pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        p.serverLevel().playSound(null, pos, sound, SoundSource.AMBIENT, volume, pitch);
    }

    public static void reset(ServerPlayer p) {
        restoreEntrance(p);
        p.setForcedPose(null);
        p.getPersistentData().remove(KNOCKDOWN_TICKS);
        p.getPersistentData().remove(LAST_MICRO_BEAT);
        p.getPersistentData().remove(ENTRANCE_SEALED);
        StationBuilder.removeTrain(p);
        if (PhysicalTrainBuilder.exists(p)) PhysicalTrainBuilder.restore(p);
    }
}

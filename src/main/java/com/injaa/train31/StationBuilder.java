package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class StationBuilder {
    private StationBuilder() {}

    public static final BlockPos TOKYO_ANCHOR = new BlockPos(-92, 61, 213);
    private static final String TRAIN_TAG = "train31_train";
    private static final String DOOR_TAG = "train31_door";

    public static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = TOKYO_ANCHOR;
        player.getPersistentData().putLong("train31_origin", origin.asLong());

        removeTrain(level, origin);
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 0.05, origin.getZ() + 0.5, 0.0F, 0.0F);
        player.sendSystemMessage(Component.literal("§a[Train 31: Tokyo Edition] §fLinked to the REAL Tokyo subway at §e-92 61 213§f."));
        player.sendSystemMessage(Component.literal("§7Tokyo city/station blocks are untouched. Crouch + right-click Director when ready."));
    }

    public static void setRedMode(ServerLevel level, BlockPos origin, boolean red) {
        // Non-destructive by design in Tokyo Edition.
    }

    public static void buildTrain(ServerLevel level, BlockPos origin) {
        removeTrain(level, origin);

        double baseX = origin.getX() + 5.2;
        double baseY = origin.getY() + 0.15;
        double baseZ = origin.getZ() + 7.0;

        for (int coach = 0; coach < 3; coach++) {
            double cz = baseZ + coach * 8.2;

            display(level, baseX, baseY + 1.55, cz, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3.6f, 2.9f, 7.7f, TRAIN_TAG);
            display(level, baseX, baseY + 3.10, cz, Blocks.SMOOTH_STONE.defaultBlockState(), 3.75f, 0.22f, 7.85f, TRAIN_TAG);
            display(level, baseX, baseY + 0.05, cz, Blocks.DEEPSLATE_TILES.defaultBlockState(), 3.55f, 0.18f, 7.65f, TRAIN_TAG);

            display(level, baseX - 1.88, baseY + 1.85, cz, Blocks.BLACK_STAINED_GLASS.defaultBlockState(), 0.12f, 1.15f, 5.3f, TRAIN_TAG);
            display(level, baseX + 1.88, baseY + 1.85, cz, Blocks.BLACK_STAINED_GLASS.defaultBlockState(), 0.12f, 1.15f, 5.3f, TRAIN_TAG);

            display(level, baseX - 1.96, baseY + 1.05, cz, Blocks.RED_CONCRETE.defaultBlockState(), 0.10f, 0.34f, 7.25f, TRAIN_TAG);
            display(level, baseX + 1.96, baseY + 1.05, cz, Blocks.RED_CONCRETE.defaultBlockState(), 0.10f, 0.34f, 7.25f, TRAIN_TAG);

            display(level, baseX - 2.02, baseY + 1.45, cz - 1.3, Blocks.IRON_BLOCK.defaultBlockState(), 0.12f, 2.35f, 1.45f, TRAIN_TAG, DOOR_TAG);
            display(level, baseX - 2.02, baseY + 1.45, cz + 1.3, Blocks.IRON_BLOCK.defaultBlockState(), 0.12f, 2.35f, 1.45f, TRAIN_TAG, DOOR_TAG);

            display(level, baseX, baseY + 2.55, cz, Blocks.SEA_LANTERN.defaultBlockState(), 2.4f, 0.08f, 5.5f, TRAIN_TAG);
        }

        double frontZ = baseZ - 4.15;
        display(level, baseX, baseY + 1.55, frontZ, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3.6f, 2.9f, 0.30f, TRAIN_TAG);
        display(level, baseX - 0.95, baseY + 1.15, frontZ - 0.18, Blocks.SEA_LANTERN.defaultBlockState(), 0.45f, 0.45f, 0.12f, TRAIN_TAG);
        display(level, baseX + 0.95, baseY + 1.15, frontZ - 0.18, Blocks.SEA_LANTERN.defaultBlockState(), 0.45f, 0.45f, 0.12f, TRAIN_TAG);
        display(level, baseX, baseY + 2.35, frontZ - 0.18, Blocks.RED_CONCRETE.defaultBlockState(), 1.2f, 0.32f, 0.12f, TRAIN_TAG);
    }

    public static void openTrainDoors(ServerLevel level, BlockPos origin) {
        AABB box = new AABB(origin).inflate(80.0);
        List<Display.BlockDisplay> entities = level.getEntitiesOfClass(Display.BlockDisplay.class, box, e -> e.getTags().contains(DOOR_TAG));
        for (Entity e : entities) e.discard();
    }

    public static void removeTrain(ServerLevel level, BlockPos origin) {
        AABB box = new AABB(origin).inflate(100.0);
        List<Display.BlockDisplay> entities = level.getEntitiesOfClass(Display.BlockDisplay.class, box, e -> e.getTags().contains(TRAIN_TAG));
        for (Entity e : entities) e.discard();
    }

    private static void display(ServerLevel level, double x, double y, double z, BlockState state,
                                float sx, float sy, float sz, String... tags) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(level);
        if (d == null) return;

        CompoundTag nbt = new CompoundTag();
        nbt.put("block_state", NbtUtils.writeBlockState(state));

        CompoundTag transform = new CompoundTag();
        transform.put("translation", floats(-sx / 2.0f, -sy / 2.0f, -sz / 2.0f));
        transform.put("scale", floats(sx, sy, sz));
        transform.put("left_rotation", floats(0f, 0f, 0f, 1f));
        transform.put("right_rotation", floats(0f, 0f, 0f, 1f));
        nbt.put("transformation", transform);
        nbt.putFloat("view_range", 1.5f);
        nbt.putFloat("shadow_radius", 0.0f);

        d.load(nbt);
        d.setPos(x, y, z);
        for (String tag : tags) d.addTag(tag);
        level.addFreshEntity(d);
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) list.add(FloatTag.valueOf(v));
        return list;
    }
}

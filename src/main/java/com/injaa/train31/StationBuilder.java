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
    // Hidden off-map set used as the Train 31 CCTV/security room.
    public static final BlockPos CCTV_ROOM = new BlockPos(-5000, 200, -5000);

    private static final String TRAIN_TAG = "train31_train";
    private static final String DOOR_TAG = "train31_door";

    public static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = TOKYO_ANCHOR;
        player.getPersistentData().putLong("train31_origin", origin.asLong());
        removeTrain(level, origin);
        buildCctvRoom(level);
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 0.05, origin.getZ() + 0.5, 0.0F, 0.0F);
        player.sendSystemMessage(Component.literal("§a[Train 31: Tokyo Edition] §fLinked to the real Tokyo subway at §e-92 61 213§f."));
        player.sendSystemMessage(Component.literal("§7CCTV set is ready. Crouch + right-click Director when ready to record."));
    }

    public static void setRedMode(ServerLevel level, BlockPos origin, boolean red) {
        // Tokyo station remains untouched.
    }

    public static void buildCctvRoom(ServerLevel level) {
        BlockPos o = CCTV_ROOM;
        // sealed room 15x7x13
        fill(level, o.offset(-7,0,-6), o.offset(7,0,6), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(level, o.offset(-7,6,-6), o.offset(7,6,6), Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, o.offset(-7,1,-6), o.offset(-7,5,6), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        fill(level, o.offset(7,1,-6), o.offset(7,5,6), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        fill(level, o.offset(-7,1,-6), o.offset(7,5,-6), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        fill(level, o.offset(-7,1,6), o.offset(7,5,6), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        clear(level, o.offset(-6,1,-5), o.offset(6,5,5));

        // desk
        fill(level, o.offset(-4,1,1), o.offset(4,1,2), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, o.offset(-4,2,2), o.offset(4,2,2), Blocks.BLACK_CONCRETE.defaultBlockState());

        // Main CCTV monitor on +Z wall.
        fill(level, o.offset(-5,1,5), o.offset(5,5,5), Blocks.BLACK_CONCRETE.defaultBlockState());
        fill(level, o.offset(-4,2,4), o.offset(4,4,4), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        fill(level, o.offset(-4,2,4), o.offset(4,2,4), Blocks.GRAY_CONCRETE.defaultBlockState());
        set(level, o.offset(-5,5,4), Blocks.REDSTONE_LAMP.defaultBlockState());
        set(level, o.offset(5,5,4), Blocks.REDSTONE_LAMP.defaultBlockState());

        // Two side monitors.
        fill(level, o.offset(-6,2,3), o.offset(-5,4,3), Blocks.CYAN_STAINED_GLASS.defaultBlockState());
        fill(level, o.offset(5,2,3), o.offset(6,4,3), Blocks.CYAN_STAINED_GLASS.defaultBlockState());

        // security-room props
        fill(level, o.offset(-6,1,-3), o.offset(-5,3,-1), Blocks.IRON_BLOCK.defaultBlockState());
        fill(level, o.offset(5,1,-3), o.offset(6,3,-1), Blocks.BARREL.defaultBlockState());
        set(level, o.offset(0,5,0), Blocks.SEA_LANTERN.defaultBlockState());

        setCctvFeed(level, 0);
    }

    public static void teleportToCctv(ServerPlayer player) {
        buildCctvRoom(player.serverLevel());
        BlockPos o = CCTV_ROOM;
        player.teleportTo(player.serverLevel(), o.getX()+0.5, o.getY()+1.1, o.getZ()-1.5, 0.0F, 0.0F);
    }

    public static void returnToStation(ServerPlayer player) {
        BlockPos o = TOKYO_ANCHOR;
        player.teleportTo(player.serverLevel(), o.getX()+0.5, o.getY()+0.05, o.getZ()+0.5, 0.0F, 0.0F);
    }

    /**
     * Scripted monitor feed: stage 0 empty platform, 1 far silhouette,
     * 2 mid-distance, 3 very close, 4 signal lost.
     */
    public static void setCctvFeed(ServerLevel level, int stage) {
        BlockPos o = CCTV_ROOM;
        if (stage == 4) {
            fill(level, o.offset(-4,2,4), o.offset(4,4,4), Blocks.BLACK_CONCRETE.defaultBlockState());
            for (int x=-4;x<=4;x+=2) set(level, o.offset(x,3,4), Blocks.WHITE_CONCRETE.defaultBlockState());
            return;
        }

        fill(level, o.offset(-4,2,4), o.offset(4,4,4), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        fill(level, o.offset(-4,2,4), o.offset(4,2,4), Blocks.GRAY_CONCRETE.defaultBlockState());

        if (stage == 1) {
            set(level, o.offset(3,3,4), Blocks.BLACK_CONCRETE.defaultBlockState());
        } else if (stage == 2) {
            set(level, o.offset(0,3,4), Blocks.BLACK_CONCRETE.defaultBlockState());
            set(level, o.offset(0,4,4), Blocks.BLACK_CONCRETE.defaultBlockState());
        } else if (stage == 3) {
            fill(level, o.offset(-1,2,4), o.offset(1,4,4), Blocks.BLACK_CONCRETE.defaultBlockState());
        }
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

    private static void set(ServerLevel level, BlockPos p, BlockState state) { level.setBlock(p,state,3); }
    private static void clear(ServerLevel level, BlockPos a, BlockPos b) { fill(level,a,b,Blocks.AIR.defaultBlockState()); }
    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState state) {
        int minX=Math.min(a.getX(),b.getX()), maxX=Math.max(a.getX(),b.getX());
        int minY=Math.min(a.getY(),b.getY()), maxY=Math.max(a.getY(),b.getY());
        int minZ=Math.min(a.getZ(),b.getZ()), maxZ=Math.max(a.getZ(),b.getZ());
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++) {
            level.setBlock(new BlockPos(x,y,z),state,3);
        }
    }
}

package com.injaa.train31;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class StationBuilder {
    private StationBuilder() {}

    public static void build(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(-18, 0, -12);
        player.getPersistentData().putLong("train31_origin", origin.asLong());
        player.sendSystemMessage(Component.literal("§c[Train 31] §fBuilding station..."));

        buildStreet(level, origin);
        buildEntrance(level, origin);
        buildTicketHall(level, origin);
        buildPlatforms(level, origin);
        buildPlatform3(level, origin);
        buildTunnel(level, origin);
        buildSecurity(level, origin);
        buildRecords(level, origin);
        buildMaintenance(level, origin);

        BlockPos spawn = origin.offset(12, 1, 10);
        player.teleportTo(level, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, 0, 0);
        player.sendSystemMessage(Component.literal("§a[Train 31] Station ready. §fCrouch + right-click the Director item to start AUTO."));
    }

    private static void buildStreet(ServerLevel level, BlockPos o) {
        fill(level, o.offset(-8,-1,-8), o.offset(68,-1,42), Blocks.SMOOTH_STONE.defaultBlockState());
        fill(level, o.offset(-8,0,18), o.offset(68,0,30), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x=-6; x<=66; x+=6) {
            fill(level, o.offset(x,0,23), o.offset(x+2,0,24), Blocks.WHITE_CONCRETE.defaultBlockState());
        }
        for (int x=-6; x<=66; x+=8) {
            column(level, o.offset(x,0,16), 4, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
            set(level, o.offset(x,4,16), Blocks.SEA_LANTERN.defaultBlockState());
        }
    }

    private static void buildEntrance(ServerLevel level, BlockPos o) {
        hollowBox(level, o.offset(6,0,3), o.offset(26,10,18), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        clear(level, o.offset(8,1,5), o.offset(24,8,16));
        fill(level, o.offset(9,0,6), o.offset(23,0,15), Blocks.POLISHED_ANDESITE.defaultBlockState());
        fill(level, o.offset(7,8,4), o.offset(25,8,17), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        fill(level, o.offset(11,1,4), o.offset(21,5,4), Blocks.GLASS.defaultBlockState());
        for (int i=0;i<8;i++) {
            fill(level, o.offset(12+i, -i-1, 10), o.offset(18+i, -i-1, 11), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (int x=9;x<=23;x+=4) set(level, o.offset(x,7,6), Blocks.SEA_LANTERN.defaultBlockState());
    }

    private static void buildTicketHall(ServerLevel level, BlockPos o) {
        BlockPos a=o.offset(-2,-11,16), b=o.offset(56,-4,48);
        hollowBox(level,a,b,Blocks.SMOOTH_QUARTZ.defaultBlockState());
        clear(level,o.offset(0,-10,18),o.offset(54,-5,46));
        fill(level,o.offset(0,-11,18),o.offset(54,-11,46),Blocks.POLISHED_ANDESITE.defaultBlockState());
        for(int x=2;x<=52;x+=5) set(level,o.offset(x,-5,20),Blocks.SEA_LANTERN.defaultBlockState());
        for(int x=8;x<=44;x+=6){
            fill(level,o.offset(x,-10,31),o.offset(x+1,-8,33),Blocks.IRON_BARS.defaultBlockState());
            set(level,o.offset(x,-7,32),Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true));
        }
        for(int x=3;x<=18;x+=5){
            fill(level,o.offset(x,-10,21),o.offset(x+2,-7,23),Blocks.WHITE_CONCRETE.defaultBlockState());
            set(level,o.offset(x+1,-8,20),Blocks.GLASS.defaultBlockState());
            set(level,o.offset(x+1,-7,20),Blocks.LIME_CONCRETE.defaultBlockState());
        }
        // vending machines
        for(int x=36;x<=48;x+=6){
            fill(level,o.offset(x,-10,22),o.offset(x+3,-6,24),Blocks.QUARTZ_BLOCK.defaultBlockState());
            fill(level,o.offset(x+1,-9,21),o.offset(x+2,-7,21),Blocks.GLASS.defaultBlockState());
            set(level,o.offset(x+1,-8,22),Blocks.RED_CONCRETE.defaultBlockState());
        }
    }

    private static void buildPlatforms(ServerLevel level, BlockPos o) {
        clear(level,o.offset(-5,-22,49),o.offset(118,-12,92));
        fill(level,o.offset(-5,-23,49),o.offset(118,-23,92),Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level,o.offset(-5,-12,49),o.offset(118,-12,92),Blocks.DEEPSLATE_TILES.defaultBlockState());
        for(int z : new int[]{53,68}){
            fill(level,o.offset(-2,-22,z),o.offset(108,-22,z+6),Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level,o.offset(-2,-21,z),o.offset(108,-21,z),Blocks.YELLOW_CONCRETE.defaultBlockState());
            for(int x=2;x<=104;x+=8){
                column(level,o.offset(x,-21,z+3),8,Blocks.POLISHED_ANDESITE.defaultBlockState());
                set(level,o.offset(x,-13,z+3),Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        buildTrack(level,o,61);
        buildTrack(level,o,76);
        for(int x=5;x<=98;x+=14){
            // benches
            fill(level,o.offset(x,-20,55),o.offset(x+4,-20,56),Blocks.DARK_OAK_SLAB.defaultBlockState());
            fill(level,o.offset(x,-20,70),o.offset(x+4,-20,71),Blocks.DARK_OAK_SLAB.defaultBlockState());
        }
    }

    private static void buildPlatform3(ServerLevel level, BlockPos o) {
        fill(level,o.offset(-2,-22,82),o.offset(108,-22,88),Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        fill(level,o.offset(-2,-21,82),o.offset(108,-21,82),Blocks.YELLOW_CONCRETE.defaultBlockState());
        buildTrack(level,o,90);
        for(int x=2;x<=104;x+=8){
            column(level,o.offset(x,-21,86),8,Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
            set(level,o.offset(x,-13,86), (x%16==2?Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true):Blocks.SEA_LANTERN.defaultBlockState()));
        }
        for(int x=10;x<=90;x+=18){
            fill(level,o.offset(x,-20,84),o.offset(x+5,-20,85),Blocks.DARK_OAK_SLAB.defaultBlockState());
        }
        // warning gate
        fill(level,o.offset(0,-21,81),o.offset(0,-15,88),Blocks.IRON_BARS.defaultBlockState());
        fill(level,o.offset(0,-18,83),o.offset(0,-17,86),Blocks.RED_CONCRETE.defaultBlockState());
    }

    private static void buildTunnel(ServerLevel level, BlockPos o) {
        clear(level,o.offset(109,-22,87),o.offset(160,-13,95));
        fill(level,o.offset(109,-23,87),o.offset(160,-23,95),Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level,o.offset(109,-12,87),o.offset(160,-12,95),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        for(int x=112;x<=156;x+=8){
            fill(level,o.offset(x,-21,87),o.offset(x,-13,87),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            fill(level,o.offset(x,-21,95),o.offset(x,-13,95),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(level,o.offset(x,-15,88),Blocks.REDSTONE_TORCH.defaultBlockState());
        }
        for(int x=109;x<=160;x++){
            set(level,o.offset(x,-22,90),Blocks.RAIL.defaultBlockState());
            set(level,o.offset(x,-22,92),Blocks.RAIL.defaultBlockState());
        }
    }

    private static void buildSecurity(ServerLevel level, BlockPos o) {
        hollowBox(level,o.offset(-16,-20,51),o.offset(-4,-13,67),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        clear(level,o.offset(-15,-19,52),o.offset(-5,-14,66));
        fill(level,o.offset(-14,-18,53),o.offset(-6,-16,53),Blocks.BLACK_CONCRETE.defaultBlockState());
        for(int x=-13;x<=-7;x+=3) for(int y=-18;y<=-16;y++) set(level,o.offset(x,y,52),Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        fill(level,o.offset(-13,-19,60),o.offset(-8,-19,63),Blocks.DARK_OAK_PLANKS.defaultBlockState());
    }

    private static void buildRecords(ServerLevel level, BlockPos o) {
        hollowBox(level,o.offset(-16,-20,70),o.offset(-4,-13,84),Blocks.STONE_BRICKS.defaultBlockState());
        clear(level,o.offset(-15,-19,71),o.offset(-5,-14,83));
        for(int z=72;z<=82;z+=2) fill(level,o.offset(-14,-19,z),o.offset(-14,-15,z),Blocks.BOOKSHELF.defaultBlockState());
        fill(level,o.offset(-11,-19,75),o.offset(-7,-18,79),Blocks.DARK_OAK_PLANKS.defaultBlockState());
    }

    private static void buildMaintenance(ServerLevel level, BlockPos o) {
        clear(level,o.offset(30,-30,96),o.offset(95,-24,102));
        fill(level,o.offset(30,-31,96),o.offset(95,-31,102),Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level,o.offset(30,-23,96),o.offset(95,-23,102),Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        for(int x=32;x<=92;x+=6){
            fill(level,o.offset(x,-30,96),o.offset(x,-24,96),Blocks.IRON_BARS.defaultBlockState());
            set(level,o.offset(x,-26,101),Blocks.REDSTONE_TORCH.defaultBlockState());
        }
    }

    private static void buildTrack(ServerLevel level, BlockPos o, int z) {
        fill(level,o.offset(-2,-23,z-1),o.offset(108,-23,z+3),Blocks.GRAVEL.defaultBlockState());
        for(int x=-2;x<=108;x++){
            if(x%2==0) fill(level,o.offset(x,-22,z-1),o.offset(x,-22,z+3),Blocks.DARK_OAK_PLANKS.defaultBlockState());
            set(level,o.offset(x,-21,z),Blocks.RAIL.defaultBlockState());
            set(level,o.offset(x,-21,z+2),Blocks.RAIL.defaultBlockState());
        }
    }

    public static void setRedMode(ServerLevel level, BlockPos o, boolean red) {
        for(int x=2;x<=104;x+=8){
            BlockPos p=o.offset(x,-13,86);
            set(level,p, red ? Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true) : Blocks.SEA_LANTERN.defaultBlockState());
        }
    }

    public static void buildTrain(ServerLevel level, BlockPos o) {
        // 36-block long Train 31, parked on Platform 3 track.
        int y=-20, z=90;
        fill(level,o.offset(28,y,z-1),o.offset(64,y+4,z+3),Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        fill(level,o.offset(29,y+1,z-2),o.offset(63,y+3,z-2),Blocks.GLASS.defaultBlockState());
        fill(level,o.offset(29,y+1,z+4),o.offset(63,y+3,z+4),Blocks.GLASS.defaultBlockState());
        fill(level,o.offset(28,y+2,z-1),o.offset(64,y+2,z+3),Blocks.RED_CONCRETE.defaultBlockState());
        // restore windows over stripe
        for(int x=31;x<=60;x+=6){
            fill(level,o.offset(x,y+2,z-2),o.offset(x+3,y+3,z-2),Blocks.BLACK_STAINED_GLASS.defaultBlockState());
            fill(level,o.offset(x,y+2,z+4),o.offset(x+3,y+3,z+4),Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        }
        set(level,o.offset(28,y+2,z),Blocks.SEA_LANTERN.defaultBlockState());
        set(level,o.offset(28,y+2,z+2),Blocks.SEA_LANTERN.defaultBlockState());
        // doors
        for(int x=34;x<=58;x+=8){
            fill(level,o.offset(x,y+1,z-2),o.offset(x+1,y+3,z-2),Blocks.IRON_BLOCK.defaultBlockState());
        }
    }

    public static void openTrainDoors(ServerLevel level, BlockPos o) {
        int y=-20,z=90;
        for(int x=34;x<=58;x+=8) clear(level,o.offset(x,y+1,z-2),o.offset(x+1,y+3,z-2));
    }

    public static void removeTrain(ServerLevel level, BlockPos o) {
        clear(level,o.offset(27,-20,88),o.offset(65,-15,94));
    }

    private static void set(ServerLevel level, BlockPos p, BlockState state){ level.setBlock(p,state,3); }
    private static void column(ServerLevel level, BlockPos p, int h, BlockState s){ for(int y=0;y<h;y++) set(level,p.above(y),s); }
    private static void clear(ServerLevel level, BlockPos a, BlockPos b){ fill(level,a,b,Blocks.AIR.defaultBlockState()); }
    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState state){
        int minX=Math.min(a.getX(),b.getX()), maxX=Math.max(a.getX(),b.getX());
        int minY=Math.min(a.getY(),b.getY()), maxY=Math.max(a.getY(),b.getY());
        int minZ=Math.min(a.getZ(),b.getZ()), maxZ=Math.max(a.getZ(),b.getZ());
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++) level.setBlock(new BlockPos(x,y,z),state,3);
    }
    private static void hollowBox(ServerLevel level, BlockPos a, BlockPos b, BlockState state){
        int minX=Math.min(a.getX(),b.getX()), maxX=Math.max(a.getX(),b.getX());
        int minY=Math.min(a.getY(),b.getY()), maxY=Math.max(a.getY(),b.getY());
        int minZ=Math.min(a.getZ(),b.getZ()), maxZ=Math.max(a.getZ(),b.getZ());
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++){
            if(x==minX||x==maxX||y==minY||y==maxY||z==minZ||z==maxZ) level.setBlock(new BlockPos(x,y,z),state,3);
        }
    }
}

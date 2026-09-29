package com.injaa.lastelevator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Player-driven version of the written film: no timed scene teleports. */
public final class TowerStory {
    private TowerStory(){}
    private static final String[] OBJECTIVES={
            "Enter the lift at the east end of the lobby. Press its button for floor 6.",
            "Find fuse 1 in the office corridor, then ride to the missing floor.",
            "Find fuse 2 in the hotel corridor. Return to the lift.",
            "After the bell, do not look at the Passenger. Press the lift button again.",
            "Press the breaker for a key, unlock the electrical room, take fuse 3, then run.",
            "Install the three fuses at the office panel. Then take the service stairs.",
            "The landing repeats. Keep climbing the actual stairs toward floor 0.",
            "Your name is on the wall. Break the emergency seal and descend the fire stairs.",
            "You reached the street. Look back at the tower directory."
    };
    private static CompoundTag data(ServerPlayer p){return p.getPersistentData().getCompound(LastElevator.ID);}
    private static void say(ServerPlayer p,String value){p.sendSystemMessage(Component.literal("[Last Elevator] "+value));}
    private static void cue(ServerPlayer p,SoundEvent event){
        p.level().playSound(null,p.blockPosition(),event,SoundSource.BLOCKS,.95f,1f);
    }
    private static void behind(ServerPlayer p,SoundEvent event){
        var v=p.getLookAngle();
        p.level().playSound(null,p.getX()-v.x*5,p.getY()+.5,p.getZ()-v.z*5,event,SoundSource.BLOCKS,.85f,1f);
    }
    private static boolean safe(ServerPlayer p,String name){
        CompoundTag m=data(p).getCompound("marks").getCompound(name);
        if(!m.contains("pos"))return false;
        BlockPos pos=BlockPos.of(m.getLong("pos"));ServerLevel w=(ServerLevel)p.level();w.getChunkAt(pos);
        return w.getBlockState(pos).getCollisionShape(w,pos).isEmpty()
                &&w.getBlockState(pos.above()).getCollisionShape(w,pos.above()).isEmpty()
                &&!w.getBlockState(pos.below()).getCollisionShape(w,pos.below()).isEmpty();
    }
    public static int check(ServerPlayer p){
        int bad=0;
        for(String n:new String[]{"lobby","car","office","hotel","maintenance","stair","zero","street",
                "fuse1","fuse2","fuse3","passenger_rule","passenger_maintenance","passenger_zero"}){
            if(!safe(p,n)){say(p,"Unsafe or missing location: "+n);bad++;}
        }
        if(bad==0)say(p,"All 14 locations clear. Start with /le setup, then /le auto 20.");
        return bad==0?1:0;
    }
    private static void despawn(ServerPlayer p){
        CompoundTag d=data(p);
        if(d.hasUUID("passengerId")){
            var e=((ServerLevel)p.level()).getEntity(d.getUUID("passengerId"));
            if(e!=null)e.discard();d.remove("passengerId");
        }
    }
    private static void spawn(ServerPlayer p,int x,int y,int z,boolean chase){
        despawn(p);ServerLevel w=(ServerLevel)p.level();
        BlockPos at=new BlockPos(x,y,z);w.getChunkAt(at);
        if(!w.getBlockState(at).isAir()||!w.getBlockState(at.above()).isAir()){
            say(p,"Passenger location blocked. Use /le reset before another take.");return;
        }
        Passenger mob=LastElevator.PASSENGER.get().create(w);
        if(mob==null)return;
        mob.moveTo(x+.5,y,z+.5,0,0);mob.setNoAi(!chase);
        w.addFreshEntity(mob);data(p).putUUID("passengerId",mob.getUUID());
        cue(p,LastElevator.STING.get());
    }
    private static void scene(ServerPlayer p,int target){
        CompoundTag d=data(p);despawn(p);
        d.putInt("scene",target);d.putInt("elapsed",0);d.putBoolean("sawPassenger",false);
        say(p,"SCENE "+target+" — "+OBJECTIVES[target]);
        if(target==1||target==2||target==4){
            int fuse=target==1?1:target==2?2:3;
            CompoundTag marker=data(p).getCompound("marks").getCompound("fuse"+fuse);
            if(!data(p).getBoolean("fuse"+fuse))
                dropFuse((ServerLevel)p.level(),BlockPos.of(marker.getLong("pos")),fuse);
        }
        if(target==2){cue(p,LastElevator.BELL.get());say(p,"FLOOR 13. The directory has no floor 13. A guest room has two cups set for one person.");}
        if(target==3){TowerLift.door((ServerLevel)p.level(),99,false);
            cue(p,LastElevator.BELL.get());say(p,"RULE: Do not look at the other passenger after the bell.");}
        if(target==4)cue(p,LastElevator.AMBIENCE.get());
        if(target==5){cue(p,LastElevator.BELL.get());say(p,"Bring all three fuses to the panel on floor 6.");}
        if(target==6){say(p,"The staircase keeps returning to the same landing.");cue(p,LastElevator.KNOCK.get());}
        if(target==7){TokyoDirector.employee(p);say(p,"FLOOR 0. That is your name on the employee board.");cue(p,LastElevator.STING.get());}
        if(target==8){((ServerLevel)p.level()).setDayTime(1000);TokyoDirector.operator(p);
            cue(p,LastElevator.BELL.get());say(p,"NIGHT OPERATOR: "+p.getGameProfile().getName());
            d.putBoolean("running",false);}
    }
    private static void dropFuse(ServerLevel w,BlockPos pos,int number){
        w.getChunkAt(pos);
        for(ItemEntity old:w.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)))
            if(old.getItem().is(LastElevator.FUSE.get()))old.discard();
        ItemStack fuse=new ItemStack(LastElevator.FUSE.get());
        fuse.getOrCreateTag().putInt("FuseNumber",number);
        ItemEntity item=new ItemEntity(w,pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,fuse);
        item.setPickUpDelay(0);w.addFreshEntity(item);
    }
    public static int setup(ServerPlayer p){
        if(check(p)==0)return 0;
        ServerLevel w=(ServerLevel)p.level();CompoundTag marks=data(p).getCompound("marks");
        for(int i=1;i<=3;i++)dropFuse(w,BlockPos.of(marks.getCompound("fuse"+i).getLong("pos")),i);
        say(p,"Three fuses placed on floors 6, 13 and maintenance. Find each one yourself.");return 1;
    }
    public static int start(ServerPlayer p,int seconds,boolean auto){
        if(seconds!=10&&seconds!=15&&seconds!=20){say(p,"Choose a 10, 15 or 20 second delay.");return 0;}
        if(check(p)==0)return 0;
        CompoundTag d=data(p);despawn(p);
        d.putBoolean("running",true);d.putBoolean("paused",false);d.putBoolean("auto",auto);
        d.putInt("delay",seconds*20);d.putInt("elapsed",0);d.putInt("scene",0);
        d.putInt("fuses",0);d.putBoolean("panelRestored",false);d.putBoolean("sealOpen",false);
        d.putBoolean("ruleSpawned",false);d.putBoolean("lookScare",false);
        d.putBoolean("zeroSpawn",false);
        d.putBoolean("hotelSealed",false);
        d.putBoolean("breakerKey",false);d.putBoolean("electricalOpen",false);
        d.putBoolean("coldOpen",false);
        d.putBoolean("loop1",false);d.putBoolean("loop2",false);
        for(int i=1;i<=3;i++)d.putBoolean("fuse"+i,false);
        TokyoDirector.seal((ServerLevel)p.level(),true);
        TokyoDirector.electricalDoor((ServerLevel)p.level(),true);
        ((ServerLevel)p.level()).setDayTime(18000);
        say(p,"Recording delay: "+seconds+" seconds. Then enter the lift; it will not move you until you press the button.");
        return 1;
    }
    public static int fuse(ServerPlayer p,int number){
        CompoundTag d=data(p);if(!d.getBoolean("running")||d.getInt("delay")>0)return 0;
        if(number<1||number>3||d.getBoolean("fuse"+number))return 0;
        int stage=d.getInt("scene");
        if((number==1&&stage!=1)||(number==2&&stage!=2)||(number==3&&stage!=4))return 0;
        if(number==3&&!d.getBoolean("electricalOpen")){say(p,"The electrical room is still locked.");return 0;}
        d.putBoolean("fuse"+number,true);d.putInt("fuses",d.getInt("fuses")+1);
        cue(p,LastElevator.ELECTRIC.get());
        if(number==1){say(p,"FUSE 1/3. The printer says: DON'T RETURN WITH TWO PEOPLE.");
            ItemStack paper=new ItemStack(Items.PAPER);paper.setHoverName(Component.literal("DON'T RETURN WITH TWO PEOPLE"));
            p.getInventory().add(paper);}
        if(number==2){say(p,"FUSE 2/3. A bell rings from the lift. Read the rule beside it.");scene(p,3);}
        if(number==3){say(p,"FUSE 3/3. The lights die. RUN BACK TO THE LIFT!");
            ServerLevel w=(ServerLevel)p.level();
            for(int x:new int[]{-263,-255,-247})w.setBlock(new BlockPos(x,90,99),Blocks.REDSTONE_TORCH.defaultBlockState(),2);
            spawn(p,-268,90,100,true);cue(p,LastElevator.RADIO.get());}
        return 1;
    }
    public static boolean pickup(ServerPlayer p,ItemStack item){
        if(!item.is(LastElevator.FUSE.get()))return false;
        int n=item.getTag()==null?0:item.getTag().getInt("FuseNumber");
        return n>0&&fuse(p,n)==1;
    }
    public static boolean click(ServerPlayer p,BlockPos pos){
        CompoundTag d=data(p);if(!d.getBoolean("running")||d.getBoolean("paused")||d.getInt("delay")>0)return false;
        if(d.getBoolean("liftMoving"))return true;
        int stage=d.getInt("scene");
        if(pos.equals(new BlockPos(-251,90,101))&&stage==4){
            if(!d.getBoolean("breakerKey")){
                d.putBoolean("breakerKey",true);cue(p,LastElevator.ELECTRIC.get());
                ItemStack key=new ItemStack(Items.TRIPWIRE_HOOK);key.setHoverName(Component.literal("Electrical room key"));
                p.getInventory().add(key);say(p,"KEY RELEASED. Unlock the barred room beside the corridor.");
            }
            return true;
        }
        if(pos.getZ()==104&&pos.getX()>=-257&&pos.getX()<=-255
                &&pos.getY()>=90&&pos.getY()<=92&&stage==4){
            if(!d.getBoolean("breakerKey")){say(p,"Locked. Find the breaker key first.");return true;}
            TokyoDirector.electricalDoor((ServerLevel)p.level(),false);
            d.putBoolean("electricalOpen",true);cue(p,LastElevator.DOOR.get());
            say(p,"Electrical room unlocked. Take the third fuse.");return true;
        }
        // Real fuse panel, approached after returning from maintenance.
        if(pos.equals(new BlockPos(-255,85,100))&&stage==5){
            if(d.getInt("fuses")<3){say(p,"The panel needs all three fuses.");return true;}
            d.putBoolean("panelRestored",true);cue(p,LastElevator.ELECTRIC.get());
            TokyoDirector.restorePanel(p);
            for(int i=0;i<p.getInventory().getContainerSize();i++)
                if(p.getInventory().getItem(i).is(LastElevator.FUSE.get()))p.getInventory().setItem(i,ItemStack.EMPTY);
            say(p,"POWER RESTORED. ONE PASSENGER MUST REMAIN. The service stairs are east of the lift.");
            spawn(p,-233,85,101,false);return true;
        }
        if((pos.getX()==-273)&&(pos.getY()>=115&&pos.getY()<=117)&&stage==7){
            if(!d.getBoolean("panelRestored")){say(p,"Restore the fuse panel first.");return true;}
            TokyoDirector.seal((ServerLevel)p.level(),false);d.putBoolean("sealOpen",true);
            cue(p,LastElevator.DOOR.get());say(p,"SEAL BROKEN! Descend the fire stair to the street.");return true;
        }
        // Floor button inside the moving car. Never change floors on a timer.
        int floor=d.getInt("carFloor");
        if(pos.equals(new BlockPos(-233,floor+1,100))&&TowerLift.inside(p,floor)){
            int target=-1,next=-1;
            if(stage==0){target=84;next=1;}
            else if(stage==1&&d.getBoolean("fuse1")){target=99;next=2;}
            else if(stage==3){
                if(!d.getBoolean("ruleSpawned")){
                    d.putBoolean("ruleSpawned",true);d.putInt("ruleBell",d.getInt("elapsed"));
                    behind(p,LastElevator.STEPS.get());cue(p,LastElevator.BELL.get());
                    spawn(p,-233,100,101,false);
                    say(p,"Someone entered behind you. Do not turn. Press the button once more after the bell.");return true;
                }
                if(d.getInt("elapsed")-d.getInt("ruleBell")<45){say(p,"Wait for the other passenger to settle.");return true;}
                target=89;next=4;
            }
            else if(stage==4&&d.getBoolean("fuse3")){target=84;next=5;}
            else {say(p,OBJECTIVES[stage]);return true;}
            despawn(p);TowerLift.begin(p,d,target,next);
            cue(p,LastElevator.DOOR.get());cue(p,LastElevator.MOTOR.get());
            say(p,"Doors closing. Riding to the next floor.");return true;
        }
        return false;
    }
    public static void tick(ServerPlayer p){
        CompoundTag d=data(p);if(!d.getBoolean("running")||d.getBoolean("paused"))return;
        if(d.getInt("delay")>0){
            int n=d.getInt("delay")-1;d.putInt("delay",n);
            if(n==0){say(p,"ACTION. "+OBJECTIVES[0]);cue(p,LastElevator.BELL.get());}
            return;
        }
        if(d.getBoolean("liftMoving")){
            int arrival=TowerLift.tick(p,d);
            if(arrival>=0)scene(p,arrival);
            return;
        }
        int stage=d.getInt("scene"),t=d.getInt("elapsed")+1;d.putInt("elapsed",t);
        if(t%60==1)p.displayClientMessage(Component.literal("OBJECTIVE: "+OBJECTIVES[stage]),true);
        if(stage==0&&t==80)behind(p,LastElevator.KNOCK.get());
        if(stage==1&&t==70){cue(p,LastElevator.BELL.get());say(p,"An empty lift rang behind you.");}
        if(stage==2&&t==110)behind(p,LastElevator.KNOCK.get());
        if(stage==2&&!d.getBoolean("hotelSealed")&&t>20&&p.getX()<-237){
            d.putBoolean("hotelSealed",true);
            TowerLift.door((ServerLevel)p.level(),99,true);
            cue(p,LastElevator.DOOR.get());
            say(p,"The lift closed while you searched the hotel. Find fuse 2 to call it back.");
        }
        if(stage==3&&d.getBoolean("ruleSpawned")&&!d.getBoolean("lookScare")&&d.hasUUID("passengerId")){
            var passenger=((ServerLevel)p.level()).getEntity(d.getUUID("passengerId"));
            if(passenger!=null&&p.distanceToSqr(passenger)<50){
                var look=passenger.getEyePosition().subtract(p.getEyePosition()).normalize();
                if(p.getLookAngle().dot(look)>.82&&p.hasLineOfSight(passenger)){
                    d.putBoolean("lookScare",true);say(p,"YOU LOOKED AT THE OTHER PASSENGER!");
                    cue(p,LastElevator.STING.get());
                    ((ServerLevel)p.level()).sendParticles(ParticleTypes.SMOKE,passenger.getX(),passenger.getY()+1.5,passenger.getZ(),28,.4,.5,.4,.02);
                }
            }
        }
        if(stage==4&&t==40){cue(p,LastElevator.RADIO.get());say(p,"The radio echoes something you said inside the lift...");}
        if(stage==5&&d.getBoolean("panelRestored")&&p.getX()>-239&&p.getZ()>103&&p.getY()<89)scene(p,6);
        if(stage==6){
            if(p.getY()>=94&&!d.getBoolean("loop1")){
                d.putBoolean("loop1",true);
                ((ServerLevel)p.level()).setBlock(new BlockPos(-225,101,104),Blocks.AIR.defaultBlockState(),2);
                cue(p,LastElevator.KNOCK.get());say(p,"The SAME landing again. One lamp has gone out.");
            }
            if(p.getY()>=104&&!d.getBoolean("loop2")){
                d.putBoolean("loop2",true);
                ((ServerLevel)p.level()).setBlock(new BlockPos(-225,106,104),Blocks.AIR.defaultBlockState(),2);
                cue(p,LastElevator.KNOCK.get());say(p,"The SAME landing again. Another lamp has gone out.");
            }
            if(p.getY()>=115&&p.getX()<=-226)scene(p,7);
        }
        if(stage==7&&!d.getBoolean("coldOpen")&&!d.getBoolean("zeroSpawn")
                &&(t>=60||p.getX()<=-247)){
            d.putBoolean("zeroSpawn",true);
            spawn(p,p.getX()<=-247?-265:-259,115,100,true);
            say(p,"The Passenger is at the far end. Reach the emergency seal!");
        }
        if(stage==7&&d.getBoolean("coldOpen")&&(t==1||t==16||t==31))cue(p,LastElevator.BELL.get());
        if(stage==8)return;
        if(stage==7&&d.getBoolean("sealOpen")&&p.getX()<=-289&&p.getY()<=68){
            despawn(p);scene(p,8);
        }
    }
    public static int reset(ServerPlayer p){
        CompoundTag d=data(p);despawn(p);d.putBoolean("running",false);d.putBoolean("paused",false);
        d.putInt("delay",0);d.putInt("scene",0);d.putInt("elapsed",0);
        d.putInt("fuses",0);d.putBoolean("panelRestored",false);d.putBoolean("sealOpen",false);
        d.putBoolean("loop1",false);d.putBoolean("loop2",false);
        d.putBoolean("coldOpen",false);
        d.putBoolean("zeroSpawn",false);
        d.putBoolean("breakerKey",false);d.putBoolean("electricalOpen",false);
        for(int i=1;i<=3;i++)d.putBoolean("fuse"+i,false);
        TowerLift.reset(p,d);TokyoDirector.seal((ServerLevel)p.level(),true);
        TokyoDirector.electricalDoor((ServerLevel)p.level(),true);
        TokyoDirector.resetPanel((ServerLevel)p.level());
        ((ServerLevel)p.level()).setBlock(new BlockPos(-225,101,104),Blocks.OCHRE_FROGLIGHT.defaultBlockState(),2);
        ((ServerLevel)p.level()).setBlock(new BlockPos(-225,106,104),Blocks.OCHRE_FROGLIGHT.defaultBlockState(),2);
        p.teleportTo((ServerLevel)p.level(),-254.5,75,107.5,0,0);
        for(int i=0;i<p.getInventory().getContainerSize();i++)
            if(p.getInventory().getItem(i).is(LastElevator.FUSE.get())
                    ||(p.getInventory().getItem(i).is(Items.TRIPWIRE_HOOK)
                    &&p.getInventory().getItem(i).hasCustomHoverName()
                    &&p.getInventory().getItem(i).getHoverName().getString().equals("Electrical room key")))
                p.getInventory().setItem(i,ItemStack.EMPTY);
        for(int x:new int[]{-263,-255,-247})((ServerLevel)p.level()).setBlock(new BlockPos(x,90,99),Blocks.AIR.defaultBlockState(),2);
        say(p,"Take reset. Run /le setup, then /le auto 20.");return 1;
    }
    public static int skip(ServerPlayer p,int target){
        if(target<0||target>8)return 0;
        say(p,"Debug retake requested. Position yourself on the correct floor before continuing.");
        scene(p,target);return 1;
    }
    public static int coldOpen(ServerPlayer p){
        if(check(p)==0)return 0;
        CompoundTag d=data(p);despawn(p);
        TowerLift.position((ServerLevel)p.level(),d,114);
        TokyoDirector.seal((ServerLevel)p.level(),false);
        d.putBoolean("running",true);d.putBoolean("paused",false);d.putBoolean("coldOpen",true);
        d.putBoolean("sealOpen",true);d.putInt("delay",0);d.putInt("scene",7);d.putInt("elapsed",0);
        p.teleportTo((ServerLevel)p.level(),-255.5,115,100.5,90,0);
        spawn(p,-242,115,100,true);
        say(p,"COLD OPEN: three bells. Run toward the fire exit; cut before the Passenger reaches you.");
        return 1;
    }
    public static int stop(ServerPlayer p){
        CompoundTag d=data(p);d.putBoolean("running",false);despawn(p);
        say(p,"Director stopped. /le reset prepares the next take.");return 1;
    }
}

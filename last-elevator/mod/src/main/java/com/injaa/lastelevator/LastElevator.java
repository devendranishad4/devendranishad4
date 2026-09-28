package com.injaa.lastelevator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/** Map-independent server-side story director. Everything happens only after /le start. */
@Mod(LastElevator.ID)
public class LastElevator {
    public static final String ID="lastelevator";
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final RegistryObject<EntityType<Passenger>> PASSENGER=ENTITIES.register("passenger",
            ()->EntityType.Builder.<Passenger>of(Passenger::new,MobCategory.MONSTER).sized(.75f,2.6f)
                    .clientTrackingRange(12).build(ID+":passenger"));
    public static final RegistryObject<Item> FUSE=ITEMS.register("maintenance_fuse",()->new Item(new Item.Properties().stacksTo(1)));
    private static final String[] MARKERS={"lobby","car","office","hotel","maintenance","stair","zero","street",
            "fuse1","fuse2","fuse3","passenger_maintenance","passenger_zero"};
    private static final String[] SCENES={"lobby","office","hotel","hotel","maintenance","office","stair","zero","street"};
    private static final String[] NAMES={"Lobby","Office","Hotel 13","The rule","Maintenance","Fuse panel","Stair loop","Floor 0","Escape"};
    // Timed solo run: about 13 minutes of action after the 20-second recording delay.
    private static final int[] AUTO_SECONDS={85,70,85,100,115,100,110,110,110};

    public LastElevator(){
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);ITEMS.register(bus);bus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::pickup);
        MinecraftForge.EVENT_BUS.addListener(this::click);
    }
    private void attributes(EntityAttributeCreationEvent e){
        AttributeSupplier.Builder b=net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,100).add(Attributes.MOVEMENT_SPEED,.28)
                .add(Attributes.ATTACK_DAMAGE,0).add(Attributes.FOLLOW_RANGE,36);
        e.put(PASSENGER.get(),b.build());
    }
    private static CompoundTag state(ServerPlayer p){
        CompoundTag data=p.getPersistentData();
        if(!data.contains(ID))data.put(ID,new CompoundTag());
        return data.getCompound(ID);
    }
    private static CompoundTag marker(ServerPlayer p,String name){return state(p).getCompound("marks").getCompound(name);}
    private static boolean marked(ServerPlayer p,String name){return state(p).getCompound("marks").contains(name);}
    private static void msg(ServerPlayer p,String message){p.sendSystemMessage(Component.literal("[Last Elevator] "+message));}
    private static void sound(ServerPlayer p,net.minecraft.sounds.SoundEvent sound,float pitch){
        p.level().playSound(null,p.blockPosition(),sound,SoundSource.BLOCKS,1.1f,pitch);
    }
    private static int mark(ServerPlayer p,String name){
        CompoundTag d=state(p),marks=d.getCompound("marks"),v=new CompoundTag();
        v.putLong("pos",p.blockPosition().asLong());v.putFloat("yaw",p.getYRot());
        marks.put(name,v);d.put("marks",marks);msg(p,"Marked "+name+" at "+p.blockPosition().toShortString());return 1;
    }
    private static boolean safe(ServerPlayer p,String name){
        if(!marked(p,name)){msg(p,"Missing marker: "+name);return false;}
        BlockPos at=BlockPos.of(marker(p,name).getLong("pos"));
        if(!p.level().hasChunkAt(at)){msg(p,"Marker chunk is not loaded: "+name);return false;}
        boolean space=p.level().getBlockState(at).getCollisionShape(p.level(),at).isEmpty()
                &&p.level().getBlockState(at.above()).getCollisionShape(p.level(),at.above()).isEmpty();
        boolean floor=!p.level().getBlockState(at.below()).getCollisionShape(p.level(),at.below()).isEmpty();
        if(!space||!floor)msg(p,"Blocked or unsafe marker "+name+" at "+at.toShortString());
        return space&&floor;
    }
    private static void teleport(ServerPlayer p,String name){
        if(!safe(p,name))return;
        CompoundTag v=marker(p,name);BlockPos at=BlockPos.of(v.getLong("pos"));
        p.teleportTo((ServerLevel)p.level(),at.getX()+.5,at.getY(),at.getZ()+.5,v.getFloat("yaw"),p.getXRot());
    }
    private static int check(ServerPlayer p){
        int missing=0;
        for(String marker:MARKERS)if(!marked(p,marker)){msg(p,"Missing: "+marker);missing++;}
        if(missing==0){for(String marker:SCENES) safe(p,marker);msg(p,"All markers present. Check each room and exit before filming.");}
        return missing==0?1:0;
    }
    private static int start(ServerPlayer p,int seconds){
        if(seconds!=10&&seconds!=15&&seconds!=20){msg(p,"Choose 10, 15 or 20 seconds.");return 0;}
        if(check(p)==0)return 0;
        CompoundTag d=state(p);d.putBoolean("running",true);d.putBoolean("paused",false);
        d.putInt("delay",seconds*20);d.putInt("scene",0);d.putInt("elapsed",0);
        d.putInt("fuses",0);d.putInt("transition",0);
        d.putBoolean("auto",false);
        for(int i=1;i<=3;i++)d.putBoolean("fuse"+i,false);
        cleanup(p);msg(p,"Recording delay: "+seconds+" seconds. Close chat and start filming.");return 1;
    }
    private static int scene(ServerPlayer p,int target,boolean bypass){
        CompoundTag d=state(p);
        if(!d.getBoolean("running")){msg(p,"Use /le start first.");return 0;}
        if(d.getInt("delay")>0){msg(p,"Wait for the countdown.");return 0;}
        if(target<0||target>=SCENES.length){msg(p,"Unknown scene.");return 0;}
        if(!bypass&&target>=5&&d.getInt("fuses")<3){msg(p,"Collect all three fuses first, or use /le scene for a retake.");return 0;}
        if(!safe(p,SCENES[target]))return 0;
        cleanup(p);d.putInt("scene",target);d.putInt("elapsed",0);
        d.putInt("transition",12); // short lift travel sound before the safe teleport
        sound(p,SoundEvents.IRON_DOOR_CLOSE,.7f);
        msg(p,"Next: "+NAMES[target]);return 1;
    }
    private static void cleanup(ServerPlayer p){
        CompoundTag d=state(p);
        if(d.hasUUID("passengerId")){
            var entity=((ServerLevel)p.level()).getEntity(d.getUUID("passengerId"));
            if(entity!=null)entity.discard();
            d.remove("passengerId");
        }
    }
    private static int reset(ServerPlayer p){
        CompoundTag d=state(p);cleanup(p);d.putBoolean("running",false);d.putBoolean("paused",false);
        d.putInt("delay",0);d.putInt("scene",0);d.putInt("elapsed",0);d.putInt("transition",0);
        d.putInt("fuses",0);d.putBoolean("auto",false);
        for(int i=0;i<p.getInventory().getContainerSize();i++)
            if(p.getInventory().getItem(i).is(FUSE.get()))p.getInventory().setItem(i,ItemStack.EMPTY);
        for(int i=1;i<=3;i++)d.putBoolean("fuse"+i,false);
        msg(p,"Story reset. Your markers remain saved.");return 1;
    }
    private static int giveFuse(ServerPlayer p,int number){
        if(number<1||number>3)return 0;
        CompoundTag d=state(p);String flag="fuse"+number;
        if(d.getBoolean(flag)){msg(p,"Fuse "+number+" already collected.");return 0;}
        d.putBoolean(flag,true);d.putInt("fuses",d.getInt("fuses")+1);
        msg(p,"Fuse "+number+"/3 collected.");sound(p,SoundEvents.LEVER_CLICK,1.0f);return 1;
    }
    private static int setup(ServerPlayer p){
        if(check(p)==0)return 0;
        ServerLevel w=(ServerLevel)p.level();
        for(int i=1;i<=3;i++){
            BlockPos at=BlockPos.of(marker(p,"fuse"+i).getLong("pos"));
            for(ItemEntity old:w.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(2)))
                if(old.getItem().is(FUSE.get()))old.discard();
            ItemStack item=new ItemStack(FUSE.get());item.getOrCreateTag().putInt("FuseNumber",i);
            ItemEntity drop=new ItemEntity(w,at.getX()+.5,at.getY()+.35,at.getZ()+.5,item);
            drop.setPickUpDelay(0);w.addFreshEntity(drop);
        }
        msg(p,"Placed three story fuses. Set pieces stay untouched.");return 1;
    }
    private void registerCommands(RegisterCommandsEvent e){
        CommandDispatcher<CommandSourceStack> d=e.getDispatcher();
        var root=Commands.literal("le").requires(s->s.hasPermission(2));
        var marks=Commands.literal("mark");
        for(String m:MARKERS)marks.then(Commands.literal(m).executes(c->mark(c.getSource().getPlayerOrException(),m)));
        root.then(marks);
        root.then(Commands.literal("check").executes(c->check(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("setup").executes(c->setup(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("start").executes(c->start(c.getSource().getPlayerOrException(),20))
                .then(Commands.argument("seconds",IntegerArgumentType.integer(10,20))
                        .executes(c->start(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"seconds")))));
        root.then(Commands.literal("auto").executes(c->auto(c.getSource().getPlayerOrException(),20))
                .then(Commands.argument("seconds",IntegerArgumentType.integer(10,20))
                        .executes(c->auto(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"seconds")))));
        root.then(Commands.literal("next").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();return scene(p,state(p).getInt("scene")+1,false);}));
        root.then(Commands.literal("scene").then(Commands.argument("number",IntegerArgumentType.integer(0,8))
                .executes(c->scene(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"number"),true))));
        var fuse=Commands.literal("fuse");
        for(int i=1;i<=3;i++){final int n=i;fuse.then(Commands.literal(""+i).executes(c->giveFuse(c.getSource().getPlayerOrException(),n)));}
        root.then(fuse);
        root.then(Commands.literal("pause").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).putBoolean("paused",true);msg(p,"Paused.");return 1;}));
        root.then(Commands.literal("resume").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).putBoolean("paused",false);msg(p,"Resumed.");return 1;}));
        root.then(Commands.literal("stop").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).putBoolean("running",false);cleanup(p);msg(p,"Stopped.");return 1;}));
        root.then(Commands.literal("reset").executes(c->reset(c.getSource().getPlayerOrException())));
        d.register(root);
    }
    private void pickup(EntityItemPickupEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!e.getItem().getItem().is(FUSE.get()))return;
        int number=e.getItem().getItem().getTag()==null?0:e.getItem().getItem().getTag().getInt("FuseNumber");
        if(number>0)giveFuse(p,number);
    }
    private void click(PlayerInteractEvent.RightClickBlock e){
        if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND
                ||!(e.getEntity() instanceof ServerPlayer p)||!p.level().getBlockState(e.getPos()).is(Blocks.STONE_BUTTON))return;
        CompoundTag d=state(p);if(!d.getBoolean("running")||d.getBoolean("paused")||d.getInt("delay")>0)return;
        if(marked(p,"car")){
            BlockPos car=BlockPos.of(marker(p,"car").getLong("pos"));
            if(e.getPos().closerThan(car,7))scene(p,d.getInt("scene")+1,false);
        }
    }
    private void spawn(ServerPlayer p,String name){
        if(!marked(p,name)||!safe(p,name))return;
        Passenger mob=PASSENGER.get().create(p.level());if(mob==null)return;
        BlockPos at=BlockPos.of(marker(p,name).getLong("pos"));
        mob.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        ((ServerLevel)p.level()).addFreshEntity(mob);
        state(p).putUUID("passengerId",mob.getUUID());
        sound(p,SoundEvents.NOTE_BLOCK_BELL,.55f);
    }
    private static int auto(ServerPlayer p,int seconds){
        int result=start(p,seconds);
        if(result==1){state(p).putBoolean("auto",true);msg(p,"AUTO mode armed: all nine scenes will advance on schedule. /le pause stops the clock.");}
        return result;
    }
    private void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        CompoundTag d=state(p);if(!d.getBoolean("running")||d.getBoolean("paused"))return;
        int delay=d.getInt("delay");
        if(delay>0){
            d.putInt("delay",delay-1);
            if(delay%20==0)msg(p,"Starts in "+(delay/20)+"...");
            if(delay==1){teleport(p,"lobby");msg(p,"ACTION");sound(p,SoundEvents.NOTE_BLOCK_BELL,1f);}
            return;
        }
        int transition=d.getInt("transition");
        if(transition>0){
            d.putInt("transition",transition-1);
            if(transition==1)teleport(p,SCENES[d.getInt("scene")]);
            return;
        }
        int t=d.getInt("elapsed")+1;d.putInt("elapsed",t);
        int stage=d.getInt("scene");
        if(d.getBoolean("auto")){
            if(t==50&&stage==1)giveFuse(p,1);
            if(t==60&&stage==2)giveFuse(p,2);
            if(t==75&&stage==4)giveFuse(p,3);
        }
        if(t==20&&stage==2){msg(p,"The directory has no floor 13.");sound(p,SoundEvents.NOTE_BLOCK_BELL,.8f);}
        if(t==40&&stage==3){msg(p,"RULE: After the bell, don't look at the other passenger.");sound(p,SoundEvents.NOTE_BLOCK_BELL,.6f);}
        if(t==85&&stage==3){sound(p,SoundEvents.WOOD_STEP,.7f);}
        if(t==40&&stage==4){sound(p,SoundEvents.LEVER_CLICK,.6f);msg(p,"The radio repeats your voice...");}
        if(t==140&&stage==4){spawn(p,"passenger_maintenance");msg(p,"Run to the lift!");}
        if(t==50&&stage==6){msg(p,"Same landing. One lamp is gone.");sound(p,SoundEvents.IRON_DOOR_CLOSE,.7f);}
        if(t==180&&stage==6){msg(p,"The landing repeats again. Find floor 0.");}
        if(t==70&&stage==7){spawn(p,"passenger_zero");msg(p,"Break the emergency seal and reach the exit!");}
        if(t==40&&stage==8){msg(p,"NIGHT OPERATOR: "+p.getGameProfile().getName());sound(p,SoundEvents.NOTE_BLOCK_BELL,.45f);}
        if((stage==4||stage==7)&&t%30==0)((ServerLevel)p.level()).sendParticles(ParticleTypes.SMOKE,p.getX(),p.getY()+.8,p.getZ(),2,.4,.3,.4,0);
        if(d.getBoolean("auto")&&t>=AUTO_SECONDS[stage]*20){
            if(stage<SCENES.length-1)scene(p,stage+1,true);
            else {d.putBoolean("running",false);msg(p,"AUTO story complete. Use /le reset for another take.");}
        }
    }
}

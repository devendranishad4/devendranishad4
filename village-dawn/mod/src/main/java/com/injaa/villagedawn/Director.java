package com.injaa.villagedawn;

import java.util.*;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Director {
    static final String TAG="vd_story_actor";
    static final String[] NAMES={"arrival","elder warning","guest room","false visitor","looping road","keeper ledger","bell plan","rope search","pin search","repair","three rings","escape","sunrise ending"};
    static final int[] MIN={80,85,80,85,85,85,75,75,80,75,55,0,45};
    static final LinkedHashMap<String,BlockPos> DEFAULTS=new LinkedHashMap<>();
    static {
        DEFAULTS.put("start",new BlockPos(65,73,-45));DEFAULTS.put("entrance",new BlockPos(83,76,-143));
        DEFAULTS.put("square",new BlockPos(110,75,-136));DEFAULTS.put("elder",new BlockPos(109,75,-138));
        DEFAULTS.put("room",new BlockPos(53,79,-123));DEFAULTS.put("hall",new BlockPos(54,79,-128));DEFAULTS.put("watcher",new BlockPos(30,75,-155));
        DEFAULTS.put("forge",new BlockPos(170,74,-110));DEFAULTS.put("church",new BlockPos(118,76,-105));
        DEFAULTS.put("ledger",new BlockPos(118,76,-117));DEFAULTS.put("bell",new BlockPos(118,76,-104));DEFAULTS.put("pursuit",new BlockPos(118,76,-97));
        DEFAULTS.put("rope",new BlockPos(52,79,-122));DEFAULTS.put("pin",new BlockPos(169,74,-110));
        DEFAULTS.put("loop",new BlockPos(65,73,-45));DEFAULTS.put("exit",new BlockPos(35,72,33));
    }
    static CompoundTag state(ServerLevel l){return StoryData.get(l).data;}
    static void dirty(ServerLevel l){StoryData.get(l).setDirty();}
    static BlockPos pos(CompoundTag d,String key){CompoundTag m=d.getCompound("markers");return m.contains(key)?BlockPos.of(m.getLong(key)):DEFAULTS.get(key);}
    static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[Village Dawn] "+s));}
    static void subtitle(ServerPlayer p,String s){
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5,90,15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s)));
    }
    static boolean owner(ServerPlayer p){CompoundTag d=state(p.serverLevel());return !d.hasUUID("owner")||d.getUUID("owner").equals(p.getUUID());}
    static boolean near(ServerPlayer p,BlockPos b,double radius){return p.distanceToSqr(b.getX()+.5,b.getY(),b.getZ()+.5)<radius*radius;}
    static boolean safe(ServerLevel l,BlockPos b){
        l.getChunkAt(b);
        return l.getBlockState(b).getCollisionShape(l,b).isEmpty()&&l.getBlockState(b.above()).getCollisionShape(l,b.above()).isEmpty()&&!l.getBlockState(b.below()).getCollisionShape(l,b.below()).isEmpty();
    }
    static boolean check(ServerPlayer p){
        boolean ok=true;CompoundTag d=state(p.serverLevel());
        for(String k:DEFAULTS.keySet()){
            if(d.getBoolean("prepared")&&List.of("rope","pin","bell","ledger").contains(k))continue;
            if(!safe(p.serverLevel(),pos(d,k))){say(p,"Check marker "+k+" at "+pos(d,k).toShortString()+". Stand in the correct clear location and /vd mark "+k);ok=false;}
        }
        if(ok)say(p,"Marker floor/headroom checks passed. Walk the route before filming.");return ok;
    }
    static void teleport(ServerPlayer p,String key){BlockPos b=pos(state(p.serverLevel()),key);if(safe(p.serverLevel(),b))p.teleportTo(p.serverLevel(),b.getX()+.5,b.getY(),b.getZ()+.5,key.equals("start")?180:p.getYRot(),0);else say(p,"Unsafe destination: "+key);}
    static void sfx(ServerPlayer p,String marker,SoundEvent e,float volume,float pitch){
        CompoundTag d=state(p.serverLevel());BlockPos b=pos(d,marker);
        String category=(e==VillageDawn.ELDER.get()||e==VillageDawn.FALSE_ELDER.get()||e==VillageDawn.IMITATION.get())?"voiceVolume":(e==VillageDawn.BREATH.get()?"ambienceVolume":"scareVolume");float scale=(d.contains("volume")?d.getFloat("volume"):1)*(d.contains(category)?d.getFloat(category):1);
        p.serverLevel().playSound(null,b,e,SoundSource.BLOCKS,volume*scale,pitch);
    }
    /** Remember only individual changed blocks, including any block-entity contents. */
    static void setRemembered(ServerLevel l,BlockPos b,BlockState replacement){
        CompoundTag d=state(l);ListTag list=d.getList("changes",Tag.TAG_COMPOUND);
        boolean known=false;for(Tag t:list)if(((CompoundTag)t).getLong("pos")==b.asLong())known=true;
        if(!known){CompoundTag n=new CompoundTag();n.putLong("pos",b.asLong());n.put("state",NbtUtils.writeBlockState(l.getBlockState(b)));BlockEntity be=l.getBlockEntity(b);if(be!=null)n.put("be",be.saveWithFullMetadata());list.add(n);d.put("changes",list);}
        l.setBlock(b,replacement,3);dirty(l);
    }
    static void restore(ServerLevel l){
        CompoundTag d=state(l);ListTag list=d.getList("changes",Tag.TAG_COMPOUND);
        for(int i=list.size()-1;i>=0;i--){CompoundTag n=list.getCompound(i);BlockPos b=BlockPos.of(n.getLong("pos"));l.getChunkAt(b);l.setBlock(b,NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),n.getCompound("state")),3);if(n.contains("be")&&l.getBlockEntity(b)!=null){l.getBlockEntity(b).load(n.getCompound("be"));l.getBlockEntity(b).setChanged();}}
        d.remove("changes");
        for(Tag t:d.getList("residents",Tag.TAG_COMPOUND)){CompoundTag n=(CompoundTag)t;l.getChunkAt(BlockPos.of(n.getLong("pos")));Entity e=l.getEntity(n.getUUID("uuid"));if(e instanceof Villager v){v.setNoAi(n.getBoolean("noai"));v.setSilent(n.getBoolean("silent"));v.setInvisible(n.getBoolean("invisible"));}}
        d.remove("residents");
        if(d.contains("dayTime"))l.setDayTime(d.getLong("dayTime"));
        if(d.contains("dayCycle"))l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(d.getBoolean("dayCycle"),l.getServer());
        cleanup(l);dirty(l);
    }
    static void cleanup(ServerLevel l){
        // UUIDs allow cleanup even after restart; explicitly load each known actor's last chunk.
        CompoundTag d=state(l);
        for(Tag t:d.getList("actors",Tag.TAG_COMPOUND)){CompoundTag n=(CompoundTag)t;BlockPos b=BlockPos.of(n.getLong("pos"));l.getChunkAt(b);Entity e=l.getEntity(n.getUUID("uuid"));if(e!=null&&e.getTags().contains(TAG))e.discard();}
        for(Entity e:l.getEntities().getAll())if(e.getTags().contains(TAG))e.discard();
        d.remove("actors");d.remove("caller");d.remove("elderActor");
    }
    static void track(ServerLevel l,Entity e){CompoundTag d=state(l),n=new CompoundTag();n.putUUID("uuid",e.getUUID());n.putLong("pos",e.blockPosition().asLong());ListTag list=d.getList("actors",Tag.TAG_COMPOUND);list.add(n);d.put("actors",list);dirty(l);}
    static Caller caller(ServerPlayer p,String marker,boolean chasing){
        ServerLevel l=p.serverLevel();CompoundTag d=state(l);Caller c=null;
        if(d.hasUUID("caller")&&l.getEntity(d.getUUID("caller")) instanceof Caller found)c=found;
        BlockPos b=pos(d,marker);if(!safe(l,b)){say(p,"Caller marker blocked: "+marker);return null;}
        if(c==null){c=VillageDawn.CALLER.get().create(l);if(c==null)return null;c.addTag(TAG);c.moveTo(b.getX()+.5,b.getY(),b.getZ()+.5,0,0);l.addFreshEntity(c);track(l,c);d.putUUID("caller",c.getUUID());}
        else c.moveTo(b.getX()+.5,b.getY(),b.getZ()+.5,0,0);
        float yaw=(float)(Math.atan2(p.getZ()-c.getZ(),p.getX()-c.getX())*180/Math.PI)-90;c.setYRot(yaw);c.setYHeadRot(yaw);c.setYBodyRot(yaw);c.direct(p.getUUID(),chasing);c.setInvulnerable(true);dirty(l);return c;
    }
    static void hideCaller(ServerLevel l){CompoundTag d=state(l);if(d.hasUUID("caller")){Entity e=l.getEntity(d.getUUID("caller"));if(e!=null)e.discard();d.remove("caller");}}
    static ItemStack named(Item item,String name){ItemStack s=new ItemStack(item);s.setHoverName(Component.literal(name));s.getOrCreateTag().putBoolean("VillageDawn",true);return s;}
    static void barrel(ServerPlayer p,String marker,ItemStack item){
        ServerLevel l=p.serverLevel();BlockPos b=pos(state(l),marker);setRemembered(l,b,Blocks.BARREL.defaultBlockState());
        if(l.getBlockEntity(b) instanceof BarrelBlockEntity be){be.setItem(13,item);be.setChanged();}
    }
    static ItemStack ledger(ServerPlayer p){
        ItemStack b=new ItemStack(Items.WRITTEN_BOOK);CompoundTag n=b.getOrCreateTag();n.putString("title","The Bell Keeper");n.putString("author","The Keeper");n.putBoolean("VillageDawn",true);
        ListTag pages=new ListTag();String[] texts={"Every dawn this village is taken away. Its people return with the next night. The travellers do not.","The road opens when the repaired bell sounds THREE times before dawn.","Missing travellers:\nArun\nRavi\nMohan\n\nExpected tonight: "+p.getGameProfile().getName(),"The rope is in the guest room loft. The fixing pin is in the forge. Fit BOTH at the bell. After ring three, the Caller crosses the church boundary. The road stays open for 90 seconds. Run.","It repeats. It does not answer. Do not follow a familiar voice."};
        for(String s:texts)pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(s))));n.put("pages",pages);return b;
    }
    static int prepare(ServerPlayer p){
        if(!owner(p)){say(p,"Another player owns this world's story session.");return 0;}
        ServerLevel l=p.serverLevel();CompoundTag d=state(l);
        if(d.getBoolean("prepared")){say(p,"Already prepared. /vd reset before preparing again.");return 0;}
        if(!check(p))return 0;
        d.putUUID("owner",p.getUUID());d.putBoolean("prepared",true);d.putInt("pace",1);d.putFloat("volume",1);
        d.putLong("dayTime",l.getDayTime());d.putBoolean("dayCycle",l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT));
        ListTag residents=new ListTag();BlockPos square=pos(d,"square");
        for(Villager v:l.getEntitiesOfClass(Villager.class,new AABB(square).inflate(220))){CompoundTag n=new CompoundTag();n.putUUID("uuid",v.getUUID());n.putBoolean("noai",v.isNoAi());n.putBoolean("silent",v.isSilent());n.putBoolean("invisible",v.isInvisible());n.putLong("pos",v.blockPosition().asLong());residents.add(n);v.setNoAi(true);v.setSilent(true);}
        d.put("residents",residents);
        barrel(p,"rope",named(VillageDawn.ROPE.get(),"Bell Rope"));barrel(p,"pin",named(VillageDawn.PIN.get(),"Fixing Pin"));
        setRemembered(l,pos(d,"ledger"),Blocks.LECTERN.defaultBlockState());
        if(l.getBlockEntity(pos(d,"ledger")) instanceof LecternBlockEntity be){be.setBook(ledger(p));be.setChanged();l.setBlock(pos(d,"ledger"),l.getBlockState(pos(d,"ledger")).setValue(LecternBlock.HAS_BOOK,true),3);}
        setRemembered(l,pos(d,"bell"),Blocks.BELL.defaultBlockState());
        Villager elder=EntityType.VILLAGER.create(l);if(elder!=null){BlockPos b=pos(d,"elder");elder.moveTo(b.getX()+.5,b.getY(),b.getZ()+.5,90,0);elder.setNoAi(true);elder.setInvulnerable(true);elder.setPersistenceRequired();elder.addTag(TAG);l.addFreshEntity(elder);track(l,elder);d.putUUID("elderActor",elder.getUUID());}
        dirty(l);say(p,"Prepared. Story objects placed with reset snapshots. Walk /vd goto markers, then /vd auto.");return 1;
    }
    static int start(ServerPlayer p,int delay){
        CompoundTag d=state(p.serverLevel());if(!owner(p)||!d.getBoolean("prepared")){say(p,"Use /vd prepare on your converted world copy first.");return 0;}
        if(d.getBoolean("running")){say(p,"Already running. Pause/resume or reset first.");return 0;}
        if(delay!=10&&delay!=15&&delay!=20){say(p,"Delay choices: 10, 15, 20 seconds.");return 0;}
        // Check travel markers, not occupied prop markers, after setup.
        for(String k:List.of("start","entrance","square","elder","room","forge","church","loop","exit"))if(!safe(p.serverLevel(),pos(d,k))){say(p,"Travel marker blocked: "+k);return 0;}
        d.putBoolean("running",true);d.putBoolean("paused",false);d.putInt("delay",delay*20);d.putInt("stage",0);d.putInt("ticks",0);d.putInt("flags",0);d.putBoolean("done",false);d.putInt("rings",0);d.putInt("escapeTicks",0);d.putBoolean("ropeFitted",false);d.putBoolean("pinFitted",false);
        p.serverLevel().setDayTime(15000);p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.serverLevel().getServer());
        teleport(p,"start");dirty(p.serverLevel());say(p,"Starting in "+delay+" seconds. Close chat and record. G opens controls.");return 1;
    }
    static boolean cue(CompoundTag d,int bit,int seconds){if(d.getInt("ticks")<seconds*20||(d.getInt("flags")&(1<<bit))!=0)return false;d.putInt("flags",d.getInt("flags")|(1<<bit));return true;}
    static void advance(ServerPlayer p){CompoundTag d=state(p.serverLevel());d.putInt("stage",d.getInt("stage")+1);d.putInt("ticks",0);d.putInt("flags",0);d.putBoolean("done",false);hideCaller(p.serverLevel());dirty(p.serverLevel());}
    static boolean consume(ServerPlayer p,Item item){for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(s.is(item)&&s.hasTag()&&s.getTag().getBoolean("VillageDawn")){s.shrink(1);p.getInventory().setChanged();return true;}}return false;}
    static boolean has(ServerPlayer p,Item item){for(ItemStack s:p.getInventory().items)if(s.is(item)&&s.hasTag()&&s.getTag().getBoolean("VillageDawn"))return true;return false;}
    static void objective(ServerPlayer p,String text){subtitle(p,text);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(ServerPlayer p:e.getServer().getPlayerList().getPlayers()){
            ServerLevel l=p.serverLevel();CompoundTag d=state(l);if(!owner(p)||!d.getBoolean("running")||d.getBoolean("paused"))continue;
            if(d.getInt("delay")>0){d.putInt("delay",d.getInt("delay")-1);dirty(l);continue;}
            if(!p.isAlive()||p.isSpectator()){pause(p,true);say(p,"Paused: player unavailable. Return and /vd resume.");continue;}
            int stage=d.getInt("stage");d.putInt("ticks",d.getInt("ticks")+Math.max(1,d.getInt("pace")));int t=d.getInt("ticks");
            // Avoid a chase continuing in unloaded/offline state. Actor pauses on logout separately.
            if(stage==0){if(cue(d,0,0))objective(p,"Follow the forest path to the village square.");if(near(p,pos(d,"square"),8))d.putBoolean("done",true);}
            if(stage==1){if(cue(d,0,0))objective(p,"Speak to the elder beside the square (right-click).");}
            if(stage==2){if(cue(d,0,0))objective(p,"Find the guest room across the bridge.");if(near(p,pos(d,"room"),4)){if(cue(d,1,0)){caller(p,"watcher",false);subtitle(p,"Someone is watching from a nearby house...");sfx(p,"watcher",VillageDawn.BREATH.get(),.6f,1);}d.putBoolean("done",true);}}
            if(stage==3){if(cue(d,0,0)){sfx(p,"bell",SoundEvents.BELL_BLOCK,1.8f,.8f);caller(p,"hall",false);for(Tag resident:d.getList("residents",Tag.TAG_COMPOUND)){Entity en=l.getEntity(((CompoundTag)resident).getUUID("uuid"));if(en instanceof Villager v)v.setInvisible(true);}subtitle(p,"The first bell. Do not open the door.");}if(cue(d,1,12))sfx(p,"room",VillageDawn.KNOCK.get(),1,1);if(cue(d,2,18)){sfx(p,"room",VillageDawn.FALSE_ELDER.get(),1,1);subtitle(p,"Beta, neeche aa jao. Tumhein doosra kamra dikhana hai.");}if(cue(d,3,34)){sfx(p,"room",VillageDawn.FALSE_ELDER.get(),1,1);subtitle(p,"The SAME sentence. The SAME voice.");}if(cue(d,4,55)){hideCaller(l);objective(p,"The footsteps stop. Leave and try the forest road.");}d.putBoolean("done",true);}
            if(stage==4){if(cue(d,0,0))objective(p,"Try the forest road you came from.");if(near(p,pos(d,"loop"),5)&&!d.getBoolean("done")){p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,30,0,false,false));teleport(p,"entrance");sfx(p,"entrance",VillageDawn.STING.get(),.6f,1);subtitle(p,"The road returned you to the SAME village.");d.putBoolean("done",true);}}
            if(stage==5){if(cue(d,0,0))objective(p,"Find the bell keeper's ledger in the church. Right-click the lectern.");}
            if(stage==6){if(cue(d,0,0))objective(p,"Inspect the bell. The rope and fixing pin are missing.");if(cue(d,1,15))subtitle(p,"Rope: guest-room loft. Pin: forge. THREE rings open the road.");if(near(p,pos(d,"church"),6))d.putBoolean("done",true);}
            if(stage==7){if(cue(d,0,0))objective(p,"Retrieve the Bell Rope from the guest-room barrel.");if(has(p,VillageDawn.ROPE.get())){if(cue(d,1,0)){sfx(p,"room",VillageDawn.FALSE_ELDER.get(),1,.92f);subtitle(p,"Beta, neeche aa jao...");}d.putBoolean("done",true);}}
            if(stage==8){if(cue(d,0,0))objective(p,"Retrieve the Fixing Pin from the forge barrel.");if(has(p,VillageDawn.PIN.get())){if(cue(d,1,0)){sfx(p,"forge",VillageDawn.IMITATION.get(),1,1);subtitle(p,"Ek bed mil jaaye toh subah nikal jaaunga... [voice-copy cue]");caller(p,"forge",false);}d.putBoolean("done",true);}}
            if(stage==9){if(cue(d,0,0))objective(p,"Return to the church. Right-click the bell to fit BOTH items.");if(d.getBoolean("ropeFitted")&&d.getBoolean("pinFitted"))d.putBoolean("done",true);}
            if(stage==10){if(cue(d,0,0))objective(p,"Ring the repaired bell THREE times. Then run to the forest exit.");if(d.getInt("rings")==3){advance(p);d=state(l);d.putInt("escapeTicks",1800);Caller c=caller(p,"pursuit",true);if(c!=null)sfx(p,"church",VillageDawn.ROAR.get(),1.4f,1);subtitle(p,"90 SECONDS. Square → bridge → forest → exit.");}}
            if(stage==11){
                int left=d.getInt("escapeTicks")-1;d.putInt("escapeTicks",left);
                if(left%100==0)subtitle(p,"Escape: "+Math.max(0,left/20)+" seconds");
                if(near(p,pos(d,"exit"),6)){hideCaller(l);advance(p);l.setDayTime(23000);subtitle(p,"You crossed the boundary. The Caller stopped.");}
                else if(left<=0){pause(p,true);say(p,"Escape take ended. /vd retrychase resets ONLY the chase for another recording take.");}
            }
            if(stage==12){if(cue(d,0,12)){l.setDayTime(0);subtitle(p,"Sunrise. Record the matching empty-clearing cut separately.");}if(cue(d,1,30)){caller(p,"exit",false);sfx(p,"exit",VillageDawn.IMITATION.get(),.85f,1);subtitle(p,"Ek bed mil jaaye toh subah nikal jaaunga...");}if(t>=45*20){d.putBoolean("running",false);hideCaller(l);say(p,"Ending take complete. /vd reset restores the story props and residents.");}}
            if(stage>=0&&stage<10&&d.getInt("stage")==stage&&d.getBoolean("done")&&t>=MIN[stage]*20)advance(p);
            dirty(l);
        }
    }
    static void pause(ServerPlayer p,boolean value){CompoundTag d=state(p.serverLevel());d.putBoolean("paused",value);if(d.hasUUID("caller")&&p.serverLevel().getEntity(d.getUUID("caller")) instanceof Caller c)c.direct(p.getUUID(),!value&&d.getInt("stage")==11);dirty(p.serverLevel());}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p&&owner(p)&&state(p.serverLevel()).getBoolean("running"))pause(p,true);}
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&owner(p)&&state(p.serverLevel()).getBoolean("running"))say(p,"Story paused across login. /vd resume when ready.");}
    @SubscribeEvent public void entityInteract(PlayerInteractEvent.EntityInteract e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!owner(p))return;CompoundTag d=state(p.serverLevel());
        if(!d.getBoolean("running")||d.getBoolean("paused")||d.getInt("stage")!=1||!d.hasUUID("elderActor")||!e.getTarget().getUUID().equals(d.getUUID("elderActor")))return;
        if(!d.getBoolean("done")){p.getInventory().add(named(VillageDawn.KEY.get(),"Bakery Key"));p.getInventory().add(new ItemStack(Items.BREAD,3));sfx(p,"elder",VillageDawn.ELDER.get(),1,1);subtitle(p,"Subah se pehle nikal jaana. Pehli ghanti ke baad darwaaza mat kholna.");d.putBoolean("done",true);dirty(p.serverLevel());}e.setCanceled(true);
    }
    @SubscribeEvent public void blockInteract(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!owner(p)||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        ServerLevel l=p.serverLevel();CompoundTag d=state(l);if(!d.getBoolean("running")||d.getBoolean("paused"))return;
        int s=d.getInt("stage");BlockPos clicked=e.getPos();
        if(clicked.equals(pos(d,"ledger"))&&s==5){d.putBoolean("done",true);subtitle(p,"The ledger predicted your arrival. Read all five pages.");dirty(l);}
        if(clicked.equals(pos(d,"rope"))&&s<7){e.setCanceled(true);subtitle(p,"Investigate the church before searching the loft.");}
        if(clicked.equals(pos(d,"pin"))&&s<8){e.setCanceled(true);subtitle(p,"Find the rope first.");}
        if(clicked.equals(pos(d,"bell"))){
            e.setCanceled(true);
            if(s==9){if(!d.getBoolean("ropeFitted")&&consume(p,VillageDawn.ROPE.get())){d.putBoolean("ropeFitted",true);subtitle(p,"Bell Rope fitted.");}else if(!d.getBoolean("pinFitted")&&consume(p,VillageDawn.PIN.get())){d.putBoolean("pinFitted",true);subtitle(p,"Fixing Pin fitted. Wait, then ring three times.");}else subtitle(p,"Both marked story items are required.");}
            else if(s==10){long now=l.getGameTime();if(now>=d.getLong("nextRing")&&d.getInt("rings")<3){int count=d.getInt("rings")+1;d.putInt("rings",count);d.putLong("nextRing",now+100);sfx(p,"bell",SoundEvents.BELL_BLOCK,2,.8f);subtitle(p,"Bell "+count+" / 3"+ (count==3?" — wait for the escape cue":""));}}
            else subtitle(p,"The bell mechanism is not ready.");dirty(l);
        }
    }
    @SubscribeEvent public void breakProp(net.minecraftforge.event.level.BlockEvent.BreakEvent e){
        if(!(e.getLevel() instanceof ServerLevel l)||!state(l).getBoolean("prepared"))return;
        for(String k:List.of("rope","pin","bell","ledger"))if(e.getPos().equals(pos(state(l),k))){e.setCanceled(true);return;}
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent e){
        var root=Commands.literal("vd").requires(s->s.hasPermission(2));
        root.then(Commands.literal("help").executes(c->{say(c.getSource().getPlayerOrException(),"/vd prepare | check | auto [10/15/20] | pause | resume | status | reset | mark NAME | goto NAME | pace 1..10 | volume 0..1 | retrychase");return 1;}));
        root.then(Commands.literal("check").executes(c->check(c.getSource().getPlayerOrException())?1:0));
        root.then(Commands.literal("prepare").executes(c->prepare(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("auto").executes(c->start(c.getSource().getPlayerOrException(),20)).then(Commands.argument("delay",IntegerArgumentType.integer(10,20)).executes(c->start(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"delay")))));
        for(String action:List.of("pause","resume","status","reset","retrychase"))root.then(Commands.literal(action).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();if(!owner(p)){say(p,"Another player owns this session.");return 0;}CompoundTag d=state(p.serverLevel());switch(action){
            case "pause"-> {pause(p,true);say(p,"Paused.");}
            case "resume"-> {pause(p,false);say(p,"Resumed.");}
            case "status"-> {int s=d.getInt("stage");say(p,"Prepared="+d.getBoolean("prepared")+", running="+d.getBoolean("running")+", paused="+d.getBoolean("paused")+", scene="+(s>=0&&s<NAMES.length?NAMES[s]:s)+", scene seconds="+d.getInt("ticks")/20+", pace="+d.getInt("pace")+", rings="+d.getInt("rings"));}
            case "reset"-> {d.putBoolean("running",false);restore(p.serverLevel());CompoundTag markers=d.getCompound("markers").copy();StoryData.get(p.serverLevel()).data=new CompoundTag();state(p.serverLevel()).put("markers",markers);for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack stack=p.getInventory().getItem(i);if(stack.hasTag()&&stack.getTag().getBoolean("VillageDawn"))p.getInventory().setItem(i,ItemStack.EMPTY);}say(p,"Reset restored changed blocks and resident flags. /vd prepare to repeat.");}
            case "retrychase"-> {if(!d.getBoolean("prepared")||d.getInt("stage")<11){say(p,"Only available after reaching the chase.");return 0;}hideCaller(p.serverLevel());teleport(p,"church");d.putInt("stage",11);d.putInt("ticks",0);d.putInt("flags",0);d.putInt("escapeTicks",1800);d.putBoolean("running",true);d.putBoolean("paused",false);caller(p,"pursuit",true);say(p,"Chase take restarted. Exit marker remains the target.");}
        }dirty(p.serverLevel());return 1;}));
        root.then(Commands.literal("mark").then(Commands.argument("name",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(DEFAULTS.keySet(),b)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();String k=StringArgumentType.getString(c,"name");CompoundTag d=state(p.serverLevel());if(!owner(p)||d.getBoolean("prepared")){say(p,"Reset before changing markers.");return 0;}if(!DEFAULTS.containsKey(k)||!safe(p.serverLevel(),p.blockPosition())){say(p,"Unknown name or unsafe player position.");return 0;}CompoundTag m=d.getCompound("markers");m.putLong(k,p.blockPosition().asLong());d.put("markers",m);dirty(p.serverLevel());say(p,"Marked "+k+" at "+p.blockPosition().toShortString());return 1;})));
        root.then(Commands.literal("goto").then(Commands.argument("name",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(DEFAULTS.keySet(),b)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();String k=StringArgumentType.getString(c,"name");if(!DEFAULTS.containsKey(k)){say(p,"Unknown marker.");return 0;}teleport(p,k);return 1;})));
        root.then(Commands.literal("pace").then(Commands.argument("speed",IntegerArgumentType.integer(1,10)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();if(!owner(p))return 0;state(p.serverLevel()).putInt("pace",IntegerArgumentType.getInteger(c,"speed"));dirty(p.serverLevel());say(p,"Pace changed. 1 = filming, 10 = rehearsal. Chase always uses real 90 seconds.");return 1;})));
        root.then(Commands.literal("volume").then(Commands.argument("value",FloatArgumentType.floatArg(0,1)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();if(!owner(p))return 0;state(p.serverLevel()).putFloat("volume",FloatArgumentType.getFloat(c,"value"));dirty(p.serverLevel());return 1;})));
        root.then(Commands.literal("mix").then(Commands.argument("channel",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(List.of("voice","ambience","scare"),b)).then(Commands.argument("value",FloatArgumentType.floatArg(0,1)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();if(!owner(p))return 0;String channel=StringArgumentType.getString(c,"channel");if(!List.of("voice","ambience","scare").contains(channel))return 0;state(p.serverLevel()).putFloat(channel+"Volume",FloatArgumentType.getFloat(c,"value"));dirty(p.serverLevel());return 1;}))));
        e.getDispatcher().register(root);
    }
}

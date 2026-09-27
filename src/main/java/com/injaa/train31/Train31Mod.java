package com.injaa.train31;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Train31Mod.MODID)
public class Train31Mod {
    public static final String MODID="train31";

    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MODID);

    public static final RegistryObject<Item> DIRECTOR=ITEMS.register("train31_director",()->new DirectorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<EntityType<ShadowGirlEntity>> SHADOW_GIRL=ENTITIES.register("shadow_girl",()->
            EntityType.Builder.<ShadowGirlEntity>of(ShadowGirlEntity::new, MobCategory.MISC)
                    .sized(0.58f,1.82f).clientTrackingRange(12).updateInterval(2).build("shadow_girl"));

    public static final RegistryObject<SoundEvent> PA_NORMAL=sound("pa_normal");
    public static final RegistryObject<SoundEvent> PA_TRAIN31=sound("pa_train31");
    public static final RegistryObject<SoundEvent> FLUORESCENT_BUZZ=sound("fluorescent_buzz");
    public static final RegistryObject<SoundEvent> CCTV_STATIC=sound("cctv_static");
    public static final RegistryObject<SoundEvent> TUNNEL_RUMBLE=sound("tunnel_rumble");
    public static final RegistryObject<SoundEvent> METAL_KNOCKS=sound("metal_knocks");
    public static final RegistryObject<SoundEvent> CAMERA_CLICK=sound("camera_click");
    public static final RegistryObject<SoundEvent> TRAIN_HORN=sound("train_horn");

    private static RegistryObject<SoundEvent> sound(String id){
        return SOUNDS.register(id,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID,id)));
    }

    public Train31Mod(){
        var bus= FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); SOUNDS.register(bus); ENTITIES.register(bus);
        bus.addListener(this::attributes);
        Train31Network.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void attributes(EntityAttributeCreationEvent e){
        e.put(SHADOW_GIRL.get(),ShadowGirlEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(p.getInventory().items.stream().noneMatch(s->s.is(DIRECTOR.get())))p.getInventory().add(new ItemStack(DIRECTOR.get()));
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent e){
        if(e.phase==TickEvent.Phase.END && !e.player.level().isClientSide && e.player instanceof ServerPlayer p) StoryDirector.tick(p);
    }

    @SubscribeEvent
    public void monitor(PlayerInteractEvent.RightClickBlock e){
        if(e.getLevel().isClientSide || !(e.getEntity() instanceof ServerPlayer p))return;
        if(!StationBuilder.isMonitorClick(p,e.getPos()))return;
        if(p.isShiftKeyDown()) StationBuilder.exitCamera(p); else StationBuilder.cycleCamera(p);
        e.setCancellationResult(InteractionResult.SUCCESS); e.setCanceled(true);
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("train31")
                .then(Commands.literal("set")
                        .then(Commands.literal("start").executes(c->{SceneSetup.markStart(c.getSource().getPlayerOrException());return 1;}))
                        .then(Commands.literal("cctv").executes(c->{SceneSetup.markCctv(c.getSource().getPlayerOrException());return 1;}))
                        .then(Commands.literal("platform").executes(c->{SceneSetup.markPlatform(c.getSource().getPlayerOrException());return 1;})))
                .then(Commands.literal("setup").executes(c->{SceneSetup.status(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("clearsetup").executes(c->{SceneSetup.clear(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("prepare").executes(c->{StationBuilder.prepare(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("start").then(Commands.argument("delay", IntegerArgumentType.integer(0,60)).executes(c->{StoryDirector.start(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"delay"));return 1;})))
                .then(Commands.literal("reset").executes(c->{StoryDirector.reset(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("skip").executes(c->{StoryDirector.skip(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("cam").then(Commands.argument("index",IntegerArgumentType.integer(1,4)).executes(c->{StationBuilder.enterCamera(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"index")-1);return 1;})))
        );
    }
}

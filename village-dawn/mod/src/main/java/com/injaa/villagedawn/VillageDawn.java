package com.injaa.villagedawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(VillageDawn.ID)
public class VillageDawn {
    public static final String ID="villagedawn";
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
    public static final RegistryObject<EntityType<Caller>> CALLER=ENTITIES.register("caller",()->EntityType.Builder.<Caller>of(Caller::new,MobCategory.MONSTER).sized(.65f,2.5f).clientTrackingRange(12).build(ID+":caller"));
    public static final RegistryObject<Item> ROPE=ITEMS.register("bell_rope",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PIN=ITEMS.register("fixing_pin",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> KEY=ITEMS.register("bakery_key",()->new Item(new Item.Properties().stacksTo(1)));
    public static RegistryObject<SoundEvent> sound(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,name)));}
    public static final RegistryObject<SoundEvent> KNOCK=sound("knock"),BREATH=sound("breath"),ROAR=sound("roar"),STING=sound("sting"),ELDER=sound("elder"),FALSE_ELDER=sound("false_elder"),IMITATION=sound("imitation");
    public VillageDawn(){
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);ITEMS.register(bus);SOUNDS.register(bus);
        bus.addListener(this::attributes);bus.addListener(this::creative);
        MinecraftForge.EVENT_BUS.register(new Director());
    }
    private void attributes(EntityAttributeCreationEvent e){e.put(CALLER.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,80).add(Attributes.MOVEMENT_SPEED,.30).add(Attributes.FOLLOW_RANGE,64).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).build());}
    private void creative(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES){e.accept(ROPE);e.accept(PIN);e.accept(KEY);}}
}

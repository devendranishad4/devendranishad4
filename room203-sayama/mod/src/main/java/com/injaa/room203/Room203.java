package com.injaa.room203;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(Room203.ID)
public final class Room203 {
 public static final String ID="room203";
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
 public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
 public static final RegistryObject<EntityType<Visitor>> VISITOR=ENTITIES.register("visitor",()->EntityType.Builder.of(Visitor::new,MobCategory.MONSTER).sized(.58f,1.95f).clientTrackingRange(48).build("visitor"));
 public static final String[] AUDIO={"rain","hall","knock","steps","breath","fault","sting","pursuit"};
 public static final java.util.Map<String,RegistryObject<SoundEvent>> AUDIO_EVENTS=new java.util.HashMap<>();
 static { for(String s:AUDIO) AUDIO_EVENTS.put(s,SOUNDS.register(s,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,s)))); }
 public Room203(){
  var bus=FMLJavaModLoadingContext.get().getModEventBus();ENTITIES.register(bus);SOUNDS.register(bus);
  bus.addListener((EntityAttributeCreationEvent e)->e.put(VISITOR.get(),net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,80).add(Attributes.MOVEMENT_SPEED,.27).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.FOLLOW_RANGE,64).build()));
  HudNetwork.register();MinecraftForge.EVENT_BUS.register(new Director());
 }
}

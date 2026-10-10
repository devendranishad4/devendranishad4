package com.devendra.slenderv2;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SlenderManMod.MODID)
public final class SlenderManMod {
    public static final String MODID = "slenderman_director";

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<SlenderEntity>> SLENDER =
            ENTITIES.register("slender_man", () ->
                    EntityType.Builder.of(SlenderEntity::new, MobCategory.MONSTER)
                            .sized(0.65F, 3.6F)
                            .clientTrackingRange(96)
                            .build(MODID + ":slender_man"));

    public SlenderManMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        modBus.addListener(this::attributes);
        modBus.addListener(this::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(SLENDER.get(), SlenderEntity.createAttributes().build());
    }

    private void commands(RegisterCommandsEvent event) {
        SlenderCommands.register(event.getDispatcher());
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SLENDER.get(), SlenderRenderer::new);
    }
}

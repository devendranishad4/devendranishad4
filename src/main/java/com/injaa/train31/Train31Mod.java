package com.injaa.train31;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Train31Mod.MODID)
public class Train31Mod {
    public static final String MODID = "train31";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<Item> DIRECTOR = ITEMS.register("train31_director", () -> new DirectorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<SoundEvent> PA_FEMALE = SOUNDS.register("pa_female",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID,"pa_female")));

    public Train31Mod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        SOUNDS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean hasDirector = player.getInventory().items.stream().anyMatch(s -> s.is(DIRECTOR.get()));
        if (!hasDirector) player.getInventory().add(new ItemStack(DIRECTOR.get()));

        player.sendSystemMessage(Component.literal("§c[Train 31: Tokyo Edition] §fDirector ready. Right-click = link Tokyo subway. Crouch + right-click = start AUTO (20s delay)."));
        player.sendSystemMessage(Component.literal("§7Quick tests: /train31 cctv  |  /train31 start 0  |  /train31 reset"));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (event.player instanceof ServerPlayer player) StoryDirector.tick(player);
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("train31")
            .then(Commands.literal("build").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                StationBuilder.build(p);
                return 1;
            }))
            .then(Commands.literal("cctv").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                StationBuilder.teleportToCctv(p);
                p.sendSystemMessage(Component.literal("§b[Train 31] CCTV security room test."));
                return 1;
            }))
            .then(Commands.literal("start")
                .then(Commands.argument("delay", IntegerArgumentType.integer(0, 60))
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        int delay = IntegerArgumentType.getInteger(ctx, "delay");
                        StoryDirector.start(p, delay);
                        return 1;
                    })))
            .then(Commands.literal("reset").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                StoryDirector.reset(p);
                return 1;
            }))
            .then(Commands.literal("skip").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                StoryDirector.skip(p);
                return 1;
            }))
        );
    }
}

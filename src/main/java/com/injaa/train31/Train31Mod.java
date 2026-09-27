package com.injaa.train31;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    public static final RegistryObject<Item> DIRECTOR = ITEMS.register("train31_director", () -> new DirectorItem(new Item.Properties().stacksTo(1)));

    public Train31Mod() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean hasDirector = player.getInventory().items.stream().anyMatch(s -> s.is(DIRECTOR.get()));
        if (!hasDirector) {
            player.getInventory().add(new ItemStack(DIRECTOR.get()));
            player.sendSystemMessage(Component.literal("§c[Train 31] §fDirector item added. Right-click = build map, crouch + right-click = start AUTO (20s delay)."));
        }
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
            .then(Commands.literal("start")
                .then(Commands.argument("delay", net.minecraft.commands.arguments.IntegerArgumentType.integer(0, 60))
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        int delay = net.minecraft.commands.arguments.IntegerArgumentType.getInteger(ctx, "delay");
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

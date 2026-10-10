package com.devendra.lightdirector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LightDirectorCommands {
    private LightDirectorCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("lightdirector")
                .then(Commands.literal("blackout").executes(ctx -> blackout(ctx.getSource())))
                .then(Commands.literal("flicker").executes(ctx -> flicker(ctx.getSource())))
                .then(Commands.literal("restore").executes(ctx -> restore(ctx.getSource())))
                .then(Commands.literal("stop").executes(ctx -> restore(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("nextrange").executes(ctx -> nextRange(ctx.getSource())))
                .then(Commands.literal("nextspeed").executes(ctx -> nextSpeed(ctx.getSource())))
                .then(Commands.literal("range")
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(32, 2500))
                                .executes(ctx -> range(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "blocks")))))
                .then(Commands.literal("speed")
                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.2D, 3.0D))
                                .executes(ctx -> speed(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "multiplier")))));

        dispatcher.register(root);

        dispatcher.register(Commands.literal("ld")
                .then(Commands.literal("blackout").executes(ctx -> blackout(ctx.getSource())))
                .then(Commands.literal("flicker").executes(ctx -> flicker(ctx.getSource())))
                .then(Commands.literal("restore").executes(ctx -> restore(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static ServerPlayer player(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return source.getPlayerOrException();
    }

    private static int blackout(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        LightDirectorController.startBlackout(p);
        source.sendSuccess(() -> Component.literal("Light Director blackout started."), false);
        return 1;
    }

    private static int flicker(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        LightDirectorController.startFlicker(p);
        source.sendSuccess(() -> Component.literal("Light Director flicker started."), false);
        return 1;
    }

    private static int restore(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        LightDirectorController.restore(p.serverLevel());
        source.sendSuccess(() -> Component.literal("Light Director restore queued."), false);
        return 1;
    }

    private static int range(CommandSourceStack source, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        int snapped = LightDirectorController.setRange(p, value);
        source.sendSuccess(() -> Component.literal("Light Director range: " + snapped + " blocks."), false);
        return 1;
    }

    private static int speed(CommandSourceStack source, double value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        double set = LightDirectorController.setSpeed(p, value);
        source.sendSuccess(() -> Component.literal("Light Director speed: " + set + "x."), false);
        return 1;
    }

    private static int nextRange(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        int set = LightDirectorController.cycleRange(p);
        source.sendSuccess(() -> Component.literal("Light Director range: " + set + " blocks."), false);
        return 1;
    }

    private static int nextSpeed(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        double set = LightDirectorController.cycleSpeed(p);
        source.sendSuccess(() -> Component.literal("Light Director speed: " + set + "x."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(source);
        source.sendSuccess(() -> Component.literal(
                "Light Director | range " + LightDirectorController.getRange(p) +
                " | speed " + LightDirectorController.getSpeed(p) + "x | " +
                LightDirectorController.status(p.serverLevel())), false);
        return 1;
    }
}

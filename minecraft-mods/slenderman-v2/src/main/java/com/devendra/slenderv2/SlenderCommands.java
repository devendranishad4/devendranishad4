package com.devendra.slenderv2;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SlenderCommands {
    private SlenderCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slender")
                .then(Commands.literal("spawn").executes(ctx -> spawn(ctx.getSource())))
                .then(Commands.literal("stalk").executes(ctx -> mode(ctx.getSource(), SlenderEntity.MODE_STALK, "stalk")))
                .then(Commands.literal("hunt").executes(ctx -> mode(ctx.getSource(), SlenderEntity.MODE_HUNT, "hunt")))
                .then(Commands.literal("stop").executes(ctx -> mode(ctx.getSource(), SlenderEntity.MODE_STOP, "stop")))
                .then(Commands.literal("teleport").executes(ctx -> teleport(ctx.getSource())))
                .then(Commands.literal("scare").executes(ctx -> scare(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    private static int spawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SlenderEntity entity = SlenderManMod.SLENDER.get().create(source.getLevel());
        if (entity == null) return 0;
        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        source.getLevel().addFreshEntity(entity);
        entity.teleportBehind(player);
        source.sendSuccess(() -> Component.literal("Slender Man spawned in stalk mode."), false);
        return 1;
    }

    private static SlenderEntity nearest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return source.getLevel().getEntitiesOfClass(
                SlenderEntity.class,
                player.getBoundingBox().inflate(160.0D)
        ).stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    private static int mode(CommandSourceStack source, int mode, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        SlenderEntity entity = nearest(source);
        if (entity == null) {
            source.sendFailure(Component.literal("No Slender Man nearby."));
            return 0;
        }
        entity.setMode(mode);
        source.sendSuccess(() -> Component.literal("Slender mode: " + name), false);
        return 1;
    }

    private static int teleport(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SlenderEntity entity = nearest(source);
        if (entity == null) return 0;
        return entity.teleportBehind(player) ? 1 : 0;
    }

    private static int scare(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SlenderEntity entity = nearest(source);
        if (entity == null) return 0;
        entity.scare(player);
        return 1;
    }

    private static int clear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var list = source.getLevel().getEntitiesOfClass(
                SlenderEntity.class,
                player.getBoundingBox().inflate(256.0D)
        );
        list.forEach(net.minecraft.world.entity.Entity::discard);
        source.sendSuccess(() -> Component.literal("Removed " + list.size() + " Slender Man entity(s)."), false);
        return list.size();
    }
}

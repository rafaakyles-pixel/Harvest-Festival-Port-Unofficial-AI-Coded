package harvestfestival.player.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import harvestfestival.player.PlayerGold;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class GoldCommands {
    private GoldCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var gold = Commands.literal("gold")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        addOperations(gold, false);
        var player = Commands.argument("player", EntityArgument.player());
        addOperations(player, true);
        gold.then(player);
        event.getDispatcher().register(Commands.literal("hf").then(gold));
    }

    private static void addOperations(ArgumentBuilder<CommandSourceStack, ?> parent, boolean targeted) {
        parent.then(Commands.literal("get")
                .executes(context -> execute(context, targeted, "get")));
        for (String operation : new String[] {"set", "add"}) {
            parent.then(Commands.literal(operation)
                    .then(Commands.argument("value", LongArgumentType.longArg())
                            .executes(context -> execute(context, targeted, operation))));
        }
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean targeted, String operation)
            throws CommandSyntaxException {
        ServerPlayer player = targeted ? EntityArgument.getPlayer(context, "player")
                : context.getSource().getPlayerOrException();
        if (operation.equals("set")) {
            PlayerGold.set(player, LongArgumentType.getLong(context, "value"));
        } else if (operation.equals("add")) {
            PlayerGold.add(player, LongArgumentType.getLong(context, "value"));
        }
        long balance = PlayerGold.get(player);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.harvestfestival.gold." + operation,
                        player.getDisplayName(), balance), !operation.equals("get"));
        return 1;
    }
}

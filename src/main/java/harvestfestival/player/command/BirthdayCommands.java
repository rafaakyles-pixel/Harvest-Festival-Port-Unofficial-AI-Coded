package harvestfestival.player.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import harvestfestival.calendar.BirthdayDate;
import harvestfestival.calendar.CalendarDate;
import harvestfestival.calendar.Season;
import harvestfestival.player.PlayerBirthday;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BirthdayCommands {
    private BirthdayCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var birthday = Commands.literal("birthday")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        addOperations(birthday, false);
        var player = Commands.argument("player", EntityArgument.player());
        addOperations(player, true);
        event.getDispatcher().register(Commands.literal("hf").then(birthday.then(player)));
    }

    private static void addOperations(ArgumentBuilder<CommandSourceStack, ?> parent, boolean targeted) {
        for (String operation : new String[]{"get", "clear"}) {
            parent.then(Commands.literal(operation).executes(context -> execute(context, targeted, operation)));
        }
        parent.then(Commands.literal("set")
                .then(Commands.argument("season", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(Season.values()).map(Season::serializedName), builder))
                        .then(Commands.argument("day", IntegerArgumentType.integer(1, CalendarDate.DAYS_PER_SEASON))
                                .executes(context -> execute(context, targeted, "set")))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean targeted, String operation)
            throws CommandSyntaxException {
        var player = targeted ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayerOrException();
        if (operation.equals("set")) {
            Season season;
            try {
                season = Season.valueOf(StringArgumentType.getString(context, "season").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new SimpleCommandExceptionType(Component.translatable(
                        "commands.harvestfestival.calendar.invalid_season")).create();
            }
            PlayerBirthday.set(player, new BirthdayDate(season, IntegerArgumentType.getInteger(context, "day")));
        } else if (operation.equals("clear")) {
            PlayerBirthday.clear(player);
        }
        var date = PlayerBirthday.get(player);
        context.getSource().sendSuccess(() -> date.map(value -> Component.translatable(
                        "commands.harvestfestival.birthday.date", player.getDisplayName(), value.day(),
                        Component.translatable("harvestfestival.season." + value.season().serializedName())))
                .orElseGet(() -> Component.translatable("commands.harvestfestival.birthday.absent", player.getDisplayName())),
                !operation.equals("get"));
        return 1;
    }
}

package harvestfestival.calendar.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import harvestfestival.calendar.CalendarDate;
import harvestfestival.calendar.Season;
import harvestfestival.calendar.WorldCalendar;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CalendarCommands {
    private static final SimpleCommandExceptionType INVALID_SEASON = new SimpleCommandExceptionType(
            Component.translatable("commands.harvestfestival.calendar.invalid_season"));
    private static final SimpleCommandExceptionType OUT_OF_RANGE = new SimpleCommandExceptionType(
            Component.translatable("commands.harvestfestival.calendar.out_of_range"));

    private CalendarCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hf")
                .then(Commands.literal("day")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> report(context.getSource(), false))
                        .then(Commands.argument("day", IntegerArgumentType.integer())
                                .executes(context -> {
                                    var source = context.getSource();
                                    var current = WorldCalendar.get(source.getServer());
                                    int day = Math.clamp(IntegerArgumentType.getInteger(context, "day"),
                                            1, CalendarDate.DAYS_PER_SEASON);
                                    return set(source, CalendarDate.of(day, current.season(), current.year()));
                                })))
                .then(Commands.literal("season")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("season", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Season.values()).map(Season::serializedName), builder))
                                .executes(context -> {
                                    Season season;
                                    try {
                                        season = Season.valueOf(StringArgumentType.getString(context, "season")
                                                .toUpperCase(Locale.ROOT));
                                    } catch (IllegalArgumentException exception) {
                                        throw INVALID_SEASON.create();
                                    }
                                    var source = context.getSource();
                                    var current = WorldCalendar.get(source.getServer());
                                    return set(source, CalendarDate.of(current.day(), season, current.year()));
                                })))
                .then(Commands.literal("year")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("year", IntegerArgumentType.integer())
                                .executes(context -> {
                                    var source = context.getSource();
                                    var current = WorldCalendar.get(source.getServer());
                                    int year = Math.max(1, IntegerArgumentType.getInteger(context, "year"));
                                    return set(source, CalendarDate.of(current.day(), current.season(), year));
                                })))
                .then(Commands.literal("newDay")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            try {
                                WorldCalendar.prepareNextDay(context.getSource().getServer());
                            } catch (ArithmeticException exception) {
                                throw OUT_OF_RANGE.create();
                            }
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("commands.harvestfestival.calendar.next_day"), true);
                            return 1;
                        })));
    }

    private static int set(CommandSourceStack source, CalendarDate date) throws CommandSyntaxException {
        try {
            WorldCalendar.set(source.getServer(), date);
        } catch (ArithmeticException exception) {
            throw OUT_OF_RANGE.create();
        }
        return report(source, true);
    }

    private static int report(CommandSourceStack source, boolean changed) {
        var date = WorldCalendar.get(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.harvestfestival.calendar.date",
                date.day(), Component.translatable("harvestfestival.season." + date.season().serializedName()),
                date.year(), Component.translatable("harvestfestival.weekday."
                        + date.weekday().name().toLowerCase(Locale.ROOT))), changed);
        return 1;
    }
}
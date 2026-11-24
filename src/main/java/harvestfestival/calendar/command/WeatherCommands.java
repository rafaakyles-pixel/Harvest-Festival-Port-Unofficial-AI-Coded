package harvestfestival.calendar.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import harvestfestival.calendar.DailyWeather;
import harvestfestival.calendar.Weather;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WeatherCommands {
    private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
            Component.translatable("commands.harvestfestival.weather.invalid"));
    private WeatherCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hf").then(Commands.literal("weather")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    var source = context.getSource();
                    var data = DailyWeather.get(source.getServer());
                    source.sendSuccess(() -> Component.translatable("commands.harvestfestival.weather.report",
                            data.today().getSerializedName(), data.recent().getSerializedName(),
                            source.getLevel().rainLevel, source.getLevel().thunderLevel,
                            data.today().rain(), data.today().storm(), data.forecast().toString()), false);
                    return 1;
                })
                .then(Commands.argument("weather", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(Weather.values()).map(Weather::getSerializedName), builder))
                        .executes(context -> {
                            Weather weather;
                            try {
                                weather = Weather.valueOf(StringArgumentType.getString(context, "weather").toUpperCase(Locale.ROOT));
                            } catch (IllegalArgumentException invalid) { throw INVALID.create(); }
                            DailyWeather.setToday(context.getSource().getServer(), weather);
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    "commands.harvestfestival.weather.set", weather.getSerializedName()), true);
                            return 1;
                        }))));
    }
}

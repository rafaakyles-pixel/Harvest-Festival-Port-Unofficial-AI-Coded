package harvestfestival.calendar.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import harvestfestival.calendar.CalendarDate;
import harvestfestival.calendar.NpcBirthdays;
import harvestfestival.calendar.Season;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NpcBirthdayCommands {
    private NpcBirthdayCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hf").then(Commands.literal("npcBirthday")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("id", IdentifierArgument.id())
                        .then(Commands.literal("get").executes(context -> execute(context, false)))
                        .then(Commands.literal("test")
                                .then(Commands.argument("season", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Season.values()).map(Season::serializedName), builder))
                                        .then(Commands.argument("day", IntegerArgumentType.integer(1, CalendarDate.DAYS_PER_SEASON))
                                                .executes(context -> execute(context, true))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean test) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");
        if (test) {
            Season season;
            try {
                season = Season.valueOf(StringArgumentType.getString(context, "season").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new SimpleCommandExceptionType(Component.translatable("commands.harvestfestival.calendar.invalid_season")).create();
            }
            boolean matches = NpcBirthdays.INSTANCE.isBirthday(id,
                    CalendarDate.of(IntegerArgumentType.getInteger(context, "day"), season, 1));
            context.getSource().sendSuccess(() -> testResult(id, matches), false);
            return matches ? 1 : 0;
        }
        var date = NpcBirthdays.INSTANCE.get(id);
        context.getSource().sendSuccess(() -> date.map(value -> Component.translatable(
                        "commands.harvestfestival.birthday.date", id.toString(), value.day(),
                        Component.translatable("harvestfestival.season." + value.season().serializedName())))
                .orElseGet(() -> Component.translatable("commands.harvestfestival.birthday.absent", id.toString())), false);
        return date.isPresent() ? 1 : 0;
    }

    static Component testResult(Identifier id, boolean matches) {
        // Boolean translation arguments become numeric NBT tags on the network.
        return Component.translatable("commands.harvestfestival.birthday.test", id.toString(), Boolean.toString(matches));
    }
}

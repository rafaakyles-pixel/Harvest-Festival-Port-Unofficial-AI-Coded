package harvestfestival.calendar;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

public final class WorldCalendar {
    private WorldCalendar() {}

    private static Holder<WorldClock> clock(MinecraftServer server) {
        return server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
    }

    public static long ticks(MinecraftServer server) {
        return server.clockManager().getTotalTicks(clock(server));
    }

    public static CalendarDate get(MinecraftServer server) {
        return CalendarDate.fromTicks(ticks(server));
    }

    public static void set(MinecraftServer server, CalendarDate date) {
        server.clockManager().setTotalTicks(clock(server), date.atTimeOfDay(ticks(server)));
    }

    public static void prepareNextDay(MinecraftServer server) {
        long start = get(server).startTicks();
        // Match /hf newDay: stop one tick before the next day and let the clock advance.
        long lastTick = Math.addExact(start, CalendarDate.TICKS_PER_DAY - 1);
        server.clockManager().setTotalTicks(clock(server), lastTick);
    }
}
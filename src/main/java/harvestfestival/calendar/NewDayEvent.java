package harvestfestival.calendar;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Fired once on the logical server after a forward day transition.
 * Consumers must not simulate intermediate days or mutate the clock.
 */
public final class NewDayEvent extends Event {
    private final MinecraftServer server;
    private final CalendarDate previous;
    private final CalendarDate current;

    public NewDayEvent(MinecraftServer server, CalendarDate previous, CalendarDate current) {
        this.server = server;
        this.previous = previous;
        this.current = current;
    }

    public MinecraftServer getServer() { return server; }
    public CalendarDate getPreviousDate() { return previous; }
    public CalendarDate getCurrentDate() { return current; }
}
package harvestfestival.calendar;

import com.mojang.logging.LogUtils;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DailyUpdates {
    private static final Map<MinecraftServer, DailyTransition> TRACKERS = new IdentityHashMap<>();

    private DailyUpdates() {}

    public static void beforeTick(ServerTickEvent.Pre event) {
        TRACKERS.computeIfAbsent(event.getServer(), ignored -> new DailyTransition())
                .begin(WorldCalendar.ticks(event.getServer()));
    }

    public static void afterTick(ServerTickEvent.Post event) {
        var tracker = TRACKERS.get(event.getServer());
        if (tracker == null) return;
        tracker.finish(WorldCalendar.ticks(event.getServer())).ifPresent(change -> {
            NeoForge.EVENT_BUS.post(new NewDayEvent(event.getServer(), change.previous(), change.current()));
            LogUtils.getLogger().info("Harvest Festival daily update: {} -> {}", change.previous(), change.current());
        });
    }

    public static void rebase(MinecraftServer server) {
        var tracker = TRACKERS.get(server);
        if (tracker != null) tracker.begin(WorldCalendar.ticks(server));
    }

    public static void stopped(ServerStoppedEvent event) {
        TRACKERS.remove(event.getServer());
    }
}
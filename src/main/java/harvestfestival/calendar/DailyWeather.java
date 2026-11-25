package harvestfestival.calendar;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DailyWeather {
    private DailyWeather() {}
    public static void register(IEventBus bus) {
        WinterUpdates.register(bus);
        WeatherSnapshot.register(bus);
        NeoForge.EVENT_BUS.addListener(WeatherRules::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, DailyWeather::newDay);
        NeoForge.EVENT_BUS.addListener(DailyWeather::afterTick);
        NeoForge.EVENT_BUS.addListener(DailyWeather::login);
        NeoForge.EVENT_BUS.addListener(DailyWeather::respawn);
        NeoForge.EVENT_BUS.addListener(DailyWeather::dimension);
        NeoForge.EVENT_BUS.addListener(DailyWeather::stopped);
    }
    public static boolean controls(Level level) {
        // HFCalendar replaces DimensionType.OVERWORLD's provider, not every dimension with skylight.
        return level.dimensionTypeRegistration().unwrapKey().map(DailyWeather::controlsType).orElse(false);
    }
    static boolean controlsType(net.minecraft.resources.ResourceKey<net.minecraft.world.level.dimension.DimensionType> type) {
        return type.equals(BuiltinDimensionTypes.OVERWORLD);
    }
    public static DailyWeatherData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DailyWeatherData.TYPE);
    }
    public static void advanceVisual(ServerLevel level) {
        var weather = get(level.getServer()).today();
        level.oRainLevel = level.rainLevel;
        level.oThunderLevel = level.thunderLevel;
        var next = new DailyWeatherData.Strength(level.rainLevel, level.thunderLevel).approach(weather);
        level.rainLevel = next.rain();
        level.thunderLevel = next.storm();
    }
    public static void newDay(NewDayEvent event) {
        get(event.getServer()).advance(event.getCycle(), event.getCurrentDate(), WeatherRules.INSTANCE.active(),
                event.getServer().overworld().getRandom()::nextDouble);
    }
    public static void setToday(MinecraftServer server, Weather weather) {
        get(server).setToday(weather);
        server.getPlayerList().getPlayers().forEach(player -> refresh(player, true));
    }
    private static void afterTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> refresh(player, false));
    }
    private static void refresh(ServerPlayer player, boolean force) {
        var level = player.level();
        var data = get(level.getServer());
        var snapshot = new WeatherSnapshot(level.dimension().identifier().toString(), data.forecast(), data.recent(),
                level.rainLevel, level.thunderLevel, WeatherRules.INSTANCE.active().snowFog());
        if (force || !snapshot.equals(player.getData(WeatherSnapshot.VIEW))) {
            player.setData(WeatherSnapshot.VIEW, snapshot);
            PacketDistributor.sendToPlayer(player, snapshot);
        }
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player, true);
    }
    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player, true);
    }
    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player, true);
    }
    private static void stopped(ServerStoppedEvent event) { WeatherRules.INSTANCE.reset(); }
}

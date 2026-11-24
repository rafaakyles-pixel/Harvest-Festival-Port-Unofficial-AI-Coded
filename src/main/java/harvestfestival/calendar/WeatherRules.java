package harvestfestival.calendar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class WeatherRules extends SimplePreparableReloadListener<WeatherRules.Settings> {
    public static final Identifier RESOURCE = Identifier.parse("harvestfestival:calendar/weather.json");
    public static final Settings DEFAULT = defaults();
    public static final WeatherRules INSTANCE = new WeatherRules();
    private volatile Settings active = DEFAULT;
    public record Weight(Weather weather, double weight) {}
    public record Settings(Map<Weather, Boolean> enabled, Map<Season, List<Weight>> seasons) {
        public Settings {
            enabled = Map.copyOf(enabled);
            var copy = new EnumMap<Season, List<Weight>>(Season.class);
            seasons.forEach((season, weights) -> copy.put(season, List.copyOf(weights)));
            seasons = Map.copyOf(copy);
        }
        public Weather generate(CalendarDate date, int offset, DoubleSupplier random) {
            // CalendarServer.getRandomWeather uses >=, not >. Day 30 uses next season's table.
            var season = date.day() + offset >= CalendarDate.DAYS_PER_SEASON
                    ? Season.values()[(date.season().ordinal() + 1) % Season.values().length] : date.season();
            var weights = seasons.get(season);
            double total = weights.stream().mapToDouble(Weight::weight).sum();
            double sample = random.getAsDouble() * total;
            double ceiling = 0;
            for (var entry : weights) {
                ceiling += entry.weight();
                if (sample <= ceiling) return enabled.get(entry.weather()) ? entry.weather() : Weather.SUNNY;
            }
            throw new IllegalArgumentException("Weather random must be in [0, 1)");
        }
    }
    public Settings active() { return active; }
    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.parse("harvestfestival:weather"), INSTANCE);
    }
    public static Settings parse(JsonObject json) {
        var enabled = new EnumMap<Weather, Boolean>(Weather.class);
        for (var weather : Weather.values()) {
            var flag = json.getAsJsonObject("enabled").get(weather.getSerializedName());
            if (flag == null || !flag.isJsonPrimitive() || !flag.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Missing weather boolean: " + weather);
            enabled.put(weather, flag.getAsBoolean());
        }
        var seasons = new EnumMap<Season, List<Weight>>(Season.class);
        for (var season : Season.values()) {
            var weights = new ArrayList<Weight>();
            var seen = java.util.EnumSet.noneOf(Weather.class);
            for (var element : json.getAsJsonObject("seasons").getAsJsonArray(season.serializedName())) {
                var entry = element.getAsJsonObject();
                var weather = Weather.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, entry.get("weather")).getOrThrow();
                var value = entry.get("weight");
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid weight");
                double weight = value.getAsDouble();
                if (!Double.isFinite(weight) || weight < 0 || !seen.add(weather)) throw new IllegalArgumentException("Invalid/duplicate weight");
                if (weight > 0) weights.add(new Weight(weather, weight));
            }
            double total = weights.stream().mapToDouble(Weight::weight).sum();
            if (weights.isEmpty() || !Double.isFinite(total)) throw new IllegalArgumentException("Empty/overflowing weather table");
            seasons.put(season, weights);
        }
        return new Settings(enabled, seasons);
    }
    private static Settings defaults() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(WeatherRules.class.getResourceAsStream(
                "/data/harvestfestival/calendar/weather.json")), StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception failure) { throw new IllegalStateException("Invalid bundled weather settings", failure); }
    }
    @Override protected Settings prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (var reader = manager.getResourceOrThrow(RESOURCE).openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception failure) { throw new IllegalArgumentException("Invalid " + RESOURCE, failure); }
    }
    @Override protected void apply(Settings settings, ResourceManager manager, ProfilerFiller profiler) { active = settings; }
    public void reset() { active = DEFAULT; }
}

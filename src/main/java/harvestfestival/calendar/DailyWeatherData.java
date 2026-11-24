package harvestfestival.calendar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Global forecast and event identity; never a clock or a visual interpolation timer. */
public final class DailyWeatherData extends SavedData {
    public record Strength(float rain, float storm) {
        public Strength approach(Weather weather) {
            return new Strength(Weather.approachRain(rain, weather.rain()), Weather.approachStorm(storm, weather.storm()));
        }
    }
    public static final List<Weather> INITIAL = java.util.Collections.nCopies(7, Weather.SUNNY);
    public static final Codec<List<Weather>> FORECAST_CODEC = Weather.CODEC.listOf().validate(
            values -> values.size() == 7 ? DataResult.success(List.copyOf(values)) : DataResult.error(() -> "Expected seven forecast days"));
    public static final Codec<DailyWeatherData> CODEC = RecordCodecBuilder.create(i -> i.group(
            FORECAST_CODEC.fieldOf("forecast").forGetter(DailyWeatherData::forecast),
            // CalendarServer omits recent when it equals today; reopening then defaults to SUNNY.
            Weather.CODEC.optionalFieldOf("recent_non_sunny", Weather.SUNNY)
                    .forGetter(data -> data.recent == data.today() ? Weather.SUNNY : data.recent),
            Codec.LONG.validate(value -> value >= -1 ? DataResult.success(value)
                    : DataResult.error(() -> "Invalid weather cycle")).fieldOf("last_cycle").forGetter(data -> data.lastCycle)
    ).apply(i, DailyWeatherData::new));
    public static final SavedDataType<DailyWeatherData> TYPE = new SavedDataType<>(
            Identifier.parse("harvestfestival:daily_weather"), DailyWeatherData::new, CODEC);
    private List<Weather> forecast;
    private Weather recent;
    private long lastCycle;
    public DailyWeatherData() { this(INITIAL, Weather.SUNNY, -1); }
    private DailyWeatherData(List<Weather> forecast, Weather recent, long lastCycle) {
        this.forecast = List.copyOf(forecast);
        this.recent = recent;
        this.lastCycle = lastCycle;
    }
    public List<Weather> forecast() { return forecast; }
    public Weather today() { return forecast.getFirst(); }
    public Weather recent() { return recent; }
    public boolean advance(long cycle, CalendarDate date, WeatherRules.Settings rules, DoubleSupplier random) {
        if (cycle > 0 && cycle <= lastCycle) return false;
        var next = new ArrayList<>(forecast.subList(1, 7));
        next.add(rules.generate(date, 6, random));
        forecast = List.copyOf(next);
        if (cycle > 0) lastCycle = cycle;
        changed();
        return true;
    }
    public void setToday(Weather weather) {
        var next = new ArrayList<>(forecast);
        next.set(0, weather);
        forecast = List.copyOf(next);
        changed();
    }
    private void changed() {
        if (today() != Weather.SUNNY) recent = today();
        setDirty();
    }
}

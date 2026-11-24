package harvestfestival.calendar;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyWeatherTest {
    private static final WeatherRules.Settings RULES = WeatherRules.DEFAULT;
    private static CalendarDate date(int day, Season season) { return CalendarDate.of(day, season, 1); }
    private static WeatherRules.Settings disabled(Weather disabled) {
        var flags = new EnumMap<Weather, Boolean>(RULES.enabled());
        flags.put(disabled, false);
        return new WeatherRules.Settings(flags, RULES.seasons());
    }

    @Test void bundledWeightsAndDefaultFlagsMatchAllOriginalSeasons() {
        int[][] expected = {{70,30,0,0,0}, {95,4,1,0,0}, {55,45,0,0,0}, {49,0,0,50,1}};
        for (var season : Season.values()) {
            int[] counts = new int[5];
            for (int i = 0; i < 100; i++) {
                double sample = (i + 0.5) / 100;
                counts[RULES.generate(date(1, season), 0, () -> sample).ordinal()]++;
            }
            assertArrayEquals(expected[season.ordinal()], counts);
        }
        assertTrue(RULES.enabled().values().stream().allMatch(Boolean::booleanValue));
    }

    @Test void cumulativeCeilingAndAutumnInsertionOrderArePreserved() {
        assertEquals(Weather.SUNNY, RULES.generate(date(1, Season.SPRING), 0, () -> .70));
        assertEquals(Weather.SUNNY, RULES.generate(date(1, Season.SPRING), 0, () -> Math.nextUp(.70)));
        assertEquals(Weather.RAIN, RULES.generate(date(1, Season.SPRING), 0, () -> .700001));
        assertEquals(Weather.RAIN, RULES.generate(date(1, Season.AUTUMN), 0, () -> 0));
        assertEquals(Weather.RAIN, RULES.generate(date(1, Season.AUTUMN), 0, () -> .45));
        assertEquals(Weather.SUNNY, RULES.generate(date(1, Season.AUTUMN), 0, () -> .46));
    }

    @Test void eachDisabledResultFallsBackToSunnyWithoutResamplingEvenIfSunnyDisabled() {
        var flags = new EnumMap<Weather, Boolean>(Weather.class);
        for (var weather : Weather.values()) flags.put(weather, false);
        var allDisabled = new WeatherRules.Settings(flags, RULES.seasons());
        var calls = new AtomicInteger();
        for (var season : Season.values()) {
            for (int i = 0; i < 100; i++) {
                double sample = (i + .5) / 100;
                assertEquals(Weather.SUNNY, allDisabled.generate(date(1, season), 0, () -> { calls.incrementAndGet(); return sample; }));
            }
        }
        assertEquals(400, calls.get());
        assertEquals(Weather.SUNNY, disabled(Weather.RAIN).generate(date(1, Season.SPRING), 0, () -> .9));
        assertEquals(Weather.TYPHOON, disabled(Weather.RAIN).generate(date(1, Season.SUMMER), 0, () -> .999));
    }

    @Test void originalDayThirtyBoundaryAppliesToAllFourSeasonEdges() {
        for (var season : Season.values()) {
            var next = Season.values()[(season.ordinal() + 1) % 4];
            for (double sample : new double[]{.1, .5, .8, .98, .999}) {
                assertEquals(RULES.generate(date(1, season), 0, () -> sample),
                        RULES.generate(date(23, season), 6, () -> sample));
                assertEquals(RULES.generate(date(1, next), 0, () -> sample),
                        RULES.generate(date(24, season), 6, () -> sample));
                assertEquals(RULES.generate(date(1, next), 0, () -> sample),
                        RULES.generate(date(30, season), 6, () -> sample));
            }
        }
    }

    @Test void initializationIsSevenSunnyDaysAndOnlyLastSlotIsGenerated() {
        var data = new DailyWeatherData();
        assertEquals(DailyWeatherData.INITIAL, data.forecast());
        var draws = new AtomicInteger();
        assertTrue(data.advance(1, date(2, Season.SPRING), RULES, () -> { draws.incrementAndGet(); return .99; }));
        assertEquals(1, draws.get());
        assertEquals(Weather.RAIN, data.forecast().get(6));
        assertEquals(DailyWeatherData.INITIAL.subList(0, 6), data.forecast().subList(0, 6));
        for (int i = 2; i <= 6; i++) data.advance(i, date(i + 1, Season.SPRING), RULES, () -> .99);
        assertEquals(Weather.SUNNY, data.today());
        data.advance(7, date(8, Season.SPRING), RULES, () -> .99);
        assertEquals(Weather.RAIN, data.today());
    }

    @Test void reopeningPreservesForecastAndCycleWithoutGeneratingOrAdvancing() {
        var data = new DailyWeatherData();
        data.setToday(Weather.TYPHOON);
        data.advance(42, date(2, Season.SUMMER), RULES, () -> .999);
        var reopened = DailyWeatherData.CODEC.parse(NbtOps.INSTANCE,
                DailyWeatherData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
        assertEquals(data.forecast(), reopened.forecast());
        assertEquals(data.recent(), reopened.recent());
        assertFalse(reopened.advance(42, date(3, Season.SUMMER), RULES, () -> { fail("duplicate drew weather"); return 0; }));
        assertTrue(reopened.advance(43, date(3, Season.SUMMER), RULES, () -> 0));
    }

    @Test void originalRecentWeatherOmissionIsNotSilentlyCorrected() {
        var data = new DailyWeatherData();
        data.setToday(Weather.SNOW);
        var saved = DailyWeatherData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        assertFalse(saved.getAsJsonObject().has("recent_non_sunny"));
        var reopened = DailyWeatherData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(Weather.SNOW, reopened.today());
        assertEquals(Weather.SUNNY, reopened.recent());
        data.setToday(Weather.SUNNY);
        assertEquals(Weather.SNOW, DailyWeatherData.CODEC.parse(JsonOps.INSTANCE,
                DailyWeatherData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow().recent());
    }

    @Test void administrativeWeatherChangesOnlyTodayAndBypassesGenerationSwitches() {
        var data = new DailyWeatherData();
        data.setToday(Weather.BLIZZARD);
        assertEquals(Weather.BLIZZARD, data.today());
        assertEquals(DailyWeatherData.INITIAL.subList(1, 7), data.forecast().subList(1, 7));
        data.advance(1, date(2, Season.WINTER), disabled(Weather.BLIZZARD), () -> .999);
        assertEquals(Weather.SUNNY, data.forecast().get(6));
        assertEquals(Weather.BLIZZARD, data.recent());
    }

    @Test void naturalAndSleepBoundariesShiftOnceWhileAdministrativeRebaseAndLoadDoNot() {
        for (long start : new long[]{23999, 18000}) {
            var tracker = new DailyTransition();
            var data = new DailyWeatherData();
            var draws = new AtomicInteger();
            tracker.begin(start);
            var transition = tracker.finish(24000).orElseThrow();
            assertTrue(data.advance(1, transition.current(), RULES, () -> { draws.incrementAndGet(); return .99; }));
            assertTrue(tracker.finish(24000).isEmpty());
            tracker.begin(99 * 24000L); // Administrative rebase, with no event or catch-up.
            assertTrue(tracker.finish(99 * 24000L + 1).isEmpty());
            var reopened = new DailyTransition();
            reopened.begin(99 * 24000L + 1);
            assertTrue(reopened.finish(99 * 24000L + 2).isEmpty());
            assertEquals(1, draws.get());
        }
    }

    @Test void skippedDatesStillGenerateOneSlotAndRewoundDatesAcceptANewCycle() {
        var tracker = new DailyTransition();
        var data = new DailyWeatherData();
        tracker.begin(0);
        var jump = tracker.finish(24000 * 80L).orElseThrow();
        data.advance(1, jump.current(), RULES, () -> .1);
        assertEquals(Weather.RAIN, data.forecast().get(6));
        tracker.begin(23999);
        data.advance(2, tracker.finish(24000).orElseThrow().current(), RULES, () -> .99);
        assertEquals(Weather.RAIN, data.forecast().get(5));
        assertEquals(Weather.RAIN, data.forecast().get(6));
    }

    @Test void transitionsKeepSeparateTargetsAndOriginalRoundingWithoutClamping() {
        assertArrayEquals(new int[]{0,100,20,100,20}, Arrays.stream(Weather.values()).mapToInt(Weather::rain).toArray());
        assertArrayEquals(new int[]{0,0,100,0,0}, Arrays.stream(Weather.values()).mapToInt(Weather::storm).toArray());
        var current = new DailyWeatherData.Strength(0, 0).approach(Weather.TYPHOON);
        assertEquals(.01F, current.rain());
        assertEquals(.01F, current.storm());
        assertEquals(.5F - .1F, Weather.approachStorm(.5F, 0));
        assertEquals(.5F - .01F, Weather.approachRain(.5F, 0));
        assertEquals(-.09F, Weather.approachStorm(.01F, 0), .000001F);
        assertEquals(.209F, Weather.approachRain(.209F, 20));
        for (int i = 0; i < 150; i++) current = current.approach(Weather.TYPHOON);
        assertEquals(20, (int) (current.rain() * 100));
        assertEquals(100, (int) (current.storm() * 100));
    }

    @Test void snapshotCodecCarriesAllStatesRecentAndUnclampedActualStrength() {
        var forecast = java.util.List.of(Weather.SUNNY, Weather.RAIN, Weather.TYPHOON, Weather.SNOW,
                Weather.BLIZZARD, Weather.RAIN, Weather.SUNNY);
        var snapshot = new WeatherSnapshot("minecraft:overworld", forecast, Weather.TYPHOON, .209F, -.09F);
        var buffer = Unpooled.buffer();
        try {
            WeatherSnapshot.STREAM_CODEC.encode(buffer, snapshot);
            assertEquals(snapshot, WeatherSnapshot.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
        assertThrows(UnsupportedOperationException.class, () -> snapshot.forecast().set(0, Weather.SNOW));
    }

    @Test void malformedForecastAndInvalidConfigurationAreRejected() {
        assertTrue(DailyWeatherData.FORECAST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[\"sunny\"]")).error().isPresent());
        assertTrue(Weather.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("\"unknown\"")).error().isPresent());
        assertThrows(RuntimeException.class, () -> WeatherRules.parse(JsonParser.parseString("{}").getAsJsonObject()));
    }

    @Test void weatherMixinAppliesToExactTargetAlongsideExistingMixins() {
        assertTrue(Arrays.stream(ServerLevel.class.getDeclaredMethods()).anyMatch(method -> method.getName().contains("dailyWeather")));
    }

    @Test void controlFollowsOverworldTypeInsteadOfGlobalCalendarOrSkylight() {
        assertTrue(DailyWeather.controlsType(net.minecraft.world.level.dimension.BuiltinDimensionTypes.OVERWORLD));
        assertFalse(DailyWeather.controlsType(net.minecraft.world.level.dimension.BuiltinDimensionTypes.NETHER));
        assertFalse(DailyWeather.controlsType(net.minecraft.world.level.dimension.BuiltinDimensionTypes.END));
        assertFalse(DailyWeather.controlsType(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION_TYPE,
                net.minecraft.resources.Identifier.parse("example:surface"))));
    }
}

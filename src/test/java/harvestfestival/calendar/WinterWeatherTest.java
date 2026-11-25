package harvestfestival.calendar;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome.Precipitation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WinterWeatherTest {
    @Test void modernBiomesUsePositionAndAltitudeWithoutChangingAgriculturalExclusions() {
        var biomes = net.minecraft.data.registries.VanillaRegistries.createLookup()
                .lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var low = new net.minecraft.core.BlockPos(0, 64, 0);
        var high = low.atY(300);
        var mountain = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS).value();
        assertEquals(Precipitation.RAIN, mountain.getPrecipitationAt(low, 63));
        assertEquals(Precipitation.SNOW, mountain.getPrecipitationAt(high, 63));
        var grove = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.GROVE).value();
        assertEquals(Precipitation.SNOW, grove.getPrecipitationAt(low, 63));
        var desert = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT).value();
        assertEquals(Precipitation.NONE, desert.getPrecipitationAt(high, 63));
        var swamp = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SWAMP).value();
        assertTrue(WinterWeather.humid(swamp));
        assertEquals(Precipitation.RAIN, WinterWeather.visual(swamp.getPrecipitationAt(low, 63), true, Weather.BLIZZARD));
        assertTrue(harvestfestival.crops.AgriculturalRain.at(Weather.RAIN, mountain.hasPrecipitation(), false,
                mountain.getModifiedClimateSettings().downfall(), true, true));
    }
    @Test void fogSeparatesCoveredHumidNativeSnowAndBlizzardColumns() {
        assertEquals(-20000, WinterWeather.fogTarget(Weather.BLIZZARD, Precipitation.RAIN, false, true, false));
        assertEquals(-1000, WinterWeather.fogTarget(Weather.SNOW, Precipitation.RAIN, false, true, false));
        assertEquals(100, WinterWeather.fogTarget(Weather.RAIN, Precipitation.RAIN, false, true, false));
        assertEquals(5000, WinterWeather.fogTarget(Weather.BLIZZARD, Precipitation.RAIN, true, true, false));
        assertEquals(5000, WinterWeather.fogTarget(Weather.SNOW, Precipitation.SNOW, false, true, false));
        assertEquals(5000, WinterWeather.fogTarget(Weather.BLIZZARD, Precipitation.RAIN, false, true, true));
        assertEquals(100, WinterWeather.fogTarget(Weather.BLIZZARD, Precipitation.RAIN, false, false, false));
    }
    @Test void seasonalFormationRequiresSnowDayAndTemperateNonHumidBiome() {
        for (var weather : Weather.values()) {
            assertEquals(weather == Weather.SNOW || weather == Weather.BLIZZARD,
                    WinterWeather.seasonal(Precipitation.RAIN, false, weather));
            assertFalse(WinterWeather.seasonal(Precipitation.NONE, false, weather));
            assertFalse(WinterWeather.seasonal(Precipitation.SNOW, false, weather));
            assertFalse(WinterWeather.seasonal(Precipitation.RAIN, true, weather));
        }
    }
    @Test void snowSitesRespectLightReplacementAndSupportWithoutStackingPerTick() {
        assertTrue(WinterWeather.snowSite(true, 9, false, true, true));
        assertFalse(WinterWeather.snowSite(true, 10, false, true, true));
        assertFalse(WinterWeather.snowSite(false, 0, false, true, true));
        assertFalse(WinterWeather.snowSite(true, 0, false, false, true));
        assertFalse(WinterWeather.snowSite(true, 0, false, true, false));
        assertFalse(WinterWeather.snowSite(true, 0, true, true, true));
    }
    @Test void freezingRequiresSourceAndShoreButNotRainIntensity() {
        assertTrue(WinterWeather.iceSite(true, 9, true, false));
        assertFalse(WinterWeather.iceSite(true, 10, true, false));
        assertFalse(WinterWeather.iceSite(false, 0, true, false));
        assertFalse(WinterWeather.iceSite(true, 0, false, false));
        assertFalse(WinterWeather.iceSite(true, 0, true, true));
    }
    @Test void visualSnowUsesRecentWeatherWhileHumidAndDryBiomesRemainExceptions() {
        for (var weather : Weather.values()) {
            assertEquals(Precipitation.NONE, WinterWeather.visual(Precipitation.NONE, false, weather));
            assertEquals(Precipitation.RAIN, WinterWeather.visual(Precipitation.RAIN, true, weather));
            assertEquals(Precipitation.SNOW, WinterWeather.visual(Precipitation.SNOW, false, weather));
        }
        assertEquals(Precipitation.SNOW, WinterWeather.visual(Precipitation.RAIN, false, Weather.SNOW));
        assertEquals(Precipitation.RAIN, WinterWeather.visual(Precipitation.RAIN, false, Weather.RAIN));
    }
    @Test void thawIsTwoOfThreeOutsideWinterUnlessTheDayIsSnowy() {
        for (var weather : Weather.values()) for (var season : Season.values()) {
            int count = 0;
            for (int sample = 0; sample < 3; sample++) if (WinterWeather.melts(weather, season, sample)) count++;
            assertEquals(season != Season.WINTER && !WinterWeather.snowy(weather) ? 2 : 0, count);
        }
    }
    @Test void onlyBlizzardAddsOneLayerAndNeverReducesThickerSnow() {
        for (var weather : Weather.values()) for (int layers = 1; layers <= 8; layers++)
            assertEquals(weather == Weather.BLIZZARD && layers < 5 ? layers + 1 : layers, WinterWeather.layers(weather, layers));
    }
    @Test void persistedCycleRejectsReplayReopenCorruptionAndExhaustion() {
        long saved = DailyCycleData.MARKER_CODEC.parse(JsonOps.INSTANCE,
                DailyCycleData.MARKER_CODEC.encodeStart(JsonOps.INSTANCE, 41L).getOrThrow()).getOrThrow();
        assertFalse(WinterUpdates.eligible(41, saved));
        assertFalse(WinterUpdates.eligible(40, saved));
        assertTrue(WinterUpdates.eligible(42, saved));
        assertTrue(WinterUpdates.eligible(1, 0));
        assertFalse(WinterUpdates.eligible(0, 0));
        assertFalse(WinterUpdates.eligible(42, DailyCycleData.MARKER_CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("-1")).getOrThrow()));
    }
    @Test void fogOptionHasCompatibleDefaultAndTravelsInAuthoritativeSnapshot() {
        assertTrue(WeatherRules.DEFAULT.snowFog());
        var original = new WeatherSnapshot("minecraft:overworld", DailyWeatherData.INITIAL, Weather.BLIZZARD, .2F, 0, false);
        var buffer = Unpooled.buffer();
        try {
            WeatherSnapshot.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, WeatherSnapshot.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void serverMixinTransformsBothExactPrecipitationCalls() {
        var methods = Arrays.stream(ServerLevel.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName).toList();
        assertTrue(methods.stream().anyMatch(name -> name.contains("winterSnow")));
        assertTrue(methods.stream().anyMatch(name -> name.contains("winterIce")));
    }
    @Test void oldWeatherDatapacksDefaultFogAndInvalidNewOptionIsRejected() throws Exception {
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream(
                "/data/harvestfestival/calendar/weather.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            json.remove("snow_fog");
            assertTrue(WeatherRules.parse(json).snowFog());
            json.addProperty("snow_fog", false);
            assertFalse(WeatherRules.parse(json).snowFog());
            json.addProperty("snow_fog", "false");
            assertThrows(IllegalArgumentException.class, () -> WeatherRules.parse(json));
        }
    }
}

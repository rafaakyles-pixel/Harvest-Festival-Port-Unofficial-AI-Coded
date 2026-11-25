package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.calendar.CalendarDate;
import harvestfestival.calendar.DailyTransition;
import harvestfestival.calendar.DailyWeatherData;
import harvestfestival.calendar.Season;
import harvestfestival.calendar.Weather;
import harvestfestival.calendar.WeatherRules;
import harvestfestival.tools.FarmlandSoil;
import harvestfestival.tools.PreparedSoilBlock;
import harvestfestival.tools.ToolContent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AgriculturalRainTest {
    @TempDir Path directory;

    @Test void allWeatherKindsRespectBiomeAndHumidityBoundary() {
        for (var weather : Weather.values()) {
            boolean rain = weather == Weather.RAIN || weather == Weather.TYPHOON;
            assertEquals(rain, AgriculturalRain.at(weather, true, false, .85F, true, true));
            assertEquals(weather != Weather.SUNNY,
                    AgriculturalRain.at(weather, true, false, Math.nextUp(.85F), true, true));
            assertFalse(AgriculturalRain.at(weather, false, false, 1, true, true));
            assertFalse(AgriculturalRain.at(weather, true, true, 1, true, true));
        }
    }

    @Test void skyAndPrecipitationHeightAreIndependentRequirements() {
        for (var weather : List.of(Weather.RAIN, Weather.TYPHOON, Weather.SNOW, Weather.BLIZZARD)) {
            assertTrue(AgriculturalRain.at(weather, true, false, 1, true, true));
            assertFalse(AgriculturalRain.at(weather, true, false, 1, false, true));
            assertFalse(AgriculturalRain.at(weather, true, false, 1, true, false));
            assertFalse(AgriculturalRain.at(weather, true, false, 1, false, false));
        }
    }

    @Test void targetBiomeDataCoversDryHumidNetherAndEndWithoutTemperatureSubstitution() {
        var biomes = net.minecraft.data.registries.VanillaRegistries.createLookup()
                .lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var key : List.of(net.minecraft.world.level.biome.Biomes.PLAINS,
                net.minecraft.world.level.biome.Biomes.SWAMP, net.minecraft.world.level.biome.Biomes.DESERT,
                net.minecraft.world.level.biome.Biomes.NETHER_WASTES, net.minecraft.world.level.biome.Biomes.THE_END)) {
            var biome = biomes.getOrThrow(key).value();
            boolean dryBiome = key.equals(net.minecraft.world.level.biome.Biomes.DESERT)
                    || key.equals(net.minecraft.world.level.biome.Biomes.NETHER_WASTES)
                    || key.equals(net.minecraft.world.level.biome.Biomes.THE_END);
            assertEquals(!dryBiome, AgriculturalRain.at(Weather.RAIN, biome.hasPrecipitation(), false,
                    biome.getModifiedClimateSettings().downfall(), true, true), key.toString());
            assertEquals(key.equals(net.minecraft.world.level.biome.Biomes.SWAMP),
                    AgriculturalRain.at(Weather.SNOW, biome.hasPrecipitation(), false,
                            biome.getModifiedClimateSettings().downfall(), true, true), key.toString());
        }
    }

    @Test void loadUsesOriginalDoubleThresholdIncludingFloatRounding() {
        assertFalse(AgriculturalRain.onLoad(0));
        assertFalse(AgriculturalRain.onLoad(Math.nextDown(.2F)));
        assertTrue(AgriculturalRain.onLoad(.2F)); // .2F promoted to double is slightly greater than .2D.
        assertTrue(AgriculturalRain.onLoad(1));
        assertFalse(AgriculturalRain.onLoad(Float.NaN));
        assertTrue(AgriculturalRain.at(Weather.TYPHOON, true, false, .5F, true, true));
        assertFalse(AgriculturalRain.at(Weather.SUNNY, true, false, .5F, true, true));
    }

    @Test void bundledExclusionsPreserveLegacySnowFlagsWithoutAltitudeRule() throws Exception {
        try (var stream = getClass().getResourceAsStream("/data/harvestfestival/tags/worldgen/biome/agricultural_rain_excluded.json")) {
            assertNotNull(stream);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var ids = json.getAsJsonArray("values").asList().stream().map(v -> v.getAsString()).collect(Collectors.toSet());
            assertEquals(Set.of("minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga",
                    "minecraft:snowy_beach", "minecraft:frozen_river", "minecraft:frozen_ocean"), ids);
            assertFalse(json.get("replace").getAsBoolean());
            assertFalse(ids.contains("minecraft:windswept_hills"));
        }
    }

    @Test void rainMaintainsAllManagedMoistureValuesAndDryDaysKeepExistingRules() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var air = Blocks.AIR.defaultBlockState();
            for (int moisture = 0; moisture <= 7; moisture++) {
                var soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, moisture);
                assertEquals(7, AgriculturalRain.soilDay(soil, air, true).getValue(FarmlandBlock.MOISTURE));
                assertEquals(FarmlandSoil.dry(soil, air), AgriculturalRain.soilDay(soil, air, false));
            }
            var stone = Blocks.STONE.defaultBlockState();
            assertSame(stone, AgriculturalRain.hydrate(stone));
            assertSame(stone, AgriculturalRain.soilDay(stone, air, true));
        }
    }

    @Test void loadHydrationAndLegacyMigrationDoNotGrowOrResetCropOrCycle() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var work = new RainWork(Weather.SUNNY);
            work.last = 42;
            var crop = work.crop;
            for (boolean oldWater : List.of(false, true)) {
                var legacy = ToolContent.PREPARED_SOIL.get().defaultBlockState().setValue(PreparedSoilBlock.WATERED, oldWater);
                work.soil = AgriculturalRain.hydrate(FarmlandSoil.migrate(legacy));
                assertEquals(7, work.soil.getValue(FarmlandBlock.MOISTURE));
                assertSame(work.soil, AgriculturalRain.hydrate(work.soil));
                CropDailyUpdates.process(42, List.of(work));
                assertSame(crop, work.crop);
                assertEquals(42, work.last);
                assertEquals(7, work.soil.getValue(FarmlandBlock.MOISTURE));
            }
            CropDailyUpdates.process(90, List.of());
            assertSame(crop, work.crop);
        }
    }

    @Test void tillDelayCountsBothPhasesAndOnlyWritesAfterThirtyCallbacks() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var pending = new AgriculturalRain.PendingTill(new BlockPos(3, 64, 4));
            for (int i = 0; i < 30; i++) assertFalse(pending.ready());
            assertTrue(pending.ready());
            // A removed/replaced target cannot acquire moisture at the delayed write.
            assertSame(Blocks.DIRT.defaultBlockState(), AgriculturalRain.hydrate(Blocks.DIRT.defaultBlockState()));
        }
    }

    @Test void naturalAndSleepBoundariesUseNewForecastBeforeGrowthAndMainBeforeSprinkler() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            for (long previousTick : new long[]{23999, 13000}) {
                var detector = new DailyTransition();
                detector.begin(previousTick);
                assertTrue(detector.finish(24000).isPresent());
                var weather = DailyWeatherData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                        "{\"forecast\":[\"sunny\",\"rain\",\"sunny\",\"sunny\",\"sunny\",\"sunny\",\"sunny\"],\"last_cycle\":-1}")).getOrThrow();
                var work = new RainWork(weather.today());
                assertTrue(weather.advance(1, CalendarDate.of(2, Season.SPRING, 1), WeatherRules.DEFAULT, () -> 0));
                work.weather = weather.today();
                var sprinkler = new SprinklerBlockEntity(BlockPos.ZERO, OrchardContent.OLD.get().defaultBlockState());
                var writes = new ArrayList<BlockPos>();
                var irrigation = new SprinklerHydration.Soil() {
                    public boolean available(BlockPos pos) { return true; }
                    public BlockState get(BlockPos pos) { assertTrue(work.crop.stage() >= 2); return work.soil; }
                    public boolean set(BlockPos pos, BlockState state) { writes.add(pos); work.soil = state; return true; }
                };
                CropDailyUpdates.process(1, List.of(work), () -> sprinkler.newDay(1, irrigation));
                assertEquals(2, work.crop.stage());
                assertEquals(1, work.crop.dryDays());
                assertEquals(7, work.soil.getValue(FarmlandBlock.MOISTURE));
                assertTrue(writes.isEmpty());
                CropDailyUpdates.process(1, List.of(work), () -> sprinkler.newDay(1, irrigation));
                assertEquals(2, work.crop.stage());
                assertEquals(1, work.mainCalls);
                work.weather = Weather.SUNNY;
                CropDailyUpdates.process(2, List.of(work), () -> sprinkler.newDay(2, irrigation));
                assertEquals(3, work.crop.stage());
                assertEquals(7, work.soil.getValue(FarmlandBlock.MOISTURE));
                assertEquals(1, writes.size());
            }
        }
    }

    @Test void administrativeWeatherAndDateChangesDoNotRunAgriculturalCycle() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var work = new RainWork(Weather.SUNNY);
            var before = work.crop;
            var climate = new DailyWeatherData();
            climate.setToday(Weather.RAIN);
            var detector = new DailyTransition();
            detector.begin(500 * 24000L);
            assertTrue(detector.finish(500 * 24000L).isEmpty());
            assertSame(before, work.crop);
            assertEquals(0, work.mainCalls);
            assertEquals(0, work.soil.getValue(FarmlandBlock.MOISTURE));
        }
    }

    @Test void rainDoesNotOverrideNullSeasonInNetherOrEnd() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            for (var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
                var season = CropDailyUpdates.seasonForDimension(dimension, Season.SPRING);
                var crop = CropLifecycle.newDay(CropState.planted(CropTestSupport.turnip().id()),
                        CropTestSupport.turnip(), season, true, false);
                assertEquals(!dimension.equals(Level.OVERWORLD), crop.dead());
            }
        }
    }

    private static final class RainWork implements CropDailyUpdates.ChunkWork {
        long last;
        int mainCalls;
        Weather weather;
        CropState crop = CropState.planted(CropTestSupport.turnip().id());
        BlockState soil = Blocks.FARMLAND.defaultBlockState();
        RainWork(Weather weather) { this.weather = weather; }
        boolean rain() { return AgriculturalRain.at(weather, true, false, .5F, true, true); }
        public long lastCycle() { return last; }
        public void mark(long cycle) { last = cycle; }
        public void grow() { crop = CropLifecycle.newDay(crop, CropTestSupport.turnip(), Season.SPRING,
                FarmlandSoil.watered(soil) || rain(), false); }
        public void dry() { mainCalls++; soil = AgriculturalRain.soilDay(soil, CropContent.CROP.get().defaultBlockState(), rain()); }
    }
}

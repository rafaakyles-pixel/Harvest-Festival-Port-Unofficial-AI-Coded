package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropDimensionTest {
    @TempDir Path directory;

    @Test void hiddenDimensionsKillEveryBundledHfCropBeforeGrowthRegardlessOfWaterOrServerMode() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var resource : CropTestSupport.RESOURCES) {
                if (!resource.startsWith("harvestfestival/crops/")) continue;
                var name = resource.substring("harvestfestival/crops/".length()).replace(".json", "");
                var d = SpecialCropTest.definition(name);
                assertNotNull(d, name);
                for (var dimension : List.of(Level.NETHER, Level.END))
                    for (var season : Season.values())
                        for (boolean dedicated : new boolean[]{false, true})
                            for (boolean watered : new boolean[]{false, true}) {
                                var before = new CropState(d.id(), d.maturity() - 1, 1, false, 0);
                                var localSeason = CropDailyUpdates.seasonForDimension(dimension, season);
                                assertNull(localSeason);
                                var next = CropLifecycle.newDay(before, d, localSeason, watered, dedicated);
                                assertTrue(next.dead(), name);
                                assertEquals(before.stage(), next.stage());
                                assertEquals(before.harvests(), next.harvests());
                                var entity = CropRegrowthTest.entity(before);
                                var target = new SpecialCropTest.Target();
                                assertTrue(CropGrowth.apply(entity, d, next, target));
                                assertEquals(0, target.fruits);
                                if (d.form().doubled()) { assertNotNull(target.top); assertTrue(target.top.dead()); }
                                assertFalse(CropLifecycle.canHarvest(entity.crop(), d));
                            }
            }
        }
    }

    @Test void overworldAndOtherDimensionsKeepCalendarWhileMissingDefinitionsStayFrozen() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var custom = ResourceKey.create(Registries.DIMENSION, Identifier.parse("test:other"));
            var d = SpecialCropTest.definition("tutorial_turnip");
            for (var dimension : List.of(Level.OVERWORLD, custom)) for (var season : Season.values()) {
                var state = CropState.planted(d.id());
                var localSeason = CropDailyUpdates.seasonForDimension(dimension, season);
                assertEquals(season, localSeason);
                state = CropLifecycle.newDay(state, d, localSeason, true, false);
                assertEquals(2, state.stage()); assertFalse(state.dead());
                state = CropLifecycle.newDay(state, d, localSeason, true, false);
                assertEquals(3, state.stage()); assertTrue(CropLifecycle.canHarvest(state, d));
                assertEquals(state, CropLifecycle.newDay(state, null, null, true, false));
            }
        }
    }

    @Test void deferredNetherWartAndConvertedFruitsStayOutsideHfDefinitionsAndVanillaRestrictions() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            assertNull(SpecialCropTest.definition("nether_wart"));
            assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                    Identifier.parse("harvestfestival:nether_wart_seeds")).isEmpty());
            var policy = new VanillaCropPolicy(true, true, true, true);
            for (var block : List.of(Blocks.NETHER_WART, Blocks.MELON, Blocks.PUMPKIN)) {
                var state = block.defaultBlockState();
                assertFalse(state.is(CropContent.CROPS));
                assertFalse(policy.denyGrowth(state));
            }
        }
    }
}

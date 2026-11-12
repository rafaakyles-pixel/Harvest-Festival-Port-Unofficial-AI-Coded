package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropLifecycleTest {
    @TempDir Path directory;
    @Test void fourWateredTransitionsMatureAndVisualsFollowData() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.turnip();
            var crop = CropState.planted(definition.id());
            assertEquals(0, definition.appearance(crop.stage()));
            for (int expected = 2; expected <= 5; expected++) {
                assertFalse(CropLifecycle.canHarvest(crop, definition));
                crop = CropLifecycle.newDay(crop, definition, Season.SPRING, true, false);
                assertEquals(expected, crop.stage());
                assertEquals(1, crop.dryDays());
            }
            assertTrue(CropLifecycle.canHarvest(crop, definition));
            assertEquals(1, definition.appearance(3));
            assertEquals(2, definition.appearance(5));
            assertEquals(5, CropLifecycle.newDay(crop, definition, Season.SPRING, true, false).stage());
        }
    }
    @Test void dryPlantStallsThenDiesOnThirdUnwateredUpdate() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.turnip();
            var crop = CropState.planted(definition.id());
            for (int i = 0; i < 2; i++) {
                crop = CropLifecycle.newDay(crop, definition, Season.SPRING, false, false);
                assertFalse(crop.dead());
                assertEquals(1, crop.stage());
            }
            assertEquals(3, crop.dryDays());
            assertTrue(CropLifecycle.newDay(crop, definition, Season.SPRING, false, false).dead());
            var rescued = CropLifecycle.newDay(crop, definition, Season.SPRING, true, false);
            assertFalse(rescued.dead());
            assertEquals(2, rescued.stage());
            assertEquals(1, rescued.dryDays());
        }
    }
    @Test void dedicatedDefaultDoesNotKillFromDroughtButStillChecksSeason() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.turnip();
            var crop = CropState.planted(definition.id());
            for (int i = 0; i < 100; i++) crop = CropLifecycle.newDay(crop, definition, Season.SPRING, false, true);
            assertFalse(crop.dead());
            assertEquals(1, crop.stage());
            assertTrue(CropLifecycle.newDay(crop, definition, Season.SUMMER, true, true).dead());
        }
    }
    @Test void outOfSeasonAndDeadPlantsCannotGrowOrHarvest() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.turnip();
            var crop = new CropState(definition.id(), 5, 1, false);
            crop = CropLifecycle.newDay(crop, definition, Season.SUMMER, true, false);
            assertTrue(crop.dead());
            assertFalse(CropLifecycle.canHarvest(crop, definition));
            assertEquals(crop, CropLifecycle.newDay(crop, definition, Season.SPRING, true, false));
        }
    }
    @Test void missingDefinitionFreezesAndRefusesHarvest() {
        var crop = CropState.planted(net.minecraft.resources.Identifier.parse("test:removed"));
        assertEquals(crop, CropLifecycle.newDay(crop, null, Season.SPRING, true, false));
        assertFalse(CropLifecycle.canHarvest(crop, null));
    }
    @Test void harvestRemovesBeforeEmittingAndCannotDuplicateOrDropOnFailure() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.turnip();
            var crop = new CropState(definition.id(), 5, 1, false);
            var present = new AtomicBoolean(true);
            var outputs = new ArrayList<ItemStack>();
            var product = List.of(new ItemStack(CropContent.TURNIP.get()));
            assertFalse(CropBlock.harvestOnce(crop, definition, () -> product, () -> false, outputs::add));
            assertTrue(outputs.isEmpty());
            assertFalse(CropBlock.harvestOnce(crop, definition, List::of, () -> fail("must not remove without loot"), outputs::add));
            assertFalse(CropBlock.harvestOnce(CropState.planted(definition.id()), definition,
                    () -> fail("immature must not roll loot"), () -> true, outputs::add));
            assertTrue(CropBlock.harvestOnce(crop, definition, () -> product, () -> present.getAndSet(false), stack -> {
                assertFalse(present.get()); outputs.add(stack);
            }));
            assertFalse(CropBlock.harvestOnce(crop, definition, () -> product, () -> present.getAndSet(false), outputs::add));
            assertEquals(1, outputs.size());
        }
    }
}

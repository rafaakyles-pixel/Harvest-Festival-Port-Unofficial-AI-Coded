package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropRegrowthTest {
    @TempDir Path directory;

    static CropBlockEntity entity(CropState state) {
        var entity = new CropBlockEntity(BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
        entity.setCrop(state);
        return entity;
    }

    @Test void eightHarvestsTakeThirtyWateredUpdatesAndLastRemovesBeforeOutput() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            var entity = entity(CropState.planted(definition.id()));
            var present = new AtomicBoolean(true);
            var output = new ArrayList<ItemStack>();
            int updates = 0;
            for (int harvest = 1; harvest <= 8; harvest++) {
                int days = harvest == 1 ? 9 : 3;
                for (int day = 0; day < days; day++) {
                    assertFalse(CropLifecycle.canHarvest(entity.crop(), definition));
                    entity.setCrop(CropLifecycle.newDay(entity.crop(), definition, Season.SPRING, true, false));
                    updates++;
                }
                assertEquals(10, entity.crop().stage());
                var expected = entity.crop();
                int number = harvest;
                assertTrue(CropBlock.harvestOnce(expected, definition, () -> List.of(new ItemStack(CropContent.CUCUMBER.get())),
                        next -> entity.commitHarvest(expected, next, () -> present.getAndSet(false)), stack -> {
                            assertEquals(number < 8, present.get());
                            if (present.get()) {
                                assertEquals(number, entity.crop().harvests());
                                assertEquals(7, entity.crop().stage());
                                assertEquals(expected.dryDays(), entity.crop().dryDays());
                            }
                            output.add(stack);
                        }));
                assertFalse(CropBlock.harvestOnce(entity.crop(), definition, () -> fail("no immediate repeat"),
                        next -> fail("no immediate repeat"), output::add));
            }
            assertEquals(30, updates);
            assertEquals(8, output.size());
            assertEquals(CropState.INVALID, entity.crop());
            assertFalse(present.get());
        }
    }

    @Test void failedCommitAndStaleSnapshotCannotYieldOrSpendHarvests() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            var mature = new CropState(definition.id(), 10, 3, false, 3);
            var entity = entity(mature);
            var output = new ArrayList<ItemStack>();
            var product = List.of(new ItemStack(CropContent.CUCUMBER.get()));
            assertFalse(CropBlock.harvestOnce(mature, definition, () -> product, next -> false, output::add));
            assertEquals(mature, entity.crop());
            assertTrue(CropBlock.harvestOnce(mature, definition, () -> product,
                    next -> entity.commitHarvest(mature, next, () -> fail("not last harvest")), output::add));
            assertFalse(CropBlock.harvestOnce(mature, definition, () -> product,
                    next -> entity.commitHarvest(mature, next, () -> fail("stale")), output::add));
            assertEquals(1, output.size());
            assertEquals(4, entity.crop().harvests());
            assertEquals(3, entity.crop().dryDays());
            var last = new CropState(definition.id(), 10, 1, false, 7);
            entity.setCrop(last);
            assertFalse(CropBlock.harvestOnce(last, definition, () -> product,
                    next -> entity.commitHarvest(last, next, () -> false), output::add));
            assertEquals(last, entity.crop());
            assertEquals(1, output.size());
        }
    }

    @Test void emptyHarvestAdvancesCucumberButKeepsLegacyTurnip() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            var entity = entity(new CropState(definition.id(), 10, 2, false, 0));
            for (int number = 0; number < 8; number++) {
                var expected = new CropState(definition.id(), 10, 2, false, number);
                entity.setCrop(expected);
                assertTrue(CropBlock.harvestOnce(expected, definition, () -> List.of(ItemStack.EMPTY),
                        next -> entity.commitHarvest(expected, next, () -> true), stack -> fail("empty loot")));
                if (number < 7) {
                    assertEquals(number + 1, entity.crop().harvests());
                    assertEquals(7, entity.crop().stage());
                } else assertEquals(CropState.INVALID, entity.crop());
            }
            var turnip = CropTestSupport.turnip();
            assertFalse(CropBlock.harvestOnce(new CropState(turnip.id(), 5, 1, false), turnip, List::of,
                    next -> fail("legacy empty loot preserves plant"), stack -> fail("empty loot")));
        }
    }

    @Test void breakingAtAnyHarvestRemovesAndDetachedEntityCannotDropAgain() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            for (int count : new int[]{0, 1, 6, 7}) {
                var entity = entity(new CropState(definition.id(), 10, 1, false, count));
                var expected = entity.crop();
                var present = new AtomicBoolean(true);
                var output = new ArrayList<ItemStack>();
                assertTrue(CropBlock.breakOnce(expected, definition, () -> List.of(new ItemStack(CropContent.CUCUMBER.get())),
                        () -> entity.commitHarvest(expected, null, () -> present.getAndSet(false)), stack -> {
                            assertFalse(present.get()); output.add(stack);
                        }));
                assertEquals(1, output.size());
                // Actual block drop entry point called by vanilla's second playerDestroy pass.
                var params = new LootParams.Builder(null).withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
                assertTrue(CropContent.CROP.get().getDrops(entity.getBlockState(), params).isEmpty());
                assertFalse(CropBlock.breakOnce(expected, definition, List::of,
                        () -> entity.commitHarvest(expected, null, () -> present.getAndSet(false)), output::add));
            }
        }
    }

    @Test void breakingImmatureDeadExhaustedOrUndefinedPlantsGivesNothingAndFailureKeepsState() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            for (var state : List.of(new CropState(definition.id(), 7, 1, false, 1),
                    new CropState(definition.id(), 10, 3, true, 2), new CropState(definition.id(), 10, 1, false, 8))) {
                var entity = entity(state);
                assertTrue(CropBlock.breakOnce(state, definition, () -> fail("ineligible"),
                        () -> entity.commitHarvest(state, null, () -> true), stack -> fail("no drop")));
                assertEquals(CropState.INVALID, entity.crop());
            }
            var mature = new CropState(definition.id(), 10, 1, false, 1);
            var entity = entity(mature);
            assertFalse(CropBlock.breakOnce(mature, definition, () -> List.of(new ItemStack(CropContent.CUCUMBER.get())),
                    () -> entity.commitHarvest(mature, null, () -> false), stack -> fail("failed removal")));
            assertEquals(mature, entity.crop());
            assertTrue(CropBlock.breakOnce(mature, null, () -> fail("undefined"), () -> true, stack -> fail("undefined")));
            assertTrue(CropBlock.breakOnce(mature, definition, List::of, () -> true, stack -> fail("empty")));
        }
    }

    @Test void regrowthPreservesHarvestCountThroughDroughtRescueAndSeasonalDeath() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var definition = CropTestSupport.cucumber();
            var crop = new CropState(definition.id(), 7, 1, false, 2);
            for (int day = 0; day < 2; day++) crop = CropLifecycle.newDay(crop, definition, Season.SPRING, false, false);
            assertEquals(7, crop.stage());
            assertEquals(3, crop.dryDays());
            assertEquals(2, crop.harvests());
            var dead = CropLifecycle.newDay(crop, definition, Season.SPRING, false, false);
            assertTrue(dead.dead());
            assertFalse(CropLifecycle.canHarvest(dead, definition));
            var rescued = CropLifecycle.newDay(crop, definition, Season.SPRING, true, false);
            assertFalse(rescued.dead());
            assertEquals(8, rescued.stage());
            assertEquals(2, rescued.harvests());
            assertTrue(CropLifecycle.newDay(rescued, definition, Season.SUMMER, true, false).dead());
            for (int day = 0; day < 10; day++) crop = CropLifecycle.newDay(crop, definition, Season.SPRING, false, true);
            assertFalse(crop.dead());
            assertEquals(7, crop.stage());
            assertTrue(CropLifecycle.newDay(crop, definition, Season.SUMMER, true, true).dead());
        }
    }
}

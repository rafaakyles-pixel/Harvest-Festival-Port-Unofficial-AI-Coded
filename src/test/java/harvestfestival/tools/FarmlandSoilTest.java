package harvestfestival.tools;

import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FarmlandSoilTest {
    @TempDir Path directory;

    @Test void emptySoilDriesInTwoStepsThenLosesPreparationOnThirdDay() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
            assertTrue(FarmlandSoil.watered(soil));
            soil = FarmlandSoil.dry(soil, Blocks.AIR.defaultBlockState());
            assertEquals(3, soil.getValue(FarmlandBlock.MOISTURE));
            assertFalse(FarmlandSoil.watered(soil));
            soil = FarmlandSoil.dry(soil, Blocks.AIR.defaultBlockState());
            assertEquals(0, soil.getValue(FarmlandBlock.MOISTURE));
            assertEquals(Blocks.DIRT.defaultBlockState(), FarmlandSoil.dry(soil, Blocks.AIR.defaultBlockState()));
        }
    }

    @Test void taggedPlantsIncludingWitheredCropsPreserveDryFarmland() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var soil = Blocks.FARMLAND.defaultBlockState();
            for (var appearance : harvestfestival.crops.CropBlock.APPEARANCE.getPossibleValues()) {
                var crop = CropContent.CROP.get().defaultBlockState().setValue(harvestfestival.crops.CropBlock.APPEARANCE, appearance);
                assertEquals(soil, FarmlandSoil.dry(soil, crop));
            }
            assertEquals(Blocks.DIRT.defaultBlockState(), FarmlandSoil.dry(soil, Blocks.STONE.defaultBlockState()));
        }
    }

    @Test void onlyFullMoistureGrowsAndOtherIntermediateValuesFollowOriginalNoOp() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            for (int value = 0; value <= 7; value++) {
                var soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, value);
                assertEquals(value == 7, FarmlandSoil.watered(soil));
                if (value != 0 && value != 3 && value != 7) assertEquals(soil, FarmlandSoil.dry(soil, Blocks.AIR.defaultBlockState()));
            }
            assertFalse(FarmlandSoil.managed(Blocks.DIRT.defaultBlockState()));
            assertEquals(Blocks.DIRT.defaultBlockState(), FarmlandSoil.dry(Blocks.DIRT.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        }
    }

    @Test void legacyStatesDecodeMigrateIdempotentlyAndPersistWithoutCreatingWater() {
        for (boolean wet : new boolean[]{false, true}) {
            var legacy = ToolContent.PREPARED_SOIL.get().defaultBlockState().setValue(PreparedSoilBlock.WATERED, wet);
            var saved = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, legacy).getOrThrow();
            var migrated = FarmlandSoil.migrate(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
            assertTrue(migrated.is(Blocks.FARMLAND));
            assertEquals(wet ? 7 : 0, migrated.getValue(FarmlandBlock.MOISTURE));
            assertSame(migrated, FarmlandSoil.migrate(migrated));
            var persisted = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, migrated).getOrThrow();
            assertEquals(migrated, BlockState.CODEC.parse(NbtOps.INSTANCE, persisted).getOrThrow());
        }
        assertSame(Blocks.STONE.defaultBlockState(), FarmlandSoil.migrate(Blocks.STONE.defaultBlockState()));
    }

    @Test void bundledWateringAcceptsEveryMoistureWithoutChargingTwice() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var rules = new ToolActionRules();
            rules.apply(rules.prepare(resources.manager, null), resources.manager, null);
            for (int value = 0; value <= 7; value++) {
                var target = new ToolActionsTest.Target(Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, value), "waterable");
                var can = ToolActionsTest.fullCan();
                assertEquals(value == 7 ? ToolActions.Result.UNCHANGED : ToolActions.Result.CHANGED,
                        ToolActions.apply(ToolAction.WATER, target, can, false, rules.rules(), message -> fail(message)));
                assertEquals(7, target.state().getValue(FarmlandBlock.MOISTURE));
                assertEquals(value == 7 ? 128 : 127, WateringCanState.get(can).water());
                assertEquals(ToolActions.Result.UNCHANGED,
                        ToolActions.apply(ToolAction.WATER, target, can, false, rules.rules(), message -> fail(message)));
            }
        }
    }

    @Test void taggedFarmlandRandomTickIsInterceptedBeforeAccessingWorld() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            // A null world deliberately fails if vanilla randomTick runs: tests the applied mixin,
            // not just its predicate. No world simulation or visual validation is implied.
            for (int moisture : new int[]{0, 3, 7}) {
                var soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, moisture);
                assertDoesNotThrow(() -> soil.randomTick(null, net.minecraft.core.BlockPos.ZERO,
                        net.minecraft.util.RandomSource.create()));
            }
        }
    }
}

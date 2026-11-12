package harvestfestival.crops;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropPlantingTest {
    @TempDir Path directory;
    @Test void oneBagPlantsNineValidPlotsAndCannotOverwrite() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var plot = new Plot();
            var seeds = new ItemStack(CropContent.TURNIP_SEEDS.get(), 2);
            var definition = CropDefinitions.INSTANCE.forSeed(seeds);
            assertEquals(9, CropSeedItem.plant(plot, BlockPos.ZERO, Direction.UP, seeds, definition));
            assertEquals(1, seeds.getCount());
            assertTrue(plot.plants.values().stream().allMatch(state -> state.stage() == 1 && state.dryDays() == 1));
            assertEquals(0, CropSeedItem.plant(plot, BlockPos.ZERO, Direction.UP, seeds, definition));
            assertEquals(1, seeds.getCount());
        }
    }
    @Test void invalidFaceSoilSpacePermissionAndFailedWritesNeverConsume() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var seeds = new ItemStack(CropContent.TURNIP_SEEDS.get(), 2);
            var definition = CropTestSupport.turnip();
            assertEquals(0, CropSeedItem.plant(new Plot(), BlockPos.ZERO, Direction.DOWN, seeds, definition));
            assertEquals(0, CropSeedItem.plant(new Plot(), BlockPos.ZERO, Direction.UP, seeds, null));
            for (int mode = 0; mode < 4; mode++) {
                var plot = new Plot();
                plot.mode = mode;
                assertEquals(0, CropSeedItem.plant(plot, BlockPos.ZERO, Direction.UP, seeds, definition));
                assertTrue(plot.plants.isEmpty());
            }
            assertEquals(2, seeds.getCount());
        }
    }
    @Test void partialBagSkipsBlockedPositionsAndConsumesOnlyOnce() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var plot = new Plot();
            plot.blocked.add(BlockPos.ZERO.above());
            plot.denied.add(new BlockPos(1, -1, 1));
            var seeds = new ItemStack(CropContent.TURNIP_SEEDS.get());
            assertEquals(7, CropSeedItem.plant(plot, BlockPos.ZERO, Direction.UP, seeds, CropTestSupport.turnip()));
            assertTrue(seeds.isEmpty());
            assertFalse(plot.plants.containsKey(BlockPos.ZERO));
            assertFalse(plot.plants.containsKey(new BlockPos(1, 0, 1)));
        }
    }
    private static final class Plot implements CropSeedItem.Plot {
        final Map<BlockPos, CropState> plants = new HashMap<>();
        final Set<BlockPos> blocked = new HashSet<>(), denied = new HashSet<>();
        int mode = -1;
        public boolean editable(BlockPos pos) { return mode != 0 && !denied.contains(pos); }
        public boolean empty(BlockPos pos) { return mode != 1 && !plants.containsKey(pos) && !blocked.contains(pos); }
        public boolean soilMatches(BlockPos pos, TagKey<Block> tag) {
            return mode != 2 && net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState().is(tag);
        }
        public boolean plant(BlockPos pos, CropState state) { if (mode == 3) return false; plants.put(pos, state); return true; }
    }
}

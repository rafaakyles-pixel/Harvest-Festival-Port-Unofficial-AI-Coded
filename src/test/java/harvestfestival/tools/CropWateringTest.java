package harvestfestival.tools;

import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropDailyUpdates;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropWateringTest {
    @TempDir Path directory;
    @Test void wateringCropResolvesSameSoilAndCostsOnlyOneUnit() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var soilPos = new BlockPos(1, 80, 2);
            var cropPos = soilPos.above();
            java.util.function.Function<BlockPos, BlockState> states = pos -> pos.equals(cropPos)
                    ? CropContent.CROP.get().defaultBlockState() : Blocks.FARMLAND.defaultBlockState();
            assertEquals(soilPos, ToolActions.resolveTarget(ToolAction.WATER, cropPos, states));
            assertEquals(soilPos, ToolActions.resolveTarget(ToolAction.WATER, soilPos, states));
            assertEquals(cropPos, ToolActions.resolveTarget(ToolAction.HOE, cropPos, states));
            assertEquals(cropPos, ToolActions.resolveTarget(ToolAction.WATER, cropPos, pos -> Blocks.STONE.defaultBlockState()));
            var rules = new ToolActionRules();
            rules.apply(rules.prepare(fixture.manager, null), fixture.manager, null);
            var target = new Target();
            var can = ToolActionsTest.fullCan();
            assertEquals(ToolActions.Result.CHANGED, ToolActions.apply(ToolAction.WATER, target, can, false, rules.rules(), message -> fail(message)));
            assertTrue(CropDailyUpdates.watered(target.state));
            assertEquals(ToolActions.Result.UNCHANGED, ToolActions.apply(ToolAction.WATER, target, can, false, rules.rules(), message -> fail(message)));
            assertEquals(127, WateringCanState.get(can).water());
            target.state = CropDailyUpdates.dry(target.state, CropContent.CROP.get().defaultBlockState());
            assertEquals(ToolActions.Result.CHANGED, ToolActions.apply(ToolAction.WATER, target, can, false, rules.rules(), message -> fail(message)));
            assertEquals(126, WateringCanState.get(can).water());
        }
    }
    private static final class Target implements ToolActions.Target {
        BlockState state = Blocks.FARMLAND.defaultBlockState();
        public BlockState state() { return state; }
        public boolean airAbove() { return false; }
        public boolean canEdit() { return true; }
        public boolean set(BlockState value) { state = value; return true; }
    }
}

package harvestfestival.crops;

import harvestfestival.tools.FarmlandSoil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Returns whether any soil was actually changed, not merely found. */
public final class SprinklerHydration {
    public interface Soil {
        boolean available(BlockPos pos);
        BlockState get(BlockPos pos);
        boolean set(BlockPos pos, BlockState state);
    }
    public static boolean water(BlockPos center, int range, Soil soil) {
        boolean changed = false;
        for (var pos : SprinklerBlockEntity.positions(center, range)) {
            if (!soil.available(pos)) continue;
            var state = soil.get(pos);
            if (FarmlandSoil.managed(state) && !FarmlandSoil.watered(state)
                    && soil.set(pos, state.setValue(FarmlandBlock.MOISTURE, 7))) changed = true;
        }
        return changed;
    }
    private SprinklerHydration() {}
}

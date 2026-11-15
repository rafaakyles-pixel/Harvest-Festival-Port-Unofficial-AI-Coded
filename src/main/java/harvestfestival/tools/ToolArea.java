package harvestfestival.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Reproduces ItemTool's rectangle and iteration order, using the actual look direction. */
public final class ToolArea {
    private ToolArea() {}
    public static List<BlockPos> positions(BlockPos anchor, Direction facing, ToolAction action, ToolTier tier) {
        int front = action == ToolAction.HOE ? tier.hoeFront() : tier.waterFront();
        int sides = action == ToolAction.HOE ? 0 : tier.waterSides();
        int minX = anchor.getX(), maxX = minX, minZ = anchor.getZ(), maxZ = minZ;
        switch (facing) {
            case SOUTH -> { minX -= sides; maxX += sides; maxZ += front; }
            case NORTH -> { minX -= sides; maxX += sides; minZ -= front; }
            case EAST -> { maxX += front; minZ -= sides; maxZ += sides; }
            case WEST -> { minX -= front; minZ -= sides; maxZ += sides; }
            default -> throw new IllegalArgumentException("Horizontal facing required");
        }
        var positions = new ArrayList<BlockPos>();
        for (int y = anchor.getY() - (action == ToolAction.WATER ? 1 : 0); y <= anchor.getY(); y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++) positions.add(new BlockPos(x, y, z));
        return List.copyOf(positions);
    }
    /** A second visit to a wet target remains UNCHANGED; never prepay an entire rectangle. */
    public static int perform(List<BlockPos> positions, BooleanSupplier usable,
            Function<BlockPos, ToolActions.Result> action, Consumer<BlockPos> changed) {
        int count = 0;
        for (var pos : positions) {
            if (!usable.getAsBoolean()) break;
            if (action.apply(pos) == ToolActions.Result.CHANGED) {
                changed.accept(pos);
                count++;
            }
        }
        return count;
    }
}

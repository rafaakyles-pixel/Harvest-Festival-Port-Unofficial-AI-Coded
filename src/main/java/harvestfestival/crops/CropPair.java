package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Pair consequences do not publish a second break event or charge a second tool task. */
public final class CropPair {
    private CropPair() {}
    public static BlockPos partner(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(CropContent.CROP_TOP.get())) return pos.below();
        // Match cleanup even while a missing/invalid datapack definition freezes the logical crop.
        if (level.getBlockState(pos).is(CropContent.CROP.get())
                && level.getBlockState(pos.above()).is(CropContent.CROP_TOP.get())) return pos.above();
        return pos;
    }
    public static boolean permitted(ServerLevel level, BlockPos pos, Player player, ItemStack tool) {
        var other = partner(level, pos);
        return permitted(pos, other, candidate -> allowed(level, candidate, player, tool));
    }
    public static boolean permitted(BlockPos pos, BlockPos other, java.util.function.Predicate<BlockPos> allowed) {
        return allowed.test(pos) && (other.equals(pos) || allowed.test(other));
    }
    private static boolean allowed(ServerLevel level, BlockPos pos, Player player, ItemStack tool) {
        return level.hasChunkAt(pos) && level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, tool)
                && (!(player instanceof ServerPlayer server) || !player.blockActionRestricted(level, pos, server.gameMode.getGameModeForPlayer()));
    }
}

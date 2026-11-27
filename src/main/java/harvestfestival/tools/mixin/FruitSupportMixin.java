package harvestfestival.tools.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import harvestfestival.crops.FruitSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** The patched removal result covers creative too, after break cancellation and permissions. */
@Mixin(ServerPlayerGameMode.class)
abstract class FruitSupportMixin {
    @Shadow protected ServerLevel level;
    @WrapMethod(method = "removeBlock")
    private boolean harvestfestival$confirmedLeafBreak(BlockPos pos, BlockState state, boolean canHarvest,
            ItemStack tool, Operation<Boolean> original) {
        var fruit = level.getBlockState(pos.below());
        boolean removed = original.call(pos, state, canHarvest, tool);
        FruitSupport.broken(level, pos, state, fruit, removed);
        return removed;
    }
}

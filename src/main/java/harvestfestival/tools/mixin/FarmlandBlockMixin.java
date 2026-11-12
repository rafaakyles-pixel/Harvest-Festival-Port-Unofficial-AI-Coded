package harvestfestival.tools.mixin;

import harvestfestival.tools.FarmlandSoil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No event is fired by randomTick; daily updates own moisture for tagged farmland. */
@Mixin(FarmlandBlock.class)
abstract class FarmlandBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void harvestfestival$dailyMoisture(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random, CallbackInfo callback) {
        if (FarmlandSoil.managed(state)) callback.cancel();
    }
}

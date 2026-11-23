package harvestfestival.tools.mixin;

import harvestfestival.crops.TreeProducers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Retains the two original sampling positions, even after their trunks are removed. */
@Mixin(ServerLevel.class)
abstract class TreeRandomTickMixin {
    @ModifyExpressionValue(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;isRandomlyTicking()Z"))
    private boolean harvestfestival$virtualSection(boolean ticking, @Local(argsOnly = true) LevelChunk chunk,
            @Local(name = "sectionIndex") int sectionIndex) {
        return ticking || TreeProducers.get((ServerLevel)(Object)this).hasVirtualTicks(chunk, sectionIndex);
    }
    @ModifyExpressionValue(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBlockRandomPos(IIII)Lnet/minecraft/core/BlockPos;", ordinal = 1))
    private BlockPos harvestfestival$virtualSample(BlockPos pos) {
        TreeProducers.get((ServerLevel)(Object)this).randomAt((ServerLevel)(Object)this, pos);
        return pos;
    }
}

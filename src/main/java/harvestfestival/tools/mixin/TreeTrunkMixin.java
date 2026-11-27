package harvestfestival.tools.mixin;

import harvestfestival.crops.TreeProducers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Break/place events do not cover fire, explosions, commands and reentrant writes. */
@Mixin(LevelChunk.class)
abstract class TreeTrunkMixin {
    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState harvestfestival$trunkChanged(LevelChunkSection section, int x, int y, int z, BlockState state,
            Operation<BlockState> original, @Local(argsOnly = true) BlockPos pos) {
        var old = original.call(section, x, y, z, state);
        if (((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level) {
            TreeProducers.changed(level, pos, old, state);
            harvestfestival.crops.FruitSupport.changed(level, pos, old, state);
        }
        return old;
    }
}

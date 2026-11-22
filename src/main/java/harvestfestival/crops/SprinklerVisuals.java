package harvestfestival.crops;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

/** Only invoked by the client ticker; no client types in the block entity's public signatures. */
final class SprinklerVisuals {
    static void particles(Level level, BlockPos pos, SprinklerBlock block, int cost, long amount) {
        long time = Math.floorMod(level.clockManager().getTotalTicks(level.registryAccess().getOrThrow(WorldClocks.OVERWORLD)), 24000);
        if (!SprinklerBlockEntity.fueled(cost, amount) || time < 6000 || time > 6250 || level.isRaining()) return;
        int count = 2 - Minecraft.getInstance().options.particles().get().ordinal();
        for (int i = 0; i < count * 32; i++) {
            double x = (level.getRandom().nextDouble() - .5) / (block.range() == 1 ? 3 : 1);
            double z = (level.getRandom().nextDouble() - .5) / (block.range() == 1 ? 3 : 1);
            for (double[] offset : new double[][]{{0,0},{-.05,-.05},{-.05,.05},{.05,-.05},{.05,.05}})
                level.addParticle(ParticleTypes.SPLASH, pos.getX() + .5, pos.getY() + block.height(), pos.getZ() + .5, x + offset[0], 0, z + offset[1]);
        }
    }
}

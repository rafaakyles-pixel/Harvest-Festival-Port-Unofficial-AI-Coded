package harvestfestival.crops;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** World writes must succeed before the logical stage is committed. No items are emitted here. */
public final class CropGrowth {
    private CropGrowth() {}
    public interface Target {
        boolean replaceWithFruit();
        boolean ensureTop(CropState next);
    }
    public static boolean apply(CropBlockEntity entity, CropDefinition definition, CropState next, Target target) {
        var before = entity.crop();
        if (definition == null || entity.isRemoved() || !before.crop().equals(definition.id())
                || !before.crop().equals(next.crop())) return false;
        if (!next.dead() && definition.form().transforms() && before.stage() < next.stage()
                && next.stage() == definition.maturity()) {
            if (entity.commitHarvest(before, null, target::replaceWithFruit)) return true;
            entity.setCrop(new CropState(before.crop(), before.stage(), next.dryDays(), false, before.harvests()));
            return false;
        }
        if (definition.form().doubled() && next.stage() >= definition.form().doubleFrom()
                && !target.ensureTop(next)) {
            // An obstruction cannot be overwritten, including after a datapack reload.
            return false;
        }
        if (entity.isRemoved()) return false;
        entity.setCrop(next);
        return true;
    }
    static void advance(ServerLevel level, CropBlockEntity entity, CropDefinition definition, CropState next) {
        var pos = entity.getBlockPos();
        apply(entity, definition, next, new Target() {
            public boolean replaceWithFruit() {
                var block = BuiltInRegistries.BLOCK.getOptional(definition.form().matureBlock());
                return block.isPresent() && level.getBlockEntity(pos) == entity
                        && level.setBlock(pos, block.get().defaultBlockState(), Block.UPDATE_ALL)
                        && level.getBlockState(pos).is(block.get());
            }
            public boolean ensureTop(CropState state) {
                var above = pos.above();
                if (!level.isInWorldBounds(above) || !level.hasChunkAt(above)) return false;
                var existing = level.getBlockState(above);
                if (!existing.isAir() && !existing.is(CropContent.CROP_TOP.get())) return false;
                var top = CropContent.CROP_TOP.get().defaultBlockState()
                        .setValue(CropTopBlock.STAGE, Math.min(11, state.stage()))
                        .setValue(CropBlock.WITHERED, state.dead());
                return (existing.equals(top) || level.setBlock(above, top, Block.UPDATE_ALL))
                        && level.getBlockEntity(pos) == entity && level.getBlockState(above).equals(top);
            }
        });
    }
}

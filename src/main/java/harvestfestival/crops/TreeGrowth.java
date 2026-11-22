package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** Server-only tree growth. Read availability before querying any destination. */
public final class TreeGrowth {
    public static boolean available(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
    public static boolean replaceable(BlockState state) { return state.isAir() || state.is(OrchardContent.REPLACEABLE); }
    public static BlockPos rotate(BlockPos offset, Rotation rotation) {
        return switch (rotation) {
            case NONE -> offset;
            case CLOCKWISE_90 -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180 -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
        };
    }
    public static Map<BlockPos, BlockState> template(TreeDefinition definition, BlockPos base, Rotation rotation) {
        var result = new LinkedHashMap<BlockPos, BlockState>();
        for (var cell : definition.template()) {
            var offset = cell.pos().offset(definition.offsetX(), 0, definition.offsetZ());
            result.put(base.offset(rotate(offset, rotation)), cell.state().rotate(rotation));
        }
        return result;
    }
    public static boolean area(ServerLevel level, BlockPos base, int radius) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (x == 0 && z == 0) continue;
            var pos = base.offset(x, 1, z);
            if (!available(level, pos) || !replaceable(level.getBlockState(pos))) return false;
        }
        return true;
    }
    /** The approved port requires the entire template to fit before changing the world. */
    public static boolean placeTemplate(ServerLevel level, BlockPos base, TreeDefinition definition, Rotation rotation) {
        var plan = template(definition, base, rotation);
        plan.replaceAll((pos, state) -> state.getBlock() instanceof FruitLeavesBlock
                ? state.setValue(FruitLeavesBlock.BLOOM, harvestfestival.calendar.WorldCalendar.get(level.getServer()).season() == Season.SPRING) : state);
        var clear = new LinkedHashMap<BlockPos, BlockState>();
        for (var pos : plan.keySet()) {
            if (!available(level, pos) || !available(level, pos.above())) return false;
            var previous = level.getBlockState(pos);
            if (previous.getDestroySpeed(level, pos) < 0 || !replaceable(previous)) return false;
            var above = level.getBlockState(pos.above());
            if (above.is(OrchardContent.BUSHES) && !plan.containsKey(pos.above())) clear.put(pos.above(), Blocks.AIR.defaultBlockState());
        }
        clear.putAll(plan);
        return commit(level, clear);
    }
    static boolean commit(ServerLevel level, Map<BlockPos, BlockState> plan) {
        var before = new LinkedHashMap<BlockPos, BlockState>();
        for (var entry : plan.entrySet()) {
            var pos = entry.getKey();
            var old = level.getBlockState(pos);
            if (old.equals(entry.getValue())) continue;
            before.put(pos, old);
            if (!level.setBlock(pos, entry.getValue(), Block.UPDATE_CLIENTS)) {
                for (var previous : before.entrySet()) level.setBlock(previous.getKey(), previous.getValue(), Block.UPDATE_CLIENTS);
                return false;
            }
        }
        for (var pos : before.keySet()) level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        return true;
    }
    public static void grow(ServerLevel level, TreeBlockEntity tree, Season season) {
        var definition = TreeDefinitions.INSTANCE.get(tree.species());
        if (!tree.valid() || definition == null || season == Season.WINTER || tree.isRemoved()) return;
        var pos = tree.getBlockPos();
        int stage = TreeLifecycle.advance(tree.stage(), definition, season, new TreeLifecycle.Space() {
            public boolean area() { return TreeGrowth.area(level, pos, definition.radius()); }
            public boolean juvenile() {
                var top = pos.above();
                if (!available(level, top) || !level.isEmptyBlock(top)) return false;
                return level.setBlock(top, OrchardContent.TREES.get(tree.species()).top().get().defaultBlockState()
                        .setValue(TreeBlock.APPEARANCE, 2), Block.UPDATE_CLIENTS);
            }
            public boolean mature() { return placeTemplate(level, pos, definition, Rotation.values()[level.getRandom().nextInt(4)]); }
            public void fruit() { TreeGrowth.fruit(level, tree.species(), pos); }
        });
        tree.stage(stage);
    }
    public static void fruit(ServerLevel level, String species, BlockPos base) {
        var leaves = OrchardContent.blockTag(species + "_fruit_leaves");
        findFruitPosition(base, level.getRandom(), pos -> available(level, pos) && available(level, pos.above())
                && level.isEmptyBlock(pos) && level.getBlockState(pos.above()).is(leaves))
                .ifPresent(pos -> level.setBlock(pos, OrchardContent.TREES.get(species).fruit().get().defaultBlockState(), Block.UPDATE_ALL));
    }
    public static java.util.Optional<BlockPos> findFruitPosition(BlockPos base, net.minecraft.util.RandomSource random,
            java.util.function.Predicate<BlockPos> suitable) {
        for (int attempt = 0; attempt <= 1000; attempt++) {
            var pos = base.offset(random.nextInt(16) - 8, random.nextInt(8), random.nextInt(16) - 8);
            if (suitable.test(pos)) return java.util.Optional.of(pos);
        }
        return java.util.Optional.empty();
    }
    private TreeGrowth() {}
}

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
        return placeTemplate(level, base, definition, rotation, null);
    }
    private static boolean placeTemplate(ServerLevel level, BlockPos base, TreeDefinition definition, Rotation rotation, TreeProducers.Producer producer) {
        var plan = template(definition, base, rotation);
        for (var cell : definition.template()) {
            var pos = base.offset(rotate(cell.pos().offset(definition.offsetX(), 0, definition.offsetZ()), rotation));
            plan.put(pos, cell.state().rotate(level, pos, rotation));
        }
        plan.replaceAll((pos, state) -> state.getBlock() instanceof FruitLeavesBlock
                ? state.setValue(FruitLeavesBlock.BLOOM, harvestfestival.calendar.WorldCalendar.get(level.getServer()).season() == Season.SPRING) : state);
        boolean placed = place(plan, new Placement() {
            public boolean available(BlockPos pos) { return TreeGrowth.available(level, pos); }
            public BlockState get(BlockPos pos) { return level.getBlockState(pos); }
            public boolean replaceable(BlockPos pos, BlockState state) { return state.getDestroySpeed(level, pos) >= 0 && TreeGrowth.replaceable(state); }
            public boolean bush(BlockState state) { return state.is(OrchardContent.BUSHES); }
            public boolean set(BlockPos pos, BlockState state) { return level.setBlock(pos, state, Block.UPDATE_CLIENTS); }
            public void notify(BlockPos pos) { level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock()); }
        });
        if (placed && producer != null) TreeProducers.get(level).matured(level, producer, plan);
        return placed;
    }
    public interface Placement {
        boolean available(BlockPos pos);
        BlockState get(BlockPos pos);
        boolean replaceable(BlockPos pos, BlockState state);
        boolean bush(BlockState state);
        boolean set(BlockPos pos, BlockState state);
        void notify(BlockPos pos);
    }
    public static boolean place(Map<BlockPos, BlockState> plan, Placement world) {
        var clear = new LinkedHashMap<BlockPos, BlockState>();
        for (var pos : plan.keySet()) {
            if (!world.available(pos) || !world.available(pos.above())) return false;
            var previous = world.get(pos);
            if (!world.replaceable(pos, previous)) return false;
            var above = world.get(pos.above());
            if (world.bush(above) && !plan.containsKey(pos.above())) clear.put(pos.above(), Blocks.AIR.defaultBlockState());
        }
        clear.putAll(plan);
        return commit(world, clear);
    }
    static boolean commit(Placement world, Map<BlockPos, BlockState> plan) {
        var before = new LinkedHashMap<BlockPos, BlockState>();
        for (var entry : plan.entrySet()) {
            var pos = entry.getKey();
            var old = world.get(pos);
            if (old.equals(entry.getValue())) continue;
            before.put(pos, old);
            if (!world.set(pos, entry.getValue())) {
                for (var previous : before.entrySet()) world.set(previous.getKey(), previous.getValue());
                return false;
            }
        }
        for (var pos : before.keySet()) world.notify(pos);
        return true;
    }
    public static void grow(ServerLevel level, TreeBlockEntity tree, Season season) {
        var data = TreeProducers.get(level);
        var producer = data.attach(level, tree);
        if (producer != null) data.grow(level, producer, season);
    }
    static void growYoung(ServerLevel level, TreeBlockEntity tree, TreeProducers.Producer producer, Season season) {
        var definition = TreeDefinitions.INSTANCE.get(tree.species());
        if (!tree.valid() || definition == null || season == Season.WINTER || tree.isRemoved()) return;
        var pos = tree.getBlockPos();
        int stage = TreeLifecycle.advance(producer.stage, definition, season, new TreeLifecycle.Space() {
            public boolean area() { return TreeGrowth.area(level, pos, definition.radius()); }
            public boolean juvenile() {
                var top = pos.above();
                if (!available(level, top) || !level.isEmptyBlock(top)) return false;
                return level.setBlock(top, OrchardContent.TREES.get(tree.species()).top().get().defaultBlockState()
                        .setValue(TreeBlock.APPEARANCE, 2), Block.UPDATE_CLIENTS);
            }
            public boolean mature() { return placeTemplate(level, pos, definition, Rotation.values()[level.getRandom().nextInt(4)], producer); }
            public void fruit() { TreeGrowth.fruit(level, tree.species(), pos); }
        });
        producer.stage = stage;
        TreeProducers.get(level).setDirty();
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

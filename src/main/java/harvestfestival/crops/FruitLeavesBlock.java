package harvestfestival.crops;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Four-step leaf connectivity. Species is the registry identity, never packed with decay flags. */
public final class FruitLeavesBlock extends Block {
    public static final BooleanProperty CHECK_DECAY = BooleanProperty.create("check_decay");
    public static final BooleanProperty BLOOM = BooleanProperty.create("bloom");
    public FruitLeavesBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(CHECK_DECAY, false).setValue(BLOOM, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CHECK_DECAY, BLOOM); }
    @Override public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above()) && !level.getBlockState(pos.below()).isSolidRender() && random.nextInt(15) == 1)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER,
                    pos.getX() + random.nextFloat(), pos.getY() - .05, pos.getZ() + random.nextFloat(), 0, 0, 0);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var level = context.getLevel();
        return defaultBlockState().setValue(BLOOM, level instanceof ServerLevel server
                && harvestfestival.calendar.WorldCalendar.get(server.getServer()).season() == harvestfestival.calendar.Season.SPRING);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState other, RandomSource random) {
        ticks.scheduleTick(pos, this, 1);
        return state;
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Modern logs notify adjacent blocks rather than Forge's old beginLeavesDecay hook.
        markNearby(level, pos, 4);
    }
    static void markNearby(ServerLevel level, BlockPos pos, int radius) {
        for (var target : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
            if (!TreeGrowth.available(level, target)) continue;
            var state = level.getBlockState(target);
            if (state.getBlock() instanceof FruitLeavesBlock && !state.getValue(CHECK_DECAY)) level.setBlock(target, state.setValue(CHECK_DECAY, true), Block.UPDATE_CLIENTS);
        }
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) { markNearby(level, pos, 1); }
    public static boolean connected(BlockPos origin, java.util.function.Predicate<BlockPos> log, java.util.function.Predicate<BlockPos> leaf) {
        record Step(BlockPos pos, int distance) {}
        var queue = new ArrayDeque<Step>(); var visited = new HashSet<BlockPos>();
        queue.add(new Step(origin, 0)); visited.add(origin);
        while (!queue.isEmpty()) {
            var step = queue.remove();
            if (step.distance() >= 4) continue;
            for (var direction : Direction.values()) {
                var next = step.pos().relative(direction);
                if (!visited.add(next)) continue;
                if (log.test(next)) return true;
                if (leaf.test(next)) queue.add(new Step(next, step.distance() + 1));
            }
        }
        return false;
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHECK_DECAY)) return;
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
            if (!TreeGrowth.available(level, pos.offset(x, 0, z))) return;
        if (connected(pos, p -> level.getBlockState(p).is(OrchardContent.LOGS), p -> level.getBlockState(p).is(OrchardContent.LEAVES)))
            level.setBlock(pos, state.setValue(CHECK_DECAY, false), Block.UPDATE_CLIENTS);
        else {
            var fruit = level.getBlockState(pos.below());
            boolean removed = level.removeBlock(pos, false);
            FruitSupport.broken(level, pos, state, fruit, removed);
        }
    }
}

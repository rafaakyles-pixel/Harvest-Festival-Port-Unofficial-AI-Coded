package harvestfestival.crops;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Short-lived confirmations, never a general survival rule or persisted producer state. */
public final class FruitSupport {
    private static final Map<ServerLevel, FruitSupport> PENDING = new WeakHashMap<>();
    private final Map<BlockPos, BlockState> fruits = new HashMap<>();

    public static void broken(ServerLevel level, BlockPos leaf, BlockState before, BlockState fruit, boolean removed) {
        if (!removed || level.restoringBlockSnapshots || !before.is(OrchardContent.LEAVES)
                || !(fruit.getBlock() instanceof TreeFruitBlock) || !TreeGrowth.available(level, leaf.below())
                || !level.getBlockState(leaf.below()).equals(fruit) || level.getBlockState(leaf).is(OrchardContent.LEAVES)) return;
        PENDING.computeIfAbsent(level, ignored -> new FruitSupport()).confirmed(leaf, fruit);
    }
    void confirmed(BlockPos leaf, BlockState fruit) { fruits.put(leaf.below().immutable(), fruit); }

    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        var pending = PENDING.get(level);
        if (pending != null && !before.equals(after)) pending.changed(pos);
    }
    void changed(BlockPos pos) {
        fruits.remove(pos); // Includes harvest, seasonal removal, replacement and snapshot restoration.
        fruits.remove(pos.below()); // Any later support write invalidates this confirmation.
    }
    void settle(boolean capturing, Predicate<BlockPos> available, Function<BlockPos, BlockState> state,
            BiConsumer<BlockPos, BlockState> collect) {
        if (capturing) return;
        var ready = new HashMap<>(fruits);
        ready.forEach((pos, expected) -> {
            // Consume before callbacks, retaining other tickets so reentrant writes can invalidate them.
            if (fruits.remove(pos, expected) && available.test(pos) && available.test(pos.above()) && state.apply(pos).equals(expected)
                    && !state.apply(pos.above()).is(OrchardContent.LEAVES)) collect.accept(pos, expected);
        });
    }
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pending = PENDING.get(level);
        if (pending == null) return;
        pending.settle(level.captureBlockSnapshots || level.restoringBlockSnapshots || !level.capturedBlockSnapshots.isEmpty(),
                pos -> TreeGrowth.available(level, pos), level::getBlockState,
                (pos, state) -> TreeFruitBlock.collect(state, level, pos));
        if (pending.fruits.isEmpty()) PENDING.remove(level);
    }
}

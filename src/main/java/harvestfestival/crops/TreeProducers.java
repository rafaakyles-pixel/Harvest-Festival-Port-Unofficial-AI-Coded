package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import harvestfestival.calendar.DailyCycleData;
import harvestfestival.calendar.Season;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-owned identities. Items and leaves never carry or create a producer. */
public final class TreeProducers extends SavedData {
    private static final Map<ServerLevel, TreeProducers> ACTIVE = new WeakHashMap<>();
    record Trunk(BlockPos pos, BlockState state) {
        static final Codec<Trunk> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Trunk::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Trunk::state)).apply(i, Trunk::new));
    }
    public static final class Producer {
        private static final Codec<Map<BlockPos, BlockState>> TRUNKS = Trunk.CODEC.listOf().xmap(list -> {
            Map<BlockPos, BlockState> result = new LinkedHashMap<>();
            list.forEach(t -> result.put(t.pos(), t.state())); return result;
        }, map -> map.entrySet().stream().map(e -> new Trunk(e.getKey(), e.getValue())).toList());
        static final Codec<Producer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter((Producer p) -> p.id),
                BlockPos.CODEC.fieldOf("origin").forGetter(p -> p.origin),
                Codec.STRING.fieldOf("species").forGetter(p -> p.species),
                Codec.intRange(1, 10000).fieldOf("stage").forGetter(p -> p.stage),
                DailyCycleData.MARKER_CODEC.fieldOf("last_cycle").forGetter(p -> p.lastCycle),
                Codec.BOOL.fieldOf("mature").forGetter(p -> p.mature),
                TRUNKS.fieldOf("trunks").forGetter(p -> p.trunks)
        ).apply(i, Producer::new));
        final String id;
        final BlockPos origin;
        final String species;
        int stage;
        long lastCycle;
        boolean mature;
        final Map<BlockPos, BlockState> trunks;
        Producer(String id, BlockPos origin, String species, int stage, long cycle, boolean mature, Map<BlockPos, BlockState> trunks) {
            this.id = id; this.origin = origin.immutable(); this.species = species; this.stage = stage;
            this.lastCycle = cycle; this.mature = mature; this.trunks = new LinkedHashMap<>(trunks);
        }
    }
    public static final Codec<TreeProducers> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter((TreeProducers data) -> 1),
            Producer.CODEC.listOf().fieldOf("trees").forGetter(data -> List.copyOf(data.trees.values()))
    ).apply(i, (schema, entries) -> new TreeProducers(entries)));
    public static final SavedDataType<TreeProducers> TYPE = new SavedDataType<>(
            Identifier.parse("harvestfestival:tree_producers"), () -> new TreeProducers(List.of()), CODEC);
    final Map<String, Producer> trees = new LinkedHashMap<>();
    private final Map<BlockPos, String> owners = new HashMap<>();
    private final Set<BlockPos> pending = new HashSet<>();
    TreeProducers(List<Producer> entries) {
        for (var p : entries) {
            // Conflicting persisted identities fail closed, never multiply producers.
            if (trees.containsKey(p.id) || p.trunks.isEmpty() || p.trunks.keySet().stream().anyMatch(owners::containsKey)) continue;
            trees.put(p.id, p);
            p.trunks.keySet().forEach(pos -> owners.put(pos, p.id));
        }
    }
    public static TreeProducers get(ServerLevel level) {
        return ACTIVE.computeIfAbsent(level, l -> l.getDataStorage().computeIfAbsent(TYPE));
    }
    Producer producer(TreeBlockEntity tree) {
        var p = trees.get(tree.producerId());
        return p != null && p.origin.equals(tree.getBlockPos()) && p.species.equals(tree.species())
                && p.id.equals(owners.get(p.origin)) ? p : null;
    }
    public Producer attach(ServerLevel level, TreeBlockEntity tree) {
        return attach(tree, pos -> TreeGrowth.available(level, pos), level::getBlockState);
    }
    Producer attach(TreeBlockEntity tree, java.util.function.Predicate<BlockPos> loaded,
            java.util.function.Function<BlockPos, BlockState> stateAt) {
        if (!tree.valid() || tree.isRemoved()) return null;
        if (!tree.producerId().isEmpty()) return producer(tree);
        var definition = TreeDefinitions.INSTANCE.get(tree.species());
        if (definition == null || owners.containsKey(tree.getBlockPos())) return null;
        var p = new Producer(UUID.randomUUID().toString(), tree.getBlockPos(), tree.species(), tree.stage(), tree.lastCycle(),
                tree.stage() >= definition.maturity(), Map.of(tree.getBlockPos(), tree.getBlockState()));
        trees.put(p.id, p); owners.put(p.origin, p.id); tree.producerId(p.id);
        // Old saves can prove only the HF base and its HF top, never template logs.
        if (p.mature && loaded.test(p.origin.above())) {
            var state = stateAt.apply(p.origin.above());
            if (state.is(OrchardContent.TREES.get(p.species).top().get()) && !TreeBlock.juvenile(state)) bind(p, p.origin.above(), state);
        }
        setDirty(); return p;
    }
    void bind(Producer p, BlockPos pos, BlockState state) {
        if (owners.containsKey(pos) && !p.id.equals(owners.get(pos))) return;
        p.trunks.put(pos.immutable(), state); owners.put(pos.immutable(), p.id); setDirty();
    }
    void matured(ServerLevel level, Producer p, Map<BlockPos, BlockState> plan) {
        matured(p, plan, pos -> TreeGrowth.available(level, pos), level::getBlockState);
    }
    void matured(Producer p, Map<BlockPos, BlockState> plan, java.util.function.Predicate<BlockPos> loaded,
            java.util.function.Function<BlockPos, BlockState> stateAt) {
        p.mature = true;
        for (var entry : plan.entrySet()) if (entry.getValue().is(OrchardContent.LOGS)
                && loaded.test(entry.getKey()) && stateAt.apply(entry.getKey()).is(entry.getValue().getBlock())) bind(p, entry.getKey(), entry.getValue());
        var top = p.origin.above();
        if (loaded.test(top) && stateAt.apply(top).is(OrchardContent.TREES.get(p.species).top().get())) bind(p, top, stateAt.apply(top));
        setDirty();
    }
    /** Called immediately after the section mutation, before reentrant neighbor callbacks. */
    public static void changed(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (before.is(after.getBlock())) return;
        var data = get(level);
        if (!data.owners.containsKey(pos)) return;
        data.changed(pos, before, after, level.captureBlockSnapshots || level.restoringBlockSnapshots);
    }
    void changed(BlockPos pos, BlockState before, BlockState after, boolean tentative) {
        if (before.is(after.getBlock()) || !owners.containsKey(pos)) return;
        if (tentative) { pending.add(pos.immutable()); return; }
        if (before.is(net.minecraft.tags.BlockTags.LOGS) && after.is(net.minecraft.tags.BlockTags.LOGS)) {
            bind(trees.get(owners.get(pos)), pos, after); return;
        }
        sever(pos);
    }
    void sever(BlockPos pos) {
        var id = owners.remove(pos);
        var p = trees.get(id);
        if (p == null) return;
        p.trunks.remove(pos);
        if (p.trunks.isEmpty()) trees.remove(id);
        setDirty();
    }
    public void reconcile(ServerLevel level) {
        reconcile(pos -> TreeGrowth.available(level, pos), level::getBlockState);
    }
    public void settlePending(ServerLevel level) {
        if (!pending.isEmpty()) reconcile(level);
    }
    void reconcile(java.util.function.Predicate<BlockPos> loaded, java.util.function.Function<BlockPos, BlockState> stateAt) {
        for (var pos : List.copyOf(pending)) {
            if (!loaded.test(pos)) continue;
            pending.remove(pos);
            var p = trees.get(owners.get(pos));
            if (p != null) changed(pos, p.trunks.get(pos), stateAt.apply(pos), false);
        }
        // Also verifies loaded saves; an unloaded trunk is unknown, not destroyed.
        for (var p : List.copyOf(trees.values())) for (var entry : List.copyOf(p.trunks.entrySet()))
            if (loaded.test(entry.getKey())) changed(entry.getKey(), entry.getValue(), stateAt.apply(entry.getKey()), false);
    }
    boolean beginCycle(Producer p, long cycle, java.util.function.Predicate<BlockPos> loaded) {
        if (cycle <= 0 || !trees.containsKey(p.id) || !loaded.test(p.origin) || p.lastCycle >= cycle
                || p.trunks.keySet().stream().noneMatch(loaded)) return false;
        p.lastCycle = cycle; setDirty(); return true;
    }
    public void daily(ServerLevel level, long cycle, Season season) {
        reconcile(level);
        for (var p : List.copyOf(trees.values())) {
            if (TreeDefinitions.INSTANCE.get(p.species) == null || !beginCycle(p, cycle, pos -> TreeGrowth.available(level, pos))) continue;
            if (TreeDefinitions.INSTANCE.settings().daily()) grow(level, p, season);
            sync(level, p);
        }
    }
    void grow(ServerLevel level, Producer p, Season season) {
        if (!trees.containsKey(p.id) || !TreeGrowth.available(level, p.origin)) return;
        // The mutation hook removes links immediately. Only this tree needs a load check here.
        for (var entry : List.copyOf(p.trunks.entrySet())) if (TreeGrowth.available(level, entry.getKey()))
            changed(entry.getKey(), entry.getValue(), level.getBlockState(entry.getKey()), false);
        if (!trees.containsKey(p.id) || p.trunks.keySet().stream().noneMatch(pos -> TreeGrowth.available(level, pos))) return;
        if (!p.mature) {
            if (level.getBlockEntity(p.origin) instanceof TreeBlockEntity tree && producer(tree) == p) TreeGrowth.growYoung(level, tree, p, season);
            return;
        }
        var definition = TreeDefinitions.INSTANCE.get(p.species);
        if (definition == null) return;
        advanceMature(p, definition, season, new TreeLifecycle.Space() {
            public boolean area() { return TreeGrowth.area(level, p.origin, definition.radius()); }
            public boolean juvenile() { return false; }
            public boolean mature() { return false; }
            public void fruit() { TreeGrowth.fruit(level, p.species, p.origin); }
        });
        sync(level, p);
    }
    void advanceMature(Producer p, TreeDefinition definition, Season season, TreeLifecycle.Space space) {
        if (!p.mature || !trees.containsKey(p.id)) return;
        p.stage = TreeLifecycle.advance(p.stage, definition, season, space);
        setDirty();
    }
    void sync(ServerLevel level, Producer p) {
        if (TreeGrowth.available(level, p.origin) && level.getBlockEntity(p.origin) instanceof TreeBlockEntity tree && producer(tree) == p) {
            tree.mark(p.lastCycle); tree.stage(p.stage);
        }
    }
    public void importChunk(ServerLevel level, LevelChunk chunk) {
        for (var entity : new ArrayList<>(chunk.getBlockEntities().values())) if (entity instanceof TreeBlockEntity tree) attach(level, tree);
    }
    // Original base/top random callbacks remain physical while present. Missing callbacks
    // use the exact original coordinates sampled by ServerLevel, never every remaining log.
    public boolean hasVirtualTicks(LevelChunk chunk, int sectionIndex) {
        if (TreeDefinitions.INSTANCE.settings().daily()) return false;
        for (var p : trees.values()) if (p.mature && chunk.getPos().x() == p.origin.getX() >> 4 && chunk.getPos().z() == p.origin.getZ() >> 4)
            for (var pos : new BlockPos[]{p.origin, p.origin.above()})
                if (!p.id.equals(owners.get(pos)) && chunk.getSectionIndex(pos.getY()) == sectionIndex) return true;
        return false;
    }
    public void randomAt(ServerLevel level, BlockPos pos) {
        if (TreeDefinitions.INSTANCE.settings().daily()) return;
        for (var p : virtualAt(pos)) random(level, p, pos);
    }
    List<Producer> virtualAt(BlockPos pos) {
        return trees.values().stream().filter(p -> p.mature && (pos.equals(p.origin) || pos.equals(p.origin.above()))
                && !p.id.equals(owners.get(pos))).toList();
    }
    void random(ServerLevel level, Producer p, BlockPos physical) {
        if (level.getMaxLocalRawBrightness(physical.above()) >= 9 && level.getRandom().nextInt(20) == 0)
            grow(level, p, CropDailyUpdates.seasonForDimension(level.dimension(), harvestfestival.calendar.WorldCalendar.get(level.getServer()).season()));
    }
    Producer at(BlockPos pos) { return trees.get(owners.get(pos)); }
}

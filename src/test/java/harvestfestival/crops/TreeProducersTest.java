package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TreeProducersTest {
    @TempDir Path directory;
    static final BlockPos ROOT = new BlockPos(15, 64, 15);
    static final BlockPos LOG = ROOT.offset(1, 3, 0);
    static TreeProducers.Producer mature(String species, int stage) {
        var content = OrchardContent.TREES.get(species);
        return new TreeProducers.Producer(UUID.randomUUID().toString(), ROOT, species, stage, 50, true,
                Map.of(ROOT, content.base().get().defaultBlockState().setValue(TreeBlock.APPEARANCE, 3),
                        ROOT.above(), content.top().get().defaultBlockState().setValue(TreeBlock.APPEARANCE, 3),
                        LOG, Blocks.OAK_LOG.defaultBlockState()));
    }
    @Test void detachedProducerKeepsCycleAndFruitsOnceAfterSaveReloadForEverySpecies() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var species : OrchardContent.TREES.keySet()) {
                var definition = TreeDefinitions.INSTANCE.get(species);
                var p = mature(species, definition.regrow());
                var data = new TreeProducers(List.of(p));
                data.sever(ROOT); data.sever(ROOT.above());
                var ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(NbtOps.INSTANCE);
                var saved = TreeProducers.CODEC.encodeStart(ops, data).getOrThrow();
                data = TreeProducers.CODEC.parse(ops, saved).getOrThrow(); p = data.trees.get(p.id);
                assertNotNull(p); assertEquals(ROOT, p.origin); assertEquals(50, p.lastCycle);
                assertEquals(definition.regrow(), p.stage); assertEquals(Set.of(LOG), p.trunks.keySet());
                var space = new OrchardTest.Space();
                var season = species.equals("apple") || species.equals("grape") ? Season.AUTUMN : Season.SUMMER;
                int days = definition.maximum() - definition.regrow();
                for (long cycle = 51; cycle <= 50 + days; cycle++) {
                    assertTrue(data.beginCycle(p, cycle, pos -> true));
                    data.advanceMature(p, definition, season, space);
                    assertFalse(data.beginCycle(p, cycle, pos -> true));
                }
                assertEquals(1, space.fruits); assertEquals(0, space.crowns); assertEquals(0, space.tops);
                assertEquals(definition.regrow(), p.stage); assertEquals(1, data.trees.size());
            }
        }
    }
    @Test void lastTrunkRemovalAndImmediateReplacementCannotReviveProducer() {
        var p = mature("peach", 39); var data = new TreeProducers(List.of(p));
        data.sever(ROOT); data.sever(ROOT.above());
        data.changed(LOG, Blocks.OAK_LOG.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
        data.changed(LOG, Blocks.AIR.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
        data.changed(ROOT, Blocks.AIR.defaultBlockState(), OrchardContent.TREES.get("peach").leaves().get().defaultBlockState(), false);
        assertTrue(data.trees.isEmpty()); assertNull(data.at(LOG)); assertFalse(data.beginCycle(p, 51, pos -> true));
        var clone = new TreeBlockEntity(ROOT, OrchardContent.TREES.get("peach").base().get().defaultBlockState());
        clone.producerId(p.id);
        assertNull(data.attach(clone, pos -> true, pos -> Blocks.AIR.defaultBlockState()));
    }
    @Test void legacyMigrationUsesOnlyProvenHfPairAndNeverTemplateOrLeaves() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var content = OrchardContent.TREES.get("peach");
            var tree = new TreeBlockEntity(ROOT, content.base().get().defaultBlockState().setValue(TreeBlock.APPEARANCE, 3));
            tree.stage(39); tree.mark(72);
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var old = tree.getUpdateTag(registry); old.putInt("schema", 1); old.remove("producer");
            tree.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, old));
            assertEquals("", tree.producerId()); assertTrue(tree.valid());
            var data = new TreeProducers(List.of());
            var p = data.attach(tree, pos -> true, pos -> {
                assertEquals(ROOT.above(), pos); return content.top().get().defaultBlockState().setValue(TreeBlock.APPEARANCE, 3);
            });
            assertNotNull(p); assertEquals(39, p.stage); assertEquals(72, p.lastCycle);
            assertEquals(Set.of(ROOT, ROOT.above()), p.trunks.keySet());
            assertSame(p, data.attach(tree, pos -> true, pos -> { fail("Must not scan again"); return null; }));
            var reloaded = new TreeBlockEntity(ROOT, tree.getBlockState());
            reloaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, tree.getUpdateTag(registry)));
            assertSame(p, data.producer(reloaded)); assertEquals(p.id, reloaded.producerId());
            data.sever(ROOT); assertEquals(1, data.trees.size()); data.sever(ROOT.above()); assertTrue(data.trees.isEmpty());
            assertNull(data.attach(reloaded, pos -> true, pos -> Blocks.OAK_LOG.defaultBlockState()));
        }
    }
    @Test void unloadedTrunksFreezeWithoutBeingDestroyedOrLoadingTheirChunks() {
        var p = mature("peach", 39); var data = new TreeProducers(List.of(p));
        data.sever(ROOT); data.sever(ROOT.above());
        data.reconcile(pos -> false, pos -> { fail("Unloaded position queried"); return null; });
        assertEquals(1, data.trees.size()); assertFalse(data.beginCycle(p, 51, pos -> pos.equals(ROOT)));
        assertEquals(50, p.lastCycle);
        data.reconcile(pos -> true, pos -> Blocks.OAK_LOG.defaultBlockState());
        assertTrue(data.beginCycle(p, 51, pos -> true));
        data.reconcile(pos -> true, pos -> Blocks.AIR.defaultBlockState()); assertTrue(data.trees.isEmpty());
    }
    @Test void canceledSnapshotAndStateOnlyChangesKeepIdentityButCommittedRemovalEndsIt() {
        var p = mature("peach", 39); var data = new TreeProducers(List.of(p));
        data.sever(ROOT); data.sever(ROOT.above());
        var log = Blocks.OAK_LOG.defaultBlockState();
        data.changed(LOG, log, log.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X), false);
        assertSame(p, data.at(LOG));
        data.changed(LOG, log, Blocks.AIR.defaultBlockState(), true);
        data.reconcile(pos -> true, pos -> log); assertSame(p, data.at(LOG));
        data.changed(LOG, log, Blocks.AIR.defaultBlockState(), true);
        data.reconcile(pos -> true, pos -> Blocks.AIR.defaultBlockState()); assertTrue(data.trees.isEmpty());
    }
    @Test void maturityBindsExactlyPlacedLogsAcrossAllTemplatesAndRotations() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var species : OrchardContent.TREES.keySet()) for (var rotation : Rotation.values()) {
                var p = mature(species, 1); p.mature = false;
                p.trunks.keySet().removeIf(pos -> !pos.equals(ROOT));
                var data = new TreeProducers(List.of(p));
                var plan = TreeGrowth.template(TreeDefinitions.INSTANCE.get(species), ROOT, rotation);
                var world = new HashMap<>(plan); world.put(ROOT, p.trunks.get(ROOT));
                world.put(ROOT.above(), OrchardContent.TREES.get(species).top().get().defaultBlockState());
                data.matured(p, plan, pos -> true, pos -> world.getOrDefault(pos, Blocks.AIR.defaultBlockState()));
                var expected = new HashSet<>(Set.of(ROOT, ROOT.above()));
                plan.forEach((pos, state) -> { if (state.is(OrchardContent.LOGS)) expected.add(pos); });
                assertEquals(expected, p.trunks.keySet()); assertTrue(p.mature);
                assertTrue(p.trunks.values().stream().noneMatch(s -> s.getBlock() instanceof FruitLeavesBlock));
            }
        }
    }
    @Test void randomModeUsesOnlyTwoOriginalPositionsAndMixinsApplyToTargetClasses() {
        var p = mature("peach", 39); var data = new TreeProducers(List.of(p));
        assertTrue(data.virtualAt(ROOT).isEmpty()); assertTrue(data.virtualAt(ROOT.above()).isEmpty());
        data.sever(ROOT); assertEquals(List.of(p), data.virtualAt(ROOT)); assertTrue(data.virtualAt(ROOT.above()).isEmpty());
        data.sever(ROOT.above()); assertEquals(List.of(p), data.virtualAt(ROOT.above()));
        assertTrue(data.virtualAt(LOG).isEmpty()); assertTrue(data.virtualAt(ROOT.offset(4, 6, 3)).isEmpty());
        assertTrue(Arrays.stream(net.minecraft.world.level.chunk.LevelChunk.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("trunkChanged")));
        assertTrue(Arrays.stream(net.minecraft.server.level.ServerLevel.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("virtualSample")));
        assertTrue(Arrays.stream(net.minecraft.server.level.ServerLevel.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("virtualSection")));
    }
    @Test void conflictingSavedOwnershipCannotCreateTwoTreesFromSameTrunk() {
        var p = mature("peach", 39); var other = mature("orange", 52);
        var data = new TreeProducers(List.of(p, other, p));
        assertEquals(1, data.trees.size()); assertSame(p, data.at(LOG));
    }
    @Test void strippingKeepsOneExistingLinkWithoutMakingHarvestedLogsProductive() {
        var registry = BuiltInRegistries.BLOCK;
        var saved = registry.listTags().collect(java.util.stream.Collectors.toMap(net.minecraft.core.HolderSet.Named::key, t -> t.stream().toList()));
        try {
            var tags = new HashMap<>(saved);
            tags.put(net.minecraft.tags.BlockTags.LOGS, List.of(Blocks.OAK_LOG.builtInRegistryHolder(), Blocks.STRIPPED_OAK_LOG.builtInRegistryHolder()));
            registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.BLOCK, tags)).apply();
            var p = mature("peach", 39); var data = new TreeProducers(List.of(p));
            data.sever(ROOT); data.sever(ROOT.above());
            data.changed(LOG, Blocks.OAK_LOG.defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), false);
            data.reconcile(pos -> true, pos -> Blocks.STRIPPED_OAK_LOG.defaultBlockState());
            assertSame(p, data.at(LOG)); assertEquals(1, data.trees.size());
            data.changed(LOG, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            data.changed(LOG, Blocks.AIR.defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), false);
            assertTrue(data.trees.isEmpty());
        } finally {
            registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.BLOCK, saved)).apply();
        }
    }
}

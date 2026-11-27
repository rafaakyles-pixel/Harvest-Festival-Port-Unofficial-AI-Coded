package harvestfestival.crops;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FruitSupportTest {
    @TempDir Path directory;
    private static final BlockPos FRUIT = new BlockPos(0, 64, 0);
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    @Test void fiveSpeciesConsumeOnlyOneConfirmationAndNeverTheSideFruit() {
        for (var tree : OrchardContent.TREES.values()) {
            var pending = new FruitSupport();
            var state = tree.fruit().get().defaultBlockState();
            var world = new HashMap<BlockPos, BlockState>();
            world.put(FRUIT, state); world.put(FRUIT.east(), state);
            pending.confirmed(FRUIT.above(), state);
            pending.confirmed(FRUIT.above(), state);
            var drops = new AtomicInteger();
            for (int i = 0; i < 3; i++) pending.settle(false, p -> true, p -> world.getOrDefault(p, AIR), (p, s) -> {
                assertEquals(FRUIT, p); assertEquals(state, s);
                world.remove(p); pending.changed(p); drops.incrementAndGet();
            });
            assertEquals(1, drops.get()); assertEquals(state, world.get(FRUIT.east()));
        }
    }
    @Test void restorationAndInterveningHarvestInvalidateEvenIdenticalReplacement() {
        var state = OrchardContent.TREES.get("grape").fruit().get().defaultBlockState();
        for (var changed : new BlockPos[]{FRUIT, FRUIT.above()}) {
            var pending = new FruitSupport(); pending.confirmed(FRUIT.above(), state);
            pending.changed(changed);
            pending.settle(false, p -> true, p -> p.equals(FRUIT) ? state : AIR,
                    (p, s) -> fail("Restored/replaced/harvested fruit must not drop"));
        }
    }
    @Test void snapshotsWaitForCommitAndRestoredLeavesDoNotYield() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var tree : OrchardContent.TREES.values()) {
                var pending = new FruitSupport(); var state = tree.fruit().get().defaultBlockState();
                pending.confirmed(FRUIT.above(), state);
                pending.settle(true, p -> true, p -> { fail("Snapshot still open"); return AIR; }, (p,s) -> fail());
                pending.settle(false, p -> true, p -> p.equals(FRUIT) ? state : tree.leaves().get().defaultBlockState(), (p,s) -> fail());
                pending.confirmed(FRUIT.above(), state);
                var count = new AtomicInteger();
                pending.settle(false, p -> true, p -> p.equals(FRUIT) ? state : AIR, (p,s) -> count.incrementAndGet());
                assertEquals(1, count.get());
            }
        }
    }
    @Test void missingChunksAndSeasonallyRemovedFruitCannotYieldOrLoad() {
        var state = OrchardContent.TREES.get("banana").fruit().get().defaultBlockState();
        var pending = new FruitSupport(); pending.confirmed(FRUIT.above(), state);
        pending.settle(false, p -> false, p -> { fail("Must not read unavailable chunk"); return AIR; }, (p,s) -> fail());
        pending.confirmed(FRUIT.above(), state);
        pending.settle(false, p -> true, p -> AIR, (p,s) -> fail());
    }
    @Test void selectionBoundsFollowOpaquePixelsOfCrossModelsAndCollisionStaysEmpty() throws Exception {
        for (var entry : OrchardContent.TREES.entrySet()) {
            var species = entry.getKey(); var state = entry.getValue().fruit().get().defaultBlockState();
            int minX = 2, maxX = 13, minY = 1, maxY = 14; // vanilla apple, exact 26.2 texture
            if (!species.equals("apple")) {
                try (var stream = getClass().getResourceAsStream("/assets/harvestfestival/textures/block/" + species + "_fruit.png")) {
                    var image = ImageIO.read(stream); minX = minY = 16; maxX = maxY = -1;
                    for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((image.getRGB(x,y) >>> 24) != 0) {
                        minX = Math.min(minX,x); maxX = Math.max(maxX,x); minY = Math.min(minY,y); maxY = Math.max(maxY,y);
                    }
                }
            }
            var bounds = state.getShape(null, FRUIT).bounds();
            assertEquals((.8 + 14.4 * minX / 16) / 16, bounds.minX, 1e-9);
            assertEquals((.8 + 14.4 * (maxX + 1) / 16) / 16, bounds.maxX, 1e-9);
            assertEquals(bounds.minX, bounds.minZ, 1e-9); assertEquals(bounds.maxX, bounds.maxZ, 1e-9);
            assertEquals((15 - maxY) / 16.0, bounds.minY, 1e-9); assertEquals((16 - minY) / 16.0, bounds.maxY, 1e-9);
            assertTrue(state.getCollisionShape(null, FRUIT, CollisionContext.empty()).isEmpty());
        }
    }
    @Test void confirmationMixinAppliesToExactServerGameMode() {
        assertTrue(Arrays.stream(net.minecraft.server.level.ServerPlayerGameMode.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().contains("confirmedLeafBreak")));
    }
    @Test void simultaneousLeafLossesAndHarvestsConsumeOnlySurvivingFruits() {
        var pending = new FruitSupport();
        var world = new HashMap<BlockPos, BlockState>();
        var removed = new java.util.HashSet<BlockPos>();
        int x = 0;
        for (var tree : OrchardContent.TREES.values()) {
            var pos = FRUIT.offset(x++ * 3, 0, 0);
            var state = tree.fruit().get().defaultBlockState();
            world.put(pos, state);
            pending.confirmed(pos.above(), state);
            pending.confirmed(pos.above(), state); // Duplicate notifications still represent one fruit.
        }
        pending.changed(FRUIT.above()); // Snapshot restoration cancels this leaf loss.
        pending.changed(FRUIT.offset(3, 0, 0)); // Direct harvest wins over queued leaf loss.
        pending.changed(FRUIT.offset(0, 0, 1)); // Unrelated adjacent leaf does not cancel others.
        pending.settle(false, p -> true, p -> world.getOrDefault(p, AIR), (p, s) -> {
            assertTrue(removed.add(p)); world.remove(p); pending.changed(p);
        });
        assertEquals(3, removed.size());
        pending.settle(false, p -> true, p -> world.getOrDefault(p, AIR), (p, s) -> fail("Already consumed"));
    }
    @Test void collectionRequiresSuccessfulRemovalAndYieldsExactlyOneCorrectProduct() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var tree : OrchardContent.TREES.values()) {
                var state = tree.fruit().get().defaultBlockState();
                var current = new java.util.concurrent.atomic.AtomicReference<>(state);
                var drops = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
                assertFalse(TreeFruitBlock.collect(state, current::get, () -> false, drops::add));
                assertTrue(drops.isEmpty());
                assertTrue(TreeFruitBlock.collect(state, current::get, () -> { current.set(AIR); return true; }, drops::add));
                assertFalse(TreeFruitBlock.collect(state, current::get, () -> { fail("Stale harvest must not write"); return true; }, drops::add));
                assertEquals(1, drops.size()); assertEquals(1, drops.getFirst().getCount());
                assertSame(tree.product().get(), drops.getFirst().getItem());
            }
        }
    }
}

package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OrchardTest {
    @Test void matureTrunkHalvesSurvivePartnerRemovalWhileJuvenileTopNeedsBase() {
        for(var tree:OrchardContent.TREES.values()) {
            var base=tree.base().get().defaultBlockState().setValue(TreeBlock.APPEARANCE,3);
            var top=tree.top().get().defaultBlockState().setValue(TreeBlock.APPEARANCE,3);
            assertFalse(TreeBlock.juvenile(base));assertFalse(TreeBlock.juvenile(top));
            // Mature removal must return before accessing/removing any neighboring block.
            tree.base().get().affectNeighborsAfterRemoval(base,null,BlockPos.ZERO,false);
            tree.top().get().affectNeighborsAfterRemoval(top,null,BlockPos.ZERO.above(),false);
            assertSame(top,tree.top().get().updateShape(top,null,null,BlockPos.ZERO.above(),net.minecraft.core.Direction.DOWN,
                    BlockPos.ZERO,Blocks.AIR.defaultBlockState(),net.minecraft.util.RandomSource.create(1)));
            var young=top.setValue(TreeBlock.APPEARANCE,2);
            assertTrue(TreeBlock.juvenile(young));
            assertTrue(tree.top().get().updateShape(young,null,null,BlockPos.ZERO.above(),net.minecraft.core.Direction.DOWN,
                    BlockPos.ZERO,Blocks.AIR.defaultBlockState(),net.minecraft.util.RandomSource.create(1)).isAir());
        }
    }
    @TempDir Path directory;
    static final class Space implements TreeLifecycle.Space {
        boolean free = true, top = true, crown = true;
        int tops, crowns, fruits;
        public boolean area() { return free; }
        public boolean juvenile() { tops++; return top; }
        public boolean mature() { crowns++; return crown; }
        public void fruit() { fruits++; }
    }
    @Test void fullTemplatePreflightProtectsObstaclesUnloadedCellsAndRollsBackFailedWrites() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            class World implements TreeGrowth.Placement {
                final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> cells=new HashMap<>();
                final Set<BlockPos> unavailable=new HashSet<>();int writes;boolean failNext;
                public boolean available(BlockPos p){return !unavailable.contains(p);}
                public net.minecraft.world.level.block.state.BlockState get(BlockPos p){assertTrue(available(p));return cells.getOrDefault(p,Blocks.AIR.defaultBlockState());}
                public boolean replaceable(BlockPos p,net.minecraft.world.level.block.state.BlockState s){return s.isAir()||s.getBlock() instanceof FruitLeavesBlock;}
                public boolean bush(net.minecraft.world.level.block.state.BlockState s){return s.is(Blocks.DANDELION);}
                public boolean set(BlockPos p,net.minecraft.world.level.block.state.BlockState s){writes++;if(failNext&&writes==3){failNext=false;return false;}if(s.isAir())cells.remove(p);else cells.put(p,s);return true;}
                public void notify(BlockPos p){}
            }
            for(var species:OrchardContent.TREES.keySet())for(var rotation:Rotation.values()) {
                var plan=TreeGrowth.template(TreeDefinitions.INSTANCE.get(species),new BlockPos(15,64,15),rotation);
                var first=plan.keySet().iterator().next();var blocked=new World();blocked.cells.put(first,Blocks.STONE.defaultBlockState());
                assertFalse(TreeGrowth.place(plan,blocked));assertEquals(0,blocked.writes);assertTrue(blocked.cells.get(first).is(Blocks.STONE));
                var missing=new World();missing.unavailable.add(first.above());assertFalse(TreeGrowth.place(plan,missing));assertEquals(0,missing.writes);
                var failed=new World();failed.failNext=true;assertFalse(TreeGrowth.place(plan,failed));assertTrue(failed.cells.isEmpty());
                var success=new World();assertTrue(TreeGrowth.place(plan,success));assertEquals(plan,success.cells);
            }
        }
    }
    @Test void fullCycleAndRepeatedHarvestKeepOriginalStages() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            for (String species : OrchardContent.TREES.keySet()) {
                var d = TreeDefinitions.INSTANCE.get(species); assertNotNull(d, species);
                var space = new Space(); int stage = 1;
                var season = Season.valueOf(d.season().toUpperCase(Locale.ROOT));
                for (int day=1;day<d.maximum();day++) stage = TreeLifecycle.advance(stage,d,season,space);
                assertEquals(d.regrow(),stage); assertEquals(1,space.tops); assertEquals(1,space.crowns); assertEquals(1,space.fruits);
                for(int day=0;day<d.maximum()-d.regrow();day++) stage=TreeLifecycle.advance(stage,d,season,space);
                assertEquals(2,space.fruits); assertEquals(1,space.crowns);
            }
        }
    }
    @Test void blockedTransitionsWinterAndNullSeasonDoNotKillTree() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var d=TreeDefinitions.INSTANCE.get("peach"); var space=new Space();
            space.top=false; assertEquals(20,TreeLifecycle.advance(20,d,Season.SUMMER,space));
            space.crown=false; assertEquals(35,TreeLifecycle.advance(35,d,Season.SUMMER,space));
            assertEquals(40,TreeLifecycle.advance(40,d,Season.WINTER,space)); assertEquals(0,space.fruits);
            space.top=true;space.crown=true;
            int stage=1;for(int i=0;i<100;i++)stage=TreeLifecycle.advance(stage,d,null,space);
            assertEquals(40,stage);assertEquals(0,space.fruits);
            space.free=false;assertEquals(37,TreeLifecycle.advance(stage,d,Season.SUMMER,space));assertEquals(1,space.fruits);
        }
    }
    @Test void realCodecRejectsInvalidThresholdsAndRoundTripsAllCells() throws Exception {
        try(var resources=new CropTestSupport(directory)) {
            for(var species:OrchardContent.TREES.keySet()) {
                var d=TreeDefinitions.INSTANCE.get(species);
                var encoded=TreeDefinition.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow();
                assertEquals(d,TreeDefinition.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
                for(var r:Rotation.values())assertEquals(d.template().size(),TreeGrowth.template(d,new BlockPos(10,60,-10),r).size());
                encoded.getAsJsonObject().addProperty("maturity",2);
                assertTrue(TreeDefinition.CODEC.parse(JsonOps.INSTANCE,encoded).error().isPresent());
            }
        }
    }
    @Test void leafConnectivityUsesFourSixNeighborStepsWithoutDiagonalShortcut() {
        var origin=BlockPos.ZERO;
        assertTrue(FruitLeavesBlock.connected(origin,p->p.equals(new BlockPos(4,0,0)),p->p.getY()==0&&p.getZ()==0));
        assertFalse(FruitLeavesBlock.connected(origin,p->p.equals(new BlockPos(5,0,0)),p->p.getY()==0&&p.getZ()==0));
        assertFalse(FruitLeavesBlock.connected(origin,p->p.equals(new BlockPos(1,1,0)),p->false));
    }
    @Test void sprinklerGeometryAndOriginalFuelThreshold() {
        for(int range:new int[]{1,4}) {
            var positions=SprinklerBlockEntity.positions(new BlockPos(15,64,15),range);
            assertEquals(range==1?17:161,positions.size());assertEquals(positions.size(),new HashSet<>(positions).size());
            assertFalse(positions.contains(new BlockPos(15,64,15)));assertTrue(positions.contains(new BlockPos(15,63,15)));
            assertTrue(positions.stream().allMatch(p->p.getY()==63||p.getY()==64));
        }
        assertTrue(SprinklerBlockEntity.fueled(0,0));assertTrue(SprinklerBlockEntity.fueled(-1,0));
        assertFalse(SprinklerBlockEntity.fueled(1000,1));assertTrue(SprinklerBlockEntity.fueled(1000,2));
    }
    @Test void fruitSearchStopsAtOneOrExactly1001FailuresWithinOriginalBounds() {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        assertTrue(TreeGrowth.findFruitPosition(BlockPos.ZERO,net.minecraft.util.RandomSource.create(42),p->{
            assertTrue(p.getX()>=-8&&p.getX()<=7&&p.getZ()>=-8&&p.getZ()<=7&&p.getY()>=0&&p.getY()<=7);
            calls.incrementAndGet();return false;
        }).isEmpty());assertEquals(1001,calls.get());
        calls.set(0);assertTrue(TreeGrowth.findFruitPosition(BlockPos.ZERO,net.minecraft.util.RandomSource.create(42),p->calls.incrementAndGet()==5).isPresent());
        assertEquals(5,calls.get());
    }
    @Test void summerSpeciesRetainDistinctOriginalThresholdsTemplatesAndWood() throws Exception {
        try(var resources=new CropTestSupport(directory)) {
            var peach=TreeDefinitions.INSTANCE.get("peach");var orange=TreeDefinitions.INSTANCE.get("orange");var banana=TreeDefinitions.INSTANCE.get("banana");
            assertEquals(List.of(157,228,55),List.of(peach.template().size(),orange.template().size(),banana.template().size()));
            assertEquals(List.of(6,21,36,40,37,2),List.of(peach.sapling(),peach.juvenile(),peach.maturity(),peach.maximum(),peach.regrow(),peach.radius()));
            assertEquals(List.of(12,34,49,53,50,3),List.of(orange.sapling(),orange.juvenile(),orange.maturity(),orange.maximum(),orange.regrow(),orange.radius()));
            assertEquals(List.of(15,30,55,60,56,1),List.of(banana.sapling(),banana.juvenile(),banana.maturity(),banana.maximum(),banana.regrow(),banana.radius()));
            assertTrue(banana.template().stream().filter(c->!(c.state().getBlock() instanceof FruitLeavesBlock)).allMatch(c->c.state().is(Blocks.JUNGLE_LOG)));
            for(var d:List.of(peach,orange,banana)){assertTrue(d.fruitsIn(Season.SUMMER));assertFalse(d.fruitsIn(Season.AUTUMN));assertFalse(d.fruitsIn(null));}
        }
    }
    @Test void autumnUsesVanillaAppleAndDistinctGrapeCycleWithoutTrellis() throws Exception {
        try(var resources=new CropTestSupport(directory)) {
            var apple=TreeDefinitions.INSTANCE.get("apple");var grape=TreeDefinitions.INSTANCE.get("grape");
            assertEquals(97,apple.template().size());assertEquals(169,grape.template().size());
            assertEquals(4,grape.template().stream().filter(c->c.pos().getY()==0).count());
            assertSame(net.minecraft.world.item.Items.APPLE,OrchardContent.TREES.get("apple").product().get());
            assertEquals(List.of(6,21,36,40,37,2),List.of(apple.sapling(),apple.juvenile(),apple.maturity(),apple.maximum(),apple.regrow(),apple.radius()));
            assertEquals(List.of(12,34,49,53,50,2),List.of(grape.sapling(),grape.juvenile(),grape.maturity(),grape.maximum(),grape.regrow(),grape.radius()));
            for(var d:List.of(apple,grape)){assertTrue(d.fruitsIn(Season.AUTUMN));assertFalse(d.fruitsIn(Season.SUMMER));assertFalse(d.fruitsIn(null));assertEquals(-3,d.offsetX());assertEquals(-4,d.offsetZ());}
        }
    }
    @Test void treeDiskAndClientPayloadPreserveSpeciesStageAndCycle() throws Exception {
        try(var resources=new CropTestSupport(directory.resolve("pack"))) {
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for(var entry:OrchardContent.TREES.entrySet()) {
                var tree=new TreeBlockEntity(BlockPos.ZERO,entry.getValue().base().get().defaultBlockState());tree.stage(37);tree.mark(123);
                var file=directory.resolve(entry.getKey()+".nbt");NbtIo.writeCompressed(tree.saveWithFullMetadata(registry),file);
                var loaded=(TreeBlockEntity)BlockEntity.loadStatic(BlockPos.ZERO,tree.getBlockState(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()),registry);
                assertNotNull(loaded);assertTrue(loaded.valid());assertEquals(37,loaded.stage());assertEquals(123,loaded.lastCycle());assertEquals(entry.getKey(),loaded.species());
                var client=new TreeBlockEntity(BlockPos.ZERO,tree.getBlockState());
                client.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,registry,loaded.getUpdateTag(registry)));
                assertEquals(loaded.stage(),client.stage());assertEquals(loaded.lastCycle(),client.lastCycle());
            }
        }
    }
    @Test void lootCodecsUseShearsOrSilkTouchForLeavesAndKeepOtherDrops() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var registry=net.minecraft.data.registries.VanillaRegistries.createLookup();
            var ops=registry.createSerializationContext(JsonOps.INSTANCE);
            for(var item:List.of(net.minecraft.world.item.Items.SHEARS,net.minecraft.world.item.Items.DIAMOND_AXE))
                item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,1).build());
            for(var species:OrchardContent.TREES.keySet())for(var part:List.of("leaves","fruit")) {
                try(var input=getClass().getResourceAsStream("/data/harvestfestival/loot_table/blocks/"+species+"_"+part+".json")) {
                    assertNotNull(input);var json=JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8));
                    assertTrue(net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.parse(ops,json).result().isPresent());
                    var pool=json.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject();assertEquals(1,pool.get("rolls").getAsInt());
                    assertEquals(1,pool.getAsJsonArray("entries").size());
                    if(part.equals("leaves")) {
                        var predicate=net.minecraft.advancements.predicates.ItemPredicate.CODEC.parse(ops,pool.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonArray("terms").get(0).getAsJsonObject().get("predicate")).getOrThrow();
                        assertTrue(predicate.test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS)));
                        assertFalse(predicate.test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_AXE)));
                        assertFalse(predicate.test(net.minecraft.world.item.ItemStack.EMPTY));
                        var silk = net.minecraft.advancements.predicates.ItemPredicate.CODEC.parse(ops,
                                pool.getAsJsonArray("conditions").get(0).getAsJsonObject().getAsJsonArray("terms").get(1).getAsJsonObject().get("predicate")).getOrThrow();
                        var axe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_AXE);
                        axe.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                        assertFalse(silk.test(axe));
                        axe.enchant(registry.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
                        assertFalse(silk.test(axe));
                        axe.enchant(registry.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
                        assertTrue(silk.test(axe));
                    }
                }
            }
            for(var machine:List.of("old_sprinkler","iron_sprinkler"))try(var input=getClass().getResourceAsStream("/data/harvestfestival/loot_table/blocks/"+machine+".json")){
                assertNotNull(input);var text=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.parse(ops,JsonParser.parseString(text)).result().isPresent());
                assertFalse(text.contains("copy_components"));assertFalse(text.contains("copy_custom_data"));assertFalse(text.contains("silk_touch"));
            }
        }
    }
    @Test void plainsTagKeepsExactOriginalSupportSet() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            for(var b:List.of(Blocks.GRASS_BLOCK,Blocks.DIRT,Blocks.COARSE_DIRT,Blocks.PODZOL,Blocks.FARMLAND))assertTrue(b.defaultBlockState().is(OrchardContent.SOILS));
            for(var b:List.of(Blocks.DIRT_PATH,Blocks.MYCELIUM,Blocks.SOUL_SAND,Blocks.ROOTED_DIRT,Blocks.MOSS_BLOCK,Blocks.STONE))assertFalse(b.defaultBlockState().is(OrchardContent.SOILS));
        }
    }
    @Test void seedPlantsOnlyCenterAndRequiresBothEmptyEditableCellsAndValidSupport() {
        class Plot implements TreeSeedItem.Plot {
            Set<BlockPos> denied=new HashSet<>(),occupied=new HashSet<>();boolean support=true,success=true;List<BlockPos> placed=new ArrayList<>();
            public boolean editable(BlockPos p){return !denied.contains(p);}public boolean empty(BlockPos p){return !occupied.contains(p);}
            public boolean support(BlockPos p){return support;}public boolean place(BlockPos p){placed.add(p);return success;}
        }
        for(var denied:List.of(BlockPos.ZERO.below(),BlockPos.ZERO,BlockPos.ZERO.above())) {
            var plot=new Plot();plot.denied.add(denied);assertFalse(TreeSeedItem.plant(plot,BlockPos.ZERO,net.minecraft.core.Direction.UP));assertTrue(plot.placed.isEmpty());
        }
        for(var occupied:List.of(BlockPos.ZERO,BlockPos.ZERO.above())) {
            var plot=new Plot();plot.occupied.add(occupied);assertFalse(TreeSeedItem.plant(plot,BlockPos.ZERO,net.minecraft.core.Direction.UP));assertTrue(plot.placed.isEmpty());
        }
        var plot=new Plot();plot.support=false;assertFalse(TreeSeedItem.plant(plot,BlockPos.ZERO,net.minecraft.core.Direction.UP));
        plot.support=true;assertFalse(TreeSeedItem.plant(plot,BlockPos.ZERO,net.minecraft.core.Direction.NORTH));assertTrue(plot.placed.isEmpty());
        assertTrue(TreeSeedItem.plant(plot,BlockPos.ZERO,net.minecraft.core.Direction.UP));assertEquals(List.of(BlockPos.ZERO),plot.placed);
    }
    @Test void leafSpeciesSurvivesBothFlagsAndInvalidTreeSaveFreezes() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for(var tree:OrchardContent.TREES.values())for(boolean decay:List.of(false,true))for(boolean bloom:List.of(false,true)) {
                var state=tree.leaves().get().defaultBlockState().setValue(FruitLeavesBlock.CHECK_DECAY,decay).setValue(FruitLeavesBlock.BLOOM,bloom);
                var encoded=net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(JsonOps.INSTANCE,state).getOrThrow();
                assertEquals(state,net.minecraft.world.level.block.state.BlockState.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
            }
            var tree=new TreeBlockEntity(BlockPos.ZERO,OrchardContent.TREES.get("peach").base().get().defaultBlockState());
            var saved=tree.getUpdateTag(registry);saved.putString("species","apple");
            tree.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,registry,saved));assertFalse(tree.valid());
            saved.putString("species","peach");saved.putInt("stage",-1);
            tree.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,registry,saved));assertFalse(tree.valid());
        }
    }
}

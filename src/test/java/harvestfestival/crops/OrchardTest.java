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
    @TempDir Path directory;
    static final class Space implements TreeLifecycle.Space {
        boolean free = true, top = true, crown = true;
        int tops, crowns, fruits;
        public boolean area() { return free; }
        public boolean juvenile() { tops++; return top; }
        public boolean mature() { crowns++; return crown; }
        public void fruit() { fruits++; }
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
}

package harvestfestival.crops;

import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SprinklerTest {
    @TempDir Path directory;
    @Test void realTankRejectsLavaRollsBackAndClampsDrain() {
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA,Fluids.EMPTY)) fluid.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        var previous=BuiltInRegistries.FLUID.listTags().collect(java.util.stream.Collectors.toMap(HolderSet.Named::key,t->t.stream().toList()));
        BuiltInRegistries.FLUID.prepareTagReload(new TagLoader.LoadResult<>(Registries.FLUID,Map.of(OrchardContent.WATER,List.of(Fluids.WATER.builtInRegistryHolder())))).apply();
        try {
            var entity=new SprinklerBlockEntity(BlockPos.ZERO,OrchardContent.OLD.get().defaultBlockState());var tank=entity.tank();
            try(var tx=Transaction.openRoot()) {assertEquals(0,tank.insert(0,FluidResource.of(Fluids.LAVA),1000,tx));assertEquals(1000,tank.insert(0,FluidResource.of(Fluids.WATER),1500,tx));}
            assertEquals(0,tank.getAmountAsLong(0));
            try(var tx=Transaction.openRoot()){assertEquals(2,tank.insert(0,FluidResource.of(Fluids.WATER),2,tx));tx.commit();}
            assertEquals(2,tank.getAmountAsLong(0));
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var saved=entity.saveWithFullMetadata(registry);
            var restored=(SprinklerBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO,entity.getBlockState(),saved,registry);
            assertNotNull(restored);assertEquals(2,restored.tank().getAmountAsLong(0));
            try(var tx=Transaction.openRoot()){assertEquals(2,tank.extract(0,FluidResource.of(Fluids.WATER),1000,tx));tx.commit();}
            assertEquals(0,tank.getAmountAsLong(0));assertEquals(2,restored.tank().getAmountAsLong(0));
        } finally {BuiltInRegistries.FLUID.prepareTagReload(new TagLoader.LoadResult<>(Registries.FLUID,previous)).apply();}
    }
    @Test void hydrationSkipsUnavailableWetInvalidAndFailedWrites() throws Exception {
        try(var resources=new CropTestSupport(directory)) {
            var center=new BlockPos(15,64,15);var candidates=SprinklerBlockEntity.positions(center,1);
            var states=new HashMap<BlockPos,BlockState>();
            for(var p:candidates)states.put(p,Blocks.FARMLAND.defaultBlockState());
            states.put(candidates.get(1),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            states.put(candidates.get(2),Blocks.STONE.defaultBlockState());
            var writes=new ArrayList<BlockPos>();
            var soil=new SprinklerHydration.Soil(){
                public boolean available(BlockPos p){return !p.equals(candidates.get(0));}
                public BlockState get(BlockPos p){assertNotEquals(candidates.get(0),p);return states.get(p);}
                public boolean set(BlockPos p,BlockState state){writes.add(p);if(p.equals(candidates.get(3)))return false;states.put(p,state);return true;}
            };
            assertTrue(SprinklerHydration.water(center,1,soil));assertEquals(14,writes.size());
            assertEquals(0,states.get(candidates.get(3)).getValue(FarmlandBlock.MOISTURE));
            writes.clear();assertFalse(SprinklerHydration.water(center,1,soil));assertEquals(List.of(candidates.get(3)),writes);
        }
    }
    @Test void registeredBucketCapabilityOnlyEmptiesWholeBucketsAndCreativeRetainsHand() {
        var previous=BuiltInRegistries.FLUID.listTags().collect(java.util.stream.Collectors.toMap(HolderSet.Named::key,t->t.stream().toList()));
        BuiltInRegistries.FLUID.prepareTagReload(new TagLoader.LoadResult<>(Registries.FLUID,Map.of(OrchardContent.WATER,List.of(Fluids.WATER.builtInRegistryHolder())))).apply();
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA,Fluids.EMPTY)) fluid.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        for(var item:List.of(net.minecraft.world.item.Items.BUCKET,net.minecraft.world.item.Items.WATER_BUCKET)) item.builtInRegistryHolder().bindComponents(
                net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE,1).build());
        TreeDefinitions.INSTANCE.apply(new TreeDefinitions.Data(Map.of(),new OrchardSettings(true,false,true,1000)),null,null);
        try {
            var entity=new SprinklerBlockEntity(BlockPos.ZERO,OrchardContent.OLD.get().defaultBlockState());
            var held=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
            var emptied=entity.fillFrom(held,false);assertNotNull(emptied);assertTrue(emptied.is(net.minecraft.world.item.Items.BUCKET));
            assertEquals(1000,entity.tank().getAmountAsLong(0));assertTrue(held.is(net.minecraft.world.item.Items.WATER_BUCKET));
            entity.tank().set(0,FluidResource.of(Fluids.WATER),1);
            assertNull(entity.fillFrom(held,false));assertEquals(1,entity.tank().getAmountAsLong(0));
            entity.tank().set(0,FluidResource.EMPTY,0);
            assertSame(held,entity.fillFrom(held,true));assertEquals(1000,entity.tank().getAmountAsLong(0));
            assertNull(entity.fillFrom(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET),false));
        } finally {
            BuiltInRegistries.FLUID.prepareTagReload(new TagLoader.LoadResult<>(Registries.FLUID,previous)).apply();
            TreeDefinitions.INSTANCE.apply(new TreeDefinitions.Data(Map.of(),OrchardSettings.DEFAULT),null,null);
        }
    }
    @Test void registeredBlockCapabilityExposesOneTankOnAllFacesAndNull() {
        var faces=new ArrayList<Direction>(List.of(Direction.values()));faces.add(null);
        for(var block:List.of(OrchardContent.OLD.get(),OrchardContent.IRON.get())) {
            var entity=new SprinklerBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            for(var face:faces)assertSame(entity.tank(),net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK
                    .getCapability(null,BlockPos.ZERO,entity.getBlockState(),entity,face));
        }
    }
    @Test void dailyCostMatrixChargesOnceOnlyAfterConfirmedHydrationAndNeverReplays() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            Fluids.WATER.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
            for(int cost:new int[]{-1,0,1,2,1000,2000})for(int amount:new int[]{0,1,2,1000}) {
                TreeDefinitions.INSTANCE.apply(new TreeDefinitions.Data(Map.of(),new OrchardSettings(true,false,true,cost)),null,null);
                var entity=new SprinklerBlockEntity(BlockPos.ZERO,OrchardContent.OLD.get().defaultBlockState());
                entity.tank().set(0,FluidResource.of(Fluids.WATER),amount);
                var changes=new java.util.concurrent.atomic.AtomicInteger();
                var soil=new SprinklerHydration.Soil(){
                    public boolean available(BlockPos p){return true;}
                    public BlockState get(BlockPos p){return Blocks.FARMLAND.defaultBlockState();}
                    public boolean set(BlockPos p,BlockState s){changes.incrementAndGet();return true;}
                };
                entity.newDay(1,soil);boolean runs=cost<=0||amount>1;
                assertEquals(runs?17:0,changes.get());
                assertEquals(runs&&cost>0?Math.max(0,amount-cost):amount,entity.tank().getAmountAsLong(0));
                entity.newDay(1,soil);assertEquals(runs?17:0,changes.get());
                entity.newDay(2,new SprinklerHydration.Soil(){
                    public boolean available(BlockPos p){return true;}
                    public BlockState get(BlockPos p){return Blocks.FARMLAND.defaultBlockState();}
                    public boolean set(BlockPos p,BlockState s){return false;}
                });
                assertEquals(runs&&cost>0?Math.max(0,amount-cost):amount,entity.tank().getAmountAsLong(0));
            }
        }
    }
    @Test void overlappingVariantsChargeOnlyMachineThatChangesSoilInEitherOrder() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            Fluids.WATER.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
            TreeDefinitions.INSTANCE.apply(new TreeDefinitions.Data(Map.of(),new OrchardSettings(true,false,true,100)),null,null);
            for(boolean ironFirst:List.of(false,true)) {
                var old=new SprinklerBlockEntity(BlockPos.ZERO,OrchardContent.OLD.get().defaultBlockState());
                var iron=new SprinklerBlockEntity(new BlockPos(1,0,0),OrchardContent.IRON.get().defaultBlockState());
                for(var entity:List.of(old,iron))entity.tank().set(0,FluidResource.of(Fluids.WATER),1000);
                var state=new BlockState[]{Blocks.FARMLAND.defaultBlockState()};var target=new BlockPos(0,-1,0);
                var soil=new SprinklerHydration.Soil(){public boolean available(BlockPos p){return p.equals(target);}
                    public BlockState get(BlockPos p){return state[0];}public boolean set(BlockPos p,BlockState s){state[0]=s;return true;}};
                var order=ironFirst?List.of(iron,old):List.of(old,iron);
                for(var entity:order)entity.newDay(1,soil);
                assertEquals(900,order.get(0).tank().getAmountAsLong(0));assertEquals(1000,order.get(1).tank().getAmountAsLong(0));
                assertEquals(7,state[0].getValue(FarmlandBlock.MOISTURE));
            }
            assertEquals(4,OrchardContent.IRON.get().range());assertEquals(.7,OrchardContent.IRON.get().height());
            assertEquals(1,OrchardContent.OLD.get().range());assertEquals(.5,OrchardContent.OLD.get().height());
        }
    }
    @Test void coordinatorGrowsAllChunksBeforeDryingAndIrrigatesOnlyAfterBoth() {
        var order=new ArrayList<String>();
        class Chunk implements CropDailyUpdates.ChunkWork {
            final String name;long cycle;Chunk(String name){this.name=name;}
            public long lastCycle(){return cycle;}public void mark(long value){cycle=value;}
            public void grow(){order.add("grow:"+name);}public void dry(){order.add("dry:"+name);}
        }
        var chunks=List.of(new Chunk("west"),new Chunk("east"));
        CropDailyUpdates.process(1,chunks,()->order.add("water"));
        assertEquals(List.of("grow:west","grow:east","dry:west","dry:east","water"),order);
        order.clear();CropDailyUpdates.process(1,chunks,()->order.add("water"));assertEquals(List.of("water"),order);
        // The POST entity's own persisted marker rejects this second call, independently of crop_cycle.
    }
    @Test void persistedPostMarkerPreventsDuplicateHydrationAfterReload() {
        var original=new SprinklerBlockEntity(BlockPos.ZERO,OrchardContent.IRON.get().defaultBlockState());
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var soil=new SprinklerHydration.Soil(){public boolean available(BlockPos p){calls.incrementAndGet();return false;}
            public BlockState get(BlockPos p){throw new AssertionError();}public boolean set(BlockPos p,BlockState s){throw new AssertionError();}};
        original.newDay(45,soil);assertEquals(161,calls.get());
        var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var restored=(SprinklerBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO,original.getBlockState(),original.saveWithFullMetadata(registry),registry);
        assertNotNull(restored);calls.set(0);restored.newDay(45,soil);assertEquals(0,calls.get());
        restored.newDay(46,soil);assertEquals(161,calls.get());
    }
}

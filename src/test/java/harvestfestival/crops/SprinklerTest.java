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
}

package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SingleHarvestExpansionTest {
    @TempDir Path directory;
    static CropDefinition definition(String name) { return CropDefinitions.INSTANCE.get(Identifier.parse("harvestfestival:"+name)); }
    @Test void twoSpeciesUseOriginalStagesSeasonsSeedsAndVisualBoundaries() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (String name : List.of("onion", "spinach")) {
                var d = definition(name); boolean onion = name.equals("onion");
                assertEquals(onion ? List.of(3,7,8) : List.of(2,5,6), d.stages());
                assertEquals(0, d.regrowStage()); assertEquals(1,d.maxHarvests()); assertTrue(d.requiresWater());
                var state = CropState.planted(d.id());
                var season = onion ? Season.SUMMER : Season.AUTUMN;
                for (int day=1;day<d.maturity();day++) {
                    assertFalse(CropLifecycle.canHarvest(state,d));
                    state=CropLifecycle.newDay(state,d,season,true,false);
                    assertEquals(day+1,state.stage()); assertFalse(state.dead());
                }
                assertTrue(CropLifecycle.canHarvest(state,d)); assertNull(CropLifecycle.afterHarvest(state,d));
                int base = onion ? 19 : 22, previous=0;
                for (int i=0;i<3;i++) {
                    assertEquals(base+i,d.appearance(previous+1)); assertEquals(base+i,d.appearance(d.stages().get(i)));
                    previous=d.stages().get(i);
                }
                assertEquals(d,CropDefinitions.INSTANCE.forSeed(new ItemStack(onion ? CropContent.ONION_SEEDS.get() : CropContent.SPINACH_SEEDS.get())));
            }
        }
    }
    @Test void droughtAndSeasonRulesMatchCommonLifecycle() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (String name : List.of("onion", "spinach")) {
                var d=definition(name); var season=name.equals("onion")?Season.SUMMER:Season.AUTUMN;
                var planted=CropState.planted(d.id()); var dry=planted;
                for(int i=0;i<2;i++){dry=CropLifecycle.newDay(dry,d,season,false,false);assertFalse(dry.dead());assertEquals(1,dry.stage());}
                assertTrue(CropLifecycle.newDay(dry,d,season,false,false).dead());
                var rescued=CropLifecycle.newDay(dry,d,season,true,false);
                assertFalse(rescued.dead());assertEquals(2,rescued.stage());
                var dedicated=planted;
                for(int i=0;i<8;i++) dedicated=CropLifecycle.newDay(dedicated,d,season,false,true);
                assertFalse(dedicated.dead());assertEquals(1,dedicated.stage());
                for(Season wrong:Season.values()) if(wrong!=season) assertTrue(CropLifecycle.newDay(planted,d,wrong,true,false).dead());
            }
        }
    }
    @Test void singleHarvestCommitsBeforeDropAndCannotDuplicateOrHarvestImmatureDeadPlants() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for(String name:List.of("onion","spinach")) {
                var d=definition(name); var item=name.equals("onion")?CropContent.ONION.get():CropContent.SPINACH.get();
                var entity=new CropBlockEntity(BlockPos.ZERO,CropContent.CROP.get().defaultBlockState());
                var mature=new CropState(d.id(),d.maturity(),2,false);entity.setCrop(mature);
                var present=new AtomicBoolean(true);var drops=new ArrayList<ItemStack>();
                assertFalse(CropBlock.harvestOnce(mature,d,List::of,next->fail("empty loot preserves"),drops::add));
                assertFalse(CropBlock.harvestOnce(mature,d,()->List.of(new ItemStack(item)),next->false,drops::add));
                assertTrue(drops.isEmpty());
                assertTrue(CropBlock.harvestOnce(mature,d,()->List.of(new ItemStack(item)),
                        next->entity.commitHarvest(mature,next,()->present.getAndSet(false)),
                        stack->{assertFalse(present.get());assertEquals(CropState.INVALID,entity.crop());drops.add(stack);}));
                assertEquals(1,drops.size());assertTrue(drops.getFirst().is(item));
                assertFalse(CropBlock.harvestOnce(mature,d,()->List.of(new ItemStack(item)),
                        next->entity.commitHarvest(mature,next,()->present.getAndSet(false)),drops::add));
                for(var state:List.of(CropState.planted(d.id()),new CropState(d.id(),d.maturity(),3,true)))
                    assertFalse(CropBlock.harvestOnce(state,d,()->fail("ineligible"),next->fail("ineligible"),drops::add));
            }
        }
    }
    @Test void intermediateMatureAndDeadEntitiesSurviveCompressedSave() throws Exception {
        try(var fixture=new CropTestSupport(directory.resolve("pack"))) {
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for(String name:List.of("onion","spinach")) for(boolean dead:new boolean[]{false,true}) {
                var d=definition(name);var state=new CropState(d.id(),d.maturity()-1,3,dead);
                var entity=new CropBlockEntity(BlockPos.ZERO,CropContent.CROP.get().defaultBlockState()
                        .setValue(CropBlock.APPEARANCE,dead?3:d.appearance(state.stage())));entity.setCrop(state);
                var path=directory.resolve(name+dead+".nbt");NbtIo.writeCompressed(entity.saveWithFullMetadata(registry),path);
                var copy=(CropBlockEntity)BlockEntity.loadStatic(BlockPos.ZERO,entity.getBlockState(),
                        NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap()),registry);
                assertNotNull(copy);assertEquals(state,copy.crop());
            }
        }
    }
}

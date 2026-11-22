package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.calendar.Season;
import harvestfestival.tools.ToolContent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SpecialCropTest {
    @TempDir Path directory;
    static CropDefinition definition(String name) { return CropDefinitions.INSTANCE.get(Identifier.parse("harvestfestival:" + name)); }
    static final class Target implements CropGrowth.Target {
        int fruits, tops; boolean allow = true; CropState top;
        public boolean replaceWithFruit() { fruits++; return allow; }
        public boolean ensureTop(CropState next) { tops++; if (allow) top = next; return allow; }
    }

    @Test void fruitReplacesAtExactWateredTransitionAndNeverOffersHfHarvest() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : List.of("watermelon", "pumpkin")) {
                var d = definition(name); var entity = CropRegrowthTest.entity(CropState.planted(d.id()));
                var target = new Target();
                for (int day = 1; day < d.maturity(); day++) {
                    assertFalse(CropLifecycle.canHarvest(entity.crop(), d));
                    var next = CropLifecycle.newDay(entity.crop(), d, Season.SUMMER, true, false);
                    assertTrue(CropGrowth.apply(entity, d, next, target));
                    assertEquals(day == d.maturity() - 1 ? 1 : 0, target.fruits);
                }
                assertEquals(CropState.INVALID, entity.crop()); assertEquals(0, target.tops);
                assertFalse(CropGrowth.apply(entity, d, new CropState(d.id(), d.maturity(), 1, false, 0), target));
                assertFalse(CropLifecycle.canHarvest(new CropState(d.id(), d.maturity(), 1, false, 0), d));
                assertEquals(1, target.fruits);
                assertEquals(Identifier.parse(name.equals("watermelon") ? "minecraft:melon" : "minecraft:pumpkin"), d.form().matureBlock());
            }
            assertEquals(11, definition("watermelon").maturity()); assertEquals(15, definition("pumpkin").maturity());
            assertFalse(Blocks.PUMPKIN.defaultBlockState().hasProperty(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING));
        }
    }

    @Test void failedTransformationRetriesAndRemovedEntityCannotReplant() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : List.of("watermelon", "pumpkin")) {
                var d = definition(name); var before = new CropState(d.id(), d.maturity()-1, 1, false, 0);
                var entity = CropRegrowthTest.entity(before); var target = new Target(); target.allow = false;
                var next = CropLifecycle.newDay(before, d, Season.SUMMER, true, false);
                assertFalse(CropGrowth.apply(entity, d, next, target)); assertEquals(before, entity.crop());
                target.allow = true; assertTrue(CropGrowth.apply(entity, d, next, target));
                assertEquals(CropState.INVALID, entity.crop());
                entity.setRemoved(); entity.setCrop(before); assertEquals(CropState.INVALID, entity.crop());
                assertFalse(CropGrowth.apply(entity, d, next, target)); assertEquals(2, target.fruits);
            }
        }
    }

    @Test void fruitDroughtAndWrongSeasonCannotConvertAndRestoredMaturityDoesNotReplay() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : List.of("watermelon", "pumpkin")) {
                var d = definition(name);
                for (boolean dedicated : new boolean[]{false,true}) {
                    var before = new CropState(d.id(), d.maturity()-1, 3, false, 0);
                    var dry = CropLifecycle.newDay(before, d, Season.SUMMER, false, dedicated);
                    assertEquals(!dedicated, dry.dead()); assertEquals(before.stage(), dry.stage());
                    var target = new Target(); var entity = CropRegrowthTest.entity(before);
                    CropGrowth.apply(entity, d, dry, target); assertEquals(0, target.fruits);
                    for (var season : List.of(Season.SPRING, Season.AUTUMN, Season.WINTER))
                        assertTrue(CropLifecycle.newDay(before, d, season, true, dedicated).dead());
                }
                var mature = new CropState(d.id(), d.maturity(), 1, false, 0);
                var target = new Target(); CropGrowth.apply(CropRegrowthTest.entity(mature), d,
                        CropLifecycle.newDay(mature,d,Season.SUMMER,true,false),target);
                assertEquals(0,target.fruits);
            }
        }
    }

    @Test void grassNeedsNoWaterBecomesDoubleOnFifthDayAndDiesOnlyInWinter() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var d = definition("grass"); assertEquals(1,d.regrowStage()); assertEquals(1,d.maxHarvests());
            for (var season : List.of(Season.SPRING,Season.SUMMER,Season.AUTUMN)) {
                var entity = CropRegrowthTest.entity(CropState.planted(d.id())); var target = new Target();
                for (int day=1;day<=10;day++) {
                    assertTrue(CropGrowth.apply(entity,d,CropLifecycle.newDay(entity.crop(),d,season,false,false),target));
                    assertEquals(day+1,entity.crop().stage()); assertFalse(entity.crop().dead());
                    assertEquals(day>=5,CropLifecycle.canHarvest(entity.crop(),d));
                    assertEquals(Math.max(0,day-4),target.tops);
                }
                var dead = CropLifecycle.newDay(entity.crop(),d,Season.WINTER,false,false);
                assertTrue(CropGrowth.apply(entity,d,dead,target)); assertTrue(entity.crop().dead()); assertTrue(target.top.dead());
            }
        }
    }

    @Test void obstructionOrFailedTopWriteKeepsStageFiveThenRetries() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var d = definition("grass"); var before = new CropState(d.id(),5,3,false,0);
            var entity = CropRegrowthTest.entity(before); var target = new Target(); target.allow=false;
            var next = CropLifecycle.newDay(before,d,Season.SUMMER,false,false);
            assertFalse(CropGrowth.apply(entity,d,next,target)); assertEquals(before,entity.crop()); assertNull(target.top);
            target.allow=true; assertTrue(CropGrowth.apply(entity,d,next,target));
            assertEquals(6,entity.crop().stage()); assertEquals(6,target.top.stage()); assertEquals(0,target.fruits);
        }
    }

    static List<ItemStack> grassLoot(int stage) throws Exception {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var table = LootTable.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,registry),
                JsonParser.parseString(SummerLateCropTest.resource("/data/harvestfestival/loot_table/crops/grass.json"))).getOrThrow();
        var state = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE,95+stage);
        var params = new LootParams.Builder(null).withParameter(LootContextParams.BLOCK_STATE,state)
                .withParameter(LootContextParams.ORIGIN,Vec3.ZERO).withParameter(LootContextParams.TOOL,new ItemStack(ToolContent.SICKLE.get()))
                .create(LootContextParamSets.BLOCK);
        var constructor = LootContext.class.getDeclaredConstructor(LootParams.class,RandomSource.class,HolderGetter.Provider.class);
        constructor.setAccessible(true);
        var context = constructor.newInstance(params,RandomSource.create(1),registry);
        var output = new ArrayList<ItemStack>(); table.getRandomItemsRaw(context,output::add); return output;
    }

    @Test void realForageLootYieldsOneToSixAndFirstCutConsumesPlantWithoutRegrowth() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var d=definition("grass");
            for(int stage=1;stage<=11;stage++) for(boolean sickle:new boolean[]{false,true}) {
                var crop = new CropState(d.id(),stage,3,false,0); var entity=CropRegrowthTest.entity(crop);
                var loot=grassLoot(stage); assertEquals(stage<6?0:stage-5,loot.stream().mapToInt(ItemStack::getCount).sum());
                var output=new ArrayList<ItemStack>(); var tool=sickle?new ItemStack(ToolContent.SICKLE.get()):ItemStack.EMPTY;
                assertFalse(CropBlock.harvestOnce(crop,d,()->loot,next->fail("no right click"),output::add));
                assertFalse(CropBlock.breakOnce(crop,d,tool,()->loot,()->false,output::add)); assertTrue(output.isEmpty());
                assertTrue(CropBlock.breakOnce(crop,d,tool,()->loot,()->entity.commitHarvest(crop,null,()->true),output::add));
                assertEquals(sickle&&stage>=6?stage-5:0,output.stream().mapToInt(ItemStack::getCount).sum());
                assertEquals(CropState.INVALID,entity.crop());
                assertFalse(CropBlock.breakOnce(crop,d,tool,()->loot,()->entity.commitHarvest(crop,null,()->fail("duplicate")),output::add));
                if(stage>=6) assertNull(CropLifecycle.afterHarvest(crop,d));
            }
        }
    }

    @Test void newVisualStatesAndOldSaveStatesRoundTripWithoutChangingCropCodec() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            for(var name:List.of("watermelon","pumpkin","grass")) {
                var d=definition(name);
                for(int stage=1;stage<=d.maturity();stage++) for(boolean dead:new boolean[]{false,true}) {
                    var crop=new CropState(d.id(),stage,3,dead,0);
                    assertEquals(crop,CropState.CODEC.parse(NbtOps.INSTANCE,CropState.CODEC.encodeStart(NbtOps.INSTANCE,crop).getOrThrow()).getOrThrow());
                    var block=CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE,d.appearance(stage)).setValue(CropBlock.WITHERED,dead);
                    assertEquals(block,BlockState.CODEC.parse(NbtOps.INSTANCE,BlockState.CODEC.encodeStart(NbtOps.INSTANCE,block).getOrThrow()).getOrThrow());
                    var entity=new CropBlockEntity(BlockPos.ZERO,block); entity.setCrop(crop);
                    var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
                    var saved=directory.resolve(name+stage+dead+".nbt");
                    net.minecraft.nbt.NbtIo.writeCompressed(entity.saveWithFullMetadata(registries),saved);
                    var restored=(CropBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO,block,
                            net.minecraft.nbt.NbtIo.readCompressed(saved,net.minecraft.nbt.NbtAccounter.unlimitedHeap()),registries);
                    assertNotNull(restored);assertEquals(crop,restored.crop());
                }
            }
            for(int slot=0;slot<=71;slot++) {
                var block=CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE,slot);
                var encoded=BlockState.CODEC.encodeStart(NbtOps.INSTANCE,block).getOrThrow();
                ((net.minecraft.nbt.CompoundTag)encoded).getCompound("Properties").orElseThrow().remove("withered");
                assertEquals(block,BlockState.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow());
            }
            assertFalse(CropContent.CROP_TOP.get().defaultBlockState().hasBlockEntity());
        }
    }

    @Test void pairPermissionsRejectEitherHalfAndTopAnchoredAreaStaysOnItsOwnPlane() {
        var base=new BlockPos(4,64,8); var top=base.above();
        for(var denied:List.of(base,top)) {
            assertFalse(CropPair.permitted(base,top,pos->!pos.equals(denied)));
            assertFalse(CropPair.permitted(top,base,pos->!pos.equals(denied)));
        }
        assertTrue(CropPair.permitted(top,base,pos->pos.equals(base)||pos.equals(top)));
        for(var tier:harvestfestival.tools.ToolTier.values()) {
            if(tier.ordinal()>harvestfestival.tools.ToolTier.MYSTRIL.ordinal()) continue;
            for(var pos:harvestfestival.tools.SickleArea.positions(top,net.minecraft.core.Direction.NORTH,tier))
                assertEquals(top.getY(),pos.getY());
        }
    }

    @Test void resourcesCoverEveryNewVisualAndKeepProductIdentitiesAndSeedTints() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var variants=JsonParser.parseString(SummerLateCropTest.resource("/assets/harvestfestival/blockstates/crop.json"))
                    .getAsJsonObject().getAsJsonObject("variants");
            for(int slot=0;slot<=106;slot++) assertTrue(variants.has("appearance="+slot));
            var tops=JsonParser.parseString(SummerLateCropTest.resource("/assets/harvestfestival/blockstates/crop_top.json"))
                    .getAsJsonObject().getAsJsonObject("variants");
            for(int stage=6;stage<=11;stage++) assertTrue(tops.has("stage="+stage));
            int[] colors={0xC92B3E,0xE09A39,0x7AC958};int index=0;
            for(var name:List.of("watermelon","pumpkin","grass")) {
                var model=JsonParser.parseString(SummerLateCropTest.resource("/assets/harvestfestival/items/"+name+"_seeds.json"))
                        .getAsJsonObject().getAsJsonObject("model");
                for(var tint:model.getAsJsonArray("tints")) assertEquals(colors[index],tint.getAsJsonObject().get("value").getAsInt());
                for(var language:List.of("en_us","pt_br")) assertTrue(JsonParser.parseString(
                        SummerLateCropTest.resource("/assets/harvestfestival/lang/"+language+".json")).getAsJsonObject().has("item.harvestfestival."+name+"_seeds"));
                index++;
            }
            assertEquals(8,definition("watermelon").appearances().size());assertEquals(8,definition("pumpkin").appearances().size());
            for(var name:List.of("watermelon","pumpkin")) assertFalse(BuiltInRegistries.ITEM.containsKey(Identifier.parse("harvestfestival:"+name)));
            for(var part:List.of("bottom","top")) for(int i=1;i<=(part.equals("bottom")?11:6);i++) {
                var name="grass_"+part+"_"+i;
                assertTrue(SummerLateCropTest.resource("/assets/harvestfestival/models/block/"+name+".json").contains("tinted_cross"));
                try(var image=CropContent.class.getResourceAsStream("/assets/harvestfestival/textures/block/crops/"+name+".png")) {
                    assertNotNull(image);assertTrue(image.readAllBytes().length>8);
                }
            }
        }
    }

    @Test void adjacentFruitChangesOnlyMatureStemAppearanceWithOriginalFirstOrLastSelection() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var directions=List.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.EAST,
                    net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST);
            for(var name:List.of("watermelon","pumpkin")) {
                var d=definition(name); var last=new CropState(d.id(),d.maturity()-1,1,false,0);
                for(int mask=0;mask<16;mask++) {
                    int bits=mask; int expected=d.appearances().getLast();
                    for(int i=0;i<4;i++) if((bits&(1<<i))!=0) {
                        expected=d.form().attachedAppearance()+i;
                        if(name.equals("pumpkin")) break;
                    }
                    assertEquals(expected,CropBlockEntity.appearance(last,d,0,dir->(bits&(1<<directions.indexOf(dir)))!=0));
                }
                var young=CropState.planted(d.id());
                assertEquals(d.appearance(1),CropBlockEntity.appearance(young,d,0,dir->fail("young stem must not attach")));
                var dead=new CropState(d.id(),last.stage(),3,true,0);
                assertEquals(d.appearances().getLast(),CropBlockEntity.appearance(dead,d,0,dir->false));
                assertFalse(CropLifecycle.canHarvest(last,d));
            }
        }
    }
}

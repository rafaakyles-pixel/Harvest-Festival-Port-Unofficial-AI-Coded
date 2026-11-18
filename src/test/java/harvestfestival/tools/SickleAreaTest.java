package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.crops.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SickleAreaTest {
    @TempDir Path directory;
    static final SickleRules.Rule RULE = new SickleRules.Rule(true,27);
    static ItemStack chargedCopper() {
        var stack = new ItemStack(ToolContent.SICKLE_COPPER.get());
        stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(0,1,false)); return stack;
    }
    static final class Field implements SickleArea.Target {
        final Map<BlockPos,CropBlockEntity> plants = new HashMap<>();
        final Set<BlockPos> denied = new HashSet<>(), unharvestable = new HashSet<>(), failed = new HashSet<>();
        final List<String> calls = new ArrayList<>();
        final List<ItemStack> output = new ArrayList<>();
        final ItemStack tool;
        Field(ItemStack tool) { this.tool=tool; }
        void plant(BlockPos pos,String species,int stage,boolean dead) {
            var e = new CropBlockEntity(pos,CropContent.CROP.get().defaultBlockState());
            e.setCrop(new CropState(Identifier.parse("harvestfestival:"+species),stage,1,dead,0)); plants.put(pos,e);
        }
        @Override public boolean isCrop(BlockPos pos) { return plants.containsKey(pos); }
        @Override public SickleArea.Candidate prepare(BlockPos pos,boolean center) {
            calls.add("authorize:"+pos.getX()+","+pos.getZ());
            if(denied.contains(pos)) return null;
            var entity=plants.get(pos); var expected=entity.crop(); var d=CropDefinitions.INSTANCE.get(expected.crop());
            return new SickleArea.Candidate() {
                @Override public boolean canHarvest() { calls.add("canHarvest"); return !unharvestable.contains(pos); }
                @Override public boolean remove() {
                    calls.add("remove");
                    return CropBlock.breakOnce(expected,d,tool,()-> List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(
                            Identifier.parse(expected.crop().getPath().equals("wheat")?"minecraft:wheat":expected.crop().toString())))),
                            ()->entity.commitHarvest(expected,null,()->!failed.contains(pos)&&plants.remove(pos,entity)),stack->{
                                assertFalse(plants.containsKey(pos)); assertEquals(CropState.INVALID,entity.crop()); output.add(stack);
                            });
                }
                @Override public void destroyed() { calls.add("destroyed"); }
                @Override public void harvest() {
                    calls.add("harvest");
                    assertTrue(entity.getBlockState().getDrops(new LootParams.Builder(null)
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY,entity)).isEmpty());
                }
            };
        }
        boolean task(SickleRules.Rule rule) {
            calls.add("task");
            return SickleItem.performTask(tool,false,rule,x->x,x->calls.add("effort"));
        }
    }

    @Test void copperGeometryUsesOnePlaneAllOrientationsAndOriginalIterationOrder() {
        var anchor=new BlockPos(8,64,12);
        for(var direction:List.of(Direction.NORTH,Direction.SOUTH))
            assertEquals(List.of(new BlockPos(7,64,12),anchor,new BlockPos(9,64,12)),SickleArea.positions(anchor,direction,ToolTier.COPPER));
        for(var direction:List.of(Direction.EAST,Direction.WEST))
            assertEquals(List.of(new BlockPos(8,64,11),anchor,new BlockPos(8,64,13)),SickleArea.positions(anchor,direction,ToolTier.COPPER));
        for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))
            assertEquals(List.of(anchor),SickleArea.positions(anchor,direction,ToolTier.BASIC));
        assertThrows(IllegalArgumentException.class,()->SickleArea.positions(anchor,Direction.UP,ToolTier.COPPER));
    }

    @Test void copperChargeUsesCompleteTwentyTickIntervalsAndInvalidStateIsInert() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            for(int elapsed:new int[]{0,19,20,39,40,32000}) {
                var next=SickleState.NEW.charge(ToolTier.COPPER,elapsed);
                assertEquals(elapsed<20?0:1,next.charge()); assertEquals(0,next.level());
            }
            var stack=chargedCopper();var state=SickleState.get(stack);
            assertTrue(SickleItem.performTask(stack,false,RULE,x->0,x->{}));
            assertEquals(1,SickleState.get(stack).charge());
            assertEquals(0,SickleState.get(stack).discharge().charge());assertEquals(1,state.charge());
            for(String json:List.of("{\"level\":42,\"charge\":null}","{\"level\":42,\"charge\":1.5}",
                    "{\"level\":42,\"charge\":\"1\"}","{\"level\":42,\"charge\":true}","{\"level\":42,\"charge\":5}"))
                assertEquals(SickleState.INVALID,SickleState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow());
            assertEquals(new SickleState(42,0,false),SickleState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"level\":42}")).getOrThrow());
            stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(42,2,false)); assertFalse(SickleItem.canUse(stack));
        }
    }

    @Test void taskPrecedesNeighborRemovalAndCenterBelongsOnlyToOuterBreak() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=chargedCopper();var f=new Field(stack);
            f.plant(new BlockPos(-1,0,0),"strawberry",9,false);f.plant(BlockPos.ZERO,"wheat",28,false);f.plant(new BlockPos(1,0,0),"tomato",10,false);
            assertEquals(3,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->f.task(RULE)));
            assertEquals(List.of("authorize:-1,0","task","effort","canHarvest","remove","destroyed","harvest",
                    "authorize:0,0","task","effort","authorize:1,0","task","effort","canHarvest","remove","destroyed","harvest"),f.calls);
            assertEquals(Set.of(BlockPos.ZERO),f.plants.keySet());assertEquals(2,f.output.size());
            assertEquals(3,stack.getDamageValue());assertEquals(3*ToolTier.COPPER.levelIncrease(),SickleState.get(stack).level(),1e-12);
            var center=f.prepare(BlockPos.ZERO,true);assertTrue(center.canHarvest());assertTrue(center.remove());center.destroyed();center.harvest();
            assertEquals(3,f.output.size());assertTrue(f.plants.isEmpty());assertEquals(3,stack.getDamageValue());
        }
    }

    @Test void deniedTargetsDoNotChargeButHarvestAndRemovalFailuresDo() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            for(int mode=0;mode<3;mode++) {
                var stack=chargedCopper();var f=new Field(stack);var left=new BlockPos(-1,0,0);
                for(var p:SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolTier.COPPER)) f.plant(p,"eggplant",10,false);
                if(mode==0) f.denied.add(left); else if(mode==1) f.unharvestable.add(left); else f.failed.add(left);
                assertEquals(mode==0?2:3,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->f.task(RULE)));
                assertTrue(f.plants.containsKey(left));assertEquals(1,f.output.size());
                assertEquals(mode==0?2:3,stack.getDamageValue());
                assertEquals(10,f.plants.get(left).crop().stage());
            }
        }
    }

    @Test void finalDurabilityPointCutsThatNeighborAndStopsTasksWithoutBlockingExternalCenter() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=chargedCopper();stack.setDamageValue(255);var f=new Field(stack);
            for(var p:SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolTier.COPPER)) f.plant(p,"wheat",28,false);
            assertEquals(1,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->f.task(RULE)));
            assertEquals(256,stack.getDamageValue());assertEquals(1,stack.getCount());assertTrue(stack.is(SickleItem.SICKLES));
            assertEquals(0.05F,stack.getItem().getDestroySpeed(stack,Blocks.STONE.defaultBlockState()));
            assertEquals(1,f.output.size());assertTrue(f.plants.containsKey(BlockPos.ZERO));
            var center=f.prepare(BlockPos.ZERO,true);assertTrue(center.remove());center.destroyed();center.harvest();
            assertEquals(2,f.output.size());assertTrue(f.plants.containsKey(new BlockPos(1,0,0)));
        }
    }

    @Test void creativeNonCropAnchorAndMissingRulesNeverStartExtraCuts() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=chargedCopper();var f=new Field(stack);f.plant(new BlockPos(-1,0,0),"cucumber",10,false);
            assertEquals(0,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->fail("no anchor")));
            f.plant(BlockPos.ZERO,"cucumber",10,false);
            assertEquals(0,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,true,f,()->fail("creative")));
            assertEquals(0,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->f.task(null)));
            assertEquals(2,f.plants.size());assertEquals(0,stack.getDamageValue());assertEquals(0,SickleState.get(stack).level());
        }
    }

    @Test void immatureAndDeadPlantsAreCutWithoutProductsAndDisabledHungerStillLevelsPerCandidate() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=chargedCopper();var f=new Field(stack);
            f.plant(new BlockPos(-1,0,0),"cucumber",7,false);f.plant(BlockPos.ZERO,"cucumber",10,false);f.plant(new BlockPos(1,0,0),"eggplant",10,true);
            assertEquals(3,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->f.task(new SickleRules.Rule(false,27))));
            assertTrue(f.output.isEmpty());assertEquals(Set.of(BlockPos.ZERO),f.plants.keySet());
            assertEquals(0,stack.getDamageValue());assertFalse(f.calls.contains("effort"));
            assertEquals(3*ToolTier.COPPER.levelIncrease(),SickleState.get(stack).level(),1e-12);
        }
    }

    @Test void chargedDamagedCopperRoundTripsCompressedInventoryAndCopiesIndependently() throws Exception {
        try(var fixture=new CropTestSupport(directory.resolve("pack"))) {
            var ops=RegistryOps.create(NbtOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for(int damage:new int[]{0,255,256}) {
                var stack=chargedCopper();stack.setDamageValue(damage);stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(42.5,1,false));
                var copy=stack.copy();stack.set(ToolContent.SICKLE_STATE.get(),SickleState.get(stack).discharge());
                var file=directory.resolve("charged-"+damage+".nbt");NbtIo.writeCompressed((CompoundTag)ItemStack.CODEC.encodeStart(ops,copy).getOrThrow(),file);
                var restored=ItemStack.CODEC.parse(ops,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())).getOrThrow();
                assertEquals(new SickleState(42.5,1,false),SickleState.get(restored));assertEquals(damage,restored.getDamageValue());
                assertEquals(0,SickleState.get(stack).charge());assertEquals(damage<256,SickleItem.canUse(restored));
            }
        }
    }

    @Test void silverAddsOneForwardRowInFourDirectionsWithXThenZOrder() {
        var south=List.of(new BlockPos(-1,0,0),new BlockPos(-1,0,1),BlockPos.ZERO,new BlockPos(0,0,1),new BlockPos(1,0,0),new BlockPos(1,0,1));
        assertEquals(south,SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolTier.SILVER));
        assertEquals(List.of(new BlockPos(-1,0,-1),new BlockPos(-1,0,0),new BlockPos(0,0,-1),BlockPos.ZERO,new BlockPos(1,0,-1),new BlockPos(1,0,0)),
                SickleArea.positions(BlockPos.ZERO,Direction.NORTH,ToolTier.SILVER));
        assertEquals(List.of(new BlockPos(0,0,-1),BlockPos.ZERO,new BlockPos(0,0,1),new BlockPos(1,0,-1),new BlockPos(1,0,0),new BlockPos(1,0,1)),
                SickleArea.positions(BlockPos.ZERO,Direction.EAST,ToolTier.SILVER));
        assertEquals(List.of(new BlockPos(-1,0,-1),new BlockPos(-1,0,0),new BlockPos(-1,0,1),new BlockPos(0,0,-1),BlockPos.ZERO,new BlockPos(0,0,1)),
                SickleArea.positions(BlockPos.ZERO,Direction.WEST,ToolTier.SILVER));
        for(int yaw:new int[]{0,90,180,270}) {
            var direction=Direction.fromYRot(yaw);
            var positions=SickleArea.positions(BlockPos.ZERO,direction,ToolTier.SILVER);
            assertEquals(6,positions.size());assertEquals(6,new HashSet<>(positions).size());
            assertTrue(positions.contains(BlockPos.ZERO.relative(direction)));assertFalse(positions.contains(BlockPos.ZERO.relative(direction.getOpposite())));
        }
    }

    @Test void silverPartialChargesUsePhysicalTierCostsAndOnlyFullChargeCutsSixPositions() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            for(int charge=0;charge<=2;charge++) {
                var stack=new ItemStack(ToolContent.SICKLE_SILVER.get());
                stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(0,charge,false));var field=new Field(stack);
                for(var pos:SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolTier.SILVER)) field.plant(pos,"tomato",10,false);
                field.plant(new BlockPos(0,-1,0),"tomato",10,false);field.plant(new BlockPos(0,1,0),"tomato",10,false);
                var costs=new ArrayList<Float>();int count=charge==0?1:charge==1?3:6;
                assertEquals(count,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,field,
                        ()->SickleItem.performTask(stack,false,RULE,x->x,costs::add)));
                assertEquals(count,stack.getDamageValue());assertEquals(count-1,field.output.size());
                assertEquals(count*ToolTier.SILVER.levelIncrease(),SickleState.get(stack).level(),1e-12);
                assertEquals(charge,SickleState.get(stack).charge());
                costs.forEach(cost->assertEquals(4F/27F*2F,cost,1e-6));
                assertTrue(field.plants.containsKey(new BlockPos(0,-1,0)));assertTrue(field.plants.containsKey(new BlockPos(0,1,0)));
            }
        }
    }

    @Test void silverAccumulatedChargeNetworkCodecAndLastDurabilityRemainIndependent() throws Exception {
        try(var fixture=new CropTestSupport(directory.resolve("pack"))) {
            for(int elapsed:new int[]{0,19,20,39,40,32000}) assertEquals(Math.min(2,elapsed/20),SickleState.NEW.charge(ToolTier.SILVER,elapsed).charge());
            var once=SickleState.NEW.charge(ToolTier.SILVER,20);
            assertEquals(1,once.charge(ToolTier.SILVER,19).charge());assertEquals(2,once.charge(ToolTier.SILVER,20).charge());
            var stack=new ItemStack(ToolContent.SICKLE_SILVER.get());stack.setDamageValue(767);
            stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(99.99,2,false));
            var costs=new ArrayList<Float>();
            assertTrue(SickleItem.performTask(stack,false,RULE,x->0,costs::add));assertEquals(767,stack.getDamageValue());
            assertEquals(100,SickleState.get(stack).level());
            assertTrue(SickleItem.performTask(stack,false,RULE,x->x,costs::add));assertEquals(768,stack.getDamageValue());
            assertFalse(SickleItem.performTask(stack,false,RULE,x->fail("broken"),x->fail("broken")));
            var ops=RegistryOps.create(NbtOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var file=directory.resolve("silver.nbt");NbtIo.writeCompressed((CompoundTag)ItemStack.CODEC.encodeStart(ops,stack).getOrThrow(),file);
            var restored=ItemStack.CODEC.parse(ops,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())).getOrThrow();
            assertTrue(restored.is(SickleItem.SICKLES));assertEquals(768,restored.getDamageValue());assertEquals(new SickleState(100,2,false),SickleState.get(restored));
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var codec=net.minecraft.network.codec.ByteBufCodecs.fromCodec(SickleState.CODEC);
                codec.encode(buffer,SickleState.get(restored));assertEquals(SickleState.get(restored),codec.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test void faintingDuringSilverActionDoesNotCancelRemainingUsableCandidates() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=new ItemStack(ToolContent.SICKLE_SILVER.get());stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(0,2,false));
            var field=new Field(stack);
            for(var pos:SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolTier.SILVER)) field.plant(pos,"strawberry",9,false);
            var effort=new ToolEffortTest.Target();effort.food=0;effort.duration=1990;
            var rule=ToolEffortSettings.parse(JsonParser.parseString(ToolEffortSettingsTest.bundled()).getAsJsonObject());
            assertEquals(6,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,field,
                    ()->SickleItem.performTask(stack,false,RULE,x->x,amount->ToolEffort.consume(effort,amount,rule))));
            assertEquals(1,effort.calls.stream().filter("teleport"::equals).count());
            assertEquals(20,effort.food);assertEquals(6,stack.getDamageValue());assertEquals(5,field.output.size());
            assertEquals(6*4F/27F*2F,effort.cost,1e-5);
        }
    }
}

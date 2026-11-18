package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedSickleTest {
    @TempDir Path directory;
    @Test void goldGeometryAndPartialChargeCosts() throws Exception { geometry(ToolContent.SICKLE_GOLD.get(),3); }
    @Test void goldPermissionsFailuresLastPointAndCreative() throws Exception { failures(ToolContent.SICKLE_GOLD.get()); }
    @Test void goldChargeAndOldStatePersistence() throws Exception { persistence(ToolContent.SICKLE_GOLD.get()); }
    @Test void goldFaintingDoesNotCancelArea() throws Exception { fainting(ToolContent.SICKLE_GOLD.get(),9); }

    @Test void mystrilGeometryAndPartialChargeCosts() throws Exception { geometry(ToolContent.SICKLE_MYSTRIL.get(),5); }
    @Test void mystrilPermissionsFailuresLastPointAndCreative() throws Exception { failures(ToolContent.SICKLE_MYSTRIL.get()); }
    @Test void mystrilChargeAndOldStatePersistence() throws Exception { persistence(ToolContent.SICKLE_MYSTRIL.get()); }
    @Test void mystrilFaintingDoesNotCancelArea() throws Exception { fainting(ToolContent.SICKLE_MYSTRIL.get(),25); }

    private ItemStack charged(SickleItem item,int charge) {
        var stack=new ItemStack(item); stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(0,charge,false));return stack;
    }
    private SickleAreaTest.Field field(ItemStack stack) {
        var f=new SickleAreaTest.Field(stack);
        for(var pos:SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,((SickleItem)stack.getItem()).tier())) f.plant(pos,"cabbage",15,false);
        return f;
    }
    private void geometry(SickleItem item,int width) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var tier=item.tier();
            for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
                var expected=new ArrayList<BlockPos>();
                for(int front=0;front<width;front++) for(int side=-width/2;side<=width/2;side++)
                    expected.add(BlockPos.ZERO.relative(direction,front).relative(direction.getClockWise(),side));
                expected.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
                assertEquals(expected,SickleArea.positions(BlockPos.ZERO,direction,tier));
            }
            for(int charge=0;charge<=tier.chargeLimit();charge++) {
                var stack=charged(item,charge);var f=field(stack);var costs=new ArrayList<Float>();
                f.plant(BlockPos.ZERO.above(),"cabbage",15,false); f.plant(BlockPos.ZERO.below(),"cabbage",15,false);
                int count=new int[]{1,3,6,9,25}[charge];
                assertEquals(count,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,
                    ()->SickleItem.performTask(stack,false,SickleAreaTest.RULE,x->x,costs::add)));
                assertEquals(count,stack.getDamageValue());assertEquals(count-1,f.output.size());
                assertEquals(count*tier.levelIncrease(),SickleState.get(stack).level(),1e-12);assertEquals(charge,SickleState.get(stack).charge());
                costs.forEach(cost->assertEquals(4F/27*tier.effort(),cost,1e-6));
                assertTrue(f.plants.containsKey(BlockPos.ZERO)); assertTrue(f.plants.containsKey(BlockPos.ZERO.above()));assertTrue(f.plants.containsKey(BlockPos.ZERO.below()));
            }
        }
    }
    private void failures(SickleItem item) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var tier=item.tier();var stack=charged(item,tier.chargeLimit());var f=field(stack);
            var positions=SickleArea.positions(BlockPos.ZERO,Direction.SOUTH,tier);
            f.denied.add(positions.get(0));f.unharvestable.add(positions.get(1));f.failed.add(positions.get(2));
            var firstField=f;
            assertEquals(positions.size()-1,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->firstField.task(SickleAreaTest.RULE)));
            assertEquals(positions.size()-1,stack.getDamageValue());assertEquals(positions.size()-4,f.output.size());
            assertEquals("authorize:"+positions.getFirst().getX()+","+positions.getFirst().getZ(),f.calls.getFirst());
            assertEquals("task",f.calls.get(2)); // Denied neighbor has no task; next task precedes harvestability.
            stack=charged(item,tier.chargeLimit());stack.setDamageValue(tier.durability()-1);var last=stack;f=field(stack);var lastField=f;
            assertEquals(1,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,last,false,lastField,()->lastField.task(SickleAreaTest.RULE)));
            assertEquals(tier.durability(),last.getDamageValue());assertEquals(1,last.getCount());assertEquals(1,lastField.output.size());
            assertFalse(SickleItem.canUse(last));assertTrue(last.is(SickleItem.SICKLES));
            var creative=charged(item,tier.chargeLimit());var cf=field(creative);
            assertEquals(0,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,creative,true,cf,()->fail("creative callback")));
            assertTrue(cf.output.isEmpty());assertEquals(0,creative.getDamageValue());assertEquals(0,SickleState.get(creative).level());
            var free=charged(item,0);assertTrue(SickleItem.performTask(free,false,new SickleRules.Rule(false,27),x->fail("damage"),x->fail("cost")));
            assertEquals(tier.levelIncrease(),SickleState.get(free).level());assertEquals(0,free.getDamageValue());
        }
    }
    private void persistence(SickleItem item) throws Exception {
        try(var fixture=new CropTestSupport(directory.resolve("pack"))) {
            var tier=item.tier();
            for(int elapsed=0;elapsed<=100;elapsed++) assertEquals(Math.min(tier.chargeLimit(),elapsed/20),SickleState.NEW.charge(tier,elapsed).charge());
            assertEquals(new SickleState(42,0,false),SickleState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"level\":42}")).getOrThrow());
            for(int charge=0;charge<=4;charge++) {
                var state=new SickleState(42,charge,false);var stack=new ItemStack(item);stack.set(ToolContent.SICKLE_STATE.get(),state);
                assertEquals(charge<=tier.chargeLimit(),SickleItem.canUse(stack));assertEquals(charge<=2,state.validFor(ToolTier.SILVER));
                assertEquals(0,state.discharge().charge());assertEquals(42,state.discharge().level());
            }
            assertEquals(SickleState.INVALID,SickleState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"level\":42,\"charge\":5}")).getOrThrow());
            var stack=charged(item,tier.chargeLimit());stack.setDamageValue(tier.durability()-1);
            var costs=new ArrayList<Float>();assertTrue(SickleItem.performTask(stack,false,SickleAreaTest.RULE,x->0,costs::add));
            assertEquals(tier.durability()-1,stack.getDamageValue());assertEquals(1,costs.size());
            var ops=RegistryOps.create(NbtOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var file=directory.resolve("advanced.nbt");NbtIo.writeCompressed((CompoundTag)ItemStack.CODEC.encodeStart(ops,stack).getOrThrow(),file);
            var copy=ItemStack.CODEC.parse(ops,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())).getOrThrow();
            assertEquals(SickleState.get(stack),SickleState.get(copy));assertEquals(stack.getDamageValue(),copy.getDamageValue());
            var state=SickleState.get(copy);stack.set(ToolContent.SICKLE_STATE.get(),state.discharge());assertEquals(state,SickleState.get(copy));
            var buffer=io.netty.buffer.Unpooled.buffer();try {var codec=net.minecraft.network.codec.ByteBufCodecs.fromCodec(SickleState.CODEC);codec.encode(buffer,state);assertEquals(state,codec.decode(buffer));}finally{buffer.release();}
        }
    }
    private void fainting(SickleItem item,int count) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=charged(item,item.tier().chargeLimit());var f=field(stack);var effort=new ToolEffortTest.Target();effort.food=0;effort.duration=1990;
            var rule=ToolEffortSettings.parse(JsonParser.parseString(ToolEffortSettingsTest.bundled()).getAsJsonObject());
            assertEquals(count,SickleArea.perform(BlockPos.ZERO,Direction.SOUTH,stack,false,f,()->SickleItem.performTask(stack,false,SickleAreaTest.RULE,x->x,amount->ToolEffort.consume(effort,amount,rule))));
            assertEquals(1,effort.calls.stream().filter("teleport"::equals).count());assertEquals(20,effort.food);
            assertEquals(count,stack.getDamageValue());assertEquals(count-1,f.output.size());assertEquals(count*4F/27*item.tier().effort(),effort.cost,1e-5);
        }
    }
}

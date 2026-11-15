package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FarmToolTest {
    @TempDir Path directory;
    static ToolEffortSettings.Rule rule(boolean consume) {
        return new ToolEffortSettings.Rule(consume, 27, true, true, true, false, false, true, false, false, true, false);
    }
    @Test void bundledTiersHaveIndependentAreasCostsAndDurability() {
        assertEquals(List.of(128,256,768), java.util.Arrays.stream(ToolTier.values()).map(ToolTier::durability).toList());
        assertEquals(List.of(0,1,2), java.util.Arrays.stream(ToolTier.values()).map(ToolTier::hoeFront).toList());
        assertEquals(List.of(0,0,2), java.util.Arrays.stream(ToolTier.values()).map(ToolTier::waterFront).toList());
        assertEquals(List.of(0,1,1), java.util.Arrays.stream(ToolTier.values()).map(ToolTier::waterSides).toList());
        assertEquals(List.of(3F,2.5F,2F), java.util.Arrays.stream(ToolTier.values()).map(ToolTier::effort).toList());
    }
    @Test void chargeAccumulatesOnlyCompleteSecondsAndPersistsThroughTasks() {
        var initial = new FarmToolState(42,0,false);
        for (int ticks : new int[]{0,1,19}) assertEquals(0,initial.charge(ToolTier.SILVER,ticks).charge());
        for (int ticks : new int[]{20,21,39}) assertEquals(1,initial.charge(ToolTier.SILVER,ticks).charge());
        for (int ticks : new int[]{40,60,32000}) assertEquals(2,initial.charge(ToolTier.SILVER,ticks).charge());
        var partial = initial.charge(ToolTier.SILVER,20);
        assertEquals(2,partial.charge(ToolTier.SILVER,20).charge());
        assertEquals(1,initial.charge(ToolTier.COPPER,32000).charge());
        assertEquals(0,initial.charge(ToolTier.BASIC,32000).charge());
        assertEquals(2,initial.charge(ToolTier.SILVER,40).advance(ToolTier.SILVER).charge());
        assertEquals(initial,partial.discharge());
        assertEquals(42,partial.charge(ToolTier.SILVER,20).level());
    }
    @Test void useDecisionResetsOnlyUpgradedHoesAndAppliesImmediatelyAtMaximum() {
        assertEquals(HoeItem.UseAction.APPLY,HoeItem.startAction(ToolTier.BASIC,FarmToolState.NEW,true));
        for (var tier : List.of(ToolTier.COPPER,ToolTier.SILVER)) {
            assertEquals(HoeItem.UseAction.CHARGE,HoeItem.startAction(tier,FarmToolState.NEW,false));
            assertEquals(HoeItem.UseAction.DISCHARGE,HoeItem.startAction(tier,FarmToolState.NEW,true));
            var full = FarmToolState.NEW.charge(tier,40);
            assertEquals(HoeItem.UseAction.APPLY,HoeItem.startAction(tier,full,false));
            assertEquals(HoeItem.UseAction.DISCHARGE,HoeItem.startAction(tier,full,true));
        }
    }
    @Test void allHoesRetainBrokenItemAndUsePhysicalTierForCostAndLevel() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            int index = 0;
            double[] increases = {0.39215682745098,0.196078431372549,0.130718954248366};
            for (var item : List.of(ToolContent.HOE.get(),ToolContent.HOE_COPPER.get(),ToolContent.HOE_SILVER.get())) {
                var stack = new ItemStack(item);
                assertTrue(stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.HOE_TILL));
                assertFalse(stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHOVEL_FLATTEN));
                var costs = new ArrayList<Float>();
                for (int i = 0; i < item.tier().durability(); i++) {
                    assertTrue(FarmToolTasks.canUse(stack,item.tier(),ToolAction.HOE));
                    FarmToolTasks.complete(stack,item.tier(),ToolAction.HOE,false,rule(true),x -> x,costs::add);
                }
                assertFalse(FarmToolTasks.canUse(stack,item.tier(),ToolAction.HOE));
                assertEquals(item.tier().durability(),stack.getDamageValue());
                assertEquals(item.tier().durability(),costs.size());
                assertEquals(4F/27*item.tier().effort(),costs.getFirst(),0.000001);
                assertEquals(Math.min(100,item.tier().durability()*increases[index++]),FarmToolState.get(stack).level(),0.00000001);
                assertEquals(1,stack.getCount());
                assertEquals(0,FarmToolState.get(stack).charge()); // Silver with no charge still pays silver's rate.
            }
        }
    }
    @Test void creativeDisabledHungerAndEnchantmentPreserveIndependentLevel() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (boolean creative : new boolean[]{false,true}) {
                var stack = new ItemStack(ToolContent.HOE_COPPER.get());
                for (int i=0;i<600;i++) FarmToolTasks.complete(stack,ToolTier.COPPER,ToolAction.HOE,creative,rule(creative),
                        x -> fail("no damage"),x -> fail("no hunger"));
                assertEquals(100,FarmToolState.get(stack).level());
                assertEquals(0,stack.getDamageValue());
            }
            var stack = new ItemStack(ToolContent.HOE_SILVER.get());
            var costs = new ArrayList<Float>();
            FarmToolTasks.complete(stack,ToolTier.SILVER,ToolAction.HOE,false,rule(true),x -> 0,costs::add);
            assertEquals(0,stack.getDamageValue()); assertEquals(1,costs.size());
            assertEquals(ToolTier.SILVER.levelIncrease(),FarmToolState.get(stack).level());
            FarmToolTasks.complete(stack,ToolTier.SILVER,ToolAction.HOE,false,null,x -> fail("no damage"),x -> fail("no cost"));
            assertEquals(2*ToolTier.SILVER.levelIncrease(),FarmToolState.get(stack).level());
        }
    }
    @Test void corruptOrIncompatibleChargeDoesNotResetOrEnableTool() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (String json : List.of("{}","null","[]","{\"level\":1,\"charge\":true}","{\"level\":1,\"charge\":1.5}",
                    "{\"level\":1,\"charge\":\"1\"}","{\"level\":1,\"charge\":3}","{\"level\":101,\"charge\":0}",
                    "{\"level\":true,\"charge\":0}","{\"level\":\"1\",\"charge\":0}","{\"level\":1e999,\"charge\":0}")) {
                var state=FarmToolState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow();
                assertEquals(FarmToolState.INVALID,state,json);
                var stack=new ItemStack(ToolContent.HOE_SILVER.get()); stack.set(ToolContent.FARM_TOOL_STATE.get(),state);
                assertFalse(FarmToolTasks.canUse(stack,ToolTier.SILVER,ToolAction.HOE));
            }
            var stack=new ItemStack(ToolContent.HOE_COPPER.get());
            stack.set(ToolContent.FARM_TOOL_STATE.get(),new FarmToolState(1,2,false));
            assertFalse(FarmToolTasks.canUse(stack,ToolTier.COPPER,ToolAction.HOE));
        }
    }
    @Test void actualStacksPersistWaterLevelChargeDamageAndIndependentCopies() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var ops=RegistryOps.create(NbtOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for (var item : List.of(ToolContent.HOE.get(),ToolContent.HOE_COPPER.get(),ToolContent.HOE_SILVER.get(),
                    ToolContent.WATERING_CAN.get(),ToolContent.WATERING_CAN_COPPER.get(),ToolContent.WATERING_CAN_SILVER.get())) {
                var stack=new ItemStack(item);
                assertEquals(FarmToolState.NEW,FarmToolState.get(stack));
                boolean hoe=item instanceof HoeItem;
                var tier=hoe?((HoeItem)item).tier():((WateringCanItem)item).tier();
                var state=new FarmToolState(42.5,hoe?tier.chargeLimit():0,false);
                stack.set(ToolContent.FARM_TOOL_STATE.get(),state);
                if(hoe) stack.setDamageValue(tier.durability());
                else stack.set(ToolContent.WATER_STATE.get(),new WateringCanState(17));
                var copy=stack.copy();
                var path=directory.resolve(BuiltInRegistries.ITEM.getKey(item).getPath()+".nbt");
                NbtIo.writeCompressed((CompoundTag)ItemStack.CODEC.encodeStart(ops,stack).getOrThrow(),path);
                var restored=ItemStack.CODEC.parse(ops,NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap())).getOrThrow();
                assertEquals(state,FarmToolState.get(restored));
                assertEquals(hoe?tier.durability():0,restored.getDamageValue());
                assertEquals(hoe?0:17,WateringCanState.get(restored).water());
                stack.set(ToolContent.FARM_TOOL_STATE.get(),state.discharge().advance(tier));
                assertEquals(state,FarmToolState.get(copy));
                assertEquals(!hoe,FarmToolTasks.canUse(restored,tier,hoe?ToolAction.HOE:ToolAction.WATER));
            }
            var old=new ItemStack(ToolContent.WATERING_CAN.get());
            old.set(ToolContent.WATER_STATE.get(),new WateringCanState(73));
            var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,old).getOrThrow()).getOrThrow();
            assertEquals(73,WateringCanState.get(restored).water());
            assertEquals(FarmToolState.NEW,FarmToolState.get(restored));
        }
    }
}

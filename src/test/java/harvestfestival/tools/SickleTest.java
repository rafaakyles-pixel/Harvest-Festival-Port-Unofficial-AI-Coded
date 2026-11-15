package harvestfestival.tools;

import harvestfestival.crops.CropTestSupport;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

class SickleTest {
    @TempDir Path directory;
    private static final SickleRules.Rule DEFAULT = new SickleRules.Rule(true,27);
    @Test void fullDurabilityConsumes128TasksThenRetainsSlowRecognizedTool() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var stack=new ItemStack(ToolContent.SICKLE.get());
            var costs=new ArrayList<Float>();
            for(int i=0;i<128;i++) {
                assertTrue(SickleItem.performTask(stack,false,DEFAULT,damage->damage,costs::add));
                assertEquals(i+1,stack.getDamageValue());
            }
            assertFalse(stack.isEmpty()); assertEquals(128,costs.size());
            assertEquals(4F/9F,costs.getFirst(),0.00001);
            assertEquals(128*SickleState.INCREASE,SickleState.get(stack).level(),0.00000001);
            assertTrue(stack.is(SickleItem.SICKLES));
            assertFalse(SickleItem.canUse(stack));
            assertEquals(0.05F,stack.getItem().getDestroySpeed(stack,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
            assertFalse(SickleItem.performTask(stack,false,DEFAULT,x->fail("no further damage"),x->fail("no further exhaustion")));
            assertEquals(1,stack.getCount());
        }
    }
    @Test void creativeAndDisabledHungerStillLevelButNeverDamageOrExhaust() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for(boolean creative:new boolean[]{false,true}) {
                var stack=new ItemStack(ToolContent.SICKLE.get());
                var rule=new SickleRules.Rule(creative,27);
                for(int i=0;i<300;i++) assertTrue(SickleItem.performTask(stack,creative,rule,x->fail("no damage"),x->fail("no exhaustion")));
                assertEquals(100,SickleState.get(stack).level()); assertEquals(0,stack.getDamageValue());
            }
        }
    }
    @Test void durabilityModifierCanPreventWearWithoutPreventingLevelOrHunger() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var stack=new ItemStack(ToolContent.SICKLE.get());
            var cost=new ArrayList<Float>();
            assertTrue(SickleItem.performTask(stack,false,DEFAULT,x->0,cost::add));
            assertEquals(0,stack.getDamageValue()); assertEquals(1,cost.size());
            assertEquals(SickleState.INCREASE,SickleState.get(stack).level());
        }
    }
    @Test void malformedLevelIsInertAndNeverResetsToNewTool() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for(String json:new String[]{"{}","null","[]","{\"level\":null}","{\"level\":true}","{\"level\":\"1\"}",
                    "{\"level\":-1}","{\"level\":101}","{\"level\":1e999}"}) {
                var state=SickleState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow();
                assertEquals(SickleState.INVALID,state,json);
                var stack=new ItemStack(ToolContent.SICKLE.get()); stack.set(ToolContent.SICKLE_STATE.get(),state);
                assertFalse(SickleItem.canUse(stack));
                assertFalse(SickleItem.performTask(stack,false,DEFAULT,x->fail("invalid"),x->fail("invalid")));
            }
        }
    }
    @Test void damagedAndBrokenStacksSurviveCompressedSaveWithoutSharingState() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var ops=RegistryOps.create(NbtOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for(int damage:new int[]{0,127,128}) {
                var stack=new ItemStack(ToolContent.SICKLE.get()); stack.setDamageValue(damage);
                stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(42.5,false));
                var copy=stack.copy();
                stack.set(ToolContent.SICKLE_STATE.get(),new SickleState(43,false));
                var path=directory.resolve("sickle-"+damage+".nbt");
                NbtIo.writeCompressed((CompoundTag)ItemStack.CODEC.encodeStart(ops,stack).getOrThrow(),path);
                var restored=ItemStack.CODEC.parse(ops,NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap())).getOrThrow();
                assertEquals(damage,restored.getDamageValue()); assertEquals(43,SickleState.get(restored).level());
                assertEquals(42.5,SickleState.get(copy).level()); assertEquals(1,restored.getCount());
                assertEquals(damage<128,SickleItem.canUse(restored));
            }
        }
    }
    @Test void actualRulesReloadRejectInvalidWinningResourceAndRestore() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var listener=new SickleRules();
            listener.apply(listener.prepare(fixture.manager,null),fixture.manager,null);
            assertEquals(DEFAULT,listener.rule());
            var path=directory.resolve("data/harvestfestival/tool_settings/sickle.json");
            for(String value:new String[]{"0","-1","1.5","\"27\"","null"}) {
                Files.writeString(path,"{\"consume_hunger\":true,\"actions_per_half_haunch\":"+value+"}");
                listener.apply(listener.prepare(fixture.manager,null),fixture.manager,null);
                assertNull(listener.rule());
            }
            Files.writeString(path,"{\"consume_hunger\":false,\"actions_per_half_haunch\":27}");
            listener.apply(listener.prepare(fixture.manager,null),fixture.manager,null);
            assertFalse(listener.rule().consumeHunger());
        }
    }
}

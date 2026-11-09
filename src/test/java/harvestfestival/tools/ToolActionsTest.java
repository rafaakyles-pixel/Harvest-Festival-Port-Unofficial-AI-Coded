package harvestfestival.tools;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolActionsTest {
    @BeforeAll static void setup() { WateringCanStateTest.bindComponents(); }

    @Test
    void hoePreparesOnlyMatchingTargetAndCanThenWaterIt() {
        var target = new Target(Blocks.DIRT.defaultBlockState(), "tillable");
        var hoe = new ItemStack(ToolContent.HOE.get());
        assertEquals(ToolActions.Result.CHANGED, act(ToolAction.HOE, target, hoe, false));
        assertEquals(ToolContent.PREPARED_SOIL.get().defaultBlockState(), target.state());
        target.tags = Set.of("waterable");
        var can = fullCan();
        assertEquals(ToolActions.Result.CHANGED, act(ToolAction.WATER, target, can, false));
        assertTrue(target.state().getValue(PreparedSoilBlock.WATERED));
        assertEquals(127, WateringCanState.get(can).water());
        assertEquals(2, target.writes);
    }

    @Test
    void emptyCanAndInvalidTargetDoNotChangeAnything() {
        var can = new ItemStack(ToolContent.WATERING_CAN.get());
        var soil = dry();
        assertEquals(ToolActions.Result.EMPTY, act(ToolAction.WATER, soil, can, false));
        var stone = new Target(Blocks.STONE.defaultBlockState());
        WateringCanState.fill(can);
        assertEquals(ToolActions.Result.NO_MATCH, act(ToolAction.WATER, stone, can, false));
        assertEquals(128, WateringCanState.get(can).water());
        assertEquals(0, soil.writes + stone.writes);
    }

    @Test
    void permissionObstructionAndWriteFailureNeverConsumeWater() {
        var can = fullCan();
        var soil = dry();
        soil.editable = false;
        assertEquals(ToolActions.Result.BLOCKED, act(ToolAction.WATER, soil, can, false));
        soil.editable = true;
        soil.acceptWrite = false;
        assertEquals(ToolActions.Result.FAILED, act(ToolAction.WATER, soil, can, false));
        assertEquals(128, WateringCanState.get(can).water());
        assertFalse(soil.state().getValue(PreparedSoilBlock.WATERED));
        var dirt = new Target(Blocks.DIRT.defaultBlockState(), "tillable");
        dirt.air = false;
        assertEquals(ToolActions.Result.BLOCKED, act(ToolAction.HOE, dirt, new ItemStack(ToolContent.HOE.get()), false));
        assertEquals(0, dirt.writes);
    }

    @Test
    void repeatedWateringAndUnchangedResultDoNotConsume() {
        var can = fullCan();
        var soil = dry();
        assertEquals(ToolActions.Result.CHANGED, act(ToolAction.WATER, soil, can, false));
        assertEquals(ToolActions.Result.NO_MATCH, act(ToolAction.WATER, soil, can, false));
        assertEquals(127, WateringCanState.get(can).water());
        var rules = ToolActionRulesTest.read("same", ToolActionRulesTest.WATER.replace("\"true\"", "\"false\""));
        assertEquals(ToolActions.Result.UNCHANGED, ToolActions.apply(ToolAction.WATER, dry(), can, false, rules, message -> fail(message)));
        assertEquals(127, WateringCanState.get(can).water());
    }

    @Test
    void lastUnitWorksOnceAndCreativeStillNeedsWater() {
        var can = new ItemStack(ToolContent.WATERING_CAN.get());
        can.set(ToolContent.WATER_STATE.get(), new WateringCanState(1));
        assertEquals(ToolActions.Result.CHANGED, act(ToolAction.WATER, dry(), can, false));
        assertEquals(ToolActions.Result.EMPTY, act(ToolAction.WATER, dry(), can, false));
        assertEquals(ToolActions.Result.EMPTY, act(ToolAction.WATER, dry(), can, true));
        WateringCanState.fill(can);
        assertEquals(ToolActions.Result.CHANGED, act(ToolAction.WATER, dry(), can, true));
        assertEquals(128, WateringCanState.get(can).water());
    }

    @Test
    void ambiguousRulesAreRejectedWithDiagnosticAndNoConsumption() {
        var rules = new ArrayList<>(ToolActionRulesTest.read("a", ToolActionRulesTest.WATER));
        rules.addAll(ToolActionRulesTest.read("b", ToolActionRulesTest.WATER));
        var warnings = new ArrayList<String>();
        var can = fullCan();
        var soil = dry();
        assertEquals(ToolActions.Result.AMBIGUOUS, ToolActions.apply(ToolAction.WATER, soil, can, false, rules, warnings::add));
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("a.json") && warnings.getFirst().contains("b.json"));
        assertEquals(0, soil.writes);
        assertEquals(128, WateringCanState.get(can).water());
    }

    @Test
    void wateredStateRoundTripsThroughBlockStateCodec() {
        var soil = dry();
        act(ToolAction.WATER, soil, fullCan(), false);
        var json = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, soil.state()).getOrThrow();
        assertEquals(soil.state(), BlockState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    static Target dry() { return new Target(ToolContent.PREPARED_SOIL.get().defaultBlockState(), "waterable"); }
    static ItemStack fullCan() {
        var can = new ItemStack(ToolContent.WATERING_CAN.get());
        WateringCanState.fill(can);
        return can;
    }
    private static ToolActions.Result act(ToolAction action, Target target, ItemStack stack, boolean creative) {
        var rules = new ArrayList<>(ToolActionRulesTest.read("hoe", ToolActionRulesTest.HOE));
        rules.addAll(ToolActionRulesTest.read("water", ToolActionRulesTest.WATER));
        return ToolActions.apply(action, target, stack, creative, rules, message -> fail(message));
    }

    static final class Target implements ToolActions.Target {
        BlockState current;
        Set<String> tags;
        boolean editable = true, air = true, acceptWrite = true;
        int writes;
        Target(BlockState state, String... tags) { current = state; this.tags = Set.of(tags); }
        public BlockState state() { return current; }
        public boolean matches(TagKey<Block> tag) {
            return tags.stream().map(name -> Identifier.parse("harvestfestival:" + name)).anyMatch(tag.location()::equals);
        }
        public boolean airAbove() { return air; }
        public boolean canEdit() { return editable; }
        public boolean set(BlockState state) { if (!acceptWrite) return false; current = state; writes++; return true; }
    }
}

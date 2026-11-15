package harvestfestival.tools;

import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ToolAreaTest {
    @TempDir Path directory;
    @Test void hoeUsesForwardLineInFourDirectionsAndWorldIterationOrder() {
        var p=new BlockPos(10,70,20);
        assertEquals(List.of(p,p.south(),p.south(2)),ToolArea.positions(p,Direction.SOUTH,ToolAction.HOE,ToolTier.SILVER));
        assertEquals(List.of(p.north(2),p.north(),p),ToolArea.positions(p,Direction.NORTH,ToolAction.HOE,ToolTier.SILVER));
        assertEquals(List.of(p,p.east(),p.east(2)),ToolArea.positions(p,Direction.EAST,ToolAction.HOE,ToolTier.SILVER));
        assertEquals(List.of(p.west(2),p.west(),p),ToolArea.positions(p,Direction.WEST,ToolAction.HOE,ToolTier.SILVER));
        assertEquals(List.of(p,p.south()),ToolArea.positions(p,Direction.SOUTH,ToolAction.HOE,ToolTier.COPPER));
        assertEquals(List.of(p),ToolArea.positions(p,Direction.SOUTH,ToolAction.HOE,ToolTier.BASIC));
        assertEquals(Direction.SOUTH,Direction.fromYRot(0)); assertEquals(Direction.WEST,Direction.fromYRot(90));
        assertEquals(Direction.NORTH,Direction.fromYRot(180)); assertEquals(Direction.EAST,Direction.fromYRot(270));
    }
    @Test void waterUsesTwoPlanesAndDifferentCopperAndSilverRectangles() {
        var p=new BlockPos(10,70,20);
        assertEquals(List.of(p.below(),p),ToolArea.positions(p,Direction.NORTH,ToolAction.WATER,ToolTier.BASIC));
        assertEquals(List.of(p.below().west(),p.below(),p.below().east(),p.west(),p,p.east()),
                ToolArea.positions(p,Direction.SOUTH,ToolAction.WATER,ToolTier.COPPER));
        assertEquals(List.of(p.below().north(),p.below(),p.below().south(),p.north(),p,p.south()),
                ToolArea.positions(p,Direction.EAST,ToolAction.WATER,ToolTier.COPPER));
        for(var dir:List.of(Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST)) {
            var area=ToolArea.positions(p,dir,ToolAction.WATER,ToolTier.SILVER);
            assertEquals(18,area.size()); assertEquals(18,area.stream().distinct().count());
            assertTrue(area.contains(p.relative(dir,2))); assertFalse(area.contains(p.relative(dir.getOpposite())));
            assertTrue(area.subList(0,9).stream().allMatch(pos->pos.getY()==69));
            assertTrue(area.subList(9,18).stream().allMatch(pos->pos.getY()==70));
        }
    }
    @Test void cropsResolveTwiceButNineSoilsConsumeExactlyNineUnitsAndNineCosts() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var rules=new ToolActionRules(); rules.apply(rules.prepare(fixture.manager,null),fixture.manager,null);
            var p=new BlockPos(0,70,0); var world=new HashMap<BlockPos,BlockState>();
            for(int x=-1;x<=1;x++) for(int z=0;z<=2;z++) {
                world.put(new BlockPos(x,70,z),CropContent.CROP.get().defaultBlockState());
                world.put(new BlockPos(x,69,z),Blocks.FARMLAND.defaultBlockState());
            }
            var stack=new ItemStack(ToolContent.WATERING_CAN_SILVER.get()); WateringCanState.fill(stack);
            var costs=new ArrayList<Float>();
            assertEquals(9,water(world,p,stack,false,rules,costs));
            assertEquals(119,WateringCanState.get(stack).water()); assertEquals(9,costs.size());
            assertEquals(9*ToolTier.SILVER.levelIncrease(),FarmToolState.get(stack).level(),1e-12);
            assertEquals(0,stack.getDamageValue());
            assertEquals(0,water(world,p,stack,false,rules,costs));
            assertEquals(9,costs.size());
            assertTrue(ToolActions.waterAnchor(world.get(p))); assertTrue(ToolActions.waterAnchor(world.get(p.below())));
            assertFalse(ToolActions.waterAnchor(Blocks.STONE.defaultBlockState()));
        }
    }
    @Test void partialWaterFollowsOrderAndCreativeStillRequiresWater() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var rules=new ToolActionRules(); rules.apply(rules.prepare(fixture.manager,null),fixture.manager,null);
            var p=new BlockPos(0,70,0); var world=new HashMap<BlockPos,BlockState>();
            for(int x=-1;x<=1;x++) for(int z=0;z<=2;z++) world.put(new BlockPos(x,70,z),Blocks.FARMLAND.defaultBlockState());
            var stack=new ItemStack(ToolContent.WATERING_CAN_SILVER.get()); stack.set(ToolContent.WATER_STATE.get(),new WateringCanState(2));
            var costs=new ArrayList<Float>();
            assertEquals(2,water(world,p,stack,false,rules,costs));
            assertEquals(7,world.get(p.west()).getValue(FarmlandBlock.MOISTURE));
            assertEquals(7,world.get(p.west().south()).getValue(FarmlandBlock.MOISTURE));
            assertEquals(0,world.get(p.west().south(2)).getValue(FarmlandBlock.MOISTURE));
            assertEquals(0,water(world,p,stack,true,rules,costs));
            stack.set(ToolContent.WATER_STATE.get(),new WateringCanState(1));
            assertEquals(7,water(world,p,stack,true,rules,costs));
            assertEquals(1,WateringCanState.get(stack).water()); assertEquals(2,costs.size());
        }
    }
    @Test void failedBlockedAndUnchangedTargetsDoNotCostAndLastDurabilityStopsArea() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var stack=new ItemStack(ToolContent.HOE_SILVER.get()); stack.setDamageValue(767);
            var positions=List.of(new BlockPos(0,0,0),new BlockPos(1,0,0),new BlockPos(2,0,0),new BlockPos(3,0,0),new BlockPos(4,0,0));
            var visited=new ArrayList<BlockPos>(); var costs=new ArrayList<Float>();
            assertEquals(1,ToolArea.perform(positions,()->FarmToolTasks.canUse(stack,ToolTier.SILVER,ToolAction.HOE),pos->{
                visited.add(pos);
                return switch(pos.getX()) { case 0 -> ToolActions.Result.BLOCKED; case 1 -> ToolActions.Result.FAILED;
                    case 2 -> ToolActions.Result.UNCHANGED; default -> ToolActions.Result.CHANGED; };
            },pos->FarmToolTasks.complete(stack,ToolTier.SILVER,ToolAction.HOE,false,FarmToolTest.rule(true),x->x,costs::add)));
            assertEquals(positions.subList(0,4),visited); assertEquals(768,stack.getDamageValue());
            assertEquals(1,costs.size()); assertEquals(ToolTier.SILVER.levelIncrease(),FarmToolState.get(stack).level());
        }
    }
    private static int water(Map<BlockPos,BlockState> world,BlockPos anchor,ItemStack stack,boolean creative,
            ToolActionRules rules,List<Float> costs) {
        return ToolArea.perform(ToolArea.positions(anchor,Direction.SOUTH,ToolAction.WATER,ToolTier.SILVER),
                ()->FarmToolTasks.canUse(stack,ToolTier.SILVER,ToolAction.WATER)&&WateringCanState.get(stack).water()>0,
                candidate->{
                    var pos=ToolActions.resolveTarget(ToolAction.WATER,candidate,key->world.getOrDefault(key,Blocks.AIR.defaultBlockState()));
                    return ToolActions.apply(ToolAction.WATER,new ToolActions.Target() {
                        public BlockState state(){return world.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
                        public boolean airAbove(){return false;}
                        public boolean canEdit(){return true;}
                        public boolean set(BlockState state){world.put(pos,state);return true;}
                    },stack,creative,rules.rules(),message->fail(message));
                },pos->FarmToolTasks.complete(stack,ToolTier.SILVER,ToolAction.WATER,creative,FarmToolTest.rule(true),
                        x->fail("watering cans never take damage"),costs::add));
    }
}

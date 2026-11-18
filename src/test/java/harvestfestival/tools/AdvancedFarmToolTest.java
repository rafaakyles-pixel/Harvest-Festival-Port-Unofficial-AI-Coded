package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedFarmToolTest {
    @TempDir Path directory;
    @Test void goldGeometryAndPartialCharges() { geometry(ToolTier.GOLD,4,5); }
    @Test void goldChargeCodecAndPhysicalTierGuards() throws Exception { charge(ToolContent.HOE_GOLD.get()); }
    @Test void goldWaterUsesConfirmedSoilsAndRetainsAnyAnchor() throws Exception { water(ToolContent.WATERING_CAN_GOLD.get(),15); }
    @Test void goldLastPointFailuresAndCreative() throws Exception { lastPoint(ToolContent.HOE_GOLD.get()); }

    @Test void mystrilGeometryAndPartialCharges() { geometry(ToolTier.MYSTRIL,6,7); }
    @Test void mystrilChargeCodecAndPhysicalTierGuards() throws Exception { charge(ToolContent.HOE_MYSTRIL.get()); }
    @Test void mystrilWaterUsesConfirmedSoilsAndRetainsAnyAnchor() throws Exception { water(ToolContent.WATERING_CAN_MYSTRIL.get(),21); }
    @Test void mystrilLastPointFailuresAndCreative() throws Exception { lastPoint(ToolContent.HOE_MYSTRIL.get()); }

    private void geometry(ToolTier tier,int depth,int width) {
        var anchor=new BlockPos(10,64,20);
        for(var dir:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) {
            var expected=new ArrayList<BlockPos>();
            for(int i=0;i<depth;i++) expected.add(anchor.relative(dir,i));
            expected.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
            assertEquals(expected,ToolArea.positions(anchor,dir,ToolAction.HOE,tier));
            var water=ToolArea.positions(anchor,dir,ToolAction.WATER,tier);
            assertEquals(width*3*2,water.size()); assertEquals(water.size(),new HashSet<>(water).size());
            var sorted=new ArrayList<>(water); sorted.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
            assertEquals(sorted,water);
            assertTrue(water.subList(0,width*3).stream().allMatch(p->p.getY()==63));
            assertTrue(water.subList(width*3,water.size()).stream().allMatch(p->p.getY()==64));
            assertTrue(water.contains(anchor.relative(dir,2).relative(dir.getClockWise(),width/2)));
            assertFalse(water.contains(anchor.relative(dir,3)));
            for(int charge=0;charge<=tier.chargeLimit();charge++)
                assertEquals(new int[]{1,2,3,4,6}[charge],ToolArea.positions(anchor,dir,ToolAction.HOE,ToolTier.charged(charge)).size());
        }
    }
    private void charge(HoeItem item) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var tier=item.tier();
            for(int ticks=0;ticks<=100;ticks++) assertEquals(Math.min(tier.chargeLimit(),ticks/20),FarmToolState.NEW.charge(tier,ticks).charge());
            var full=FarmToolState.NEW.charge(tier,32000);
            assertEquals(HoeItem.UseAction.APPLY,HoeItem.startAction(tier,full,false));
            assertEquals(HoeItem.UseAction.DISCHARGE,HoeItem.startAction(tier,full,true));
            assertEquals(0,full.discharge().charge());
            for(int count=0;count<=4;count++) {
                var state=FarmToolState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"level\":42,\"charge\":"+count+"}")).getOrThrow();
                assertEquals(new FarmToolState(42,count,false),state);
                var stack=new ItemStack(item);stack.set(ToolContent.FARM_TOOL_STATE.get(),state);
                assertEquals(count<=tier.chargeLimit(),FarmToolTasks.canUse(stack,tier,ToolAction.HOE));
                assertEquals(count<=2,state.validFor(ToolTier.SILVER));
                var buffer=io.netty.buffer.Unpooled.buffer();
                try { var codec=net.minecraft.network.codec.ByteBufCodecs.fromCodec(FarmToolState.CODEC);
                    codec.encode(buffer,state);assertEquals(state,codec.decode(buffer));
                } finally {buffer.release();}
            }
            assertEquals(FarmToolState.INVALID,FarmToolState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"level\":42,\"charge\":5}")).getOrThrow());
        }
    }
    private void water(WateringCanItem item,int soils) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var rules=new ToolActionRules();rules.apply(rules.prepare(fixture.manager,null),fixture.manager,null);
            var tier=item.tier();var anchor=new BlockPos(0,70,0);
            var positions=ToolArea.positions(anchor,Direction.SOUTH,ToolAction.WATER,tier);
            for(var clicked:List.of(Blocks.STONE,Blocks.WHEAT,CropContent.CROP.get())) {
                var world=new HashMap<BlockPos,BlockState>();
                for(var pos:positions) world.put(pos,pos.getY()==69?Blocks.FARMLAND.defaultBlockState():CropContent.CROP.get().defaultBlockState());
                world.put(anchor,clicked.defaultBlockState());
                var stack=new ItemStack(item);WateringCanState.fill(stack);var costs=new ArrayList<Float>();
                for(int pass=0;pass<2;pass++) {
                    int changed=ToolArea.perform(positions,()->WateringCanState.get(stack).water()>0,p->{
                        var pos=ToolActions.resolveTarget(ToolAction.WATER,p,key->world.getOrDefault(key,Blocks.AIR.defaultBlockState()));
                        return ToolActions.apply(ToolAction.WATER,new ToolActions.Target(){
                            public BlockState state(){return world.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
                            public boolean airAbove(){return false;} public boolean canEdit(){return true;}
                            public boolean set(BlockState state){world.put(pos,state);return true;}
                        },stack,false,rules.rules(),message->fail(message));
                    },p->FarmToolTasks.complete(stack,tier,ToolAction.WATER,false,FarmToolTest.rule(true),x->fail("can damage"),costs::add));
                    assertEquals(pass==0?soils:0,changed);
                }
                assertEquals(128-soils,WateringCanState.get(stack).water());assertEquals(soils,costs.size());
                assertEquals(soils*tier.levelIncrease(),FarmToolState.get(stack).level(),1e-12);
                assertEquals(0,FarmToolState.get(stack).charge());assertEquals(0,stack.getMaxDamage());
            }
        }
    }
    private void lastPoint(HoeItem item) throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            var tier=item.tier();var stack=new ItemStack(item);stack.setDamageValue(tier.durability()-1);
            stack.set(ToolContent.FARM_TOOL_STATE.get(),new FarmToolState(0,tier.chargeLimit(),false));
            var positions=ToolArea.positions(BlockPos.ZERO,Direction.SOUTH,ToolAction.HOE,tier);var costs=new ArrayList<Float>();
            assertEquals(1,ToolArea.perform(positions,()->FarmToolTasks.canUse(stack,tier,ToolAction.HOE),p->
                p.getZ()==0?ToolActions.Result.BLOCKED:p.getZ()==1?ToolActions.Result.FAILED:ToolActions.Result.CHANGED,
                p->FarmToolTasks.complete(stack,tier,ToolAction.HOE,false,FarmToolTest.rule(true),x->x,costs::add)));
            assertEquals(tier.durability(),stack.getDamageValue());assertEquals(1,stack.getCount());assertEquals(1,costs.size());
            assertEquals(4F/27*tier.effort(),costs.getFirst(),1e-6);
            assertEquals(tier.levelIncrease(),FarmToolState.get(stack).level());assertEquals(tier.chargeLimit(),FarmToolState.get(stack).charge());
            for(boolean creative:new boolean[]{false,true}) {
                var fresh=new ItemStack(item);
                FarmToolTasks.complete(fresh,tier,ToolAction.HOE,creative,FarmToolTest.rule(creative),x->fail("damage"),x->fail("cost"));
                assertEquals(tier.levelIncrease(),FarmToolState.get(fresh).level());assertEquals(0,fresh.getDamageValue());
                FarmToolTasks.complete(fresh,tier,ToolAction.HOE,false,FarmToolTest.rule(true),x->0,x->{});
                assertEquals(0,fresh.getDamageValue());
            }
        }
    }
}

package harvestfestival.crops;

import harvestfestival.calendar.Season;
import harvestfestival.tools.ToolContent;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WheatCropTest {
    @TempDir Path directory;
    static CropDefinition wheat() { return CropDefinitions.INSTANCE.get(Identifier.parse("harvestfestival:wheat")); }

    @Test void originalStagesRequire27WateredUpdatesAndUseEightVanillaAppearances() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var definition = wheat();
            assertEquals(List.of(2,5,9,12,16,22,27,28), definition.stages());
            assertEquals(0, definition.regrowStage());
            assertEquals(1, definition.maxHarvests());
            for (Season season : List.of(Season.SPRING, Season.SUMMER, Season.AUTUMN)) {
                var state = CropState.planted(definition.id());
                assertEquals(1, state.dryDays());
                for (int day = 1; day <= 27; day++) {
                    assertFalse(CropLifecycle.canHarvest(state, definition));
                    state = CropLifecycle.newDay(state, definition, season, true, false);
                    assertEquals(day + 1, state.stage());
                }
                assertTrue(CropLifecycle.canHarvest(state, definition));
                assertEquals(18, definition.appearance(state.stage()));
                assertNull(CropLifecycle.afterHarvest(state, definition));
            }
            int previous = 0;
            for (int i = 0; i < 8; i++) {
                assertEquals(11+i, definition.appearance(previous + 1));
                assertEquals(11+i, definition.appearance(definition.stages().get(i)));
                previous = definition.stages().get(i);
            }
            assertEquals(definition, CropDefinitions.INSTANCE.forSeed(new ItemStack(CropContent.WHEAT_SEEDS.get())));
            assertNull(CropDefinitions.INSTANCE.forSeed(new ItemStack(Items.WHEAT_SEEDS)));
        }
    }
    @Test void interactionsAndBreakMatrixNeverGiveImmatureOrWrongToolProducts() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var definition = wheat();
            var sickle = new ItemStack(ToolContent.SICKLE.get());
            var broken = sickle.copy(); broken.setDamageValue(128);
            for (var tool : List.of(ItemStack.EMPTY, new ItemStack(ToolContent.HOE.get()), sickle, broken)) {
                for (int stage : new int[]{1, 27, 28}) for (boolean dead : new boolean[]{false, true}) {
                    var expected = new CropState(definition.id(), stage, 1, dead, 0);
                    var entity = new CropBlockEntity(BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
                    entity.setCrop(expected);
                    assertFalse(CropBlock.harvestOnce(expected, definition, () -> fail("no right-click harvest"),
                            next -> fail("no interaction transition"), stack -> fail("no interaction drop")));
                    var present = new AtomicBoolean(true);
                    var output = new ArrayList<ItemStack>();
                    assertTrue(CropBlock.breakOnce(expected, definition, tool, () -> List.of(new ItemStack(Items.WHEAT)),
                            () -> entity.commitHarvest(expected, null, () -> present.getAndSet(false)),
                            stack -> { assertFalse(present.get()); output.add(stack); }));
                    assertEquals(!dead && stage == 28 && definition.acceptsTool(tool) ? 1 : 0, output.size());
                    assertEquals(CropState.INVALID, entity.crop());
                    assertTrue(CropContent.CROP.get().getDrops(entity.getBlockState(), new LootParams.Builder(null)
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity)).isEmpty());
                    assertFalse(CropBlock.breakOnce(expected, definition, tool, List::of,
                            () -> entity.commitHarvest(expected, null, () -> present.getAndSet(false)), output::add));
                }
            }
        }
    }
    @Test void failureDoesNotYieldAndEmptyLootStillRemoves() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var definition = wheat();
            var state = new CropState(definition.id(),28,1,false);
            var sickle = new ItemStack(ToolContent.SICKLE.get());
            assertFalse(CropBlock.breakOnce(state, definition, sickle, () -> List.of(new ItemStack(Items.WHEAT)),
                    () -> false, stack -> fail("failed removal")));
            var removed = new AtomicBoolean();
            assertTrue(CropBlock.breakOnce(state, definition, sickle, List::of, () -> { removed.set(true); return true; },
                    stack -> fail("empty loot")));
            assertTrue(removed.get());
        }
    }
    @Test void droughtWinterAndReloadPreserveOriginalLifecycleAndSavedState() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var definition = wheat();
            var crop = CropState.planted(definition.id());
            for (int i=0;i<2;i++) crop=CropLifecycle.newDay(crop,definition,Season.SPRING,false,false);
            assertEquals(1,crop.stage()); assertEquals(3,crop.dryDays());
            assertTrue(CropLifecycle.newDay(crop,definition,Season.SPRING,false,false).dead());
            assertFalse(CropLifecycle.newDay(crop,definition,Season.SPRING,true,false).dead());
            assertFalse(CropLifecycle.newDay(crop,definition,Season.SPRING,false,true).dead());
            assertTrue(CropLifecycle.newDay(crop,definition,Season.WINTER,true,true).dead());
            var saved=CropState.CODEC.encodeStart(NbtOps.INSTANCE,crop).getOrThrow();
            String json=Files.readString(directory.resolve("data/harvestfestival/crops/wheat.json"));
            Files.writeString(directory.resolve("data/harvestfestival/crops/wheat.json"),json.replace("\"minimum_cut\": 0","\"minimum_cut\": 1"));
            CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager,null),fixture.manager,null);
            assertNull(wheat());
            var restored=CropState.CODEC.parse(NbtOps.INSTANCE,saved).getOrThrow();
            assertEquals(crop, CropLifecycle.newDay(restored,wheat(),Season.SPRING,true,false));
            Files.writeString(directory.resolve("data/harvestfestival/crops/wheat.json"),json);
            CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager,null),fixture.manager,null);
            assertEquals(crop.stage()+1,CropLifecycle.newDay(restored,wheat(),Season.SPRING,true,false).stage());
        }
    }
    @Test void originalBreakSpeedDistinguishesSickleHandAndWitheredPlant() {
        assertEquals(1F,CropBlock.destroyProgress(1F,false,true),0.00001);
        assertEquals(0.1F,CropBlock.destroyProgress(1F,false,false),0.00001);
        assertEquals(0.2F,CropBlock.destroyProgress(1F,true,true),0.00001);
        assertEquals(0.05F,CropBlock.destroyProgress(0.05F,false,true),0.00001);
    }
}

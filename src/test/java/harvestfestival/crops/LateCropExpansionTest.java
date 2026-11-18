package harvestfestival.crops;

import com.google.gson.JsonParser;
import harvestfestival.calendar.Season;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LateCropExpansionTest {
    @TempDir Path directory;
    static final List<String> NAMES = List.of("cabbage");
    static final List<Season> SEASONS = List.of(Season.SPRING);
    static CropDefinition definition(String name) { return CropDefinitions.INSTANCE.get(Identifier.parse("harvestfestival:" + name)); }
    static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("harvestfestival:" + name))); }
    static String resource(String path) throws Exception {
        try (var input = CropContent.class.getResourceAsStream(path)) {
            assertNotNull(input, path); return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void completeCyclesUseIndividualStagesAndExactHarvestLimits() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var stages = List.of(List.of(4,9,14,15));
            int[] returns = {0}, limits = {1}, totals = {14};
            for (int i = 0; i < NAMES.size(); i++) {
                var name = NAMES.get(i); var d = definition(name);
                assertEquals(stages.get(i), d.stages()); assertEquals(returns[i], d.regrowStage());
                assertEquals(limits[i], d.maxHarvests()); assertFalse(d.requiresTool());
                assertEquals(d, CropDefinitions.INSTANCE.forSeed(item(name + "_seeds")));
                var entity = CropRegrowthTest.entity(CropState.planted(d.id()));
                var output = new ArrayList<ItemStack>(); int days = 0;
                for (int harvest = 1; harvest <= d.maxHarvests(); harvest++) {
                    int interval = harvest == 1 ? d.maturity() - 1 : d.maturity() - d.regrowStage();
                    for (int day = 0; day < interval; day++) {
                        assertFalse(CropLifecycle.canHarvest(entity.crop(), d));
                        entity.setCrop(CropLifecycle.newDay(entity.crop(), d, SEASONS.get(i), true, false)); days++;
                    }
                    var expected = entity.crop(); int count = harvest;
                    assertTrue(CropBlock.harvestOnce(expected, d, () -> List.of(item(name)),
                            next -> entity.commitHarvest(expected, next, () -> true), stack -> {
                                if (count == d.maxHarvests()) assertEquals(CropState.INVALID, entity.crop());
                                else {
                                    assertEquals(d.regrowStage(), entity.crop().stage()); assertEquals(count, entity.crop().harvests());
                                    assertEquals(expected.dryDays(), entity.crop().dryDays());
                                }
                                output.add(stack);
                            }));
                    assertFalse(CropBlock.harvestOnce(expected, d, () -> List.of(item(name)),
                            next -> entity.commitHarvest(expected, next, () -> fail("stale")), output::add));
                    assertFalse(CropLifecycle.canHarvest(entity.crop(), d));
                }
                assertEquals(totals[i], days); assertEquals(d.maxHarvests(), output.size());
            }
        }
    }

    @Test void droughtSeasonAndDedicatedDefaultsAlsoApplyDuringRegrowth() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (int i = 0; i < NAMES.size(); i++) {
                var d = definition(NAMES.get(i)); var season = SEASONS.get(i);
                for (var initial : List.of(CropState.planted(d.id()), new CropState(d.id(), Math.max(1,d.regrowStage()), 1, false, 2))) {
                    var dry = initial;
                    for (int day = 0; day < 2; day++) dry = CropLifecycle.newDay(dry, d, season, false, false);
                    assertEquals(initial.stage(), dry.stage()); assertEquals(3, dry.dryDays());
                    assertTrue(CropLifecycle.newDay(dry, d, season, false, false).dead());
                    var rescued = CropLifecycle.newDay(dry, d, season, true, false);
                    assertFalse(rescued.dead()); assertEquals(initial.stage()+1, rescued.stage());
                    assertEquals(initial.harvests(), rescued.harvests());
                    for (int day = 0; day < 10; day++) dry = CropLifecycle.newDay(dry, d, season, false, true);
                    assertFalse(dry.dead()); assertEquals(initial.stage(), dry.stage());
                    for (var wrong : Season.values()) if (wrong != season)
                        assertTrue(CropLifecycle.newDay(dry, d, wrong, true, true).dead());
                }
            }
        }
    }

    @Test void emptyLootAdvancesAndFailedLastRemovalPreservesStateWithoutOutput() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : NAMES) {
                var d = definition(name);
                for (int count = 0; count < d.maxHarvests(); count++) {
                    var mature = new CropState(d.id(), d.maturity(), 2, false, count);
                    var entity = CropRegrowthTest.entity(mature);
                    assertFalse(CropBlock.harvestOnce(mature, d, () -> List.of(item(name)), next -> false, stack -> fail("failed")));
                    assertEquals(mature, entity.crop());
                    if (count == d.maxHarvests()-1) {
                        assertFalse(CropBlock.harvestOnce(mature, d, List::of,
                                next -> entity.commitHarvest(mature, next, () -> false), stack -> fail("empty")));
                        assertEquals(mature, entity.crop());
                    }
                    assertTrue(CropBlock.harvestOnce(mature, d, List::of,
                            next -> entity.commitHarvest(mature, next, () -> true), stack -> fail("empty")));
                    if (count < d.maxHarvests()-1) assertEquals(new CropState(d.id(), Math.max(1,d.regrowStage()), 2, false, count+1), entity.crop());
                    else assertEquals(CropState.INVALID, entity.crop());
                }
            }
        }
    }

    @Test void breakingAlwaysRemovesAndSecondVanillaLootPassCannotDuplicate() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : NAMES) {
                var d = definition(name);
                for (var tool : List.of(ItemStack.EMPTY, new ItemStack(harvestfestival.tools.ToolContent.SICKLE.get()))) {
                    for (var state : List.of(CropState.planted(d.id()), new CropState(d.id(), d.maturity(), 1, false, 0),
                            new CropState(d.id(), d.maturity(), 3, true, 1), new CropState(d.id(), d.maturity(), 1, false, d.maxHarvests()))) {
                        var entity = CropRegrowthTest.entity(state); var output = new ArrayList<ItemStack>();
                        boolean eligible = CropLifecycle.canHarvest(state, d);
                        assertFalse(CropBlock.breakOnce(state, d, tool, () -> List.of(item(name)), () -> false, output::add));
                        assertTrue(output.isEmpty()); assertEquals(state, entity.crop());
                        assertTrue(CropBlock.breakOnce(state, d, tool, () -> { assertTrue(eligible); return List.of(item(name)); },
                                () -> entity.commitHarvest(state, null, () -> true), stack -> {
                                    assertEquals(CropState.INVALID, entity.crop()); output.add(stack);
                                }));
                        assertEquals(eligible ? 1 : 0, output.size());
                        assertTrue(CropContent.CROP.get().getDrops(entity.getBlockState(), new LootParams.Builder(null)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity)).isEmpty());
                    }
                }
            }
        }
    }

    @Test void persistedCountsAndVisualStatesSurviveReloadAndInvalidDefinitionsFreezeWithoutReset() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for (int i = 0; i < NAMES.size(); i++) {
                var name = NAMES.get(i); var d = definition(name);
                for (int stage : new int[]{1,Math.max(1,d.regrowStage()),d.maturity()}) for (boolean dead : new boolean[]{false,true}) {
                    var state = new CropState(d.id(), stage, 2, dead, d.maxHarvests()-1);
                    var block = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE, dead ? 3 : d.appearance(stage));
                    var entity = new CropBlockEntity(BlockPos.ZERO, block); entity.setCrop(state);
                    var file = directory.resolve(name+stage+dead+".nbt");
                    NbtIo.writeCompressed(entity.saveWithFullMetadata(registry), file);
                    var restored = (CropBlockEntity) BlockEntity.loadStatic(BlockPos.ZERO, block,
                            NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), registry);
                    assertNotNull(restored); assertEquals(state, restored.crop());
                }
                var path = directory.resolve("pack/data/harvestfestival/crops/"+name+".json");
                var valid = Files.readString(path); var saved = new CropState(d.id(), Math.max(1,d.regrowStage()), 2, false, d.maxHarvests()-1);
                Files.writeString(path, "{");
                CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager,null),fixture.manager,null);
                assertNull(definition(name)); assertEquals(saved, CropLifecycle.newDay(saved,definition(name),SEASONS.get(i),true,false));
                Files.writeString(path, valid);
                CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager,null),fixture.manager,null);
                assertEquals(saved.harvests(), CropLifecycle.newDay(saved,definition(name),SEASONS.get(i),true,false).harvests());
            }
        }
    }

    @Test void visualBoundariesTintsModelsProductsAndTranslationsArePackaged() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            int[] bases = {50}, colors = {0x8FFF40};
            var variants = JsonParser.parseString(resource("/assets/harvestfestival/blockstates/crop.json")).getAsJsonObject().getAsJsonObject("variants");
            for (int n = 0; n < NAMES.size(); n++) {
                var name = NAMES.get(n); var d = definition(name); int previous = 0;
                for (int i = 0; i < d.stages().size(); i++) {
                    assertEquals(bases[n]+i,d.appearance(previous+1)); assertEquals(bases[n]+i,d.appearance(d.stages().get(i)));
                    previous=d.stages().get(i);
                    assertEquals("harvestfestival:block/"+name+"_"+(i+1), variants.getAsJsonObject("appearance="+(bases[n]+i)).get("model").getAsString());
                    assertTrue(resource("/assets/harvestfestival/models/block/"+name+"_"+(i+1)+".json").contains("crop_original"));
                    try (var png = CropContent.class.getResourceAsStream("/assets/harvestfestival/textures/block/crops/"+name+"_"+(i+1)+".png")) { assertNotNull(png); assertTrue(png.readAllBytes().length > 8); }
                }
                assertEquals(bases[n], d.appearance(1));
                var model = JsonParser.parseString(resource("/assets/harvestfestival/items/"+name+"_seeds.json")).getAsJsonObject().getAsJsonObject("model");
                for (var tint : model.getAsJsonArray("tints")) assertEquals(colors[n], tint.getAsJsonObject().get("value").getAsInt());
                for (var locale : List.of("en_us","pt_br")) {
                    var lang = JsonParser.parseString(resource("/assets/harvestfestival/lang/"+locale+".json")).getAsJsonObject();
                    assertTrue(lang.has("item.harvestfestival."+name)); assertTrue(lang.has("item.harvestfestival."+name+"_seeds"));
                }
                assertTrue(resource("/data/harvestfestival/loot_table/crops/"+name+".json").contains("harvestfestival:"+name));
            }
        }
    }
}

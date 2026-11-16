package harvestfestival.crops;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropDefinitionsTest {
    @TempDir Path directory;
    @Test void speciesHaveOriginalVisualBoundariesAndRegrowthReturnsToThirdCucumberTexture() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var turnip = CropTestSupport.turnip();
            var cucumber = CropTestSupport.cucumber();
            int[] turnipVisuals = {4, 4, 5, 5, 6};
            int[] cucumberVisuals = {7, 7, 7, 8, 8, 8, 9, 9, 9, 10};
            for (int i = 0; i < turnipVisuals.length; i++) assertEquals(turnipVisuals[i], turnip.appearance(i + 1));
            for (int i = 0; i < cucumberVisuals.length; i++) {
                assertEquals(cucumberVisuals[i], cucumber.appearance(i + 1));
                var block = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE, cucumberVisuals[i]);
                var encoded = net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, block).getOrThrow();
                assertEquals(block, net.minecraft.world.level.block.state.BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow());
            }
            var regrown = CropLifecycle.afterHarvest(new CropState(cucumber.id(), 10, 2, false, 1), cucumber);
            assertEquals(9, cucumber.appearance(regrown.stage()));
            assertEquals(2, regrown.harvests());
            assertEquals(2, regrown.dryDays());
            assertEquals(6, turnip.appearance(100));
            assertEquals(10, cucumber.appearance(100));
        }
    }

    @Test void legacyVisualsRemainValidAndMalformedMappingsAreIsolated() throws Exception {
        String mapped = bundled();
        String legacy = mapped.replace("  \"appearances\": [4, 5, 6],\n", "").replace("  \"appearances\": [4, 5, 6],\r\n", "");
        var definition = CropDefinitions.read(Map.ofEntries(resource("legacy", legacy)), message -> fail(message)).values().iterator().next();
        assertEquals(0, definition.appearance(1));
        assertEquals(1, definition.appearance(3));
        assertEquals(2, definition.appearance(5));
        for (String invalid : List.of("[4, 5]", "[4, 5, 50]", "[4, 5, -1]", "[4, 5, 3]", "[4, 5, 6.5]", "[4, 5, \"6\"]", "null", "{}")) {
            var warnings = new ArrayList<String>();
            var definitions = CropDefinitions.read(Map.ofEntries(resource("valid", legacy),
                    resource("invalid", mapped.replace("[4, 5, 6]", invalid))), warnings::add);
            assertEquals(1, definitions.size(), invalid);
            assertEquals(1, warnings.size(), invalid);
        }
    }
    static String cucumberJson() throws Exception {
        try (var stream = CropContent.class.getResourceAsStream("/data/harvestfestival/crops/cucumber.json")) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void oldDefinitionKeepsSingleHarvestAndCucumberLoadsOriginalLimits() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var turnip = CropTestSupport.turnip();
            assertEquals(0, turnip.regrowStage());
            assertEquals(1, turnip.maxHarvests());
            assertEquals(CropDefinition.EmptyHarvestPolicy.PRESERVE, turnip.emptyHarvestPolicy());
            var cucumber = CropTestSupport.cucumber();
            assertEquals(List.of(3, 6, 9, 10), cucumber.stages());
            assertEquals(7, cucumber.regrowStage());
            assertEquals(8, cucumber.maxHarvests());
            assertEquals(CropDefinition.EmptyHarvestPolicy.ADVANCE, cucumber.emptyHarvestPolicy());
            assertEquals(cucumber, CropDefinitions.INSTANCE.forSeed(new ItemStack(CropContent.CUCUMBER_SEEDS.get())));
        }
    }

    @Test void invalidHarvestFieldsAreRejectedWithoutHidingValidNeighbors() throws Exception {
        String valid = cucumberJson();
        for (String invalid : List.of(
                valid.replace("\"regrow_stage\": 7", "\"regrow_stage\": 10"),
                valid.replace("\"regrow_stage\": 7", "\"regrow_stage\": 0"),
                valid.replace("\"regrow_stage\": 7", "\"regrow_stage\": -1"),
                valid.replace("\"regrow_stage\": 7", "\"regrow_stage\": 7.5"),
                valid.replace("\"max_harvests\": 8", "\"max_harvests\": 0"),
                valid.replace("\"max_harvests\": 8", "\"max_harvests\": 1000001"),
                valid.replace("\"max_harvests\": 8", "\"max_harvests\": \"8\""),
                valid.replace("\"max_harvests\": 8", "\"max_harvests\": null"),
                valid.replace("\"advance\"", "\"unknown\""), valid.replace("\"advance\"", "true"))) {
            var warnings = new ArrayList<String>();
            var definitions = CropDefinitions.read(Map.ofEntries(resource("valid", valid), resource("invalid", invalid)), warnings::add);
            assertEquals(1, definitions.size(), invalid);
            assertEquals(1, warnings.size(), invalid);
        }
    }

    @Test void reloadChangesLimitsWithoutResettingSavedStageOrCountAndMissingRuleFreezes() throws Exception {
        var low = directory.resolve("low");
        try (var fixture = new CropTestSupport(low)) {
            var saved = new CropState(CropTestSupport.cucumber().id(), 10, 2, false, 4);
            var listener = new CropDefinitions();
            var high = directory.resolve("high");
            var target = high.resolve("data/harvestfestival/crops/cucumber.json");
            Files.createDirectories(target.getParent());
            var valid = cucumberJson();
            Files.writeString(target, valid.replace("\"max_harvests\": 8", "\"max_harvests\": 4"));
            try (var manager = CropTestSupport.manager(low, high)) {
                listener.apply(listener.prepare(manager, null), manager, null);
            }
            assertFalse(CropLifecycle.canHarvest(saved, listener.get(saved.crop())));
            Files.writeString(target, valid.replace("\"regrow_stage\": 7", "\"regrow_stage\": 6"));
            try (var manager = CropTestSupport.manager(low, high)) {
                listener.apply(listener.prepare(manager, null), manager, null);
            }
            var next = CropLifecycle.afterHarvest(saved, listener.get(saved.crop()));
            assertEquals(6, next.stage());
            assertEquals(5, next.harvests());
            assertEquals(2, next.dryDays());
            Files.writeString(target, "{");
            try (var manager = CropTestSupport.manager(low, high)) {
                listener.apply(listener.prepare(manager, null), manager, null);
            }
            assertNull(listener.get(saved.crop()));
            assertEquals(saved, CropLifecycle.newDay(saved, listener.get(saved.crop()), harvestfestival.calendar.Season.SPRING, true, false));
            assertFalse(CropLifecycle.canHarvest(saved, listener.get(saved.crop())));
            listener.apply(listener.prepare(fixture.manager, null), fixture.manager, null);
            assertTrue(CropLifecycle.canHarvest(saved, listener.get(saved.crop())));
            assertEquals(4, saved.harvests());
            assertEquals(10, saved.stage());
        }
    }
    static String bundled() throws Exception {
        try (var stream = CropContent.class.getResourceAsStream("/data/harvestfestival/crops/turnip.json")) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static Map.Entry<Identifier, Resource> resource(String name, String text) {
        return Map.entry(Identifier.parse("harvestfestival:crops/" + name + ".json"),
                new Resource(null, () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
    }
    @Test void invalidDefinitionsAreIsolatedWithResourceDiagnostics() throws Exception {
        var valid = bundled();
        var invalid = List.of("{", "null", "{}", valid.replace("[2, 4, 5]", "[2, 2, 5]"),
                valid.replace("[2, 4, 5]", "[2, 4, 5.5]"), valid.replace("[2, 4, 5]", "[]"),
                valid.replace("\"spring\"", "\"summerish\""), valid.replace("[\"spring\"]", "[]"),
                valid.replace("\"planting_radius\": 1", "\"planting_radius\": 2"),
                valid.replace("\"drought_limit\": 2", "\"drought_limit\": -1"),
                valid.replace("\"requires_water\": true", "\"requires_water\": \"true\""));
        for (var json : invalid) {
            var warnings = new ArrayList<String>();
            var definitions = CropDefinitions.read(Map.ofEntries(resource("valid", valid), resource("invalid", json)), warnings::add);
            assertEquals(1, definitions.size(), json);
            assertEquals(1, warnings.size(), json);
            assertTrue(warnings.getFirst().contains("invalid.json"));
        }
    }
    @Test void dataCanDefineLongGrowthWithoutNewBlockOrProperty() throws Exception {
        var definition = CropDefinitions.read(Map.ofEntries(resource("long_crop", bundled().replace("[2, 4, 5]", "[20, 40, 50]"))),
                message -> fail(message)).values().iterator().next();
        var crop = CropState.planted(definition.id());
        for (int i = 0; i < 49; i++) crop = CropLifecycle.newDay(crop, definition, harvestfestival.calendar.Season.SPRING, true, false);
        assertEquals(50, crop.stage());
        assertTrue(CropLifecycle.canHarvest(crop, definition));
        assertNull(CropContent.CROP.get().getStateDefinition().getProperty("stage"));
    }
    @Test void realPacksOverrideInvalidWinnerDoesNotFallbackAndReloadRestores() throws Exception {
        var low = directory.resolve("low");
        try (var fixture = new CropTestSupport(low)) {
            var high = directory.resolve("high");
            var target = high.resolve("data/harvestfestival/crops/turnip.json");
            Files.createDirectories(target.getParent());
            Files.writeString(target, bundled().replace("[2, 4, 5]", "[20, 40, 50]"));
            var listener = new CropDefinitions();
            try (var manager = CropTestSupport.manager(low, high)) {
                listener.apply(listener.prepare(manager, null), manager, null);
            }
            assertEquals(50, listener.get(Identifier.parse("harvestfestival:turnip")).maturity());
            Files.writeString(target, "{");
            try (var manager = CropTestSupport.manager(low, high)) {
                listener.apply(listener.prepare(manager, null), manager, null);
            }
            assertNull(listener.get(Identifier.parse("harvestfestival:turnip")));
            listener.apply(listener.prepare(fixture.manager, null), fixture.manager, null);
            assertEquals(5, listener.get(Identifier.parse("harvestfestival:turnip")).maturity());
            listener.apply(Map.of(), null, null);
            assertNull(listener.get(Identifier.parse("harvestfestival:turnip")));
        }
    }
    @Test void missingLootDisablesRuleAndAmbiguousSeedTagsRefusePlanting() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var definitions = CropDefinitions.read(Map.ofEntries(resource("turnip", bundled()), resource("other", bundled())), message -> fail(message));
            CropDefinitions.INSTANCE.apply(definitions, null, null);
            assertNull(CropDefinitions.INSTANCE.forSeed(new ItemStack(CropContent.TURNIP_SEEDS.get())));
            Files.writeString(directory.resolve("data/harvestfestival/crops/turnip.json"), bundled().replace("harvestfestival:crops/turnip", "test:missing"));
            try (var manager = CropTestSupport.manager(directory)) {
                assertNull(new CropDefinitions().prepare(manager, null).get(Identifier.parse("harvestfestival:turnip")));
            }
        }
    }
}

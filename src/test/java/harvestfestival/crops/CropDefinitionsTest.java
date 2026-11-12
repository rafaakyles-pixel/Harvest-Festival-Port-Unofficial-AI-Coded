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
                assertTrue(new CropDefinitions().prepare(manager, null).isEmpty());
            }
        }
    }
}

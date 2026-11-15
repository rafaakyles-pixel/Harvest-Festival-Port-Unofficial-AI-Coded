package harvestfestival.tools;

import com.google.gson.JsonParser;
import harvestfestival.crops.CropTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ToolEffortSettingsTest {
    @TempDir Path directory;
    static String bundled() throws Exception {
        try (var stream = ToolEffort.class.getResourceAsStream("/data/harvestfestival/tool_settings/effort.json")) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    @Test void originalDefaultsAndIndependentFlags() throws Exception {
        var json = JsonParser.parseString(bundled()).getAsJsonObject();
        var rule = ToolEffortSettings.parse(json);
        assertEquals(4F/27F*3F, rule.exhaustion());
        assertTrue(rule.consumeHunger() && rule.restoreOnSleep() && rule.restoreOnFaint() && rule.fainting() && rule.instantSleep() && rule.sleepAnytime());
        assertFalse(rule.deathFainting() || rule.earlyFainting() || rule.attackFainting() || rule.blockFainting() || rule.sleepOnlyAtNight());
        json.addProperty("enable_fainting", false);
        rule = ToolEffortSettings.parse(json);
        assertFalse(rule.fainting()); assertTrue(rule.restoreOnFaint());
        json.addProperty("consume_hunger", false);
        assertFalse(ToolEffortSettings.parse(json).consumeHunger());
    }
    @Test void malformedBooleansAndNonpositiveOrFractionalDivisorsAreRejected() throws Exception {
        for (String value : new String[]{"0", "-1", "1.5", "true", "null", "\"27\""}) {
            var json = JsonParser.parseString(bundled()).getAsJsonObject();
            json.add("actions_per_half_haunch", JsonParser.parseString(value));
            assertThrows(RuntimeException.class, () -> ToolEffortSettings.parse(json));
        }
        var json = JsonParser.parseString(bundled()).getAsJsonObject();
        for (String key : json.keySet()) if (!key.equals("actions_per_half_haunch")) {
            var invalid = json.deepCopy(); invalid.addProperty(key, "true");
            assertThrows(RuntimeException.class, () -> ToolEffortSettings.parse(invalid), key);
        }
    }
    @Test void winningInvalidResourceDisablesNewRulesWithoutFallbackAndCanRestore() throws Exception {
        var low = directory.resolve("low"); var high = directory.resolve("high");
        String name = "data/harvestfestival/tool_settings/effort.json";
        Files.createDirectories(low.resolve(name).getParent()); Files.createDirectories(high.resolve(name).getParent());
        Files.writeString(low.resolve(name), bundled()); Files.writeString(high.resolve(name), "{}");
        var listener = new ToolEffortSettings();
        try (var manager = CropTestSupport.manager(low, high)) {
            listener.apply(listener.prepare(manager, null), manager, null); assertNull(listener.rule());
        }
        try (var manager = CropTestSupport.manager(low)) {
            listener.apply(listener.prepare(manager, null), manager, null); assertNotNull(listener.rule());
        }
        try (var manager = CropTestSupport.manager(directory.resolve("empty"))) {
            listener.apply(listener.prepare(manager, null), manager, null); assertNull(listener.rule());
        }
    }
}

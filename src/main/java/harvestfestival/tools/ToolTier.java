package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Bundled item definitions, loaded equally on both sides before item registration (not reloadable rules). */
public enum ToolTier {
    BASIC, COPPER, SILVER, GOLD, MYSTRIL;

    private final Definition definition;
    ToolTier() { definition = Definitions.read(name().toLowerCase(Locale.ROOT)); }
    public int chargeLimit() { return ordinal(); }
    public int durability() { return definition.durability; }
    public float effort() { return definition.effort; }
    public double levelIncrease() { return definition.levelIncrease; }
    public int hoeFront() { return definition.hoeFront; }
    public int waterFront() { return definition.waterFront; }
    public int waterSides() { return definition.waterSides; }
    public int sickleFront() { return definition.sickleFront; }
    public int sickleSides() { return definition.sickleSides; }
    public static ToolTier charged(int charge) { return values()[charge]; }

    private record Definition(int durability, float effort, double levelIncrease, int hoeFront, int waterFront, int waterSides,
            int sickleFront, int sickleSides) {}
    private static final class Definitions {
        private static final JsonObject DATA = load();
        private static JsonObject load() {
            try (var stream = ToolTier.class.getResourceAsStream("/data/harvestfestival/tool_settings/tiers.json")) {
                if (stream == null) throw new IllegalStateException("Missing bundled tool tiers");
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            } catch (Exception failure) { throw new IllegalStateException("Invalid bundled tool tiers", failure); }
        }
        private static Definition read(String name) {
            var json = DATA.getAsJsonObject(name);
            var result = new Definition(json.get("durability").getAsInt(), json.get("effort").getAsFloat(),
                    json.get("level_increase").getAsDouble(), json.get("hoe_front").getAsInt(),
                    json.get("water_front").getAsInt(), json.get("water_sides").getAsInt(),
                    json.get("sickle_front").getAsInt(), json.get("sickle_sides").getAsInt());
            if (result.durability <= 0 || !Float.isFinite(result.effort) || result.effort <= 0
                    || !Double.isFinite(result.levelIncrease) || result.levelIncrease <= 0
                    || result.hoeFront < 0 || result.waterFront < 0 || result.waterSides < 0
                    || result.sickleFront < 0 || result.sickleSides < 0)
                throw new IllegalStateException("Invalid bundled tier " + name);
            return result;
        }
    }
}

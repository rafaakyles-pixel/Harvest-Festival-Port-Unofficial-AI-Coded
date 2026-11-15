package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Original hunger configuration, supplied by data; invalid winning resources disable tasks. */
public final class SickleRules extends SimplePreparableReloadListener<SickleRules.Rule> {
    public record Rule(boolean consumeHunger, int actionsPerHalfHaunch) {
        public Rule { if (actionsPerHalfHaunch <= 0) throw new IllegalArgumentException("actions_per_half_haunch must be positive"); }
        public float exhaustion() { return 4F / actionsPerHalfHaunch * 3F; }
    }
    public static final SickleRules INSTANCE = new SickleRules();
    public static final Identifier RESOURCE = Identifier.parse("harvestfestival:tool_settings/sickle.json");
    private volatile Rule active;
    public Rule rule() { return active; }
    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.parse("harvestfestival:sickle"), INSTANCE);
    }
    @Override protected Rule prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (var reader = manager.getResource(RESOURCE).orElseThrow().openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var consume = json.get("consume_hunger");
            var actions = json.get("actions_per_half_haunch");
            if (!consume.isJsonPrimitive() || !consume.getAsJsonPrimitive().isBoolean()
                    || !actions.isJsonPrimitive() || !actions.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("invalid setting type");
            return new Rule(consume.getAsBoolean(), Integer.parseInt(actions.getAsString()));
        } catch (Exception invalid) {
            LogUtils.getLogger().warn("Ignoring {}: {}", RESOURCE, invalid.toString());
            return null;
        }
    }
    @Override protected void apply(Rule rule, ResourceManager manager, ProfilerFiller profiler) { active = rule; }
}

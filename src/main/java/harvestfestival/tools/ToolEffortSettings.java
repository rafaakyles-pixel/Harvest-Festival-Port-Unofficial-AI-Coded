package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** Independent switches from HFTools/HFCore. Sickle cost remains in its existing resource. */
public final class ToolEffortSettings extends SimplePreparableReloadListener<ToolEffortSettings.Rule> {
    public record Rule(boolean consumeHunger, int actionsPerHalfHaunch, boolean restoreOnSleep,
            boolean restoreOnFaint, boolean fainting, boolean earlyFainting, boolean deathFainting,
            boolean instantSleep, boolean attackFainting, boolean blockFainting,
            boolean sleepAnytime, boolean sleepOnlyAtNight) {
        public Rule { if (actionsPerHalfHaunch <= 0) throw new IllegalArgumentException("actions_per_half_haunch must be positive"); }
        public float exhaustion() { return 4F / actionsPerHalfHaunch * 3F; }
        public boolean sleepHandlerEnabled() { return sleepAnytime || sleepOnlyAtNight; }
    }
    public static final ToolEffortSettings INSTANCE = new ToolEffortSettings();
    public static final Identifier RESOURCE = Identifier.parse("harvestfestival:tool_settings/effort.json");
    private volatile Rule active;
    public Rule rule() { return active; }
    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.parse("harvestfestival:effort"), INSTANCE);
    }
    static boolean flag(JsonObject json, String name) {
        var value = json.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException(name + " must be boolean");
        return value.getAsBoolean();
    }
    static Rule parse(JsonObject json) {
        var number = json.get("actions_per_half_haunch");
        if (number == null || !number.isJsonPrimitive() || !number.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("actions_per_half_haunch must be integer");
        return new Rule(flag(json, "consume_hunger"), Integer.parseInt(number.getAsString()),
                flag(json, "restore_hunger_on_sleep"), flag(json, "restore_hunger_on_fainting"),
                flag(json, "enable_fainting"), flag(json, "early_fainting"), flag(json, "death_fainting"),
                flag(json, "instant_fainting_sleep"), flag(json, "attack_fainting"), flag(json, "block_fainting"),
                flag(json, "sleep_anytime"), flag(json, "sleep_only_at_night"));
    }
    @Override protected Rule prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (var reader = manager.getResource(RESOURCE).orElseThrow().openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception invalid) {
            LogUtils.getLogger().warn("Ignoring {}: {}", RESOURCE, invalid.toString());
            return null;
        }
    }
    @Override protected void apply(Rule rule, ResourceManager manager, ProfilerFiller profiler) { active = rule; }
}

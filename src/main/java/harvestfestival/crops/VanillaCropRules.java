package harvestfestival.crops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class VanillaCropRules extends SimplePreparableReloadListener<VanillaCropPolicy> {
    public static final VanillaCropRules INSTANCE = new VanillaCropRules();
    public static final Identifier RESOURCE = Identifier.parse("harvestfestival:crop_settings/vanilla.json");
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private volatile VanillaCropPolicy active = VanillaCropPolicy.OPEN;

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.parse("harvestfestival:vanilla_crop_rules"), INSTANCE);
    }
    public VanillaCropPolicy active() { return active; }
    static VanillaCropPolicy read(ResourceManager manager, Consumer<String> warn) {
        var resource = manager.getResource(RESOURCE);
        if (resource.isEmpty()) {
            warn.accept("Missing vanilla crop rules; restrictions disabled");
            return VanillaCropPolicy.OPEN;
        }
        try (var reader = resource.get().openAsReader()) {
            var json = JSON.fromJson(reader, JsonObject.class);
            return new VanillaCropPolicy(bool(json, "disable_planting"), bool(json, "disable_growth"),
                    bool(json, "disable_drops"), bool(json, "disable_grass_seeds"));
        } catch (IOException | RuntimeException failure) {
            warn.accept("Invalid vanilla crop rules; restrictions disabled: " + failure.getMessage());
            return VanillaCropPolicy.OPEN;
        }
    }
    private static boolean bool(JsonObject json, String field) {
        var value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("expected boolean " + field);
        return value.getAsBoolean();
    }
    @Override protected VanillaCropPolicy prepare(ResourceManager manager, ProfilerFiller profiler) {
        return read(manager, message -> LogUtils.getLogger().warn(message));
    }
    @Override protected void apply(VanillaCropPolicy policy, ResourceManager manager, ProfilerFiller profiler) { active = policy; }
}

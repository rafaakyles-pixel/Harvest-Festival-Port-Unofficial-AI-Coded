package harvestfestival.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.mojang.logging.LogUtils;
import harvestfestival.HarvestFestival;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class ToolActionRules extends SimplePreparableReloadListener<List<ToolActionRules.Rule>> {
    static final ToolActionRules INSTANCE = new ToolActionRules();
    private static final String DIRECTORY = "tool_actions";
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    static final Consumer<String> WARN = message -> LogUtils.getLogger().warn(message);
    private volatile List<Rule> active = List.of();

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(HarvestFestival.MOD_ID, DIRECTORY), INSTANCE);
    }

    @Override
    protected List<Rule> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return read(manager.listResources(DIRECTORY, id -> id.getNamespace().equals(HarvestFestival.MOD_ID)
                && id.getPath().endsWith(".json")), WARN);
    }

    static List<Rule> read(Map<Identifier, Resource> resources, Consumer<String> warn) {
        var rules = new ArrayList<Rule>();
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var json = JSON.fromJson(reader, JsonObject.class);
                var action = ToolAction.valueOf(string(json, "action").toUpperCase(Locale.ROOT));
                var tag = TagKey.create(Registries.BLOCK, Identifier.parse(string(json, "target_tag")));
                boolean air = false;
                if (json.has("requires_air_above")) {
                    var value = json.get("requires_air_above");
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                        throw new IllegalArgumentException("requires_air_above must be boolean");
                    }
                    air = value.getAsBoolean();
                }
                var output = json.getAsJsonObject("result");
                var blockId = Identifier.parse(string(output, "block"));
                var block = BuiltInRegistries.BLOCK.getOptional(blockId)
                        .orElseThrow(() -> new IllegalArgumentException("unknown block " + blockId));
                var result = block.defaultBlockState();
                for (var property : properties(output, "properties").entrySet()) {
                    result = withProperty(result, property.getKey(), property.getValue());
                }
                var required = properties(json, "requires_properties");
                // Required properties belong to the same state schema as the output.
                for (var property : required.entrySet()) withProperty(result, property.getKey(), property.getValue());
                rules.add(new Rule(entry.getKey(), action, tag, air, required, result));
            } catch (IOException | RuntimeException ex) {
                warn.accept("Ignoring tool action " + entry.getKey() + ": " + ex.getMessage());
            }
        }
        return List.copyOf(rules);
    }

    private static String string(JsonObject object, String field) {
        var value = object == null ? null : object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("expected string " + field);
        }
        return value.getAsString();
    }

    private static Map<String, String> properties(JsonObject object, String field) {
        if (!object.has(field)) return Map.of();
        var properties = object.getAsJsonObject(field);
        var result = new HashMap<String, String>();
        for (String key : properties.keySet()) result.put(key, string(properties, key));
        return Map.copyOf(result);
    }

    private static BlockState withProperty(BlockState state, String name, String value) {
        var property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalArgumentException("unknown property " + name);
        return withValue(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value)
                .orElseThrow(() -> new IllegalArgumentException("invalid property value " + value)));
    }

    @Override
    protected void apply(List<Rule> rules, ResourceManager manager, ProfilerFiller profiler) {
        active = List.copyOf(rules);
    }

    List<Rule> rules() { return active; }

    record Rule(Identifier id, ToolAction action, TagKey<Block> targetTag, boolean requiresAir,
            Map<String, String> required, BlockState result) {
        boolean matches(ToolAction requested, ToolActions.Target target) {
            if (requested != action || !target.matches(targetTag)) return false;
            for (var entry : required.entrySet()) {
                var property = target.state().getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null || !valueName(target.state(), property).equals(entry.getValue())) return false;
            }
            return true;
        }

        private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
            return property.getName(state.getValue(property));
        }
    }
}

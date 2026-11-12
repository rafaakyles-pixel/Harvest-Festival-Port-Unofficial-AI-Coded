package harvestfestival.crops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.mojang.logging.LogUtils;
import harvestfestival.calendar.Season;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class CropDefinitions extends SimplePreparableReloadListener<Map<Identifier, CropDefinition>> {
    public static final CropDefinitions INSTANCE = new CropDefinitions();
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private volatile Map<Identifier, CropDefinition> active = Map.of();

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.parse("harvestfestival:crops"), INSTANCE);
    }

    @Override protected Map<Identifier, CropDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var definitions = new HashMap<>(read(manager.listResources("crops", id -> id.getPath().endsWith(".json")),
                message -> LogUtils.getLogger().warn(message)));
        definitions.values().removeIf(definition -> {
            var loot = definition.harvestLoot();
            if (manager.getResource(Identifier.fromNamespaceAndPath(loot.getNamespace(), "loot_table/" + loot.getPath() + ".json")).isPresent()) return false;
            LogUtils.getLogger().warn("Ignoring crop definition {}: missing harvest loot {}", definition.id(), loot);
            return true;
        });
        return Map.copyOf(definitions);
    }

    static Map<Identifier, CropDefinition> read(Map<Identifier, Resource> resources, Consumer<String> warn) {
        var definitions = new HashMap<Identifier, CropDefinition>();
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var json = JSON.fromJson(reader, JsonObject.class);
                var path = entry.getKey().getPath();
                var id = Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(), path.substring(6, path.length() - 5));
                var seasons = new HashSet<Season>();
                for (var value : json.getAsJsonArray("seasons")) {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("invalid season");
                    var name = value.getAsString();
                    var season = Season.valueOf(name.toUpperCase(java.util.Locale.ROOT));
                    if (!season.serializedName().equals(name)) throw new IllegalArgumentException("season must be lowercase");
                    seasons.add(season);
                }
                var stages = new ArrayList<Integer>();
                for (var value : json.getAsJsonArray("stages")) stages.add(integer(value));
                var loot = Identifier.parse(string(json, "harvest_loot"));
                definitions.put(id, new CropDefinition(id,
                        TagKey.create(Registries.ITEM, Identifier.parse(string(json, "seed_tag"))),
                        TagKey.create(Registries.BLOCK, Identifier.parse(string(json, "soil_tag"))), seasons, stages,
                        bool(json, "requires_water"), integer(json.get("drought_limit")),
                        bool(json, "dies_without_water_integrated"), bool(json, "dies_without_water_dedicated"),
                        integer(json.get("planting_radius")), loot));
            } catch (IOException | RuntimeException ex) {
                warn.accept("Ignoring crop definition " + entry.getKey() + ": " + ex.getMessage());
            }
        }
        return Map.copyOf(definitions);
    }

    private static int integer(com.google.gson.JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("expected integer");
        return Integer.parseInt(value.getAsString());
    }
    private static String string(JsonObject json, String field) {
        var value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("expected string " + field);
        return value.getAsString();
    }
    private static boolean bool(JsonObject json, String field) {
        var value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("expected boolean " + field);
        return value.getAsBoolean();
    }
    @Override protected void apply(Map<Identifier, CropDefinition> definitions, ResourceManager manager, ProfilerFiller profiler) {
        active = Map.copyOf(definitions);
    }
    public CropDefinition get(Identifier id) { return active.get(id); }
    public CropDefinition forSeed(ItemStack stack) {
        CropDefinition match = null;
        for (var definition : active.values()) {
            if (!stack.is(definition.seedTag())) continue;
            if (match != null) {
                LogUtils.getLogger().warn("Ambiguous crop seed tags for {}: {} and {}", stack, match.id(), definition.id());
                return null;
            }
            match = definition;
        }
        return match;
    }
}

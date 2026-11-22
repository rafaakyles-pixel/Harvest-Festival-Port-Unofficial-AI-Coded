package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class TreeDefinitions extends SimplePreparableReloadListener<TreeDefinitions.Data> {
    public static final TreeDefinitions INSTANCE = new TreeDefinitions();
    public record Data(Map<String, TreeDefinition> trees, OrchardSettings settings) {}
    private volatile Data active = new Data(Map.of(), OrchardSettings.DEFAULT);
    public static void register(AddServerReloadListenersEvent event) { event.addListener(Identifier.parse("harvestfestival:orchard"), INSTANCE); }
    public TreeDefinition get(String species) { return active.trees().get(species); }
    public OrchardSettings settings() { return active.settings(); }
    @Override protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
        var trees = new HashMap<String, TreeDefinition>();
        for (var entry : manager.listResources("fruit_trees", id -> id.getNamespace().equals("harvestfestival") && id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                String name = entry.getKey().getPath().substring(12, entry.getKey().getPath().length() - 5);
                var definition = TreeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
                if (!OrchardContent.TREES.containsKey(name)) throw new IllegalArgumentException("unregistered tree " + name);
                trees.put(name, definition);
            } catch (Exception ex) { LogUtils.getLogger().warn("Ignoring tree {}: {}", entry.getKey(), ex.getMessage()); }
        }
        var settings = OrchardSettings.DEFAULT;
        var resource = manager.getResource(Identifier.parse("harvestfestival:orchard/settings.json"));
        if (resource.isPresent()) try (var reader = resource.get().openAsReader()) {
            settings = OrchardSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
        } catch (Exception ex) { LogUtils.getLogger().warn("Invalid orchard settings; using defaults: {}", ex.getMessage()); }
        return new Data(Map.copyOf(trees), settings);
    }
    @Override protected void apply(Data data, ResourceManager manager, ProfilerFiller profiler) { active = data; }
}

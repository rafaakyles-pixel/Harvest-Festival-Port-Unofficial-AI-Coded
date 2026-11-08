package harvestfestival.calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import harvestfestival.HarvestFestival;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class NpcBirthdays extends SimplePreparableReloadListener<Map<Identifier, BirthdayDate>> {
    public static final NpcBirthdays INSTANCE = new NpcBirthdays();
    private static final String DIRECTORY = "birthdays";
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private volatile Map<Identifier, BirthdayDate> active = Map.of();

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(HarvestFestival.MOD_ID, DIRECTORY), INSTANCE);
    }

    @Override
    protected Map<Identifier, BirthdayDate> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return read(manager.listResources(DIRECTORY, id -> id.getNamespace().equals(HarvestFestival.MOD_ID)
                && id.getPath().endsWith(".json")), message -> LogUtils.getLogger().warn(message));
    }

    static Map<Identifier, BirthdayDate> read(Map<Identifier, Resource> resources, Consumer<String> warn) {
        Map<Identifier, BirthdayDate> dates = new HashMap<>();
        for (var entry : resources.entrySet()) {
            var path = entry.getKey().getPath();
            try (var reader = entry.getValue().openAsReader()) {
                String[] parts = path.substring(DIRECTORY.length() + 1, path.length() - 5).split("/", 2);
                if (parts.length != 2) throw new IllegalArgumentException("expected namespace/path");
                var target = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
                var rule = JSON.fromJson(reader, JsonObject.class);
                var day = rule == null ? null : rule.get("day");
                if (day == null || !day.isJsonPrimitive() || !day.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException("expected integer day");
                }
                // Reject fractions and numeric strings rather than allowing codec coercion.
                Integer.parseInt(day.getAsString());
                dates.put(target, BirthdayDate.CODEC.parse(JsonOps.INSTANCE, rule).getOrThrow());
            } catch (IOException | RuntimeException ex) {
                warn.accept("Ignoring NPC birthday " + entry.getKey() + ": " + ex.getMessage());
            }
        }
        return Map.copyOf(dates);
    }

    @Override
    protected void apply(Map<Identifier, BirthdayDate> dates, ResourceManager manager, ProfilerFiller profiler) {
        active = Map.copyOf(dates);
    }

    public Optional<BirthdayDate> get(Identifier id) { return Optional.ofNullable(active.get(id)); }

    public boolean isBirthday(Identifier id, CalendarDate date) {
        return get(id).map(birthday -> birthday.matches(date)).orElse(false);
    }
}

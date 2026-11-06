package harvestfestival.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.mojang.logging.LogUtils;
import harvestfestival.HarvestFestival;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class ShippingPrices extends SimplePreparableReloadListener<ShippingPrices.Table> {
    static final ShippingPrices INSTANCE = new ShippingPrices();
    private static final String DIRECTORY = "shipping_prices";
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final Consumer<String> WARN = message -> LogUtils.getLogger().warn(message);
    private volatile Table active = new Table(Map.of(), Map.of());

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(HarvestFestival.MOD_ID, DIRECTORY), INSTANCE);
    }

    @Override
    protected Table prepare(ResourceManager manager, ProfilerFiller profiler) {
        return read(manager.listResources(DIRECTORY, id -> id.getNamespace().equals(HarvestFestival.MOD_ID)
                && id.getPath().endsWith(".json")), WARN);
    }

    static Table read(Map<Identifier, Resource> resources, Consumer<String> warn) {
        Map<Item, Long> items = new HashMap<>();
        Map<TagKey<Item>, TagPrice> tags = new HashMap<>();
        for (var entry : resources.entrySet()) {
            var path = entry.getKey().getPath();
            try (var reader = entry.getValue().openAsReader()) {
                String[] parts = path.substring(DIRECTORY.length() + 1, path.length() - 5).split("/", 3);
                if (parts.length != 3) throw new IllegalArgumentException("expected kind/namespace/path");
                var target = Identifier.fromNamespaceAndPath(parts[1], parts[2]);
                var rule = JSON.fromJson(reader, JsonObject.class);
                long price = Long.parseLong(integer(rule, "price"));
                if (price <= 0) throw new IllegalArgumentException("price must be positive");
                switch (parts[0]) {
                    case "items" -> {
                        var item = BuiltInRegistries.ITEM.getOptional(target)
                                .orElseThrow(() -> new IllegalArgumentException("unknown item " + target));
                        items.put(item, price);
                    }
                    case "tags" -> tags.put(TagKey.create(Registries.ITEM, target),
                            new TagPrice(price, Integer.parseInt(integer(rule, "priority"))));
                    default -> throw new IllegalArgumentException("expected items or tags");
                }
            } catch (IOException | RuntimeException ex) {
                warn.accept("Ignoring shipping price " + entry.getKey() + ": " + ex.getMessage());
            }
        }
        return new Table(Map.copyOf(items), Map.copyOf(tags));
    }

    private static String integer(JsonObject rule, String field) {
        var value = rule == null ? null : rule.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("expected integer " + field);
        }
        return value.getAsString();
    }

    @Override
    protected void apply(Table prices, ResourceManager manager, ProfilerFiller profiler) {
        active = prices;
    }

    long price(ItemStack stack) {
        return active.price(stack, WARN);
    }

    private record TagPrice(long price, int priority) {}

    static final class Table {
        private final Map<Item, Long> items;
        private final Map<TagKey<Item>, TagPrice> tags;
        private final Set<Item> warned = new HashSet<>();

        private Table(Map<Item, Long> items, Map<TagKey<Item>, TagPrice> tags) {
            this.items = items;
            this.tags = tags;
        }

        long price(ItemStack stack, Consumer<String> warn) {
            if (stack.isEmpty()) return 0;
            var specific = items.get(stack.getItem());
            if (specific != null) return specific;
            TagPrice best = null;
            var winners = new TreeSet<Identifier>();
            for (var entry : tags.entrySet()) {
                var candidate = entry.getValue();
                if (!stack.typeHolder().is(entry.getKey())) continue;
                if (best != null && candidate.priority < best.priority) continue;
                if (best == null || candidate.priority > best.priority) {
                    winners.clear();
                    best = candidate;
                }
                winners.add(entry.getKey().location());
                if (candidate.price > best.price) best = candidate;
            }
            if (best == null) return PlayerShipping.testPrice(stack);
            if (winners.size() > 1 && warned.add(stack.getItem())) {
                warn.accept("Tied shipping tags " + winners + " for " + stack.typeHolder()
                        + " at priority " + best.priority + "; using " + best.price);
            }
            return best.price;
        }
    }
}

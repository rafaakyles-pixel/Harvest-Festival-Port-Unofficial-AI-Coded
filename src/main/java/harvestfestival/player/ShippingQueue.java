package harvestfestival.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.ExtraCodecs;

/** Immutable snapshots: changing a held stack cannot change an accepted shipment. */
public record ShippingQueue(List<Entry> entries) {
    public static final ShippingQueue EMPTY = new ShippingQueue(List.of());
    public static final Codec<ShippingQueue> CODEC = Entry.CODEC.listOf()
            .xmap(ShippingQueue::new, ShippingQueue::entries);

    public ShippingQueue {
        entries = List.copyOf(entries);
    }

    public ShippingQueue append(ItemStack stack, long value) {
        var updated = new ArrayList<>(entries);
        updated.add(new Entry(stack, value));
        return new ShippingQueue(updated);
    }

    public long total() {
        long total = 0;
        for (var entry : entries) total += entry.value();
        return total;
    }

    public record Entry(ItemStack stack, long value) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack),
                ExtraCodecs.POSITIVE_LONG.fieldOf("value").forGetter(Entry::value)
        ).apply(instance, Entry::new));

        public Entry {
            if (stack.isEmpty() || value <= 0) throw new IllegalArgumentException("Invalid shipment");
            stack = stack.copyWithCount(1);
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}

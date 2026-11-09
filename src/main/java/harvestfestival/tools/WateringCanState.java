package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.math.BigDecimal;
import net.minecraft.world.item.ItemStack;

public record WateringCanState(int water) {
    public static final int CAPACITY = 128;
    public static final WateringCanState EMPTY = new WateringCanState(0);
    public static final WateringCanState FULL = new WateringCanState(CAPACITY);
    // Tolerant decoding preserves the item even if its water payload is corrupt.
    public static final Codec<WateringCanState> CODEC = Codec.PASSTHROUGH.xmap(
            WateringCanState::decode, state -> {
                var json = new JsonObject();
                json.addProperty("water", state.water);
                return new Dynamic<>(JsonOps.INSTANCE, json);
            });

    public WateringCanState {
        if (water < 0 || water > CAPACITY) throw new IllegalArgumentException("water outside 0..128");
    }

    private static WateringCanState decode(Dynamic<?> value) {
        try {
            var number = value.get("water").asNumber().result().orElseThrow();
            return new WateringCanState(new BigDecimal(number.toString()).intValueExact());
        } catch (RuntimeException invalid) {
            return EMPTY;
        }
    }

    public static WateringCanState get(ItemStack stack) {
        return stack.getOrDefault(ToolContent.WATER_STATE.get(), EMPTY);
    }

    public static boolean fill(ItemStack stack) {
        if (stack.isEmpty() || get(stack).water == CAPACITY) return false;
        stack.set(ToolContent.WATER_STATE.get(), FULL);
        return true;
    }

    public WateringCanState consume() {
        return water == 0 ? this : new WateringCanState(water - 1);
    }
}

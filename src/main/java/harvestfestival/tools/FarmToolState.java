package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.math.BigDecimal;
import net.minecraft.world.item.ItemStack;

/** Charge is retained after use; absent state upgrades old stacks without changing their water or damage. */
public record FarmToolState(double level, int charge, boolean invalid) {
    public static final FarmToolState NEW = new FarmToolState(0, 0, false);
    public static final FarmToolState INVALID = new FarmToolState(0, 0, true);
    public static final Codec<FarmToolState> CODEC = Codec.PASSTHROUGH.xmap(FarmToolState::decode, state -> {
        var json = new JsonObject();
        json.addProperty("level", state.level);
        json.addProperty("charge", state.charge);
        json.addProperty("invalid", state.invalid);
        return new Dynamic<>(JsonOps.INSTANCE, json);
    });
    public FarmToolState {
        if (!Double.isFinite(level) || level < 0 || level > 100 || charge < 0 || charge > 4)
            throw new IllegalArgumentException("Invalid farm tool state");
    }
    private static FarmToolState decode(Dynamic<?> value) {
        try {
            if (value.get("invalid").asBoolean(false)) return INVALID;
            return new FarmToolState(value.get("level").asNumber().result().orElseThrow().doubleValue(),
                    new BigDecimal(value.get("charge").asNumber().result().orElseThrow().toString()).intValueExact(), false);
        } catch (RuntimeException failure) { return INVALID; }
    }
    public static FarmToolState get(ItemStack stack) { return stack.getOrDefault(ToolContent.FARM_TOOL_STATE.get(), NEW); }
    public boolean validFor(ToolTier tier) { return !invalid && charge <= tier.chargeLimit(); }
    public FarmToolState advance(ToolTier tier) { return new FarmToolState(Math.min(100, level + tier.levelIncrease()), charge, invalid); }
    public FarmToolState charge(ToolTier tier, int elapsed) {
        return validFor(tier) ? new FarmToolState(level, Math.min(tier.chargeLimit(), charge + Math.max(0, elapsed) / 20), false) : INVALID;
    }
    public FarmToolState discharge() { return invalid ? this : new FarmToolState(level, 0, false); }
}

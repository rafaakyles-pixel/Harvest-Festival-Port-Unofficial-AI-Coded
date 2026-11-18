package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;

/** Experience is independent of durability; malformed state never resets a used tool. */
public record SickleState(double level, int charge, boolean invalid) {
    public static final SickleState NEW = new SickleState(0, 0, false);
    public static final SickleState INVALID = new SickleState(0, 0, true);
    public static final double INCREASE = 0.39215682745098D;
    public static final Codec<SickleState> CODEC = Codec.PASSTHROUGH.xmap(SickleState::decode, state -> {
        var json = new JsonObject();
        json.addProperty("level", state.level);
        json.addProperty("charge", state.charge);
        json.addProperty("invalid", state.invalid);
        return new Dynamic<>(JsonOps.INSTANCE, json);
    });
    public SickleState {
        if (!Double.isFinite(level) || level < 0 || level > 100 || charge < 0 || charge > 4)
            throw new IllegalArgumentException("Invalid sickle state");
    }
    public SickleState(double level, boolean invalid) { this(level, 0, invalid); }
    private static SickleState decode(Dynamic<?> value) {
        try {
            if (value.get("invalid").asBoolean(false)) return INVALID;
            // Old sickles have no charge field; malformed present fields must not reset to zero.
            var json = value.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            int charge = !json.has("charge") ? 0 : new java.math.BigDecimal(
                    value.get("charge").asNumber().result().orElseThrow().toString()).intValueExact();
            return new SickleState(value.get("level").asNumber().result().orElseThrow().doubleValue(), charge, false);
        } catch (RuntimeException invalid) { return INVALID; }
    }
    public static SickleState get(ItemStack stack) { return stack.getOrDefault(ToolContent.SICKLE_STATE.get(), NEW); }
    public boolean validFor(ToolTier tier) { return !invalid && charge <= tier.chargeLimit(); }
    public SickleState advance(ToolTier tier) { return invalid ? this : new SickleState(Math.min(100, level + tier.levelIncrease()), charge, false); }
    public SickleState advance() { return advance(ToolTier.BASIC); }
    public SickleState charge(ToolTier tier, int elapsed) {
        return validFor(tier) ? new SickleState(level, Math.min(tier.chargeLimit(), charge + Math.max(0, elapsed) / 20), false) : INVALID;
    }
    public SickleState discharge() { return invalid ? this : new SickleState(level, 0, false); }
}

package harvestfestival.tools;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;

/** Experience is independent of durability; malformed state never resets a used tool. */
public record SickleState(double level, boolean invalid) {
    public static final SickleState NEW = new SickleState(0, false);
    public static final SickleState INVALID = new SickleState(0, true);
    public static final double INCREASE = 0.39215682745098D;
    public static final Codec<SickleState> CODEC = Codec.PASSTHROUGH.xmap(SickleState::decode, state -> {
        var json = new JsonObject();
        json.addProperty("level", state.level);
        json.addProperty("invalid", state.invalid);
        return new Dynamic<>(JsonOps.INSTANCE, json);
    });
    public SickleState {
        if (!Double.isFinite(level) || level < 0 || level > 100) throw new IllegalArgumentException("level outside 0..100");
    }
    private static SickleState decode(Dynamic<?> value) {
        try {
            if (value.get("invalid").asBoolean(false)) return INVALID;
            return new SickleState(value.get("level").asNumber().result().orElseThrow().doubleValue(), false);
        } catch (RuntimeException invalid) { return INVALID; }
    }
    public static SickleState get(ItemStack stack) { return stack.getOrDefault(ToolContent.SICKLE_STATE.get(), NEW); }
    public SickleState advance() { return invalid ? this : new SickleState(Math.min(100, level + INCREASE), false); }
}

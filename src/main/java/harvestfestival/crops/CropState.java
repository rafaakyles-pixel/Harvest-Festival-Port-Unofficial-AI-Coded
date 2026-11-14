package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CropState(Identifier crop, int stage, int dryDays, boolean dead, int harvests) {
    public CropState(Identifier crop, int stage, int dryDays, boolean dead) {
        this(crop, stage, dryDays, dead, 0);
    }
    public CropState {
        if (crop == null || stage < 1 || stage > 1_000_000 || dryDays < 0 || dryDays > 1_000_000
                || harvests < 0 || harvests > 1_000_000) throw new IllegalArgumentException("invalid crop state");
    }
    private static final Codec<Integer> INTEGER = Codec.PASSTHROUGH.comapFlatMap(value -> {
        try {
            int number = new BigDecimal(value.asNumber().result().orElseThrow().toString()).intValueExact();
            return number >= 0 && number <= 1_000_000 ? DataResult.success(number) : DataResult.error(() -> "crop counter outside bounds");
        } catch (RuntimeException invalid) { return DataResult.error(() -> "expected integral crop counter"); }
    }, number -> new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(number)));
    public static final CropState INVALID = new CropState(Identifier.parse("harvestfestival:missing"), 1, 1, true);
    private static final Codec<CropState> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("crop").forGetter(CropState::crop),
            INTEGER.validate(value -> value > 0 ? DataResult.success(value) : DataResult.error(() -> "stage must be positive")).fieldOf("stage").forGetter(CropState::stage),
            INTEGER.fieldOf("dry_days").forGetter(CropState::dryDays),
            Codec.BOOL.fieldOf("dead").forGetter(CropState::dead),
            INTEGER.optionalFieldOf("harvests", 0).forGetter(CropState::harvests)).apply(instance, CropState::new));

    // ValueInput accepts partial codec results. Never expose an optional-field fallback for corrupt counters.
    public static final Codec<CropState> CODEC = Codec.PASSTHROUGH.comapFlatMap(value -> {
        var json = value.convert(JsonOps.INSTANCE).getValue();
        if (json.isJsonObject() && json.getAsJsonObject().has("harvests")
                && json.getAsJsonObject().get("harvests").isJsonNull()) {
            return DataResult.error(() -> "harvests must be an integer, not null");
        }
        var parsed = FIELDS.parse(value);
        return parsed.error().isPresent() ? DataResult.error(() -> parsed.error().orElseThrow().message()) : parsed;
    }, state -> new Dynamic<>(JsonOps.INSTANCE, FIELDS.encodeStart(JsonOps.INSTANCE, state).getOrThrow()));

    public static CropState planted(Identifier crop) { return new CropState(crop, 1, 1, false); }
}

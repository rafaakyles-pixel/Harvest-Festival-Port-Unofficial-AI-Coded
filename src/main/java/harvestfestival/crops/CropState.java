package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CropState(Identifier crop, int stage, int dryDays, boolean dead) {
    private static final Codec<Integer> INTEGER = Codec.PASSTHROUGH.comapFlatMap(value -> {
        try {
            int number = new BigDecimal(value.asNumber().result().orElseThrow().toString()).intValueExact();
            return number >= 0 && number <= 1_000_000 ? DataResult.success(number) : DataResult.error(() -> "crop counter outside bounds");
        } catch (RuntimeException invalid) { return DataResult.error(() -> "expected integral crop counter"); }
    }, number -> new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(number)));
    public static final CropState INVALID = new CropState(Identifier.parse("harvestfestival:missing"), 1, 1, true);
    public static final Codec<CropState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("crop").forGetter(CropState::crop),
            INTEGER.validate(value -> value > 0 ? DataResult.success(value) : DataResult.error(() -> "stage must be positive")).fieldOf("stage").forGetter(CropState::stage),
            INTEGER.fieldOf("dry_days").forGetter(CropState::dryDays),
            Codec.BOOL.fieldOf("dead").forGetter(CropState::dead)).apply(instance, CropState::new));

    public static CropState planted(Identifier crop) { return new CropState(crop, 1, 1, false); }
}

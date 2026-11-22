package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record OrchardSettings(boolean daily, boolean bonemeal, boolean seasonalBonemeal, int sprinklerConsumption) {
    public static final OrchardSettings DEFAULT = new OrchardSettings(true, false, true, 0);
    public static final Codec<OrchardSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("trees_grow_daily").forGetter(OrchardSettings::daily),
            Codec.BOOL.fieldOf("enable_bonemeal").forGetter(OrchardSettings::bonemeal),
            Codec.BOOL.fieldOf("seasonal_bonemeal").forGetter(OrchardSettings::seasonalBonemeal),
            Codec.INT.fieldOf("sprinkler_daily_consumption").forGetter(OrchardSettings::sprinklerConsumption)).apply(i, OrchardSettings::new));
}

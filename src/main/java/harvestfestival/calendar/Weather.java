package harvestfestival.calendar;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum Weather implements StringRepresentable {
    SUNNY(0, 0), RAIN(100, 0), TYPHOON(20, 100), SNOW(100, 0), BLIZZARD(20, 0);

    public static final Codec<Weather> CODEC = StringRepresentable.fromEnum(Weather::values);
    private final int rain;
    private final int storm;
    Weather(int rain, int storm) { this.rain = rain; this.storm = storm; }
    public int rain() { return rain; }
    public int storm() { return storm; }
    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }

    // Preserve the original integer comparison and asymmetric storm decay, including float rounding.
    public static float approachRain(float current, int target) {
        int strength = (int) (current * 100);
        return strength > target ? current - 0.01F : strength < target ? current + 0.01F : current;
    }
    public static float approachStorm(float current, int target) {
        int strength = (int) (current * 100);
        return strength > target ? current - 0.1F : strength < target ? current + 0.01F : current;
    }
}

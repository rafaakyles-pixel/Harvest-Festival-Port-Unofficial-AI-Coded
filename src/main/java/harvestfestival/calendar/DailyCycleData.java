package harvestfestival.calendar;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Identity of observed transitions, NOT a clock or a count of elapsed days. */
public final class DailyCycleData extends SavedData {
    // Corrupt markers fail closed instead of replaying previously processed growth.
    public static final Codec<Long> MARKER_CODEC = Codec.PASSTHROUGH.xmap(DailyCycleData::decode,
            value -> new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(value)));
    public static final Codec<DailyCycleData> CODEC = Codec.PASSTHROUGH.xmap(
            value -> new DailyCycleData(value.get("sequence").result().map(DailyCycleData::decode).orElse(Long.MAX_VALUE)),
            data -> {
                var json = new JsonObject();
                json.addProperty("sequence", data.sequence);
                return new Dynamic<>(JsonOps.INSTANCE, json);
            });
    public static final SavedDataType<DailyCycleData> TYPE = new SavedDataType<>(
            Identifier.parse("harvestfestival:daily_cycles"), () -> new DailyCycleData(0), CODEC);
    private long sequence;
    public DailyCycleData(long sequence) { this.sequence = sequence; }
    public long sequence() { return sequence; }
    private static long decode(Dynamic<?> value) {
        try {
            long number = new BigDecimal(value.asNumber().result().orElseThrow().toString()).longValueExact();
            if (number < 0) throw new IllegalArgumentException("negative cycle");
            return number;
        } catch (RuntimeException invalid) { return Long.MAX_VALUE; }
    }
    public long next() {
        if (sequence == Long.MAX_VALUE) {
            LogUtils.getLogger().error("Invalid or exhausted Harvest Festival daily sequence; crop updates disabled, calendar and shipping remain active");
            return 0;
        }
        sequence++;
        setDirty();
        return sequence;
    }
}

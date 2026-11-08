package harvestfestival.calendar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** An annual date, independent of the year in the world calendar. */
public record BirthdayDate(Season season, int day) {
    private static final Codec<Season> SEASON_CODEC = Codec.STRING.comapFlatMap(name -> {
        for (Season season : Season.values()) {
            if (season.serializedName().equals(name)) return DataResult.success(season);
        }
        return DataResult.error(() -> "Unknown season: " + name);
    }, Season::serializedName);
    public static final Codec<BirthdayDate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SEASON_CODEC.fieldOf("season").forGetter(BirthdayDate::season),
            Codec.intRange(1, CalendarDate.DAYS_PER_SEASON).fieldOf("day").forGetter(BirthdayDate::day)
    ).apply(instance, BirthdayDate::new));
    public static final StreamCodec<ByteBuf, BirthdayDate> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public BirthdayDate {
        CalendarDate.of(day, season, 1);
    }

    public boolean matches(CalendarDate date) {
        return season == date.season() && day == date.day();
    }
}

package harvestfestival.calendar;

import harvestfestival.HarvestFestival;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Transient, player-owned client mirror. Replacing the player also clears this mirror. */
public record WeatherSnapshot(String dimension, List<Weather> forecast, Weather recent, float rain, float storm, boolean snowFog)
        implements CustomPacketPayload {
    public WeatherSnapshot(String dimension, List<Weather> forecast, Weather recent, float rain, float storm) {
        this(dimension, forecast, recent, rain, storm, true);
    }
    public WeatherSnapshot { forecast = List.copyOf(forecast); }
    public static final WeatherSnapshot EMPTY = new WeatherSnapshot("", DailyWeatherData.INITIAL, Weather.SUNNY, 0, 0);
    public static final Type<WeatherSnapshot> TYPE = new Type<>(Identifier.parse("harvestfestival:weather"));
    public static final StreamCodec<ByteBuf, WeatherSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WeatherSnapshot::dimension,
            ByteBufCodecs.fromCodec(DailyWeatherData.FORECAST_CODEC), WeatherSnapshot::forecast,
            ByteBufCodecs.fromCodec(Weather.CODEC), WeatherSnapshot::recent,
            ByteBufCodecs.FLOAT, WeatherSnapshot::rain,
            ByteBufCodecs.FLOAT, WeatherSnapshot::storm,
            ByteBufCodecs.BOOL, WeatherSnapshot::snowFog, WeatherSnapshot::new);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HarvestFestival.MOD_ID);
    public static final Supplier<AttachmentType<WeatherSnapshot>> VIEW = ATTACHMENTS.register("weather_view",
            () -> AttachmentType.builder(() -> EMPTY).build());
    @Override public Type<WeatherSnapshot> type() { return TYPE; }
    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        bus.addListener(WeatherSnapshot::payloads);
    }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, STREAM_CODEC, (snapshot, context) -> {
            var player = context.player();
            var level = player.level();
            if (!level.dimension().identifier().toString().equals(snapshot.dimension())) return;
            player.setData(VIEW, snapshot);
            if (DailyWeather.controls(level)) {
                // Modern setters clamp negative storm decay; original provider does not.
                level.oRainLevel = level.rainLevel = snapshot.rain();
                level.oThunderLevel = level.thunderLevel = snapshot.storm();
            }
        });
    }
}

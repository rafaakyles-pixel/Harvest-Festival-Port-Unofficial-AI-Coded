package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client presentation only. Templates and authoritative settings stay on the server. */
public final class OrchardSync {
    public record Info(int maturity, int maximum, int radius, String season) {
        public static final Codec<Info> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("maturity").forGetter(Info::maturity), Codec.INT.fieldOf("maximum").forGetter(Info::maximum),
                Codec.INT.fieldOf("radius").forGetter(Info::radius), Codec.STRING.fieldOf("season").forGetter(Info::season)).apply(i, Info::new));
    }
    public record Snapshot(Map<String, Info> trees, boolean bonemeal) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(Identifier.parse("harvestfestival:orchard"));
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Info.CODEC).fieldOf("trees").forGetter(Snapshot::trees),
                Codec.BOOL.fieldOf("bonemeal").forGetter(Snapshot::bonemeal)).apply(i, Snapshot::new));
        public Snapshot { trees = Map.copyOf(trees); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static volatile Snapshot client = new Snapshot(Map.of(), false);
    public static Snapshot client() { return client; }
    public static void clearClient() { client = new Snapshot(Map.of(), false); }
    public static void register(IEventBus bus) { bus.addListener(OrchardSync::payload); NeoForge.EVENT_BUS.addListener(OrchardSync::sync); }
    private static void payload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Snapshot.TYPE, ByteBufCodecs.fromCodec(Snapshot.CODEC),
                (message, context) -> context.enqueueWork(() -> client = message));
    }
    private static void sync(OnDatapackSyncEvent event) {
        var definitions = TreeDefinitions.INSTANCE;
        var trees = OrchardContent.TREES.keySet().stream().filter(s -> definitions.get(s) != null).collect(Collectors.toMap(s -> s, s -> {
            var d = definitions.get(s); return new Info(d.maturity(), d.maximum(), d.radius(), d.season());
        }));
        var snapshot = new Snapshot(trees, definitions.settings().bonemeal());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, snapshot));
    }
    private OrchardSync() {}
}

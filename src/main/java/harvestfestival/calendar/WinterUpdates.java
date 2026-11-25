package harvestfestival.calendar;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Original SnowLoader: registration on seasonal formation and disk load; work only on new days. */
public final class WinterUpdates {
    private static final Map<ServerLevel, Map<net.minecraft.world.level.ChunkPos, Set<BlockPos>>> TRACKED = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Set<LevelChunk>> LOADED = new ConcurrentHashMap<>();
    private static final Set<LevelChunk> PENDING = ConcurrentHashMap.newKeySet();
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "harvestfestival");
    public static final Supplier<AttachmentType<Long>> LAST_CYCLE = ATTACHMENTS.register("winter_cycle",
            () -> AttachmentType.builder(() -> 0L).serialize(DailyCycleData.MARKER_CODEC.optionalFieldOf("last_cycle", Long.MAX_VALUE)).build());
    private WinterUpdates() {}
    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        NeoForge.EVENT_BUS.addListener(WinterUpdates::read);
        NeoForge.EVENT_BUS.addListener(WinterUpdates::load);
        NeoForge.EVENT_BUS.addListener(WinterUpdates::unload);
        NeoForge.EVENT_BUS.addListener(WinterUpdates::tick);
        NeoForge.EVENT_BUS.addListener(WinterUpdates::stop);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, WinterUpdates::newDay);
    }
    public static void track(ServerLevel level, BlockPos pos) {
        TRACKED.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4),
                        ignored -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }
    private static void read(ChunkDataEvent.Load event) {
        var chunk = event.getChunk() instanceof ImposterProtoChunk wrapped ? wrapped.getWrapped()
                : event.getChunk() instanceof LevelChunk full ? full : null;
        if (chunk != null && chunk.getLevel() instanceof ServerLevel) PENDING.add(chunk);
    }
    private static void load(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level)
            LOADED.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(event.getChunk());
    }
    private static void unload(ChunkEvent.Unload event) {
        PENDING.remove(event.getChunk());
        if (event.getLevel() instanceof ServerLevel level) {
            var chunks = LOADED.get(level);
            if (chunks != null) chunks.remove(event.getChunk());
        }
    }
    private static boolean ready(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        return level.getChunkSource().getChunkNow(pos.x(), pos.z()) == chunk;
    }
    private static void importChunk(ServerLevel level, LevelChunk chunk) {
        if (!PENDING.remove(chunk)) return;
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (!section.maybeHas(WinterWeather::tracked)) continue;
            int base = chunk.getSectionYFromSectionIndex(i) * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!WinterWeather.tracked(section.getBlockState(x, y, z))) continue;
                var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, base + y, chunk.getPos().getMinBlockZ() + z);
                if (level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) != Biome.Precipitation.SNOW) track(level, pos);
            }
        }
    }
    private static void tick(ServerTickEvent.Pre event) {
        LOADED.forEach((level, chunks) -> {
            if (level.getServer() == event.getServer())
                chunks.forEach(chunk -> { if (ready(level, chunk)) importChunk(level, chunk); });
        });
    }
    public static boolean eligible(long cycle, long last) { return cycle > 0 && last < cycle; }
    private static void newDay(NewDayEvent event) {
        LOADED.forEach((level, chunks) -> {
            if (level.getServer() != event.getServer()) return;
            var weather = DailyWeather.get(event.getServer()).today();
            for (var chunk : chunks) {
                if (!ready(level, chunk)) continue;
                importChunk(level, chunk);
                if (!eligible(event.getCycle(), chunk.getData(LAST_CYCLE))) continue;
                var dimension = TRACKED.get(level);
                var tracked = dimension == null ? null : dimension.get(chunk.getPos());
                if (tracked != null) tracked.removeIf(pos -> {
                    var state = level.getBlockState(pos);
                    if (!WinterWeather.tracked(state)) return true;
                    boolean snow = state.is(WinterWeather.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS);
                    if (!WinterWeather.snowy(weather) && event.getCurrentDate().season() != Season.WINTER
                            && WinterWeather.melts(weather, event.getCurrentDate().season(), level.getRandom().nextInt(3))) {
                        level.setBlock(pos, snow ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                        if (snow && level.getRandom().nextInt(32) == 0 && level.isAreaLoaded(pos.below(), 1)) {
                            var below = level.getBlockState(pos.below());
                            if (below.getBlock() instanceof BonemealableBlock plant
                                    && plant.isValidBonemealTarget(level, pos.below(), below)
                                    && plant.isBonemealSuccess(level, level.getRandom(), pos.below(), below))
                                plant.performBonemeal(level, level.getRandom(), pos.below(), below);
                        }
                    } else if (snow) {
                        int layers = state.getValue(SnowLayerBlock.LAYERS);
                        int next = WinterWeather.layers(weather, layers);
                        if (next != layers) level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, next), Block.UPDATE_CLIENTS);
                    }
                    return false;
                });
                chunk.setData(LAST_CYCLE, event.getCycle());
                chunk.markUnsaved();
            }
        });
    }
    private static void stop(ServerStoppedEvent event) {
        TRACKED.keySet().removeIf(level -> level.getServer() == event.getServer());
        PENDING.removeIf(chunk -> chunk.getLevel() instanceof ServerLevel level && level.getServer() == event.getServer());
        LOADED.keySet().removeIf(level -> level.getServer() == event.getServer());
    }
}

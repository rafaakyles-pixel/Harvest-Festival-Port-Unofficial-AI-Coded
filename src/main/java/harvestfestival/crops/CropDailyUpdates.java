package harvestfestival.crops;

import harvestfestival.calendar.DailyCycleData;
import harvestfestival.calendar.NewDayEvent;
import harvestfestival.calendar.Season;
import harvestfestival.tools.FarmlandSoil;
import harvestfestival.tools.ToolContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CropDailyUpdates {
    private static final Map<ServerLevel, Set<LevelChunk>> LOADED = new ConcurrentHashMap<>();
    private static final Set<LevelChunk> MIGRATION_PENDING = ConcurrentHashMap.newKeySet();
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "harvestfestival");
    public static final Supplier<AttachmentType<Long>> LAST_CYCLE = ATTACHMENTS.register("crop_cycle",
            () -> AttachmentType.builder(() -> 0L).serialize(DailyCycleData.MARKER_CODEC.optionalFieldOf("last_cycle", Long.MAX_VALUE)).build());
    private CropDailyUpdates() {}
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }

    public static void loaded(ChunkEvent.Load event) {
        // Load can precede FULL status. Only keep the reference here; never touch the world.
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(event.getChunk());
            MIGRATION_PENDING.add(event.getChunk());
        }
    }
    public static void unloaded(ChunkEvent.Unload event) {
        MIGRATION_PENDING.remove(event.getChunk());
        if (event.getLevel() instanceof ServerLevel level) {
            var chunks = LOADED.get(level);
            if (chunks != null) chunks.remove(event.getChunk());
        }
    }
    public static void stopped(ServerStoppedEvent event) {
        LOADED.entrySet().removeIf(entry -> {
            if (entry.getKey().getServer() != event.getServer()) return false;
            MIGRATION_PENDING.removeAll(entry.getValue());
            return true;
        });
    }

    /** Load only queues work: conversion waits for a FULL chunk on the server thread. */
    public static void migrateLoaded(ServerTickEvent.Pre event) {
        LOADED.forEach((level, chunks) -> {
            if (level.getServer() != event.getServer()) return;
            OrchardDaily.refreshIfChanged(level, chunks);
            for (var chunk : chunks) {
                var pos = chunk.getPos();
                if (MIGRATION_PENDING.contains(chunk) && level.getChunkSource().getChunkNow(pos.x(), pos.z()) == chunk) {
                    migrate(level, chunk);
                    OrchardDaily.refresh(new OrchardDaily.Work(level, chunk), harvestfestival.calendar.WorldCalendar.get(level.getServer()).season());
                    MIGRATION_PENDING.remove(chunk);
                }
            }
        });
    }

    private static void migrate(ServerLevel level, LevelChunk chunk) {
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.is(ToolContent.PREPARED_SOIL.get()) || state.is(CropContent.CROP.get()))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(index) * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                var state = section.getBlockState(x, y, z);
                var next = FarmlandSoil.migrate(state);
                if (next == state && !state.is(CropContent.CROP.get())) continue;
                var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
                // Refresh old visual caches only after FULL; never grow or reset saved crop counters here.
                if (state.is(CropContent.CROP.get()) && chunk.getBlockEntity(pos) instanceof CropBlockEntity crop) {
                    crop.setCrop(crop.crop());
                }
            }
        }
    }

    public interface ChunkWork {
        long lastCycle();
        void mark(long cycle);
        void grow();
        void dry();
    }
    /** All plants see yesterday's watering before any participating soil is dried. */
    public static void process(long cycle, List<? extends ChunkWork> chunks) {
        if (cycle <= 0) return;
        var eligible = chunks.stream().filter(chunk -> chunk.lastCycle() < cycle).toList();
        for (var chunk : eligible) chunk.grow();
        for (var chunk : eligible) { chunk.dry(); chunk.mark(cycle); }
    }

    public static void newDay(NewDayEvent event) {
        var work = new ArrayList<ChunkWork>();
        var orchard = new ArrayList<OrchardDaily.Work>();
        LOADED.forEach((level, chunks) -> {
            if (level.getServer() != event.getServer()) return;
            for (var chunk : chunks) {
                var pos = chunk.getPos();
                // Non-loading lookup; freshly announced or unloaded chunks cannot participate.
                if (level.getChunkSource().getChunkNow(pos.x(), pos.z()) == chunk) {
                    if (MIGRATION_PENDING.remove(chunk)) migrate(level, chunk);
                    work.add(new WorldChunk(level, chunk, seasonForDimension(level.dimension(), event.getCurrentDate().season())));
                    orchard.add(new OrchardDaily.Work(level, chunk));
                }
            }
        });
        OrchardDaily.grow(event, orchard);
        process(event.getCycle(), work);
        OrchardDaily.post(event, orchard);
    }

    /** The original hidden season providers return null in the Nether and End. */
    public static Season seasonForDimension(ResourceKey<Level> dimension, Season calendarSeason) {
        return Level.NETHER.equals(dimension) || Level.END.equals(dimension) ? null : calendarSeason;
    }

    public static boolean watered(BlockState soil) {
        return FarmlandSoil.watered(soil);
    }
    public static BlockState dry(BlockState soil, BlockState above) {
        return FarmlandSoil.dry(soil, above);
    }

    private static final class WorldChunk implements ChunkWork {
        private final ServerLevel level;
        private final LevelChunk chunk;
        private final Season season;
        private final List<BlockPos> soils = new ArrayList<>();
        private final List<BlockPos> crops = new ArrayList<>();
        WorldChunk(ServerLevel level, LevelChunk chunk, Season season) { this.level = level; this.chunk = chunk; this.season = season; }
        public long lastCycle() { return chunk.getData(LAST_CYCLE); }
        public void mark(long cycle) { chunk.setData(LAST_CYCLE, cycle); chunk.markUnsaved(); }
        public void grow() {
            var sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++) {
                var section = sections[index];
                if (!section.maybeHas(state -> state.is(CropContent.SOILS) || state.is(CropContent.CROPS))) continue;
                int baseY = chunk.getSectionYFromSectionIndex(index) * 16;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    var state = section.getBlockState(x, y, z);
                    if (!state.is(CropContent.SOILS) && !state.is(CropContent.CROPS)) continue;
                    var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                    if (state.is(CropContent.SOILS)) soils.add(pos);
                    if (state.is(CropContent.CROPS)) crops.add(pos);
                }
            }
            for (var pos : crops) {
                if (!(chunk.getBlockEntity(pos) instanceof CropBlockEntity crop)) continue;
                var state = crop.crop();
                var next = CropLifecycle.newDay(state, CropDefinitions.INSTANCE.get(state.crop()), season,
                        watered(chunk.getBlockState(pos.below())), level.getServer().isDedicatedServer());
                CropGrowth.advance(level, crop, CropDefinitions.INSTANCE.get(state.crop()), next);
            }
        }
        public void dry() {
            for (var pos : soils) {
                var state = chunk.getBlockState(pos);
                var next = CropDailyUpdates.dry(state, chunk.getBlockState(pos.above()));
                if (!next.equals(state)) level.setBlock(pos, next, Block.UPDATE_ALL);
            }
        }
    }
}

package harvestfestival.crops;

import harvestfestival.calendar.NewDayEvent;
import harvestfestival.calendar.Season;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Invoked explicitly by the crop coordinator, never relying on event-listener order. */
public final class OrchardDaily {
    private static final Map<ServerLevel, Season> VISUAL_SEASONS = new java.util.WeakHashMap<>();
    public static void refreshIfChanged(ServerLevel level, java.util.Collection<LevelChunk> chunks) {
        var season = harvestfestival.calendar.WorldCalendar.get(level.getServer()).season();
        if (VISUAL_SEASONS.put(level, season) != season) for (var chunk : chunks) {
            var work = new Work(level, chunk);
            if (current(work)) refresh(work, season);
        }
    }
    public static void refresh(Work work, Season season) {
        var sections = work.chunk().getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.getBlock() instanceof FruitLeavesBlock)) continue;
            int baseY = work.chunk().getSectionYFromSectionIndex(index) * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                var state = section.getBlockState(x, y, z);
                if (state.getBlock() instanceof FruitLeavesBlock && state.getValue(FruitLeavesBlock.BLOOM) != (season == Season.SPRING))
                    work.level().setBlock(new BlockPos(work.chunk().getPos().getMinBlockX()+x, baseY+y, work.chunk().getPos().getMinBlockZ()+z),
                            state.setValue(FruitLeavesBlock.BLOOM, season == Season.SPRING), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    }
    public record Work(ServerLevel level, LevelChunk chunk) {}
    public static void grow(NewDayEvent event, List<Work> chunks) {
        long cycle = event.getCycle();
        if (cycle <= 0) return;
        var levels = new java.util.LinkedHashSet<ServerLevel>();
        for (var work : chunks) {
            if (!current(work)) continue;
            TreeProducers.get(work.level()).importChunk(work.level(), work.chunk());
            levels.add(work.level());
        }
        for (var level : levels) TreeProducers.get(level).daily(level, cycle,
                CropDailyUpdates.seasonForDimension(level.dimension(), event.getCurrentDate().season()));
    }
    public static void post(NewDayEvent event, List<Work> chunks) {
        if (event.getCycle() <= 0) return;
        for (var work : chunks) {
            if (!current(work)) continue;
            cleanFruit(work, event.getCurrentDate().season());
            for (var entity : new ArrayList<>(work.chunk().getBlockEntities().values()))
                if (entity instanceof SprinklerBlockEntity sprinkler && !sprinkler.isRemoved()) sprinkler.newDay(work.level(), event.getCycle());
        }
    }
    private static boolean current(Work work) { return work.level().getChunkSource().getChunkNow(work.chunk().getPos().x(), work.chunk().getPos().z()) == work.chunk(); }
    private static void cleanFruit(Work work, Season globalSeason) {
        var sections = work.chunk().getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.getBlock() instanceof TreeFruitBlock)) continue;
            int baseY = work.chunk().getSectionYFromSectionIndex(index) * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!(section.getBlockState(x, y, z).getBlock() instanceof TreeFruitBlock fruit)) continue;
                var definition = TreeDefinitions.INSTANCE.get(fruit.species());
                if (definition != null && !definition.fruitsIn(globalSeason)) work.level().removeBlock(
                        new BlockPos(work.chunk().getPos().getMinBlockX() + x, baseY + y, work.chunk().getPos().getMinBlockZ() + z), false);
            }
        }
    }
    private OrchardDaily() {}
}

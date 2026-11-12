package harvestfestival.crops;

import harvestfestival.calendar.DailyCycleData;
import harvestfestival.calendar.Season;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropDailyUpdatesTest {
    @TempDir Path directory;
    @Test void everyCropGrowsBeforeAnySoilDriesIncludingEmptySoil() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var order = new ArrayList<String>();
            var a = new Work("a", order, true);
            var b = new Work("b", order, false);
            CropDailyUpdates.process(1, List.of(a, b));
            assertEquals(List.of("grow:a", "grow:b", "dry:a", "dry:b"), order);
            assertEquals(2, a.crop.stage());
            assertFalse(CropDailyUpdates.watered(a.soil));
            assertFalse(CropDailyUpdates.watered(b.soil));
            assertTrue(a.soil.is(Blocks.FARMLAND));
        }
    }
    @Test void repeatedCycleDoesNotGrowOrEraseNewWater() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var a = new Work("a", new ArrayList<>(), true);
            CropDailyUpdates.process(10, List.of(a));
            a.soil = a.soil.setValue(FarmlandBlock.MOISTURE, 7);
            CropDailyUpdates.process(10, List.of(a));
            CropDailyUpdates.process(9, List.of(a));
            assertEquals(2, a.crop.stage());
            assertTrue(CropDailyUpdates.watered(a.soil));
            CropDailyUpdates.process(11, List.of(a));
            assertEquals(3, a.crop.stage());
            assertFalse(CropDailyUpdates.watered(a.soil));
        }
    }
    @Test void absentChunkDoesNotCatchUpOnReturnAndMissingRuleStillDries() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var a = new Work("a", new ArrayList<>(), true);
            CropDailyUpdates.process(1, List.of());
            CropDailyUpdates.process(100, List.of());
            assertEquals(1, a.crop.stage());
            assertTrue(CropDailyUpdates.watered(a.soil));
            CropDailyUpdates.process(101, List.of(a));
            assertEquals(2, a.crop.stage());
            CropDefinitions.INSTANCE.apply(java.util.Map.of(), null, null);
            a.soil = a.soil.setValue(FarmlandBlock.MOISTURE, 7);
            CropDailyUpdates.process(102, List.of(a));
            assertEquals(2, a.crop.stage());
            assertFalse(CropDailyUpdates.watered(a.soil));
        }
    }
    @Test void savedAttachmentPreventsReplayAndInvalidMarkerFailsClosed() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var holder = new SavedHolder();
            assertEquals(0, holder.getData(CropDailyUpdates.LAST_CYCLE));
            holder.setData(CropDailyUpdates.LAST_CYCLE, 7L);
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registry);
            holder.serializeAttachments(output);
            var copy = new SavedHolder();
            copy.read(TagValueInput.create(ProblemReporter.DISCARDING, registry, output.buildResult()));
            assertEquals(7, copy.getData(CropDailyUpdates.LAST_CYCLE));
            var a = new Work("a", new ArrayList<>(), true);
            a.last = copy.getData(CropDailyUpdates.LAST_CYCLE);
            CropDailyUpdates.process(7, List.of(a));
            assertEquals(1, a.crop.stage());
            a.last = DailyCycleData.MARKER_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString("-1")).getOrThrow();
            CropDailyUpdates.process(8, List.of(a));
            assertEquals(1, a.crop.stage());
        }
    }
    static final class SavedHolder extends AttachmentHolder {
        void read(net.minecraft.world.level.storage.ValueInput input) { deserializeAttachments(input); }
    }
    private static final class Work implements CropDailyUpdates.ChunkWork {
        final String name;
        final List<String> order;
        long last;
        CropState crop;
        net.minecraft.world.level.block.state.BlockState soil = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
        Work(String name, List<String> order, boolean planted) {
            this.name = name; this.order = order;
            crop = planted ? CropState.planted(CropTestSupport.turnip().id()) : null;
        }
        public long lastCycle() { return last; }
        public void mark(long cycle) { last = cycle; }
        public void grow() {
            order.add("grow:" + name);
            if (crop != null) crop = CropLifecycle.newDay(crop, CropDefinitions.INSTANCE.get(crop.crop()),
                    Season.SPRING, CropDailyUpdates.watered(soil), false);
        }
        public void dry() { order.add("dry:" + name); soil = CropDailyUpdates.dry(soil, crop == null ? Blocks.AIR.defaultBlockState() : CropContent.CROP.get().defaultBlockState()); }
    }
}

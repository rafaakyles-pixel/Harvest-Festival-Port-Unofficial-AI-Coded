package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropPersistenceTest {
    @TempDir Path directory;
    @Test void regrowingEntityAndUpdatePayloadPreserveHarvestCountAcrossDiskReload() throws Exception {
        try (var resources = new CropTestSupport(directory.resolve("pack"))) {
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var expected = new CropState(CropTestSupport.cucumber().id(), 7, 3, false, 6);
            var entity = CropRegrowthTest.entity(expected);
            var file = directory.resolve("regrowth.nbt");
            NbtIo.writeCompressed(entity.saveWithFullMetadata(registry), file);
            var copy = (CropBlockEntity) BlockEntity.loadStatic(entity.getBlockPos(), entity.getBlockState(),
                    NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), registry);
            assertNotNull(copy);
            assertEquals(expected, copy.crop());
            var client = CropRegrowthTest.entity(CropState.INVALID);
            client.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, copy.getUpdateTag(registry)));
            assertEquals(expected, client.crop());
            for (int i = 0; i < 3; i++) copy.setCrop(CropLifecycle.newDay(copy.crop(), CropTestSupport.cucumber(),
                    harvestfestival.calendar.Season.SPRING, true, false));
            var seventh = CropLifecycle.afterHarvest(copy.crop(), CropTestSupport.cucumber());
            assertNotNull(seventh);
            assertEquals(7, seventh.harvests());
            assertEquals(expected, entity.crop());
        }
    }

    @Test void absentHarvestCountLoadsLegacyTurnipWithoutChangingOtherFields() {
        var json = JsonParser.parseString("{\"crop\":\"harvestfestival:turnip\",\"stage\":4,\"dry_days\":2,\"dead\":false}");
        var crop = CropState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(0, crop.harvests());
        assertEquals(4, crop.stage());
        assertEquals(2, crop.dryDays());
        assertFalse(crop.dead());
    }

    @Test void invalidPresentHarvestCountsNeverResetToZeroOrGrantProducts() {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (String invalid : new String[]{"-1", "1000001", "2147483648", "1.5", "\"2\"", "true", "null", "{}", "[]"}) {
            var json = JsonParser.parseString("{\"crop\":\"harvestfestival:cucumber\",\"stage\":10,\"dry_days\":1,\"dead\":false,\"harvests\":" + invalid + "}");
            assertTrue(CropState.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), invalid);
            // NBT has neither null nor a boolean type (booleans become numeric bytes).
            if (invalid.equals("null") || invalid.equals("true")) continue;
            var nbt = new CompoundTag();
            nbt.put("crop_state", JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json));
            var entity = CropRegrowthTest.entity(new CropState(net.minecraft.resources.Identifier.parse("harvestfestival:cucumber"), 10, 1, false, 2));
            entity.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, nbt));
            assertEquals(CropState.INVALID, entity.crop(), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> new CropState(net.minecraft.resources.Identifier.parse("test:crop"), 1, 1, false, -1));
    }
    @Test void actualRegisteredEntityAndSoilSurviveCompressedDiskRoundTrip() throws Exception {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var state = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE, 1);
        var entity = new CropBlockEntity(new BlockPos(2, 80, 3), state);
        var crop = new CropState(net.minecraft.resources.Identifier.parse("harvestfestival:turnip"), 4, 3, false);
        entity.setCrop(crop);
        var soil = net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7);
        var root = new CompoundTag();
        root.put("entity", entity.saveWithFullMetadata(registry));
        root.put("block", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow());
        root.put("soil", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, soil).getOrThrow());
        var file = directory.resolve("crop.nbt");
        NbtIo.writeCompressed(root, file);
        var loaded = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        var loadedState = BlockState.CODEC.parse(NbtOps.INSTANCE, loaded.get("block")).getOrThrow();
        var copy = (CropBlockEntity) BlockEntity.loadStatic(entity.getBlockPos(), loadedState,
                loaded.getCompoundOrEmpty("entity"), registry);
        assertNotNull(copy);
        assertEquals(crop, copy.crop());
        assertEquals(soil, BlockState.CODEC.parse(NbtOps.INSTANCE, loaded.get("soil")).getOrThrow());
        copy.setCrop(new CropState(crop.crop(), 5, 1, true));
        assertEquals(crop, entity.crop());
    }
    @Test void updateTagsContainIdentityStageDroughtAndDeath() {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var server = new CropBlockEntity(BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
        var client = new CropBlockEntity(BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
        for (boolean dead : new boolean[]{false, true}) {
            server.setCrop(new CropState(net.minecraft.resources.Identifier.parse("test:long_crop"), 250, 3, dead));
            client.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, server.getUpdateTag(registry)));
            assertEquals(server.crop(), client.crop());
        }
    }
    @Test void malformedEntityCountersBecomeInertAndCannotGrantProduct() {
        var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (var json : new String[]{"{}", "{\"crop\":\"harvestfestival:turnip\",\"stage\":1.5,\"dry_days\":0,\"dead\":false}",
                "{\"crop\":\"harvestfestival:turnip\",\"stage\":5,\"dry_days\":-1,\"dead\":false}"}) {
            assertTrue(CropState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent());
        }
        var entity = new CropBlockEntity(BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
        entity.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registry, new CompoundTag()));
        assertEquals(CropState.INVALID, entity.crop());
    }
}

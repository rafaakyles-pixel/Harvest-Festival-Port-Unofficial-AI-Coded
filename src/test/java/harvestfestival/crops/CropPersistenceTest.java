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

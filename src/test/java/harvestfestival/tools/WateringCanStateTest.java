package harvestfestival.tools;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WateringCanStateTest {
    @TempDir Path directory;

    @BeforeAll
    static void bindComponents() {
        ToolContent.WATERING_CAN.get().builtInRegistryHolder().bindComponents(
                DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
        ToolContent.HOE.get().builtInRegistryHolder().bindComponents(
                DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
    }

    @Test
    void absentStartsEmptyAndFillIsBoundedAndIndependent() {
        var first = new ItemStack(ToolContent.WATERING_CAN.get());
        var second = new ItemStack(ToolContent.WATERING_CAN.get());
        assertEquals(WateringCanState.EMPTY, WateringCanState.get(first));
        assertTrue(WateringCanState.fill(first));
        assertFalse(WateringCanState.fill(first));
        assertEquals(128, WateringCanState.get(first).water());
        assertEquals(0, WateringCanState.get(second).water());
        assertFalse(WateringCanState.fill(ItemStack.EMPTY));
    }

    @Test
    void consumptionStopsAtZeroAndConstructionRejectsInvalidBounds() {
        var state = WateringCanState.FULL;
        for (int i = 127; i >= 0; i--) {
            state = state.consume();
            assertEquals(i, state.water());
        }
        assertEquals(WateringCanState.EMPTY, state.consume());
        assertThrows(IllegalArgumentException.class, () -> new WateringCanState(-1));
        assertThrows(IllegalArgumentException.class, () -> new WateringCanState(129));
    }

    @Test
    void invalidPayloadsBecomeEmptyWithoutRejectingTheItemComponent() {
        for (var json : new String[]{"{}", "null", "[]", "4", "{\"water\":-1}",
                "{\"water\":129}", "{\"water\":1.5}", "{\"water\":2147483648}",
                "{\"water\":\"128\"}", "{\"water\":true}", "{\"water\":null}"}) {
            assertEquals(WateringCanState.EMPTY,
                    WateringCanState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(), json);
        }
    }

    @Test
    void validCodecRoundTripsAllAmounts() {
        for (int amount = 0; amount <= 128; amount++) {
            var state = new WateringCanState(amount);
            var json = WateringCanState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            assertEquals(state, WateringCanState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }

    @Test
    void actualItemStackSurvivesCompressedDiskSaveAndCopyIsolation() throws Exception {
        var stack = new ItemStack(ToolContent.WATERING_CAN.get());
        WateringCanState.fill(stack);
        var copy = stack.copy();
        stack.set(ToolContent.WATER_STATE.get(), new WateringCanState(17));
        assertEquals(128, WateringCanState.get(copy).water());
        var ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var tag = (CompoundTag) ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var file = directory.resolve("watering-can.dat");
        NbtIo.writeCompressed(tag, file);
        var loaded = ItemStack.CODEC.parse(ops, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())).getOrThrow();
        assertTrue(loaded.is(ToolContent.WATERING_CAN.get()));
        assertEquals(1, loaded.getCount());
        assertEquals(17, WateringCanState.get(loaded).water());
        assertTrue(WateringCanState.fill(loaded));
        assertEquals(17, WateringCanState.get(stack).water());
    }

    @Test
    void registeredNetworkCodecRoundTripsBounds() {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        try {
            var codec = ToolContent.WATER_STATE.get().streamCodec();
            for (int amount : new int[]{0, 1, 127, 128}) {
                var state = new WateringCanState(amount);
                codec.encode(buffer, state);
                assertEquals(state, codec.decode(buffer));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void corruptWaterInsideActualItemStackDoesNotLoseTheItem() {
        var stack = new ItemStack(ToolContent.WATERING_CAN.get());
        WateringCanState.fill(stack);
        var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var json = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow().getAsJsonObject();
        json.getAsJsonObject("components").add("harvestfestival:watering_can_state", JsonParser.parseString("{\"water\":999}"));
        var loaded = ItemStack.CODEC.parse(ops, json).getOrThrow();
        assertTrue(loaded.is(ToolContent.WATERING_CAN.get()));
        assertEquals(0, WateringCanState.get(loaded).water());
    }
}

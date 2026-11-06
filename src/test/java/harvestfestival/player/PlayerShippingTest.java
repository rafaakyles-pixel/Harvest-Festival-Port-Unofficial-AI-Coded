package harvestfestival.player;

import com.mojang.serialization.Lifecycle;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PlayerShippingTest {
    @TempDir Path directory;

    @BeforeAll
    static void bindItemComponents() {
        // The unit-test launcher loads registries, but does not load a world's components.
        // Only the fixture needs defaults; do not initialize unrelated gameplay registries.
        Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64).build());
    }

    @Test
    void depositConsumesOneAndFreezesItemAndPrice() {
        var holder = new SavedHolder();
        var held = pricedStack(3, 25);
        assertTrue(PlayerShipping.depositInto(holder, held));
        assertEquals(2, held.getCount());
        assertEquals(0L, holder.getData(PlayerGold.GOLD));
        held.set(DataComponents.CUSTOM_DATA, priceData(99));
        var queue = holder.getData(PlayerShipping.QUEUE);
        assertEquals(1, queue.entries().size());
        assertEquals(1, queue.entries().getFirst().stack().getCount());
        assertEquals(25, PlayerShipping.testPrice(queue.entries().getFirst().stack()));
        assertEquals(25, queue.total());
        queue.entries().getFirst().stack().shrink(1);
        assertEquals(1, queue.entries().getFirst().stack().getCount());
    }

    @Test
    void rejectsEmptyUnpricedZeroAndNegativeWithoutConsuming() {
        var holder = new SavedHolder();
        for (var stack : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK, 2),
                pricedStack(2, 0), pricedStack(2, -1)}) {
            int count = stack.getCount();
            assertFalse(PlayerShipping.depositInto(holder, stack));
            assertEquals(count, stack.getCount());
        }
        assertTrue(holder.getData(PlayerShipping.QUEUE).entries().isEmpty());
    }

    @Test
    void queueSurvivesCompressedDiskSaveAndFreshHolder() throws Exception {
        var original = new SavedHolder();
        PlayerShipping.depositInto(original, pricedStack(1, 42));
        original.setData(PlayerGold.GOLD, 100L);
        var reloaded = reopen(original, "pending");
        assertEquals(100L, reloaded.getData(PlayerGold.GOLD));
        var entry = reloaded.getData(PlayerShipping.QUEUE).entries().getFirst();
        assertTrue(entry.stack().is(Items.STICK));
        assertEquals(1, entry.stack().getCount());
        assertEquals(42, entry.value());
        assertEquals(42, PlayerShipping.testPrice(entry.stack()));
        PlayerShipping.pay(reloaded);
        assertEquals(142L, reloaded.getData(PlayerGold.GOLD));
    }

    @Test
    void repeatedPaymentAndReopeningCannotCreditAnItemTwice() throws Exception {
        var holder = new SavedHolder();
        holder.setData(PlayerGold.GOLD, 100L);
        var held = pricedStack(2, 25);
        PlayerShipping.depositInto(holder, held);
        PlayerShipping.depositInto(holder, held);
        assertTrue(held.isEmpty());
        PlayerShipping.pay(holder);
        PlayerShipping.pay(holder);
        assertEquals(150L, holder.getData(PlayerGold.GOLD));
        assertTrue(holder.getData(PlayerShipping.QUEUE).entries().isEmpty());
        var reopened = reopen(holder, "paid");
        PlayerShipping.pay(reopened);
        PlayerShipping.pay(reopened);
        assertEquals(150L, reopened.getData(PlayerGold.GOLD));
        PlayerShipping.depositInto(reopened, pricedStack(1, 7));
        PlayerShipping.pay(reopened);
        PlayerShipping.pay(reopened);
        assertEquals(157L, reopen(reopened, "paid-again").getData(PlayerGold.GOLD));
    }

    @Test
    void playersRemainIndependentAndAbsentQueueDoesNotChangeGold() {
        var first = new SavedHolder();
        var second = new SavedHolder();
        second.setData(PlayerGold.GOLD, -20L);
        PlayerShipping.depositInto(first, pricedStack(1, 30));
        PlayerShipping.pay(second);
        assertEquals(-20L, second.getData(PlayerGold.GOLD));
        assertEquals(1, first.getData(PlayerShipping.QUEUE).entries().size());
        PlayerShipping.pay(first);
        assertEquals(30L, first.getData(PlayerGold.GOLD));
        assertEquals(-20L, second.getData(PlayerGold.GOLD));
    }

    @Test
    void paymentUsesExistingSignedLongGoldArithmetic() {
        var holder = new SavedHolder();
        holder.setData(PlayerGold.GOLD, Long.MAX_VALUE);
        PlayerShipping.depositInto(holder, pricedStack(1, 1));
        PlayerShipping.pay(holder);
        assertEquals(Long.MIN_VALUE, holder.getData(PlayerGold.GOLD));
        PlayerShipping.pay(holder);
        assertEquals(Long.MIN_VALUE, holder.getData(PlayerGold.GOLD));
    }

    @Test
    void specificItemBeatsTagAndSellValue() {
        var stack = taggedStick();
        stack.set(DataComponents.CUSTOM_DATA, priceData(90));
        var table = ShippingPrices.read(Map.ofEntries(rule("items/minecraft/stick", "{\"price\":7}"),
                rule("tags/test/a", "{\"price\":50,\"priority\":10}")), Assertions::fail);
        assertEquals(7, table.price(stack, Assertions::fail));
    }

    @Test
    void higherPriorityWinsEvenWithLowerPriceAndSellValue() {
        var stack = taggedStick();
        stack.set(DataComponents.CUSTOM_DATA, priceData(90));
        var table = ShippingPrices.read(Map.ofEntries(rule("tags/test/a", "{\"price\":50,\"priority\":-2}"),
                rule("tags/test/b", "{\"price\":7,\"priority\":-1}")), Assertions::fail);
        assertEquals(7, table.price(stack, Assertions::fail));
    }

    @Test
    void tiedTagsWarnOnceAndPayHigherPrice() {
        var stack = taggedStick();
        var warnings = new ArrayList<String>();
        var table = ShippingPrices.read(Map.ofEntries(rule("tags/test/a", "{\"price\":50,\"priority\":1}"),
                rule("tags/test/b", "{\"price\":7,\"priority\":1}")), Assertions::fail);
        assertEquals(50, table.price(stack, warnings::add));
        assertEquals(50, table.price(stack, warnings::add));
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("test:a") && warnings.getFirst().contains("test:b"));
    }

    @Test
    void nonMatchingTagFallsBackToSellValueOrRejects() {
        var stack = taggedStick();
        var prices = new ShippingPrices();
        prices.apply(ShippingPrices.read(Map.ofEntries(rule("tags/test/absent",
                "{\"price\":80,\"priority\":1}")), Assertions::fail), null, null);
        var holder = new SavedHolder();
        assertFalse(PlayerShipping.depositInto(holder, stack, prices));
        assertEquals(2, stack.getCount());
        stack.set(DataComponents.CUSTOM_DATA, priceData(17));
        assertTrue(PlayerShipping.depositInto(holder, stack, prices));
        assertEquals(17, holder.getData(PlayerShipping.QUEUE).total());
    }

    @Test
    void malformedAndNegativeRulesDoNotDiscardValidSibling() {
        var warnings = new ArrayList<String>();
        var table = ShippingPrices.read(Map.ofEntries(rule("items/minecraft/carrot", "{"),
                rule("tags/test/a", "{\"price\":-1,\"priority\":0}"),
                rule("items/minecraft/stick", "{\"price\":12}")), warnings::add);
        assertEquals(2, warnings.size());
        assertEquals(12, table.price(pricedStack(1, 3), Assertions::fail));
    }

    @Test
    void unknownItemIsSkippedWithItsIdentifier() {
        var warnings = new ArrayList<String>();
        var table = ShippingPrices.read(Map.ofEntries(rule("items/test/missing", "{\"price\":90}")), warnings::add);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("unknown item test:missing"));
        assertEquals(0, table.price(ItemStack.EMPTY, Assertions::fail));
        assertEquals(3, table.price(pricedStack(1, 3), Assertions::fail));
    }

    @Test
    void reloadBetweenDepositsPaysBothFrozenPrices() {
        var holder = new SavedHolder();
        var stack = taggedStick();
        var prices = new ShippingPrices();
        for (int price : List.of(100, 140)) {
            var table = ShippingPrices.read(Map.ofEntries(rule("tags/test/a",
                    "{\"price\":" + price + ",\"priority\":0}")), Assertions::fail);
            prices.apply(table, null, null);
            assertTrue(PlayerShipping.depositInto(holder, stack, prices));
        }
        PlayerShipping.pay(holder);
        assertEquals(240L, holder.getData(PlayerGold.GOLD));
    }

    private static Map.Entry<Identifier, Resource> rule(String path, String json) {
        return Map.entry(Identifier.fromNamespaceAndPath("harvestfestival", "shipping_prices/" + path + ".json"),
                new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    private static ItemStack taggedStick() {
        var registry = new MappedRegistry<Item>(Registries.ITEM, Lifecycle.stable());
        var stick = registry.register(ResourceKey.create(Registries.ITEM, Identifier.parse("minecraft:stick")),
                Items.STICK, RegistrationInfo.BUILT_IN);
        var a = TagKey.create(Registries.ITEM, Identifier.parse("test:a"));
        var b = TagKey.create(Registries.ITEM, Identifier.parse("test:b"));
        registry.bindTags(Map.of(a, List.of(stick), b, List.of(stick)));
        registry.freeze();
        stick.bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
        return new ItemStack(stick, 2);
    }

    private SavedHolder reopen(SavedHolder original, String name) throws Exception {
        var file = directory.resolve(name + ".dat");
        NbtIo.writeCompressed(original.save(), file);
        var reloaded = new SavedHolder();
        reloaded.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
        return reloaded;
    }

    private static ItemStack pricedStack(int count, long price) {
        var stack = new ItemStack(Items.STICK, count);
        stack.set(DataComponents.CUSTOM_DATA, priceData(price));
        return stack;
    }

    private static CustomData priceData(long price) {
        var tag = new CompoundTag();
        tag.putLong("SellValue", price);
        return CustomData.of(tag);
    }

    private static final class SavedHolder extends AttachmentHolder {
        CompoundTag save() {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            serializeAttachments(output);
            return output.buildResult();
        }

        void load(CompoundTag tag) {
            deserializeAttachments(TagValueInput.create(ProblemReporter.DISCARDING,
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), tag));
        }
    }
}

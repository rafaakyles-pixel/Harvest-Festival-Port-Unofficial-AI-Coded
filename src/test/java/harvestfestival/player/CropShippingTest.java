package harvestfestival.player;

import harvestfestival.crops.CropBlock;
import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropLifecycle;
import harvestfestival.crops.CropState;
import harvestfestival.crops.CropTestSupport;
import harvestfestival.calendar.Season;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CropShippingTest {
    @TempDir Path directory;
    @Test void vanillaProductsUseSeparateTagPricesAndPersistPaymentWithoutSeedConversion() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var prices = new ShippingPrices(); prices.apply(prices.prepare(fixture.manager, null), fixture.manager, null);
            var products = List.of(net.minecraft.world.item.Items.CARROT, net.minecraft.world.item.Items.BEETROOT,
                    net.minecraft.world.item.Items.POTATO, net.minecraft.world.item.Items.POISONOUS_POTATO);
            long[] expected = {160, 120, 80, 1};
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var holder = new SavedHolder();
            long total = 0;
            for (int i = 0; i < products.size(); i++) {
                var stack = new ItemStack(products.get(i), 2);
                assertTrue(PlayerShipping.depositInto(holder, stack, prices));
                total += expected[i]; assertEquals(total, holder.getData(PlayerShipping.QUEUE).total());
                assertEquals(1, stack.getCount());
            }
            var poison = new ItemStack(net.minecraft.world.item.Items.POISONOUS_POTATO);
            assertFalse(poison.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.Identifier.parse("c:crops/potato"))));
            for (var seed : List.of(CropContent.CARROT_SEEDS.get(), CropContent.BEETROOT_SEEDS.get(), CropContent.POTATO_SEEDS.get())) {
                var stack = new ItemStack(seed);
                assertFalse(PlayerShipping.depositInto(holder, stack, prices)); assertEquals(1, stack.getCount());
            }
            prices.apply(ShippingPrices.read(java.util.Map.of(), message -> fail(message)), null, null);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registry); holder.serializeAttachments(output);
            var file = directory.resolve("vanilla-products.nbt"); NbtIo.writeCompressed(output.buildResult(), file);
            var copy = new SavedHolder();
            copy.read(TagValueInput.create(ProblemReporter.DISCARDING, registry, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())));
            PlayerShipping.pay(copy); PlayerShipping.pay(copy);
            assertEquals(361, copy.getData(PlayerGold.GOLD)); assertTrue(copy.getData(PlayerShipping.QUEUE).entries().isEmpty());
        }
    }
    @Test void onionAndSpinachEachPay900AfterQueueSaveWithoutSeedPrices() throws Exception {
        try(var fixture=new CropTestSupport(directory.resolve("pack"))) {
            var prices=new ShippingPrices();prices.apply(prices.prepare(fixture.manager,null),fixture.manager,null);
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for(var item:List.of(CropContent.ONION.get(),CropContent.SPINACH.get())) {
                var holder=new SavedHolder();
                var stack=new ItemStack(item,9);
                for(int i=0;i<9;i++) assertTrue(PlayerShipping.depositInto(holder,stack,prices));
                assertTrue(stack.isEmpty());assertEquals(900,holder.getData(PlayerShipping.QUEUE).total());
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registry);holder.serializeAttachments(output);
                var file=directory.resolve(BuiltInRegistries.ITEM.getKey(item).getPath()+".nbt");NbtIo.writeCompressed(output.buildResult(),file);
                var copy=new SavedHolder();copy.read(TagValueInput.create(ProblemReporter.DISCARDING,registry,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())));
                PlayerShipping.pay(copy);PlayerShipping.pay(copy);assertEquals(900,copy.getData(PlayerGold.GOLD));
            }
            for(var seed:List.of(CropContent.ONION_SEEDS.get(),CropContent.SPINACH_SEEDS.get()))
                assertFalse(PlayerShipping.depositInto(new SavedHolder(),new ItemStack(seed),prices));
        }
    }
    @Test void wheatUsesExistingVanillaProductAndPriceThenPaysOnceAfterSave() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var definition = harvestfestival.crops.CropDefinitions.INSTANCE.get(net.minecraft.resources.Identifier.parse("harvestfestival:wheat"));
            var crop = CropState.planted(definition.id());
            for (int i=0;i<27;i++) crop=CropLifecycle.newDay(crop,definition,Season.SPRING,true,false);
            var drops=new ArrayList<ItemStack>();
            assertTrue(CropBlock.breakOnce(crop,definition,new ItemStack(harvestfestival.tools.ToolContent.SICKLE.get()),
                    ()->List.of(new ItemStack(net.minecraft.world.item.Items.WHEAT)),()->true,drops::add));
            var prices=new ShippingPrices(); prices.apply(prices.prepare(fixture.manager,null),fixture.manager,null);
            var holder=new SavedHolder();
            assertTrue(PlayerShipping.depositInto(holder,drops.getFirst(),prices));
            assertEquals(100,holder.getData(PlayerShipping.QUEUE).total());
            assertFalse(PlayerShipping.depositInto(holder,new ItemStack(CropContent.WHEAT_SEEDS.get()),prices));
            prices.apply(ShippingPrices.read(java.util.Map.of(),message->fail(message)),null,null);
            var registry=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registry);
            holder.serializeAttachments(output);
            var file=directory.resolve("wheat-shipping.nbt"); NbtIo.writeCompressed(output.buildResult(),file);
            var reopened=new SavedHolder();
            reopened.read(TagValueInput.create(ProblemReporter.DISCARDING,registry,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())));
            PlayerShipping.pay(reopened); PlayerShipping.pay(reopened);
            assertEquals(100,reopened.getData(PlayerGold.GOLD));
            assertTrue(reopened.getData(PlayerShipping.QUEUE).entries().isEmpty());
        }
    }
    @Test void eightCucumberHarvestsUseBundledPriceAndPayOnlyOnceAfterReload() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var definition = CropTestSupport.cucumber();
            var entity = new harvestfestival.crops.CropBlockEntity(net.minecraft.core.BlockPos.ZERO, CropContent.CROP.get().defaultBlockState());
            entity.setCrop(CropState.planted(definition.id()));
            var prices = new ShippingPrices();
            prices.apply(prices.prepare(fixture.manager, null), fixture.manager, null);
            var holder = new SavedHolder();
            for (int harvest = 0; harvest < 8; harvest++) {
                for (int day = 0; day < (harvest == 0 ? 9 : 3); day++)
                    entity.setCrop(CropLifecycle.newDay(entity.crop(), definition, Season.SPRING, true, false));
                var expected = entity.crop();
                assertTrue(CropBlock.harvestOnce(expected, definition, () -> List.of(new ItemStack(CropContent.CUCUMBER.get())),
                        next -> entity.commitHarvest(expected, next, () -> true), stack -> assertTrue(PlayerShipping.depositInto(holder, stack, prices))));
            }
            assertEquals(CropState.INVALID, entity.crop());
            assertEquals(320, holder.getData(PlayerShipping.QUEUE).total());
            assertFalse(PlayerShipping.depositInto(holder, new ItemStack(CropContent.CUCUMBER_SEEDS.get()), prices));
            prices.apply(ShippingPrices.read(java.util.Map.of(), message -> fail(message)), null, null); // Deposited values remain fixed.
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registry);
            holder.serializeAttachments(output);
            var file = directory.resolve("cucumber-shipping.nbt");
            NbtIo.writeCompressed(output.buildResult(), file);
            var reopened = new SavedHolder();
            reopened.read(TagValueInput.create(ProblemReporter.DISCARDING, registry, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())));
            PlayerShipping.pay(reopened);
            PlayerShipping.pay(reopened);
            assertEquals(320, reopened.getData(PlayerGold.GOLD));
            assertTrue(reopened.getData(PlayerShipping.QUEUE).entries().isEmpty());
        }
    }
    @Test void matureTurnipUsesRealTagAndExistingQueueThenPaysOnceAfterDiskReload() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var definition = CropTestSupport.turnip();
            var crop = CropState.planted(definition.id());
            for (int i = 0; i < 4; i++) crop = CropLifecycle.newDay(crop, definition, Season.SPRING, true, false);
            var drops = new ArrayList<ItemStack>();
            assertTrue(CropBlock.harvestOnce(crop, definition, () -> List.of(new ItemStack(CropContent.TURNIP.get())), next -> { assertNull(next); return true; }, drops::add));
            var prices = new ShippingPrices();
            prices.apply(prices.prepare(fixture.manager, null), fixture.manager, null);
            var holder = new SavedHolder();
            assertTrue(PlayerShipping.depositInto(holder, drops.getFirst(), prices));
            assertTrue(drops.getFirst().isEmpty());
            assertEquals(60, holder.getData(PlayerShipping.QUEUE).total());
            assertEquals(0, holder.getData(PlayerGold.GOLD));
            assertFalse(PlayerShipping.depositInto(holder, new ItemStack(CropContent.TURNIP_SEEDS.get()), prices));
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registry);
            holder.serializeAttachments(output);
            var file = directory.resolve("shipping.nbt");
            NbtIo.writeCompressed(output.buildResult(), file);
            var reopened = new SavedHolder();
            reopened.read(TagValueInput.create(ProblemReporter.DISCARDING, registry, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())));
            PlayerShipping.pay(reopened);
            PlayerShipping.pay(reopened);
            assertEquals(60, reopened.getData(PlayerGold.GOLD));
            assertTrue(reopened.getData(PlayerShipping.QUEUE).entries().isEmpty());
        }
    }
    private static final class SavedHolder extends AttachmentHolder {
        void read(net.minecraft.world.level.storage.ValueInput input) { deserializeAttachments(input); }
    }
}

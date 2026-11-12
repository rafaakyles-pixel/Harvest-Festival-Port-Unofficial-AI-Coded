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
    @Test void matureTurnipUsesRealTagAndExistingQueueThenPaysOnceAfterDiskReload() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var definition = CropTestSupport.turnip();
            var crop = CropState.planted(definition.id());
            for (int i = 0; i < 4; i++) crop = CropLifecycle.newDay(crop, definition, Season.SPRING, true, false);
            var drops = new ArrayList<ItemStack>();
            assertTrue(CropBlock.harvestOnce(crop, definition, () -> List.of(new ItemStack(CropContent.TURNIP.get())), () -> true, drops::add));
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

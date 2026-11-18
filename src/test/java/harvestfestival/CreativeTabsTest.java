package harvestfestival;

import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropTestSupport;
import harvestfestival.tools.ToolContent;
import harvestfestival.tools.WateringCanState;
import harvestfestival.tools.SickleState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CreativeTabsTest {
    @TempDir Path directory;
    @Test void exactContentsOrderAndSearchOnlyProductsUseOrdinaryStacks() throws Exception {
        try (var resources = new CropTestSupport(directory)) {
            var visible = new ArrayList<ItemStack>();
            var searchOnly = new ArrayList<ItemStack>();
            CreativeModeTab.Output output = (stack, visibility) -> {
                assertEquals(1, stack.getCount());
                if (visibility == CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) searchOnly.add(stack);
                else visible.add(stack);
            };
            CreativeTabs.seeds(output);
            assertEquals(List.of("turnip_seeds", "cucumber_seeds", "wheat_seeds", "onion_seeds", "spinach_seeds",
                    "carrot_seeds", "beetroot_seeds", "potato_seeds", "strawberry_seeds", "tomato_seeds", "eggplant_seeds", "cabbage_seeds", "pineapple_seeds", "corn_seeds", "sweet_potato_seeds", "green_pepper_seeds"), ids(visible));
            visible.clear();
            CreativeTabs.toolsAndBlocks(output);
            assertEquals(List.of("hoe", "hoe_copper", "hoe_silver", "hoe_gold", "hoe_mystril", "watering_can", "watering_can_copper", "watering_can_silver", "watering_can_gold", "watering_can_mystril", "sickle", "sickle_copper", "sickle_silver", "sickle_gold", "sickle_mystril", "shipping_box"), ids(visible));
            assertEquals(List.of("turnip", "cucumber", "onion", "spinach", "strawberry", "tomato", "eggplant", "cabbage", "pineapple", "corn", "sweet_potato", "green_pepper"), ids(searchOnly));
            for (var stack : visible) if (stack.getItem() instanceof harvestfestival.tools.WateringCanItem) assertEquals(0, WateringCanState.get(stack).water());
            for (int i=0;i<visible.size();i++) if (visible.get(i).getItem() instanceof harvestfestival.tools.SickleItem) {
                assertEquals(0, visible.get(i).getDamageValue());
                assertEquals(SickleState.NEW, SickleState.get(visible.get(i)));
            }
            WateringCanState.fill(visible.stream().filter(stack -> stack.getItem() instanceof harvestfestival.tools.WateringCanItem).findFirst().orElseThrow());
            visible.clear();
            CreativeTabs.toolsAndBlocks(output);
            assertEquals(0, WateringCanState.get(visible.stream().filter(stack -> stack.getItem() instanceof harvestfestival.tools.WateringCanItem).findFirst().orElseThrow()).water());
            assertEquals(16, ids(visible).stream().distinct().count());
        }
    }
    @Test void registeredTabsHaveExistingIconsAndTranslatedTitles() {
        assertTrue(CreativeTabs.SEEDS.get().getIconItem().is(CropContent.TURNIP_SEEDS.get()));
        assertTrue(CreativeTabs.TOOLS_AND_BLOCKS.get().getIconItem().is(ToolContent.HOE.get()));
        assertNotEquals(CreativeTabs.SEEDS.get().getDisplayName(), CreativeTabs.TOOLS_AND_BLOCKS.get().getDisplayName());
        assertEquals("seeds", CreativeTabs.SEEDS.getId().getPath());
        assertEquals("tools_and_blocks", CreativeTabs.TOOLS_AND_BLOCKS.getId().getPath());
    }
    private static List<String> ids(List<ItemStack> stacks) {
        return stacks.stream().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).toList();
    }
}

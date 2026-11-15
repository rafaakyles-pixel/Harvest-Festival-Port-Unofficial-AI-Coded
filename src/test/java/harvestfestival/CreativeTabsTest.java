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
            assertEquals(List.of("turnip_seeds", "cucumber_seeds", "wheat_seeds", "onion_seeds", "spinach_seeds"), ids(visible));
            visible.clear();
            CreativeTabs.toolsAndBlocks(output);
            assertEquals(List.of("hoe", "watering_can", "sickle", "shipping_box"), ids(visible));
            assertEquals(List.of("turnip", "cucumber", "onion", "spinach"), ids(searchOnly));
            assertEquals(0, WateringCanState.get(visible.get(1)).water());
            assertEquals(0, visible.get(2).getDamageValue());
            assertEquals(SickleState.NEW, SickleState.get(visible.get(2)));
            WateringCanState.fill(visible.get(1));
            visible.clear();
            CreativeTabs.toolsAndBlocks(output);
            assertEquals(0, WateringCanState.get(visible.get(1)).water());
            assertEquals(4, ids(visible).stream().distinct().count());
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

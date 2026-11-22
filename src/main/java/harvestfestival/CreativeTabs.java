package harvestfestival;

import harvestfestival.crops.CropContent;
import harvestfestival.tools.ToolContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Explicit order and ordinary stacks; new content must choose its creative destination here. */
public final class CreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HarvestFestival.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEEDS = TABS.register("seeds",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.harvestfestival.seeds"))
                    .icon(() -> new ItemStack(CropContent.TURNIP_SEEDS.get()))
                    .displayItems((parameters, output) -> seeds(output)).build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_AND_BLOCKS = TABS.register("tools_and_blocks",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.harvestfestival.tools_and_blocks"))
                    .icon(() -> new ItemStack(ToolContent.HOE.get())).withTabsBefore(SEEDS.getKey())
                    .displayItems((parameters, output) -> toolsAndBlocks(output)).build());

    private CreativeTabs() {}
    public static void register(IEventBus bus) { TABS.register(bus); }
    static void seeds(CreativeModeTab.Output output) {
        output.accept(CropContent.TURNIP_SEEDS.get());
        output.accept(CropContent.CUCUMBER_SEEDS.get());
        output.accept(CropContent.WHEAT_SEEDS.get());
        output.accept(CropContent.ONION_SEEDS.get());
        output.accept(CropContent.SPINACH_SEEDS.get());
        output.accept(CropContent.CARROT_SEEDS.get());
        output.accept(CropContent.BEETROOT_SEEDS.get());
        output.accept(CropContent.POTATO_SEEDS.get());
        output.accept(CropContent.STRAWBERRY_SEEDS.get());
        output.accept(CropContent.TOMATO_SEEDS.get());
        output.accept(CropContent.EGGPLANT_SEEDS.get());
        output.accept(CropContent.CABBAGE_SEEDS.get());
        output.accept(CropContent.PINEAPPLE_SEEDS.get());
        output.accept(CropContent.CORN_SEEDS.get());
        output.accept(CropContent.SWEET_POTATO_SEEDS.get());
        output.accept(CropContent.GREEN_PEPPER_SEEDS.get());
        output.accept(CropContent.WATERMELON_SEEDS.get());
        output.accept(CropContent.PUMPKIN_SEEDS.get());
        output.accept(CropContent.GRASS_SEEDS.get());
        output.accept(CropContent.TUTORIAL_TURNIP_SEEDS.get());
    }
    static void toolsAndBlocks(CreativeModeTab.Output output) {
        output.accept(ToolContent.HOE.get());
        output.accept(ToolContent.HOE_COPPER.get());
        output.accept(ToolContent.HOE_SILVER.get());
        output.accept(ToolContent.HOE_GOLD.get());
        output.accept(ToolContent.HOE_MYSTRIL.get());
        output.accept(ToolContent.WATERING_CAN.get());
        output.accept(ToolContent.WATERING_CAN_COPPER.get());
        output.accept(ToolContent.WATERING_CAN_SILVER.get());
        output.accept(ToolContent.WATERING_CAN_GOLD.get());
        output.accept(ToolContent.WATERING_CAN_MYSTRIL.get());
        output.accept(ToolContent.SICKLE.get());
        output.accept(ToolContent.SICKLE_COPPER.get());
        output.accept(ToolContent.SICKLE_SILVER.get());
        output.accept(ToolContent.SICKLE_GOLD.get());
        output.accept(ToolContent.SICKLE_MYSTRIL.get());
        output.accept(ShippingContent.SHIPPING_BOX.get());
        output.accept(CropContent.TURNIP.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.CUCUMBER.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.ONION.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.SPINACH.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.STRAWBERRY.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.TOMATO.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.EGGPLANT.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.CABBAGE.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.PINEAPPLE.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.CORN.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.SWEET_POTATO.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.GREEN_PEPPER.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.GRASS.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        output.accept(CropContent.TUTORIAL_TURNIP.get(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }
}

package harvestfestival;

import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShippingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HarvestFestival.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HarvestFestival.MOD_ID);
    public static final DeferredBlock<ShippingBoxBlock> SHIPPING_BOX = BLOCKS.registerBlock(
            "shipping_box", ShippingBoxBlock::new, properties -> properties.strength(1.5F).sound(SoundType.WOOD));

    static {
        ITEMS.registerSimpleBlockItem(SHIPPING_BOX);
    }

    private ShippingContent() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}

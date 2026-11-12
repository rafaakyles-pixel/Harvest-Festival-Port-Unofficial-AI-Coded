package harvestfestival.crops;

import harvestfestival.HarvestFestival;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CropContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HarvestFestival.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HarvestFestival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HarvestFestival.MOD_ID);
    public static final TagKey<Block> CROPS = TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:crops"));
    public static final TagKey<Block> SOILS = TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:crop_soils"));
    public static final TagKey<Item> WATERING_CANS = TagKey.create(Registries.ITEM, Identifier.parse("harvestfestival:watering_cans"));
    public static final DeferredBlock<CropBlock> CROP = BLOCKS.registerBlock("crop", CropBlock::new,
            props -> props.noCollision().noOcclusion().strength(0.1F).sound(SoundType.CROP));
    public static final Supplier<BlockEntityType<CropBlockEntity>> CROP_ENTITY = ENTITIES.register("crop",
            () -> new BlockEntityType<>(CropBlockEntity::new, CROP.get()));
    public static final DeferredItem<CropSeedItem> TURNIP_SEEDS = ITEMS.registerItem("turnip_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> TURNIP = ITEMS.registerSimpleItem("turnip");

    private CropContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        CropDailyUpdates.register(bus);
    }
}

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
    public static final DeferredItem<CropSeedItem> CUCUMBER_SEEDS = ITEMS.registerItem("cucumber_seeds", CropSeedItem::new);
    public static final DeferredItem<CropSeedItem> WHEAT_SEEDS = ITEMS.registerItem("wheat_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> CUCUMBER = ITEMS.registerSimpleItem("cucumber");

    public static final DeferredItem<CropSeedItem> ONION_SEEDS = ITEMS.registerItem("onion_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> ONION = ITEMS.registerSimpleItem("onion");
    public static final DeferredItem<CropSeedItem> SPINACH_SEEDS = ITEMS.registerItem("spinach_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> SPINACH = ITEMS.registerSimpleItem("spinach");
    public static final DeferredItem<CropSeedItem> CARROT_SEEDS = ITEMS.registerItem("carrot_seeds", CropSeedItem::new);
    public static final DeferredItem<CropSeedItem> BEETROOT_SEEDS = ITEMS.registerItem("beetroot_seeds", CropSeedItem::new);
    public static final DeferredItem<CropSeedItem> POTATO_SEEDS = ITEMS.registerItem("potato_seeds", CropSeedItem::new);

    public static final DeferredItem<CropSeedItem> STRAWBERRY_SEEDS = ITEMS.registerItem("strawberry_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> STRAWBERRY = ITEMS.registerSimpleItem("strawberry");
    public static final DeferredItem<CropSeedItem> TOMATO_SEEDS = ITEMS.registerItem("tomato_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> TOMATO = ITEMS.registerSimpleItem("tomato");
    public static final DeferredItem<CropSeedItem> EGGPLANT_SEEDS = ITEMS.registerItem("eggplant_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> EGGPLANT = ITEMS.registerSimpleItem("eggplant");

    public static final DeferredItem<CropSeedItem> CABBAGE_SEEDS = ITEMS.registerItem("cabbage_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> CABBAGE = ITEMS.registerSimpleItem("cabbage");

    public static final DeferredItem<CropSeedItem> PINEAPPLE_SEEDS = ITEMS.registerItem("pineapple_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> PINEAPPLE = ITEMS.registerSimpleItem("pineapple");

    public static final DeferredItem<CropSeedItem> CORN_SEEDS = ITEMS.registerItem("corn_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> CORN = ITEMS.registerSimpleItem("corn");

    public static final DeferredItem<CropSeedItem> SWEET_POTATO_SEEDS = ITEMS.registerItem("sweet_potato_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> SWEET_POTATO = ITEMS.registerSimpleItem("sweet_potato");

    public static final DeferredItem<CropSeedItem> GREEN_PEPPER_SEEDS = ITEMS.registerItem("green_pepper_seeds", CropSeedItem::new);
    public static final DeferredItem<Item> GREEN_PEPPER = ITEMS.registerSimpleItem("green_pepper");

    private CropContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        PotatoHarvestLoot.register(bus);
        CropDailyUpdates.register(bus);
    }
}

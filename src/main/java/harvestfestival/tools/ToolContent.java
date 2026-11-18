package harvestfestival.tools;

import harvestfestival.HarvestFestival;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ToolContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HarvestFestival.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HarvestFestival.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HarvestFestival.MOD_ID);
    public static final Supplier<DataComponentType<WateringCanState>> WATER_STATE = COMPONENTS.register(
            "watering_can_state", () -> DataComponentType.<WateringCanState>builder()
                    .persistent(WateringCanState.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(WateringCanState.CODEC)).build());
    public static final Supplier<DataComponentType<SickleState>> SICKLE_STATE = COMPONENTS.register(
            "sickle_state", () -> DataComponentType.<SickleState>builder().persistent(SickleState.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(SickleState.CODEC)).build());
    public static final Supplier<DataComponentType<FarmToolState>> FARM_TOOL_STATE = COMPONENTS.register(
            "farm_tool_state", () -> DataComponentType.<FarmToolState>builder().persistent(FarmToolState.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(FarmToolState.CODEC)).build());
    public static final DeferredItem<SickleItem> SICKLE = ITEMS.registerItem("sickle", SickleItem::new,
            props -> props.durability(SickleItem.MAX_DAMAGE));
    public static final DeferredItem<SickleItem> SICKLE_COPPER = ITEMS.registerItem("sickle_copper",
            props -> new SickleItem(props, ToolTier.COPPER), props -> props.durability(ToolTier.COPPER.durability()));
    public static final DeferredItem<SickleItem> SICKLE_SILVER = ITEMS.registerItem("sickle_silver",
            props -> new SickleItem(props, ToolTier.SILVER), props -> props.durability(ToolTier.SILVER.durability()));
    public static final DeferredBlock<PreparedSoilBlock> PREPARED_SOIL = BLOCKS.registerBlock(
            "prepared_soil", PreparedSoilBlock::new, props -> props.strength(0.6F).sound(SoundType.GRAVEL));
    public static final DeferredItem<HoeItem> HOE = hoe("hoe", ToolTier.BASIC);
    public static final DeferredItem<HoeItem> HOE_COPPER = hoe("hoe_copper", ToolTier.COPPER);
    public static final DeferredItem<HoeItem> HOE_SILVER = hoe("hoe_silver", ToolTier.SILVER);
    public static final DeferredItem<WateringCanItem> WATERING_CAN = can("watering_can", ToolTier.BASIC);
    public static final DeferredItem<WateringCanItem> WATERING_CAN_COPPER = can("watering_can_copper", ToolTier.COPPER);
    public static final DeferredItem<WateringCanItem> WATERING_CAN_SILVER = can("watering_can_silver", ToolTier.SILVER);

    private static DeferredItem<HoeItem> hoe(String id, ToolTier tier) {
        return ITEMS.registerItem(id, properties -> new HoeItem(properties, tier), props -> props.durability(tier.durability()));
    }
    private static DeferredItem<WateringCanItem> can(String id, ToolTier tier) {
        return ITEMS.registerItem(id, properties -> new WateringCanItem(properties, tier), props -> props.stacksTo(1));
    }

    public static final DeferredItem<HoeItem> HOE_GOLD = hoe("hoe_gold", ToolTier.GOLD);

    public static final DeferredItem<WateringCanItem> WATERING_CAN_GOLD = can("watering_can_gold", ToolTier.GOLD);

    public static final DeferredItem<HoeItem> HOE_MYSTRIL = hoe("hoe_mystril", ToolTier.MYSTRIL);

    public static final DeferredItem<WateringCanItem> WATERING_CAN_MYSTRIL = can("watering_can_mystril", ToolTier.MYSTRIL);

    public static final DeferredItem<SickleItem> SICKLE_GOLD = ITEMS.registerItem("sickle_gold", props -> new SickleItem(props, ToolTier.GOLD), props -> props.durability(ToolTier.GOLD.durability()));

    public static final DeferredItem<SickleItem> SICKLE_MYSTRIL = ITEMS.registerItem("sickle_mystril", props -> new SickleItem(props, ToolTier.MYSTRIL), props -> props.durability(ToolTier.MYSTRIL.durability()));

    private ToolContent() {}

    public static void register(IEventBus bus) {
        bus.addListener(WateringCanItem::registerTooltips);
        bus.addListener(HoeItem::registerTooltips);
        bus.addListener(SickleItem::registerTooltips);
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}

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
    public static final DeferredItem<SickleItem> SICKLE = ITEMS.registerItem("sickle", SickleItem::new,
            props -> props.durability(SickleItem.MAX_DAMAGE));
    public static final DeferredBlock<PreparedSoilBlock> PREPARED_SOIL = BLOCKS.registerBlock(
            "prepared_soil", PreparedSoilBlock::new, props -> props.strength(0.6F).sound(SoundType.GRAVEL));
    public static final DeferredItem<HoeItem> HOE = ITEMS.registerItem("hoe", HoeItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<WateringCanItem> WATERING_CAN = ITEMS.registerItem(
            "watering_can", WateringCanItem::new, props -> props.stacksTo(1));

    private ToolContent() {}

    public static void register(IEventBus bus) {
        bus.addListener(WateringCanItem::registerTooltips);
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}

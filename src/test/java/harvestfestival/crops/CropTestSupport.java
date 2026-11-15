package harvestfestival.crops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Real bundled resources and registry tags; restores shared registries after each test. */
public final class CropTestSupport implements AutoCloseable {
    public static final String[] RESOURCES = {
        "harvestfestival/crops/onion.json",
        "harvestfestival/loot_table/crops/onion.json",
        "harvestfestival/tags/item/seeds/onion.json",
        "c/tags/item/crops/onion.json",
        "harvestfestival/shipping_prices/tags/c/crops/onion.json",
        "harvestfestival/crops/spinach.json",
        "harvestfestival/loot_table/crops/spinach.json",
        "harvestfestival/tags/item/seeds/spinach.json",
        "c/tags/item/crops/spinach.json",
        "harvestfestival/shipping_prices/tags/c/crops/spinach.json",

        "harvestfestival/crops/wheat.json", "harvestfestival/loot_table/crops/wheat.json",
        "harvestfestival/tags/item/seeds/wheat.json", "harvestfestival/tags/item/sickles.json",
        "harvestfestival/shipping_prices/tags/c/crops/wheat.json", "harvestfestival/tool_settings/sickle.json",
        "harvestfestival/crops/cucumber.json", "harvestfestival/loot_table/crops/cucumber.json",
        "harvestfestival/tags/item/seeds/cucumber.json", "c/tags/item/crops/cucumber.json",
        "harvestfestival/shipping_prices/tags/c/crops/cucumber.json",
        "minecraft/tags/block/maintains_farmland.json", "harvestfestival/crops/turnip.json", "harvestfestival/loot_table/crops/turnip.json",
        "harvestfestival/tags/block/crops.json", "harvestfestival/tags/block/crop_soils.json",
        "harvestfestival/tags/block/waterable.json", "harvestfestival/tags/block/tillable.json",
        "harvestfestival/tags/item/seeds/turnip.json", "harvestfestival/tags/item/watering_cans.json", "harvestfestival/tags/item/hoes.json",
        "c/tags/item/crops/turnip.json", "c/tags/item/crops.json", "c/tags/item/seeds.json",
        "harvestfestival/shipping_prices/tags/c/crops/turnip.json",
        "harvestfestival/tool_actions/water.json", "harvestfestival/tool_actions/hoe.json"
    };
    public final MultiPackResourceManager manager;
    private final Map<TagKey<Block>, List<Holder<Block>>> blocks;
    private final Map<TagKey<Item>, List<Holder<Item>>> items;
    public CropTestSupport(Path path) throws Exception {
        bindItems();
        for (var file : RESOURCES) {
            var target = path.resolve("data/" + file);
            Files.createDirectories(target.getParent());
            try (var stream = CropContent.class.getResourceAsStream("/data/" + file)) {
                if (stream == null) throw new IllegalStateException(file);
                Files.copy(stream, target);
            }
        }
        blocks = BuiltInRegistries.BLOCK.listTags().collect(Collectors.toMap(HolderSet.Named::key, tag -> tag.stream().toList()));
        items = BuiltInRegistries.ITEM.listTags().collect(Collectors.toMap(HolderSet.Named::key, tag -> tag.stream().toList()));
        manager = manager(path);
        BuiltInRegistries.BLOCK.prepareTagReload(new TagLoader.LoadResult<>(Registries.BLOCK,
                TagLoader.loadTagsForRegistry(manager, Registries.BLOCK, (id, required) -> BuiltInRegistries.BLOCK.get(id)))).apply();
        var itemTags = new java.util.HashMap<>(TagLoader.loadTagsForRegistry(manager, Registries.ITEM, (id, required) -> BuiltInRegistries.ITEM.get(id)));
        // Supplied by NeoForge's common tags in the real game; this fixture loads only mod resources.
        itemTags.put(TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.parse("c:crops/wheat")),
                List.of(net.minecraft.world.item.Items.WHEAT.builtInRegistryHolder()));
        BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM, itemTags)).apply();
        CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(manager, null), manager, null);
    }
    public static void bindItems() {
        for (var item : List.of(CropContent.ONION.get(), CropContent.ONION_SEEDS.get(), CropContent.SPINACH.get(), CropContent.SPINACH_SEEDS.get(), CropContent.TURNIP.get(), CropContent.TURNIP_SEEDS.get(),
                CropContent.CUCUMBER.get(), CropContent.CUCUMBER_SEEDS.get(),
                CropContent.WHEAT_SEEDS.get(), net.minecraft.world.item.Items.WHEAT, net.minecraft.world.item.Items.WHEAT_SEEDS,
                harvestfestival.ShippingContent.SHIPPING_BOX.get().asItem(),
                harvestfestival.tools.ToolContent.WATERING_CAN.get(), harvestfestival.tools.ToolContent.HOE.get())) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
        harvestfestival.tools.ToolContent.SICKLE.get().builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 1).set(DataComponents.MAX_DAMAGE, 128).set(DataComponents.DAMAGE, 0).build());
        for (var item : List.of(harvestfestival.tools.ToolContent.HOE.get(), harvestfestival.tools.ToolContent.HOE_COPPER.get(),
                harvestfestival.tools.ToolContent.HOE_SILVER.get())) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1)
                    .set(DataComponents.MAX_DAMAGE, item.tier().durability()).set(DataComponents.DAMAGE, 0).build());
        }
        for (var item : List.of(harvestfestival.tools.ToolContent.WATERING_CAN.get(), harvestfestival.tools.ToolContent.WATERING_CAN_COPPER.get(),
                harvestfestival.tools.ToolContent.WATERING_CAN_SILVER.get())) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
        }
    }
    public static MultiPackResourceManager manager(Path... paths) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.Arrays.stream(paths)
                .<net.minecraft.server.packs.PackResources>map(path -> new PathPackResources(
                        new PackLocationInfo(path.toString(), Component.literal("Crop test"), PackSource.DEFAULT, Optional.empty()), path)).toList());
    }
    public static CropDefinition turnip() { return CropDefinitions.INSTANCE.get(net.minecraft.resources.Identifier.parse("harvestfestival:turnip")); }
    public static CropDefinition cucumber() { return CropDefinitions.INSTANCE.get(net.minecraft.resources.Identifier.parse("harvestfestival:cucumber")); }
    public void close() {
        CropDefinitions.INSTANCE.apply(Map.of(), null, null);
        BuiltInRegistries.BLOCK.prepareTagReload(new TagLoader.LoadResult<>(Registries.BLOCK, blocks)).apply();
        BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM, items)).apply();
        manager.close();
    }
}

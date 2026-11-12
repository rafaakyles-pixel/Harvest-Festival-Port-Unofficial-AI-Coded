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
        "minecraft/tags/block/maintains_farmland.json", "harvestfestival/crops/turnip.json", "harvestfestival/loot_table/crops/turnip.json",
        "harvestfestival/tags/block/crops.json", "harvestfestival/tags/block/crop_soils.json",
        "harvestfestival/tags/block/waterable.json", "harvestfestival/tags/block/tillable.json",
        "harvestfestival/tags/item/seeds/turnip.json", "harvestfestival/tags/item/watering_cans.json",
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
        BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM,
                TagLoader.loadTagsForRegistry(manager, Registries.ITEM, (id, required) -> BuiltInRegistries.ITEM.get(id)))).apply();
        CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(manager, null), manager, null);
    }
    public static void bindItems() {
        for (var item : List.of(CropContent.TURNIP.get(), CropContent.TURNIP_SEEDS.get(),
                harvestfestival.tools.ToolContent.WATERING_CAN.get(), harvestfestival.tools.ToolContent.HOE.get())) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }
    public static MultiPackResourceManager manager(Path... paths) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.Arrays.stream(paths)
                .<net.minecraft.server.packs.PackResources>map(path -> new PathPackResources(
                        new PackLocationInfo(path.toString(), Component.literal("Crop test"), PackSource.DEFAULT, Optional.empty()), path)).toList());
    }
    public static CropDefinition turnip() { return CropDefinitions.INSTANCE.get(net.minecraft.resources.Identifier.parse("harvestfestival:turnip")); }
    public void close() {
        CropDefinitions.INSTANCE.apply(Map.of(), null, null);
        BuiltInRegistries.BLOCK.prepareTagReload(new TagLoader.LoadResult<>(Registries.BLOCK, blocks)).apply();
        BuiltInRegistries.ITEM.prepareTagReload(new TagLoader.LoadResult<>(Registries.ITEM, items)).apply();
        manager.close();
    }
}

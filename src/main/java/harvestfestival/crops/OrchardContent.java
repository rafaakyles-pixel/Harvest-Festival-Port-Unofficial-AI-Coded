package harvestfestival.crops;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OrchardContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("harvestfestival");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("harvestfestival");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "harvestfestival");
    public static TagKey<Block> blockTag(String name) { return TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:" + name)); }
    public static final TagKey<Block> SOILS = blockTag("tree_soils"), REPLACEABLE = blockTag("tree_replaceable"),
            LOGS = blockTag("tree_leaf_supports"), LEAVES = blockTag("tree_leaves"), BUSHES = blockTag("tree_template_bushes");
    public static final TagKey<Fluid> WATER = TagKey.create(Registries.FLUID, Identifier.parse("harvestfestival:sprinkler_water"));
    public static final Map<String, TreeContent> TREES = new LinkedHashMap<>();
    public record TreeContent(DeferredBlock<TreeBlock> base, DeferredBlock<TreeTopBlock> top,
            DeferredBlock<FruitLeavesBlock> leaves, DeferredBlock<TreeFruitBlock> fruit,
            DeferredItem<TreeSeedItem> seed, Supplier<? extends Item> product) {}
    static { tree("peach"); tree("orange"); tree("banana"); tree("apple"); tree("grape"); }
    private static void tree(String species) {
        var base = BLOCKS.registerBlock(species + "_tree", p -> new TreeBlock(species, p), p -> p.noOcclusion().forceSolidOff().randomTicks().strength(.1F).sound(SoundType.WOOD).noLootTable());
        var top = BLOCKS.registerBlock(species + "_tree_top", p -> new TreeTopBlock(species, p), p -> p.noOcclusion().forceSolidOff().randomTicks().strength(.1F).sound(SoundType.WOOD).noLootTable());
        var leaves = BLOCKS.registerBlock(species + "_leaves", FruitLeavesBlock::new, p -> p.noOcclusion().isSuffocating((s,l,pos)->false).isViewBlocking((s,l,pos)->false).randomTicks().strength(.2F).sound(SoundType.GRASS));
        var fruit = BLOCKS.registerBlock(species + "_fruit", p -> new TreeFruitBlock(species, p), p -> p.noCollision().noOcclusion().sound(SoundType.GRASS));
        ITEMS.registerSimpleBlockItem(leaves);
        var seed = ITEMS.registerItem(species + "_seeds", p -> new TreeSeedItem(species, p));
        Supplier<? extends Item> product = species.equals("apple") ? () -> net.minecraft.world.item.Items.APPLE : ITEMS.registerSimpleItem(species);
        TREES.put(species, new TreeContent(base, top, leaves, fruit, seed, product));
    }
    public static final DeferredBlock<SprinklerBlock> OLD = BLOCKS.registerBlock("old_sprinkler", p -> new SprinklerBlock(1, .5, p), p -> p.noCollision().noOcclusion().sound(SoundType.GRAVEL));
    public static final DeferredBlock<SprinklerBlock> IRON = BLOCKS.registerBlock("iron_sprinkler", p -> new SprinklerBlock(4, .7, p), p -> p.noCollision().noOcclusion().sound(SoundType.GRAVEL));
    static { ITEMS.registerSimpleBlockItem(OLD); ITEMS.registerSimpleBlockItem(IRON); }
    public static final Supplier<BlockEntityType<TreeBlockEntity>> TREE_ENTITY = ENTITIES.register("fruit_tree", () -> new BlockEntityType<>(TreeBlockEntity::new,
            TREES.values().stream().map(t -> t.base().get()).toArray(Block[]::new)));
    public static final Supplier<BlockEntityType<SprinklerBlockEntity>> SPRINKLER_ENTITY = ENTITIES.register("sprinkler", () -> new BlockEntityType<>(SprinklerBlockEntity::new, OLD.get(), IRON.get()));
    public static void register(IEventBus bus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FruitSupport::tick);
        OrchardSync.register(bus);
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener(OrchardContent::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SPRINKLER_ENTITY.get(), (entity, face) -> entity.tank());
    }
    private OrchardContent() {}
}

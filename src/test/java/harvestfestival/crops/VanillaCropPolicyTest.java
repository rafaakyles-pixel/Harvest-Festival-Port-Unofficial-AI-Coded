package harvestfestival.crops;

import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VanillaCropPolicyTest {
    @TempDir Path directory;
    static VanillaCropPolicy policy(int mask) { return new VanillaCropPolicy((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0); }
    static void bindExtras() {
        for (var item : List.of(Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.NETHER_WART, Items.BONE_MEAL,
                Items.STICK, Items.SHORT_GRASS, Items.FERN, Items.SUNFLOWER, Items.DIAMOND_HOE))
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
    }

    @Test void wateringCansByTagBypassOnlyGrowthInteractionRestriction() throws Exception {
        try(var fixture=new CropTestSupport(directory)) {
            bindExtras(); var p=policy(15);
            for(var can:List.of(harvestfestival.tools.ToolContent.WATERING_CAN.get(),
                    harvestfestival.tools.ToolContent.WATERING_CAN_COPPER.get(),harvestfestival.tools.ToolContent.WATERING_CAN_SILVER.get())) {
                for(var block:List.of(Blocks.WHEAT,Blocks.CARROTS,Blocks.BEETROOTS,Blocks.POTATOES)) {
                    assertFalse(p.denyItemUse(new ItemStack(can),block.defaultBlockState()));
                    assertTrue(p.denyGrowth(block.defaultBlockState()));
                    assertTrue(p.denyItemUse(new ItemStack(Items.BONE_MEAL),block.defaultBlockState()));
                    assertTrue(p.denyItemUse(new ItemStack(Items.WHEAT_SEEDS),block.defaultBlockState()));
                }
            }
        }
    }

    @Test void allSixteenCombinationsKeepPlantingGrowthCropDropsAndGrassSeedsIndependent() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            bindExtras();
            for (int mask = 0; mask < 16; mask++) {
                var p = policy(mask);
                for (var seed : List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO)) {
                    var held = new ItemStack(seed, 3);
                    assertEquals(p.disablePlanting(), p.denyItemUse(held, Blocks.FARMLAND.defaultBlockState()));
                    assertEquals(p.disablePlanting() || p.disableGrowth(), p.denyItemUse(held, Blocks.WHEAT.defaultBlockState()));
                    assertEquals(3, held.getCount());
                }
                for (var block : List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.POTATOES)) {
                    var state = block.defaultBlockState();
                    assertEquals(p.disableGrowth(), p.denyGrowth(state));
                    assertEquals(p.disableGrowth(), p.denyItemUse(new ItemStack(Items.BONE_MEAL), state));
                    assertEquals(p.disableGrowth(), p.denyItemUse(new ItemStack(Items.STICK), state));
                    var drops = new ArrayList<>(List.of(new ItemStack(Items.POTATO), new ItemStack(Items.POISONOUS_POTATO), new ItemStack(Items.WHEAT_SEEDS)));
                    p.filterDrops(state, drops); assertEquals(p.disableDrops() ? 0 : 3, drops.size());
                }
                var grassDrops = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.SHORT_GRASS)));
                p.filterDrops(Blocks.SHORT_GRASS.defaultBlockState(), grassDrops);
                assertEquals(p.disableGrassSeeds() ? 1 : 2, grassDrops.size());
                assertTrue(grassDrops.getLast().is(Items.SHORT_GRASS));
            }
        }
    }

    @Test void defaultTagsExcludeHfCropsOtherSeedsAndUnrelatedLootAndPreserveShearedPlants() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            bindExtras(); var p = policy(15);
            for (var seed : List.of(CropContent.WHEAT_SEEDS.get(), CropContent.CARROT_SEEDS.get(), CropContent.BEETROOT_SEEDS.get(),
                    CropContent.POTATO_SEEDS.get(), Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.NETHER_WART, Items.DIAMOND_HOE))
                assertFalse(p.denyItemUse(new ItemStack(seed), Blocks.FARMLAND.defaultBlockState()));
            for (var block : List.of(CropContent.CROP.get(), Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.NETHER_WART, Blocks.STONE)) {
                assertFalse(p.denyGrowth(block.defaultBlockState()));
                var drops = new ArrayList<>(List.of(new ItemStack(Items.POTATO), new ItemStack(Items.WHEAT_SEEDS)));
                p.filterDrops(block.defaultBlockState(), drops); assertEquals(2, drops.size());
            }
            for (var block : List.of(Blocks.SHORT_GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN,
                    Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY)) {
                var drops = new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.FERN), new ItemStack(Items.SUNFLOWER)));
                p.filterDrops(block.defaultBlockState(), drops);
                assertEquals(List.of(Items.FERN, Items.SUNFLOWER), drops.stream().map(ItemStack::getItem).toList());
            }
        }
    }

    @Test void growthEventChangesOnlyTaggedCropsAndDoesNotOverrideUnrelatedResults() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            try {
                VanillaCropRules.INSTANCE.apply(policy(15), null, null);
                var event = new CropGrowEvent.Pre(null, BlockPos.ZERO, Blocks.CARROTS.defaultBlockState());
                VanillaCropEvents.grow(event); assertEquals(CropGrowEvent.Pre.Result.DO_NOT_GROW, event.getResult());
                var other = new CropGrowEvent.Pre(null, BlockPos.ZERO, Blocks.MELON_STEM.defaultBlockState());
                other.setResult(CropGrowEvent.Pre.Result.GROW);
                VanillaCropEvents.grow(other); assertEquals(CropGrowEvent.Pre.Result.GROW, other.getResult());
                VanillaCropRules.INSTANCE.apply(policy(13), null, null);
                var allowed = new CropGrowEvent.Pre(null, BlockPos.ZERO, Blocks.CARROTS.defaultBlockState());
                VanillaCropEvents.grow(allowed); assertEquals(CropGrowEvent.Pre.Result.DEFAULT, allowed.getResult());
            } finally { VanillaCropEvents.stopped(null); }
            assertEquals(VanillaCropPolicy.OPEN, VanillaCropRules.INSTANCE.active());
        }
    }

    @Test void missingInvalidAndHigherPriorityRulesFailOpenThenRestoreWithoutFallback() throws Exception {
        var low = directory.resolve("low"); var high = directory.resolve("high");
        try (var fixture = new CropTestSupport(low)) {
            var warnings = new ArrayList<String>();
            assertEquals(policy(15), VanillaCropRules.read(fixture.manager, warnings::add)); assertTrue(warnings.isEmpty());
            var path = high.resolve("data/harvestfestival/crop_settings/vanilla.json"); Files.createDirectories(path.getParent());
            var text = Files.readString(low.resolve("data/harvestfestival/crop_settings/vanilla.json"));
            var rules = new VanillaCropRules();
            for (String invalid : List.of("{", "null", "{}", text.replace("true", "\"true\""), text.replace("true", "1"), text.replace("true", "null"))) {
                Files.writeString(path, invalid);
                try (var manager = CropTestSupport.manager(low, high)) {
                    warnings.clear();
                    assertEquals(VanillaCropPolicy.OPEN, VanillaCropRules.read(manager, warnings::add));
                    assertEquals(1, warnings.size());
                    rules.apply(rules.prepare(manager, null), manager, null); assertEquals(VanillaCropPolicy.OPEN, rules.active());
                }
            }
            Files.writeString(path, text.replace("\"disable_planting\": true", "\"disable_planting\": false"));
            try (var manager = CropTestSupport.manager(low, high)) {
                rules.apply(rules.prepare(manager, null), manager, null); assertEquals(policy(14), rules.active());
            }
            Files.delete(path);
            try (var manager = CropTestSupport.manager(low, high)) {
                rules.apply(rules.prepare(manager, null), manager, null); assertEquals(policy(15), rules.active());
            }
            try (var manager = CropTestSupport.manager(high)) {
                warnings.clear(); assertEquals(VanillaCropPolicy.OPEN, VanillaCropRules.read(manager, warnings::add));
                assertEquals(1, warnings.size());
            }
        }
    }

    @Test void policyCodecRoundTripsEveryCombinationAndAttachmentDoesNotPersist() {
        for (int mask = 0; mask < 16; mask++) {
            var buffer = Unpooled.buffer();
            try {
                VanillaCropPolicy.STREAM_CODEC.encode(buffer, policy(mask));
                assertEquals(policy(mask), VanillaCropPolicy.STREAM_CODEC.decode(buffer)); assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
        var holder = new AttachmentHolder() {};
        assertEquals(VanillaCropPolicy.OPEN, holder.getData(VanillaCropPolicy.CLIENT_POLICY));
        holder.setData(VanillaCropPolicy.CLIENT_POLICY, policy(15));
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        holder.serializeAttachments(output);
        var reloaded = new AttachmentHolder() {
            void read(net.minecraft.world.level.storage.ValueInput input) { deserializeAttachments(input); }
        };
        var input = net.minecraft.world.level.storage.TagValueInput.create(ProblemReporter.DISCARDING,
                HolderLookup.Provider.create(java.util.stream.Stream.empty()), output.buildResult());
        reloaded.read(input);
        assertEquals(VanillaCropPolicy.OPEN, reloaded.getData(VanillaCropPolicy.CLIENT_POLICY));
        assertEquals(policy(15), holder.getData(VanillaCropPolicy.CLIENT_POLICY));
    }

    @Test void replacingTagsNarrowsBothInteractionAndDropScopeWithoutRecreatingPolicy() throws Exception {
        var low = directory.resolve("low"); var high = directory.resolve("high");
        try (var fixture = new CropTestSupport(low)) {
            var p = policy(15);
            assertTrue(p.denyItemUse(new ItemStack(Items.CARROT), Blocks.FARMLAND.defaultBlockState()));
            var crops = high.resolve("data/harvestfestival/tags/block/vanilla_crops.json");
            var seeds = high.resolve("data/harvestfestival/tags/item/vanilla_crop_seeds.json");
            Files.createDirectories(crops.getParent()); Files.createDirectories(seeds.getParent());
            Files.writeString(crops, "{\"replace\":true,\"values\":[\"minecraft:potatoes\"]}");
            Files.writeString(seeds, "{\"replace\":true,\"values\":[\"minecraft:wheat_seeds\"]}");
            try (var manager = CropTestSupport.manager(low, high)) {
                var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
                var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                blocks.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.BLOCK,
                        net.minecraft.tags.TagLoader.loadTagsForRegistry(manager, net.minecraft.core.registries.Registries.BLOCK,
                                (id, required) -> blocks.get(id)))).apply();
                items.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.tags.TagLoader.loadTagsForRegistry(manager, net.minecraft.core.registries.Registries.ITEM,
                                (id, required) -> items.get(id)))).apply();
            }
            assertFalse(p.denyGrowth(Blocks.WHEAT.defaultBlockState()));
            assertTrue(p.denyGrowth(Blocks.POTATOES.defaultBlockState()));
            assertFalse(p.denyItemUse(new ItemStack(Items.CARROT), Blocks.FARMLAND.defaultBlockState()));
            assertTrue(p.denyItemUse(new ItemStack(Items.WHEAT_SEEDS), Blocks.FARMLAND.defaultBlockState()));
            var drops = new ArrayList<>(List.of(new ItemStack(Items.CARROT), new ItemStack(Items.WHEAT_SEEDS)));
            p.filterDrops(Blocks.SHORT_GRASS.defaultBlockState(), drops);
            assertEquals(1, drops.size()); assertTrue(drops.getFirst().is(Items.CARROT));
        }
    }

    @Test void refreshReplacesStalePolicyAndInitializesReplacementHolderFromCurrentRules() {
        var first = new AttachmentHolder() {};
        var replacement = new AttachmentHolder() {};
        try {
            VanillaCropRules.INSTANCE.apply(policy(15), null, null);
            VanillaCropEvents.refresh(first); assertEquals(policy(15), first.getData(VanillaCropPolicy.CLIENT_POLICY));
            VanillaCropRules.INSTANCE.apply(policy(8), null, null);
            VanillaCropEvents.refresh(first); VanillaCropEvents.refresh(replacement);
            assertEquals(policy(8), first.getData(VanillaCropPolicy.CLIENT_POLICY));
            assertEquals(policy(8), replacement.getData(VanillaCropPolicy.CLIENT_POLICY));
            VanillaCropRules.INSTANCE.apply(VanillaCropPolicy.OPEN, null, null);
            VanillaCropEvents.refresh(first); assertEquals(VanillaCropPolicy.OPEN, first.getData(VanillaCropPolicy.CLIENT_POLICY));
            assertEquals(policy(8), replacement.getData(VanillaCropPolicy.CLIENT_POLICY));
        } finally { VanillaCropEvents.stopped(null); }
    }
}

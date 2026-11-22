package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VanillaSeedRecipesTest {
    @TempDir Path directory;
    @Test void fruitRecipesAreOneWayAndIndependentFromVanillaPolicy() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var melon=recipe("watermelon_seeds_from_hf"); var pumpkin=recipe("pumpkin_seeds_from_hf");
            var slices=recipe("melon_slices_from_block");
            assertInstanceOf(ShapelessRecipe.class,melon); assertInstanceOf(ShapelessRecipe.class,pumpkin);
            assertInstanceOf(ShapedRecipe.class,slices);
            for(int mask=0;mask<16;mask++) {
                VanillaCropRules.INSTANCE.apply(VanillaCropPolicyTest.policy(mask),null,null);
                result(melon,input(CropContent.WATERMELON_SEEDS.get()),Items.MELON_SEEDS,9);
                result(pumpkin,input(CropContent.PUMPKIN_SEEDS.get()),Items.PUMPKIN_SEEDS,9);
                result(slices,input(Items.MELON),Items.MELON_SLICE,9);
                for(var recipe:List.of(melon,pumpkin,slices)) {
                    for(var item:List.of(Items.MELON_SEEDS,Items.PUMPKIN_SEEDS,Items.MELON_SLICE,Items.PUMPKIN))
                        assertFalse(recipe.matches(input(item),null));
                }
            }
        } finally { VanillaCropEvents.stopped(null); }
    }
    private CraftingRecipe recipe(String name) throws Exception {
        var json = JsonParser.parseString(Files.readString(directory.resolve("data/harvestfestival/recipe/" + name + ".json")));
        var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        return assertInstanceOf(CraftingRecipe.class, Recipe.CODEC.parse(ops, json).getOrThrow());
    }
    private static CraftingInput input(Item item) { return CraftingInput.of(1, 1, List.of(new ItemStack(item))); }
    private static void result(CraftingRecipe recipe, CraftingInput input, Item item, int count) {
        assertTrue(recipe.matches(input, null));
        var output = recipe.assemble(input); assertTrue(output.is(item)); assertEquals(count, output.getCount());
    }
    @Test void bagsConvertToNineVanillaSeedsAndRejectWrongOrAdditionalIngredients() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var wheat = recipe("wheat_seeds_from_hf"); var beetroot = recipe("beetroot_seeds_from_hf");
            assertInstanceOf(ShapelessRecipe.class, wheat); assertInstanceOf(ShapelessRecipe.class, beetroot);
            result(wheat, input(CropContent.WHEAT_SEEDS.get()), Items.WHEAT_SEEDS, 9);
            result(beetroot, input(CropContent.BEETROOT_SEEDS.get()), Items.BEETROOT_SEEDS, 9);
            for (var seed : List.of(CropContent.TURNIP_SEEDS.get(), CropContent.CARROT_SEEDS.get(), CropContent.POTATO_SEEDS.get(), Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS)) {
                assertFalse(wheat.matches(input(seed), null)); assertFalse(beetroot.matches(input(seed), null));
            }
            assertFalse(wheat.matches(input(CropContent.BEETROOT_SEEDS.get()), null));
            assertFalse(beetroot.matches(input(CropContent.WHEAT_SEEDS.get()), null));
            assertFalse(wheat.matches(CraftingInput.of(2, 1, List.of(new ItemStack(CropContent.WHEAT_SEEDS.get()), new ItemStack(Items.WHEAT))), null));
        }
    }
    @Test void cropRecipesUseOriginalCountsAndTwoSeparateBeetrootIngredients() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var wheat = recipe("wheat_seeds_from_crop"); var beetroot = recipe("beetroot_seeds_from_crop");
            assertInstanceOf(ShapedRecipe.class, wheat); assertInstanceOf(ShapelessRecipe.class, beetroot);
            result(wheat, input(Items.WHEAT), Items.WHEAT_SEEDS, 1);
            result(beetroot, CraftingInput.of(2, 1, List.of(new ItemStack(Items.BEETROOT), new ItemStack(Items.BEETROOT))), Items.BEETROOT_SEEDS, 2);
            result(beetroot, CraftingInput.of(1, 2, List.of(new ItemStack(Items.BEETROOT), new ItemStack(Items.BEETROOT))), Items.BEETROOT_SEEDS, 2);
            assertFalse(beetroot.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.BEETROOT, 2))), null));
            for (var recipe : List.of(wheat, beetroot)) for (var item : List.of(Items.CARROT, Items.POTATO, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS))
                assertFalse(recipe.matches(input(item), null));
        }
    }
}

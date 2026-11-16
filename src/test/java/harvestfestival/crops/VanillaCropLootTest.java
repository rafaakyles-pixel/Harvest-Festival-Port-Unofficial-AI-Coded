package harvestfestival.crops;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VanillaCropLootTest {
    @TempDir Path directory;
    private static final class Manager extends LootModifierManager {
        void load(ResourceManager manager) { apply(prepare(manager, null), manager, null); }
    }
    // No server world: construct the exact vanilla context to exercise registered GLM.apply.
    private static LootContext context(BlockState state, boolean explosion) throws Exception {
        var builder = new LootParams.Builder(null);
        if (state != null) {
            builder.withParameter(LootContextParams.BLOCK_STATE, state).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
            if (explosion) builder.withParameter(LootContextParams.EXPLOSION_RADIUS, 4F);
        }
        var params = builder.create(state == null ? LootContextParamSets.EMPTY : LootContextParamSets.BLOCK);
        var constructor = LootContext.class.getDeclaredConstructor(LootParams.class, RandomSource.class, HolderGetter.Provider.class);
        constructor.setAccessible(true);
        return constructor.newInstance(params, RandomSource.create(1), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }
    @Test void realModifierManagerLoadsJsonAndFiltersBlockAndExplosionContextsButNotHfOrMissingState() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            VanillaCropPolicyTest.bindExtras();
            var manager = new Manager(); manager.load(fixture.manager);
            var modifier = manager.getModifier(Identifier.parse("harvestfestival:vanilla_crop_policy"));
            assertInstanceOf(VanillaCropLootModifier.class, modifier); assertEquals(1000, modifier.priority());
            assertEquals(1, java.util.stream.StreamSupport.stream(manager.getSortedModifiers().spliterator(), false).count());
            try {
                VanillaCropRules.INSTANCE.apply(VanillaCropPolicyTest.policy(15), null, null);
                for (boolean explosion : new boolean[]{false, true}) for (var block : List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.POTATOES)) {
                    var drops = new ObjectArrayList<>(List.of(new ItemStack(Items.POTATO), new ItemStack(Items.POISONOUS_POTATO)));
                    assertSame(drops, modifier.apply(drops, context(block.defaultBlockState(), explosion))); assertTrue(drops.isEmpty());
                }
                for (var block : List.of(CropContent.CROP.get(), Blocks.MELON_STEM, Blocks.STONE)) {
                    var drops = new ObjectArrayList<>(List.of(new ItemStack(Items.POTATO)));
                    modifier.apply(drops, context(block.defaultBlockState(), false)); assertEquals(1, drops.size());
                }
                var drops = new ObjectArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS)));
                modifier.apply(drops, context(null, false)); assertEquals(1, drops.size());
                VanillaCropRules.INSTANCE.apply(VanillaCropPolicy.OPEN, null, null);
                modifier.apply(drops, context(Blocks.WHEAT.defaultBlockState(), false)); assertEquals(1, drops.size());
            } finally { VanillaCropEvents.stopped(null); }
        }
    }
    @Test void grassModifierFiltersOnlyBlacklistedSeedsAndObservesRulesChangedAfterLoading() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            VanillaCropPolicyTest.bindExtras();
            var manager = new Manager(); manager.load(fixture.manager);
            var modifier = manager.getModifier(Identifier.parse("harvestfestival:vanilla_crop_policy"));
            try {
                for (int mask : new int[]{1, 8, 0, 15}) {
                    VanillaCropRules.INSTANCE.apply(VanillaCropPolicyTest.policy(mask), null, null);
                    var drops = new ObjectArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.FERN), new ItemStack(Items.MELON_SEEDS)));
                    modifier.apply(drops, context(Blocks.TALL_GRASS.defaultBlockState(), true));
                    assertEquals((mask & 8) != 0 ? 2 : 3, drops.size());
                    assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.FERN)));
                    assertTrue(drops.stream().anyMatch(stack -> stack.is(Items.MELON_SEEDS)));
                }
            } finally { VanillaCropEvents.stopped(null); }
        }
    }
}

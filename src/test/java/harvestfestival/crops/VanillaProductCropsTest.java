package harvestfestival.crops;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import harvestfestival.calendar.Season;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VanillaProductCropsTest {
    @TempDir Path directory;
    private static final List<String> NAMES = List.of("carrot", "beetroot", "potato");
    private static CropDefinition definition(String name) {
        return CropDefinitions.INSTANCE.get(Identifier.parse("harvestfestival:" + name));
    }
    private static Season season(String name) { return name.equals("potato") ? Season.SPRING : Season.AUTUMN; }

    private static com.google.gson.JsonObject asset(String path) throws Exception {
        try (var stream = CropContent.class.getResourceAsStream("/assets/harvestfestival/" + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void packagedAssetsCoverEveryNewSlotAndOriginalSeedBagColors() throws Exception {
        var variants = asset("blockstates/crop.json").getAsJsonObject("variants");
        int[] colors = {0xF8AC33, 0x690000, 0xBE8D2B};
        var models = List.of("carrots", "beetroots", "potatoes");
        for (int index = 0; index < NAMES.size(); index++) {
            var name = NAMES.get(index);
            for (int stage = 0; stage < 4; stage++) assertEquals("minecraft:block/" + models.get(index) + "_stage" + stage,
                    variants.getAsJsonObject("appearance=" + (25 + index * 4 + stage)).get("model").getAsString());
            var item = asset("items/" + name + "_seeds.json").getAsJsonObject("model");
            assertEquals("harvestfestival:item/" + name + "_seeds", item.get("model").getAsString());
            for (var tint : item.getAsJsonArray("tints")) assertEquals(colors[index], tint.getAsJsonObject().get("value").getAsInt());
            var textures = asset("models/item/" + name + "_seeds.json").getAsJsonObject("textures");
            assertEquals("harvestfestival:item/seed_bag_neck", textures.get("layer0").getAsString());
            assertEquals("harvestfestival:item/seed_bag_body", textures.get("layer1").getAsString());
            for (var lang : List.of("pt_br", "en_us"))
                assertTrue(asset("lang/" + lang + ".json").has("item.harvestfestival." + name + "_seeds"));
        }
    }

    @Test void stagesSeasonsSeedsAndVanillaVisualsMatchEachSpecies() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var seeds = List.of(CropContent.CARROT_SEEDS.get(), CropContent.BEETROOT_SEEDS.get(), CropContent.POTATO_SEEDS.get());
            var appearances = List.of(List.of(25,25,26,26,27,27,27,28), List.of(29,30,31,32), List.of(33,33,34,34,35,35,35,36));
            for (int index = 0; index < NAMES.size(); index++) {
                var name = NAMES.get(index);
                var d = definition(name);
                assertEquals(d, CropDefinitions.INSTANCE.forSeed(new ItemStack(seeds.get(index))));
                assertEquals(0, d.regrowStage()); assertEquals(1, d.maxHarvests());
                assertFalse(d.requiresTool()); assertTrue(d.harvestOnPlayerBreak());
                assertEquals(CropDefinition.EmptyHarvestPolicy.ADVANCE, d.emptyHarvestPolicy());
                assertEquals(appearances.get(index), d.appearances());
                var state = CropState.planted(d.id());
                for (int stage = 1; stage <= d.maturity(); stage++) {
                    assertEquals(stage, state.stage());
                    assertEquals(stage == d.maturity(), CropLifecycle.canHarvest(state, d));
                    var block = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE, d.appearance(stage));
                    var encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, block).getOrThrow();
                    assertEquals(block, BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
                    state = CropLifecycle.newDay(state, d, season(name), true, false);
                }
                assertNull(CropLifecycle.afterHarvest(state, d));
                assertEquals(name.equals("beetroot") ? 4 : 8, d.maturity());
            }
            assertNull(CropDefinitions.INSTANCE.forSeed(new ItemStack(Items.CARROT)));
            assertNull(CropDefinitions.INSTANCE.forSeed(new ItemStack(Items.POTATO)));
            assertNull(CropDefinitions.INSTANCE.forSeed(new ItemStack(Items.BEETROOT_SEEDS)));
            for (String legacy : List.of("turnip", "onion", "spinach")) assertFalse(definition(legacy).harvestOnPlayerBreak());
        }
    }

    @Test void waterDroughtRescueDedicatedAndSeasonalDeathUseExistingLifecycle() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : NAMES) {
                var d = definition(name); var dry = CropState.planted(d.id());
                assertTrue(d.requiresWater()); assertEquals(1, dry.dryDays());
                for (int day = 0; day < 2; day++) {
                    dry = CropLifecycle.newDay(dry, d, season(name), false, false);
                    assertFalse(dry.dead()); assertEquals(1, dry.stage());
                }
                var dead = CropLifecycle.newDay(dry, d, season(name), false, false);
                assertTrue(dead.dead());
                assertEquals(dead, CropLifecycle.newDay(dead, d, season(name), true, false));
                assertEquals(2, CropLifecycle.newDay(dry, d, season(name), true, false).stage());
                for (int day = 0; day < 10; day++) dry = CropLifecycle.newDay(dry, d, season(name), false, true);
                assertFalse(dry.dead()); assertEquals(1, dry.stage());
                for (var wrong : Season.values()) if (wrong != season(name))
                    assertTrue(CropLifecycle.newDay(dry, d, wrong, true, true).dead());
            }
        }
    }

    @Test void harvestAndBreakCommitBeforeProductsAndInvalidateSecondDropPass() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var products = List.of(Items.CARROT, Items.BEETROOT, Items.POTATO);
            for (int index = 0; index < NAMES.size(); index++) for (boolean breaking : new boolean[]{false, true}) {
                var d = definition(NAMES.get(index));
                var mature = new CropState(d.id(), d.maturity(), 1, false);
                var entity = CropRegrowthTest.entity(mature);
                var present = new AtomicBoolean(true);
                var drops = new ArrayList<ItemStack>();
                var product = products.get(index);
                java.util.function.Consumer<ItemStack> output = stack -> {
                    assertFalse(present.get()); assertEquals(CropState.INVALID, entity.crop()); drops.add(stack);
                };
                assertFalse(CropBlock.harvestOnce(mature, d, () -> List.of(new ItemStack(product)), next -> false, output));
                assertFalse(CropBlock.breakOnce(mature, d, () -> List.of(new ItemStack(product)), () -> false, output));
                if (breaking) assertTrue(CropBlock.breakOnce(mature, d, () -> List.of(new ItemStack(product)),
                        () -> entity.commitHarvest(mature, null, () -> present.getAndSet(false)), output));
                else assertTrue(CropBlock.harvestOnce(mature, d, () -> List.of(new ItemStack(product)),
                        next -> entity.commitHarvest(mature, next, () -> present.getAndSet(false)), output));
                assertEquals(1, drops.size()); assertTrue(drops.getFirst().is(product));
                assertTrue(CropContent.CROP.get().getDrops(entity.getBlockState(), new LootParams.Builder(null)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity)).isEmpty());
                assertFalse(CropBlock.harvestOnce(mature, d, () -> List.of(new ItemStack(product)),
                        next -> entity.commitHarvest(mature, next, () -> fail("stale snapshot")), output));
            }
        }
    }

    @Test void emptyLootRemovesButIneligiblePlantsNeverEvaluateDrops() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            for (var name : NAMES) {
                var d = definition(name);
                var mature = new CropState(d.id(), d.maturity(), 1, false);
                var entity = CropRegrowthTest.entity(mature);
                assertTrue(CropBlock.harvestOnce(mature, d, List::of,
                        next -> entity.commitHarvest(mature, next, () -> true), stack -> fail("empty")));
                assertEquals(CropState.INVALID, entity.crop());
                for (var state : List.of(CropState.planted(d.id()), new CropState(d.id(), d.maturity(), 3, true))) {
                    assertFalse(CropBlock.harvestOnce(state, d, () -> fail("no rolls"), next -> fail("ineligible"), stack -> fail("ineligible")));
                    assertTrue(CropBlock.breakOnce(state, d, () -> fail("no rolls"), () -> true, stack -> fail("ineligible")));
                    var ineligible = CropRegrowthTest.entity(state);
                    assertTrue(CropContent.CROP.get().getDrops(ineligible.getBlockState(), new LootParams.Builder(null)
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ineligible)).isEmpty());
                }
            }
        }
    }

    @Test void entitiesAndVisualSlotsPersistAndInvalidReloadDoesNotResetSavedProgress() throws Exception {
        try (var fixture = new CropTestSupport(directory.resolve("pack"))) {
            var registry = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for (var name : NAMES) {
                var d = definition(name);
                for (int stage : new int[]{1, d.maturity() - 1, d.maturity()}) for (boolean dead : new boolean[]{false, true}) {
                    var state = new CropState(d.id(), stage, 3, dead);
                    var block = CropContent.CROP.get().defaultBlockState().setValue(CropBlock.APPEARANCE, dead ? 3 : d.appearance(stage));
                    var entity = new CropBlockEntity(BlockPos.ZERO, block); entity.setCrop(state);
                    var file = directory.resolve(name + stage + dead + ".nbt");
                    NbtIo.writeCompressed(entity.saveWithFullMetadata(registry), file);
                    var copy = (CropBlockEntity) BlockEntity.loadStatic(BlockPos.ZERO, block, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), registry);
                    assertNotNull(copy); assertEquals(state, copy.crop());
                }
                var path = directory.resolve("pack/data/harvestfestival/crops/" + name + ".json");
                var json = Files.readString(path);
                Files.writeString(path, json.replace("\"harvest_on_player_break\": true", "\"harvest_on_player_break\": \"true\""));
                CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager, null), fixture.manager, null);
                assertNull(definition(name));
                var saved = new CropState(d.id(), d.maturity() - 1, 2, false);
                assertEquals(saved, CropLifecycle.newDay(saved, definition(name), season(name), true, false));
                Files.writeString(path, json);
                CropDefinitions.INSTANCE.apply(CropDefinitions.INSTANCE.prepare(fixture.manager, null), fixture.manager, null);
                assertEquals(d.maturity(), CropLifecycle.newDay(saved, definition(name), season(name), true, false).stage());
            }
        }
    }

    static PotatoHarvestLoot potatoLoot(Path pack) throws Exception {
        var json = JsonParser.parseString(Files.readString(pack.resolve("data/harvestfestival/loot_table/crops/potato.json")));
        var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        assertNotNull(LootTable.DIRECT_CODEC.parse(ops, json).getOrThrow());
        var entry = json.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0);
        return assertInstanceOf(PotatoHarvestLoot.class, LootPoolEntries.CODEC.parse(ops, entry).getOrThrow());
    }

    @Test void registeredPotatoLootPreservesEveryLoopRollAndPoisonReplacesEachUnit() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var loot = potatoLoot(directory);
            // The first roll must not determine quantity. Only the second condition permits unit two.
            int[][] values = {{0, 1, 1}, {1, 0, 1}, {1, 1, 0, 0, 0}, {0, 0, 0, 0, 1}, {1, 0, 0, 1, 1}};
            var expected = List.of(List.of(Items.POTATO), List.of(Items.POISONOUS_POTATO),
                    List.of(Items.POTATO, Items.POISONOUS_POTATO), List.of(Items.POISONOUS_POTATO, Items.POISONOUS_POTATO),
                    List.of(Items.POISONOUS_POTATO, Items.POTATO));
            for (int test = 0; test < values.length; test++) {
                int[] sequence = values[test], cursor = {0};
                var bounds = new ArrayList<Integer>(); var drops = new ArrayList<ItemStack>();
                loot.roll(bound -> { bounds.add(bound); return sequence[cursor[0]++]; }, drops::add);
                assertEquals(sequence.length, cursor[0]);
                assertEquals(sequence.length == 3 ? List.of(20,50,20) : List.of(20,50,20,50,20), bounds);
                assertEquals(expected.get(test), drops.stream().map(ItemStack::getItem).toList());
                assertTrue(drops.stream().allMatch(stack -> stack.getCount() == 1));
            }
        }
    }

    @Test void allLootTablesDecodeAndPotatoDivisorsRejectZeroOrUnknownProducts() throws Exception {
        try (var fixture = new CropTestSupport(directory)) {
            var ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for (var name : NAMES) {
                var text = Files.readString(directory.resolve("data/harvestfestival/loot_table/crops/" + name + ".json"));
                assertNotNull(LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString(text)).getOrThrow());
                if (name.equals("potato")) for (String invalid : List.of(text.replace("\"extra_divisor\": 20", "\"extra_divisor\": 0"),
                        text.replace("\"poison_divisor\": 50", "\"poison_divisor\": 0"), text.replace("minecraft:potato", "test:missing"))) {
                    assertTrue(LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString(invalid)).error().isPresent());
                }
            }
        }
    }
}

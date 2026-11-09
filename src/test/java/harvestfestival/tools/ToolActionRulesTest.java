package harvestfestival.tools;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ToolActionRulesTest {
    @TempDir Path directory;
    static final String HOE = """
            {"action":"hoe","target_tag":"harvestfestival:tillable","requires_air_above":true,
             "result":{"block":"harvestfestival:prepared_soil","properties":{"watered":"false"}}}
            """;
    static final String WATER = """
            {"action":"water","target_tag":"harvestfestival:waterable","requires_properties":{"watered":"false"},
             "result":{"block":"harvestfestival:prepared_soil","properties":{"watered":"true"}}}
            """;
    @BeforeAll static void setup() { WateringCanStateTest.bindComponents(); }

    @Test
    void invalidRulesAreIgnoredAlongsideValidRule() {
        var invalid = List.of("{", "null", HOE.replace("\"hoe\"", "\"sickle\""),
                HOE.replace("harvestfestival:prepared_soil", "test:unknown"),
                HOE.replace("\"watered\"", "\"unknown\""), HOE.replace("\"false\"", "\"invalid\""),
                HOE.replace(":true", ":\"true\""), HOE.replace("\"target_tag\"", "\"missing\""),
                WATER.replace("\"false\"", "\"invalid\""));
        for (var json : invalid) {
            var warnings = new ArrayList<String>();
            var rules = ToolActionRules.read(Map.ofEntries(resource("valid", WATER), resource("invalid", json)), warnings::add);
            assertEquals(1, rules.size(), json);
            assertEquals(1, warnings.size(), json);
            assertTrue(warnings.getFirst().contains("invalid.json"));
        }
    }

    @Test
    void reloadReplacesTableAndRemovedOrInvalidRuleCannotLinger() {
        var listener = new ToolActionRules();
        listener.apply(read("water", WATER), null, null);
        assertEquals(1, listener.rules().size());
        listener.apply(ToolActionRules.read(Map.ofEntries(resource("water", "{")), ignored -> {}), null, null);
        assertTrue(listener.rules().isEmpty());
        listener.apply(read("hoe", HOE), null, null);
        assertEquals(ToolAction.HOE, listener.rules().getFirst().action());
        listener.apply(List.of(), null, null);
        assertTrue(listener.rules().isEmpty());
    }

    @Test
    void actualPacksOverrideAndInvalidWinningRuleDoesNotFallback() throws Exception {
        write("low", "hoe", HOE);
        write("low", "water", WATER);
        write("high", "water", WATER.replace("waterable", "other"));
        var listener = new ToolActionRules();
        try (var manager = manager(directory.resolve("low"), directory.resolve("high"))) {
            listener.apply(listener.prepare(manager, null), manager, null);
        }
        assertEquals(2, listener.rules().size());
        assertTrue(listener.rules().stream().anyMatch(rule -> rule.targetTag().location().getPath().equals("other")));
        write("high", "water", "{");
        var warnings = new ArrayList<String>();
        try (var manager = manager(directory.resolve("low"), directory.resolve("high"))) {
            listener.apply(ToolActionRules.read(manager.listResources("tool_actions", id -> id.getPath().endsWith(".json")),
                    warnings::add), manager, null);
        }
        assertEquals(1, warnings.size());
        assertEquals(1, listener.rules().size());
        assertEquals(ToolAction.HOE, listener.rules().getFirst().action());
        try (var manager = manager(directory.resolve("low"))) {
            listener.apply(listener.prepare(manager, null), manager, null);
        }
        assertEquals(2, listener.rules().size());
    }

    @Test
    void bundledRulesAndRealBlockTagsPrepareAndWaterTheSameTarget() throws Exception {
        var registry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var key = net.minecraft.core.registries.Registries.BLOCK;
        var saved = registry.listTags().collect(java.util.stream.Collectors.toMap(
                net.minecraft.core.HolderSet.Named::key, tag -> tag.stream().toList()));
        var bundled = directory.resolve("bundled");
        for (var file : List.of("tool_actions/hoe.json", "tool_actions/water.json",
                "tags/block/tillable.json", "tags/block/waterable.json")) {
            var resource = "data/harvestfestival/" + file;
            var destination = bundled.resolve(resource);
            Files.createDirectories(destination.getParent());
            try (var input = ToolContent.class.getResourceAsStream("/" + resource)) {
                assertNotNull(input, resource);
                Files.copy(input, destination);
            }
        }
        try (var manager = manager(bundled)) {
            var tags = net.minecraft.tags.TagLoader.loadTagsForRegistry(manager, key,
                    (id, required) -> registry.get(id));
            registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(key, tags)).apply();
            var listener = new ToolActionRules();
            listener.apply(listener.prepare(manager, null), manager, null);
            assertEquals(2, listener.rules().size());
            var target = new ToolActions.Target() {
                net.minecraft.world.level.block.state.BlockState state = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
                public net.minecraft.world.level.block.state.BlockState state() { return state; }
                public boolean airAbove() { return true; }
                public boolean canEdit() { return true; }
                public boolean set(net.minecraft.world.level.block.state.BlockState value) { state = value; return true; }
            };
            assertEquals(ToolActions.Result.CHANGED, ToolActions.apply(ToolAction.HOE, target,
                    new net.minecraft.world.item.ItemStack(ToolContent.HOE.get()), false, listener.rules(), message -> fail(message)));
            var can = ToolActionsTest.fullCan();
            assertEquals(ToolActions.Result.CHANGED, ToolActions.apply(ToolAction.WATER, target, can,
                    false, listener.rules(), message -> fail(message)));
            assertTrue(target.state().getValue(PreparedSoilBlock.WATERED));
            assertEquals(127, WateringCanState.get(can).water());
        } finally {
            registry.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(key, saved)).apply();
        }
    }

    private void write(String pack, String name, String json) throws Exception {
        var path = directory.resolve(pack + "/data/harvestfestival/tool_actions/" + name + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

    private MultiPackResourceManager manager(Path... paths) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.Arrays.stream(paths)
                .<net.minecraft.server.packs.PackResources>map(path -> new PathPackResources(
                        new PackLocationInfo(path.toString(), Component.literal("Tools test"), PackSource.DEFAULT, Optional.empty()),
                        path)).toList());
    }

    @Test
    void missingTagOrRequiredPropertyDoesNotMatch() {
        var water = read("water", WATER).getFirst();
        var soil = ToolActionsTest.dry();
        assertTrue(water.matches(ToolAction.WATER, soil));
        assertFalse(water.matches(ToolAction.HOE, soil));
        soil.tags = java.util.Set.of();
        assertFalse(water.matches(ToolAction.WATER, soil));
        var dirt = new ToolActionsTest.Target(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), "waterable");
        assertFalse(water.matches(ToolAction.WATER, dirt));
    }

    static List<ToolActionRules.Rule> read(String name, String json) {
        return ToolActionRules.read(Map.ofEntries(resource(name, json)), message -> fail(message));
    }
    static Map.Entry<Identifier, Resource> resource(String name, String json) {
        return Map.entry(Identifier.parse("harvestfestival:tool_actions/" + name + ".json"),
                new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }
}

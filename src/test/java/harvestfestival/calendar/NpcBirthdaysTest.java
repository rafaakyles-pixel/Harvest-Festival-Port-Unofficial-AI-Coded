package harvestfestival.calendar;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NpcBirthdaysTest {
    @TempDir Path directory;
    private static final Identifier TARGET = Identifier.parse("test:npc/example");

    @Test
    void invalidRulesDoNotDiscardValidSiblingAndWarningsContainPaths() {
        var warnings = new ArrayList<String>();
        var rules = Map.ofEntries(rule("valid", "{\"season\":\"summer\",\"day\":3}"),
                rule("malformed", "{"), rule("season", "{\"season\":\"unknown\",\"day\":1}"),
                rule("day", "{\"season\":\"spring\",\"day\":31}"),
                rule("fraction", "{\"season\":\"spring\",\"day\":1.5}"));
        var table = NpcBirthdays.read(rules, warnings::add);
        assertEquals(1, table.size());
        assertEquals(new BirthdayDate(Season.SUMMER, 3), table.get(Identifier.parse("test:valid")));
        assertEquals(4, warnings.size());
        for (String path : List.of("malformed", "season", "day", "fraction")) {
            assertTrue(warnings.stream().anyMatch(message -> message.contains("birthdays/test/" + path + ".json")));
        }
    }

    @Test
    void pureQueriesHandleEqualDifferentAndAbsentDates() {
        var birthdays = new NpcBirthdays();
        birthdays.apply(Map.of(TARGET, new BirthdayDate(Season.SUMMER, 3)), null, null);
        assertEquals(Optional.of(new BirthdayDate(Season.SUMMER, 3)), birthdays.get(TARGET));
        assertTrue(birthdays.isBirthday(TARGET, CalendarDate.of(3, Season.SUMMER, 9)));
        assertFalse(birthdays.isBirthday(TARGET, CalendarDate.of(4, Season.SUMMER, 9)));
        assertFalse(birthdays.isBirthday(TARGET, CalendarDate.of(3, Season.WINTER, 9)));
        assertTrue(birthdays.get(Identifier.parse("test:absent")).isEmpty());
        assertFalse(birthdays.isBirthday(Identifier.parse("test:absent"), CalendarDate.of(3, Season.SUMMER, 9)));
    }

    @Test
    void realPackPrecedenceAndReloadReplaceTheWholeTable() throws Exception {
        write("low", "example", "{\"season\":\"spring\",\"day\":15}");
        write("high", "example", "{\"season\":\"winter\",\"day\":30}");
        write("high", "removed", "{\"season\":\"summer\",\"day\":2}");
        var birthdays = new NpcBirthdays();
        try (var resources = manager("low", "high")) {
            birthdays.apply(birthdays.prepare(resources, null), resources, null);
        }
        assertEquals(new BirthdayDate(Season.WINTER, 30), birthdays.get(TARGET).orElseThrow());
        assertTrue(birthdays.get(Identifier.parse("test:npc/removed")).isPresent());
        try (var resources = manager("low")) {
            birthdays.apply(birthdays.prepare(resources, null), resources, null);
        }
        assertEquals(new BirthdayDate(Season.SPRING, 15), birthdays.get(TARGET).orElseThrow());
        assertTrue(birthdays.get(Identifier.parse("test:npc/removed")).isEmpty());
    }

    @Test
    void invalidWinningPackDiscardsRuleWithoutFallingBack() throws Exception {
        write("low", "example", "{\"season\":\"spring\",\"day\":15}");
        write("high", "example", "{");
        write("low", "valid", "{\"season\":\"summer\",\"day\":2}");
        var warnings = new ArrayList<String>();
        var birthdays = new NpcBirthdays();
        try (var resources = manager("low")) {
            birthdays.apply(birthdays.prepare(resources, null), resources, null);
        }
        assertTrue(birthdays.get(TARGET).isPresent());
        try (var resources = manager("low", "high")) {
            var table = NpcBirthdays.read(resources.listResources("birthdays", id -> id.getPath().endsWith(".json")), warnings::add);
            birthdays.apply(table, resources, null);
        }
        assertTrue(birthdays.get(TARGET).isEmpty());
        assertTrue(birthdays.get(Identifier.parse("test:npc/valid")).isPresent());
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("harvestfestival:birthdays/test/npc/example.json"));
    }

    private static Map.Entry<Identifier, Resource> rule(String path, String json) {
        return Map.entry(Identifier.parse("harvestfestival:birthdays/test/" + path + ".json"),
                new Resource(null, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    private void write(String pack, String name, String json) throws Exception {
        var path = directory.resolve(pack + "/data/harvestfestival/birthdays/test/npc/" + name + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

    private MultiPackResourceManager manager(String... names) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.Arrays.stream(names)
                .<net.minecraft.server.packs.PackResources>map(name -> new PathPackResources(
                        new PackLocationInfo(name, Component.literal(name), PackSource.DEFAULT, Optional.empty()),
                        directory.resolve(name))).toList());
    }
}

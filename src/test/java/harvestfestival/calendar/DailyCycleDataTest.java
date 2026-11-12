package harvestfestival.calendar;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyCycleDataTest {
    @Test void sequenceSurvivesReloadAndDoesNotDependOnAdministrativeDate() {
        var sequence = new DailyCycleData(0);
        var tracker = new DailyTransition();
        tracker.begin(23999);
        assertTrue(tracker.finish(24000).isPresent());
        assertEquals(1, sequence.next());
        var encoded = DailyCycleData.CODEC.encodeStart(NbtOps.INSTANCE, sequence).getOrThrow();
        var reopened = DailyCycleData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(1, reopened.sequence());
        // Rebase at an earlier date does not allocate an event; its next real boundary does.
        tracker.begin(0);
        assertTrue(tracker.finish(0).isEmpty());
        assertEquals(1, reopened.sequence());
        tracker.begin(23999);
        assertTrue(tracker.finish(24000).isPresent());
        assertEquals(2, reopened.next());
    }
    @Test void invalidSequenceDoesNotResetToZeroOrWrap() {
        for (var json : new String[]{"{}", "null", "{\"sequence\":-1}", "{\"sequence\":1.5}", "{\"sequence\":\"7\"}"}) {
            var data = DailyCycleData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
            assertEquals(Long.MAX_VALUE, data.sequence());
            assertEquals(0, data.next());
        }
        assertEquals(0, new DailyCycleData(Long.MAX_VALUE).next());
    }
}

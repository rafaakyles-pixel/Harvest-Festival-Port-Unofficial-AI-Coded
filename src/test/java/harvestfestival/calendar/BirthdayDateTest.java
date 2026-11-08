package harvestfestival.calendar;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BirthdayDateTest {
    @Test
    void validatesCalendarLimitsInValueAndCodec() {
        for (Season season : Season.values()) {
            for (int day : new int[]{1, CalendarDate.DAYS_PER_SEASON}) {
                var date = new BirthdayDate(season, day);
                assertEquals(date, BirthdayDate.CODEC.parse(JsonOps.INSTANCE,
                        BirthdayDate.CODEC.encodeStart(JsonOps.INSTANCE, date).getOrThrow()).getOrThrow());
            }
        }
        for (int day : new int[]{-1, 0, 31}) {
            assertThrows(IllegalArgumentException.class, () -> new BirthdayDate(Season.SPRING, day));
            assertTrue(BirthdayDate.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                    "{\"season\":\"spring\",\"day\":" + day + "}")).error().isPresent());
        }
        assertTrue(BirthdayDate.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"season\":\"unknown\",\"day\":1}")).error().isPresent());
    }

    @Test
    void annualMatchIgnoresYearButRequiresSeasonAndDay() {
        var birthday = new BirthdayDate(Season.AUTUMN, 12);
        assertTrue(birthday.matches(CalendarDate.of(12, Season.AUTUMN, 7)));
        assertFalse(birthday.matches(CalendarDate.of(13, Season.AUTUMN, 7)));
        assertFalse(birthday.matches(CalendarDate.of(12, Season.SPRING, 7)));
    }
}

package harvestfestival.calendar;

import java.nio.file.Path;
import java.time.DayOfWeek;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.clock.ClockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CalendarDateTest {
    @TempDir
    Path directory;

    @Test
    void startsOnMondayInSpringOfYearOne() {
        var date = CalendarDate.fromTicks(0);
        assertEquals(1, date.day());
        assertEquals(Season.SPRING, date.season());
        assertEquals(1L, date.year());
        assertEquals(DayOfWeek.MONDAY, date.weekday());
        assertEquals(date, CalendarDate.fromTicks(23999));
    }

    @Test
    void advancesAtDaySeasonAndYearBoundaries() {
        assertEquals(CalendarDate.of(2, Season.SPRING, 1), CalendarDate.fromTicks(24000));
        Season[] seasons = Season.values();
        for (int index = 0; index < seasons.length; index++) {
            long boundary = (index + 1L) * 30 * 24000;
            assertEquals(CalendarDate.of(30, seasons[index], 1), CalendarDate.fromTicks(boundary - 1));
            assertEquals(CalendarDate.of(1, seasons[(index + 1) % 4], index == 3 ? 2 : 1),
                    CalendarDate.fromTicks(boundary));
        }
    }

    @Test
    void preservesTimeOfDayAndUsesLongArithmetic() {
        var date = CalendarDate.of(30, Season.WINTER, Integer.MAX_VALUE);
        long ticks = date.atTimeOfDay(12500);
        assertEquals(12500, ticks % CalendarDate.TICKS_PER_DAY);
        assertEquals(date, CalendarDate.fromTicks(ticks));
        assertEquals(CalendarDate.of(1, Season.SPRING, 1), CalendarDate.fromTicks(-1));
        assertThrows(IllegalArgumentException.class, () -> CalendarDate.of(31, Season.SPRING, 1));
        assertThrows(IllegalArgumentException.class, () -> CalendarDate.of(1, Season.SPRING, 0));
    }

    @Test
    void derivesSameDateAfterClockStateIsSavedAndReloaded() throws Exception {
        var date = CalendarDate.of(30, Season.WINTER, 7);
        var original = new ClockState(date.atTimeOfDay(18000), 0.5F, 1.0F, true);
        var tag = (CompoundTag) ClockState.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        Path file = directory.resolve("clock.dat");
        NbtIo.writeCompressed(tag, file);
        var loaded = ClockState.CODEC.parse(NbtOps.INSTANCE,
                NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())).getOrThrow();
        assertEquals(original, loaded);
        assertEquals(date, CalendarDate.fromTicks(loaded.totalTicks()));
    }

    @Test
    void weekdayAndDatesRoundTripAcrossMultipleYears() {
        for (long day = 0; day < 1200; day++) {
            var date = new CalendarDate(day);
            assertEquals(date, CalendarDate.of(date.day(), date.season(), date.year()));
            assertEquals(date, CalendarDate.fromTicks(date.startTicks()));
            assertEquals(DayOfWeek.of((int) (day % 7) + 1), date.weekday());
        }
    }
}
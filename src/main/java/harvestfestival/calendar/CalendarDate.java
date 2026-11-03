package harvestfestival.calendar;

import java.time.DayOfWeek;
import java.util.Objects;

public record CalendarDate(long elapsedDays) {
    public static final int DAYS_PER_SEASON = 30;
    public static final int DAYS_PER_YEAR = DAYS_PER_SEASON * 4;
    public static final long TICKS_PER_DAY = 24000L;

    public CalendarDate {
        if (elapsedDays < 0 || elapsedDays > Long.MAX_VALUE / TICKS_PER_DAY) {
            throw new IllegalArgumentException("Date is outside the world clock range");
        }
    }

    public static CalendarDate fromTicks(long ticks) {
        return new CalendarDate(Math.max(0L, ticks) / TICKS_PER_DAY);
    }

    public static CalendarDate of(int day, Season season, long year) {
        Objects.requireNonNull(season);
        if (day < 1 || day > DAYS_PER_SEASON || year < 1) {
            throw new IllegalArgumentException("Invalid calendar date");
        }
        long days = Math.addExact(Math.multiplyExact(year - 1, DAYS_PER_YEAR),
                (long) season.ordinal() * DAYS_PER_SEASON + day - 1);
        return new CalendarDate(days);
    }

    public int day() {
        return (int) (elapsedDays % DAYS_PER_SEASON) + 1;
    }

    public Season season() {
        return Season.values()[(int) (elapsedDays / DAYS_PER_SEASON % 4)];
    }

    public long year() {
        return elapsedDays / DAYS_PER_YEAR + 1;
    }

    public DayOfWeek weekday() {
        // The original calendar starts on Monday (totalDays = 1).
        return DayOfWeek.of((int) (elapsedDays % 7) + 1);
    }

    public long startTicks() {
        return elapsedDays * TICKS_PER_DAY;
    }

    public long atTimeOfDay(long previousTicks) {
        return Math.addExact(startTicks(), Math.floorMod(previousTicks, TICKS_PER_DAY));
    }
}
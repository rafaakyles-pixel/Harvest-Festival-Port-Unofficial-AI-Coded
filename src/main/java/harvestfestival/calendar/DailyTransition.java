package harvestfestival.calendar;

import java.util.Optional;

public final class DailyTransition {
    private CalendarDate baseline;

    public void begin(long ticks) {
        baseline = CalendarDate.fromTicks(ticks);
    }

    public Optional<Change> finish(long ticks) {
        CalendarDate previous = baseline;
        baseline = null;
        CalendarDate current = CalendarDate.fromTicks(ticks);
        if (previous == null || current.elapsedDays() <= previous.elapsedDays()) {
            return Optional.empty();
        }
        return Optional.of(new Change(previous, current));
    }

    public record Change(CalendarDate previous, CalendarDate current) {}
}
package harvestfestival.calendar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyTransitionTest {
    @Test void naturalBoundaryEmitsOnce() {
        var tracker = new DailyTransition();
        tracker.begin(23999);
        var change = tracker.finish(24000).orElseThrow();
        assertEquals(0, change.previous().elapsedDays());
        assertEquals(1, change.current().elapsedDays());
        assertTrue(tracker.finish(24000).isEmpty());
        tracker.begin(24000);
        assertTrue(tracker.finish(24001).isEmpty());
    }

    @Test void sleepingAcrossYearBoundaryEmitsOneChange() {
        var tracker = new DailyTransition();
        tracker.begin(119 * 24000L + 18000);
        var change = tracker.finish(120 * 24000L).orElseThrow();
        assertEquals(Season.WINTER, change.previous().season());
        assertEquals(2, change.current().year());
    }

    @Test void administrativeRebaseDoesNotReplaySkippedDays() {
        var tracker = new DailyTransition();
        tracker.begin(0);
        tracker.begin(80 * 24000L + 100);
        assertTrue(tracker.finish(80 * 24000L + 101).isEmpty());
    }

    @Test void reloadPauseAndBackwardsMovementDoNotEmit() {
        var tracker = new DailyTransition();
        assertTrue(tracker.finish(24000).isEmpty());
        tracker.begin(24000);
        assertTrue(tracker.finish(24000).isEmpty());
        tracker.begin(48000);
        assertTrue(tracker.finish(0).isEmpty());
        var reopened = new DailyTransition();
        reopened.begin(48000);
        assertTrue(reopened.finish(48001).isEmpty());
    }

    @Test void independentServersAndSkippedDaysProduceNoCatchupLoop() {
        var first = new DailyTransition();
        var second = new DailyTransition();
        first.begin(23999);
        second.begin(100);
        assertTrue(first.finish(24000).isPresent());
        assertTrue(second.finish(101).isEmpty());
        first.begin(24000);
        assertEquals(10, first.finish(240000).orElseThrow().current().elapsedDays());
        assertTrue(first.finish(240000).isEmpty());
    }
}
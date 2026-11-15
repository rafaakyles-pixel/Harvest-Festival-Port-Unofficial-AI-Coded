package harvestfestival.tools;

import harvestfestival.calendar.Season;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolSleepTest {
    @Test void seasonalRestrictionUsesOriginalShiftAndInclusiveSunset() {
        int[] sunsets = {20500,21500,19000,16500};
        int i = 0;
        for (Season season : Season.values()) {
            int sunset = sunsets[i++];
            assertTrue(ToolSleep.restrictedTime(0, season));
            assertTrue(ToolSleep.restrictedTime(sunset-6000, season));
            assertFalse(ToolSleep.restrictedTime(sunset-5999, season));
            assertFalse(ToolSleep.restrictedTime(23999, season));
            assertEquals(ToolSleep.restrictedTime(0,season), ToolSleep.restrictedTime(24000,season));
        }
    }
    @Test void legacySpawnOffsetsUseHalfFuzzAndBoundedDraws() {
        assertEquals(5, ToolSleep.spawnOffset(10, bound -> {assertEquals(10,bound);return 0;}));
        assertEquals(-4, ToolSleep.spawnOffset(10, bound -> bound-1));
        assertEquals(1, ToolSleep.spawnOffset(1, bound -> {assertEquals(2,bound);return 0;}));
    }
    @Test void authorizedAccessorIsAppliedToActualPlayerClass() {
        assertTrue(harvestfestival.tools.mixin.PlayerSleepAccessor.class.isAssignableFrom(net.minecraft.world.entity.player.Player.class));
    }
}

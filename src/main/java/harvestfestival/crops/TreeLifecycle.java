package harvestfestival.crops;

import harvestfestival.calendar.Season;

/** Shared by daily, random and bonemeal growth; world mutations must confirm success. */
public final class TreeLifecycle {
    public interface Space {
        boolean area();
        boolean juvenile();
        boolean mature();
        void fruit();
    }
    public static int advance(int stage, TreeDefinition definition, Season season, Space space) {
        if (season == Season.WINTER) return stage;
        if (space.area() && stage < definition.maximum()) {
            int next = stage + 1;
            if (next == definition.juvenile() && !space.juvenile()) return stage;
            if (next == definition.maturity() && !space.mature()) return stage;
            stage = next;
        }
        if (stage >= definition.maximum() && definition.fruitsIn(season)) {
            space.fruit();
            return definition.regrow();
        }
        return stage;
    }
    private TreeLifecycle() {}
}

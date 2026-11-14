package harvestfestival.crops;

import harvestfestival.calendar.Season;

/** The same transition is used by the world adapter and the automated tests. */
public final class CropLifecycle {
    private CropLifecycle() {}

    public static CropState newDay(CropState state, CropDefinition definition, Season season,
            boolean watered, boolean dedicated) {
        if (definition == null || !state.crop().equals(definition.id()) || state.dead()) return state;
        int dry = watered ? 0 : state.dryDays();
        boolean dieFromDrought = dedicated ? definition.diesWithoutWaterDedicated() : definition.diesWithoutWaterIntegrated();
        if (!definition.seasons().contains(season)
                || (definition.requiresWater() && dieFromDrought && dry > definition.droughtLimit())) {
            return new CropState(state.crop(), state.stage(), dry, true, state.harvests());
        }
        int stage = state.stage();
        if ((!definition.requiresWater() || dry == 0) && stage < definition.maturity()) stage++;
        return new CropState(state.crop(), stage, Math.min(dry + 1, definition.droughtLimit() + 1), false, state.harvests());
    }

    public static boolean canHarvest(CropState state, CropDefinition definition) {
        return definition != null && definition.id().equals(state.crop()) && !state.dead()
                && state.stage() >= definition.maturity() && state.harvests() < definition.maxHarvests();
    }

    /** Null means removal. Called only after canHarvest and before emitting any products. */
    public static CropState afterHarvest(CropState state, CropDefinition definition) {
        if (!canHarvest(state, definition)) throw new IllegalArgumentException("crop cannot be harvested");
        int harvests = state.harvests() + 1;
        if (definition.regrowStage() == 0 || harvests >= definition.maxHarvests()) return null;
        return new CropState(state.crop(), definition.regrowStage(), state.dryDays(), false, harvests);
    }
}

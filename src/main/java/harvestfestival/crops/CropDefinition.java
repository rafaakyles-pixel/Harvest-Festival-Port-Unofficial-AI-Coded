package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;

/** Data-defined crop with optional fruit transformation or paired foliage. */
public record CropDefinition(Identifier id, TagKey<Item> seedTag, TagKey<Block> soilTag,
        Set<Season> seasons, List<Integer> stages, boolean requiresWater, int droughtLimit,
        boolean diesWithoutWaterIntegrated, boolean diesWithoutWaterDedicated,
        int plantingRadius, Identifier harvestLoot, int regrowStage, int maxHarvests,
        EmptyHarvestPolicy emptyHarvestPolicy, List<Integer> appearances, TagKey<Item> harvestToolTag,
        boolean harvestOnPlayerBreak, CropForm form) {
    public enum EmptyHarvestPolicy { PRESERVE, ADVANCE }

    public CropDefinition(Identifier id, TagKey<Item> seedTag, TagKey<Block> soilTag,
            Set<Season> seasons, List<Integer> stages, boolean requiresWater, int droughtLimit,
            boolean diesWithoutWaterIntegrated, boolean diesWithoutWaterDedicated,
            int plantingRadius, Identifier harvestLoot, int regrowStage, int maxHarvests,
            EmptyHarvestPolicy emptyHarvestPolicy, List<Integer> appearances, TagKey<Item> harvestToolTag,
            boolean harvestOnPlayerBreak) {
        this(id, seedTag, soilTag, seasons, stages, requiresWater, droughtLimit,
                diesWithoutWaterIntegrated, diesWithoutWaterDedicated, plantingRadius, harvestLoot,
                regrowStage, maxHarvests, emptyHarvestPolicy, appearances, harvestToolTag,
                harvestOnPlayerBreak, CropForm.SINGLE);
    }

    public CropDefinition(Identifier id, TagKey<Item> seedTag, TagKey<Block> soilTag,
            Set<Season> seasons, List<Integer> stages, boolean requiresWater, int droughtLimit,
            boolean diesWithoutWaterIntegrated, boolean diesWithoutWaterDedicated,
            int plantingRadius, Identifier harvestLoot, int regrowStage, int maxHarvests,
            EmptyHarvestPolicy emptyHarvestPolicy, List<Integer> appearances, TagKey<Item> harvestToolTag) {
        this(id, seedTag, soilTag, seasons, stages, requiresWater, droughtLimit,
                diesWithoutWaterIntegrated, diesWithoutWaterDedicated, plantingRadius, harvestLoot,
                regrowStage, maxHarvests, emptyHarvestPolicy, appearances, harvestToolTag, false);
    }

    public CropDefinition {
        seasons = Set.copyOf(seasons);
        stages = List.copyOf(stages);
        appearances = List.copyOf(appearances);
        if (!appearances.isEmpty() && (appearances.size() != stages.size()
                || appearances.stream().anyMatch(value -> value < 0 || value > 109 || value == 3))) {
            throw new IllegalArgumentException("appearances must match stages and use living visual slots 0..109 except 3");
        }
        if (seasons.isEmpty() || stages.isEmpty() || plantingRadius < 0 || plantingRadius > 1
                || droughtLimit < 0 || droughtLimit >= 1_000_000) {
            throw new IllegalArgumentException("invalid crop limits");
        }
        int previous = 0;
        for (int stage : stages) {
            if (stage <= previous || stage > 1_000_000) throw new IllegalArgumentException("stages must increase within 1..1000000");
            previous = stage;
        }
        if (regrowStage < 0 || regrowStage >= stages.getLast() || maxHarvests < 1 || maxHarvests > 1_000_000
                || (regrowStage == 0 && maxHarvests != 1) || emptyHarvestPolicy == null) {
            throw new IllegalArgumentException("invalid harvest limits or policy");
        }
        if (harvestToolTag != null && regrowStage != 0 && !(form.doubled() && maxHarvests == 1)) {
            throw new IllegalArgumentException("tool-required regrowth is not supported");
        }
        if (form.doubleFrom() > stages.getLast() || form.minimumCut() > stages.getLast()
                || (form.doubled() && (form.doubleFrom() != 6 || stages.getLast() != 11 || maxHarvests != 1))
                || (form.minimumCut() > 0 && (!form.doubled() || harvestToolTag == null))
                || (form.transforms() && (regrowStage != 0 || harvestToolTag != null || appearances.size() != 8)))
            throw new IllegalArgumentException("unsupported crop form limits");
    }

    public int maturity() { return stages.getLast(); }
    public boolean requiresTool() { return harvestToolTag != null; }
    public boolean acceptsTool(ItemInstance stack) { return !requiresTool() || stack.is(harvestToolTag); }

    /** Data selects visual slots; missing mappings retain the legacy three-form fallback. */
    public int appearance(int stage) {
        for (int i = 0; i < stages.size(); i++) {
            if (stage <= stages.get(i)) return !appearances.isEmpty() ? appearances.get(i)
                    : stages.size() == 1 ? 2 : i * 2 / (stages.size() - 1);
        }
        return appearances.isEmpty() ? 2 : appearances.getLast();
    }
}

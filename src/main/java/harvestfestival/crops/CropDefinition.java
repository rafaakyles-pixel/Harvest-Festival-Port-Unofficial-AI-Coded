package harvestfestival.crops;

import harvestfestival.calendar.Season;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** One data-defined, single-height, single-harvest crop. */
public record CropDefinition(Identifier id, TagKey<Item> seedTag, TagKey<Block> soilTag,
        Set<Season> seasons, List<Integer> stages, boolean requiresWater, int droughtLimit,
        boolean diesWithoutWaterIntegrated, boolean diesWithoutWaterDedicated,
        int plantingRadius, Identifier harvestLoot) {
    public CropDefinition {
        seasons = Set.copyOf(seasons);
        stages = List.copyOf(stages);
        if (seasons.isEmpty() || stages.isEmpty() || plantingRadius < 0 || plantingRadius > 1
                || droughtLimit < 0 || droughtLimit >= 1_000_000) {
            throw new IllegalArgumentException("invalid crop limits");
        }
        int previous = 0;
        for (int stage : stages) {
            if (stage <= previous || stage > 1_000_000) throw new IllegalArgumentException("stages must increase within 1..1000000");
            previous = stage;
        }
    }

    public int maturity() { return stages.getLast(); }

    /** Three provisional visual forms, independent of the number of growth days. */
    public int appearance(int stage) {
        for (int i = 0; i < stages.size(); i++) {
            if (stage <= stages.get(i)) return stages.size() == 1 ? 2 : i * 2 / (stages.size() - 1);
        }
        return 2;
    }
}

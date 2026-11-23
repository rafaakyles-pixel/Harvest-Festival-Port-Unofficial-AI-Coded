package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import harvestfestival.calendar.Season;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Tree thresholds are absolute stages; no crop water or harvest counters are shared. */
public record TreeDefinition(int sapling, int juvenile, int maturity, int maximum, int regrow,
        int radius, String season, int offsetX, int offsetZ, List<Cell> template) {
    public record Cell(BlockPos pos, BlockState state) {
        public static final Codec<Cell> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Cell::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Cell::state)).apply(i, Cell::new));
    }
    public static final Codec<TreeDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(2, 10000).fieldOf("sapling").forGetter(TreeDefinition::sapling),
            Codec.intRange(3, 10000).fieldOf("juvenile").forGetter(TreeDefinition::juvenile),
            Codec.intRange(4, 10000).fieldOf("maturity").forGetter(TreeDefinition::maturity),
            Codec.intRange(5, 10000).fieldOf("maximum").forGetter(TreeDefinition::maximum),
            Codec.intRange(1, 10000).fieldOf("regrow").forGetter(TreeDefinition::regrow),
            Codec.intRange(1, 8).fieldOf("radius").forGetter(TreeDefinition::radius),
            Codec.STRING.fieldOf("season").forGetter(TreeDefinition::season),
            Codec.intRange(-16, 16).fieldOf("offset_x").forGetter(TreeDefinition::offsetX),
            Codec.intRange(-16, 16).fieldOf("offset_z").forGetter(TreeDefinition::offsetZ),
            Cell.CODEC.listOf().fieldOf("template").forGetter(TreeDefinition::template)).apply(i, TreeDefinition::new));
    public TreeDefinition {
        if (!(sapling < juvenile && juvenile < maturity && maturity < regrow && regrow < maximum)
                || template.isEmpty() || template.size() > 4096) throw new IllegalArgumentException("invalid tree thresholds/template");
        Season.valueOf(season.toUpperCase(java.util.Locale.ROOT));
        template = List.copyOf(template);
        var positions = new java.util.HashSet<BlockPos>();
        for (var cell : template) {
            if (!positions.add(cell.pos()) || Math.abs(cell.pos().getX()) > 32 || Math.abs(cell.pos().getZ()) > 32
                    || cell.pos().getY() < 0 || cell.pos().getY() > 32) throw new IllegalArgumentException("invalid tree cell");
            if (cell.pos().getX() + offsetX == 0 && cell.pos().getZ() + offsetZ == 0 && cell.pos().getY() < 2)
                throw new IllegalArgumentException("template overlaps producer");
        }
    }
    public int appearance(int stage) { return stage < sapling ? 0 : stage < juvenile ? 1 : stage < maturity ? 2 : 3; }
    public boolean fruitsIn(Season local) { return local != null && local.serializedName().equals(season); }
}

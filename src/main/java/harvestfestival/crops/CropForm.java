package harvestfestival.crops;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Optional world behavior, separate from the unchanged persisted crop counters. */
public record CropForm(Identifier matureBlock, TagKey<Block> fruitTag, int attachedAppearance,
        boolean lastNeighbor, int doubleFrom, int minimumCut) {
    public static final CropForm SINGLE = new CropForm(null, null, 0, false, 0, 0);
    public CropForm {
        if (doubleFrom < 0 || minimumCut < 0 || (matureBlock == null) != (fruitTag == null)
                || (matureBlock != null && (doubleFrom != 0 || minimumCut != 0
                || attachedAppearance < 72 || attachedAppearance > 103))
                || (matureBlock == null && (attachedAppearance != 0 || lastNeighbor)))
            throw new IllegalArgumentException("invalid crop form");
    }
    public boolean transforms() { return matureBlock != null; }
    public boolean doubled() { return doubleFrom > 0; }
}

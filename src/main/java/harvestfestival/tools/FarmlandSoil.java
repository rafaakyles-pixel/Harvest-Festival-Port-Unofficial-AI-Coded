package harvestfestival.tools;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared soil policy; moisture is saved and synchronized by the vanilla block state. */
public final class FarmlandSoil {
    private static final TagKey<Block> SOILS = TagKey.create(Registries.BLOCK,
            Identifier.parse("harvestfestival:crop_soils"));

    private FarmlandSoil() {}

    public static boolean managed(BlockState state) {
        return state.is(SOILS) && state.hasProperty(FarmlandBlock.MOISTURE);
    }

    public static boolean watered(BlockState state) {
        return managed(state) && state.getValue(FarmlandBlock.MOISTURE) == 7;
    }

    public static BlockState dry(BlockState state, BlockState above) {
        if (!managed(state)) return state;
        return switch (state.getValue(FarmlandBlock.MOISTURE)) {
            case 7 -> state.setValue(FarmlandBlock.MOISTURE, 3);
            case 3 -> state.setValue(FarmlandBlock.MOISTURE, 0);
            case 0 -> above.is(BlockTags.MAINTAINS_FARMLAND) ? state : Blocks.DIRT.defaultBlockState();
            // Intermediate values from commands/other mods were unchanged by the original handler.
            default -> state;
        };
    }

    public static BlockState migrate(BlockState state) {
        if (!state.is(ToolContent.PREPARED_SOIL.get())) return state;
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,
                state.getValue(PreparedSoilBlock.WATERED) ? 7 : 0);
    }
}

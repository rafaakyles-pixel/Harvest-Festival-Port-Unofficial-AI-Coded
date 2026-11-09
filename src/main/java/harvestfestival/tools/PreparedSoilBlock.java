package harvestfestival.tools;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Passive soil: the future crop consumer owns the daily use/reset of WATERED. */
public final class PreparedSoilBlock extends Block {
    public static final BooleanProperty WATERED = BooleanProperty.create("watered");

    public PreparedSoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERED);
    }
}

package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public final class TreeTopBlock extends Block implements BonemealableBlock {
    private final String species;
    public TreeTopBlock(String species, Properties properties) { super(properties); this.species = species; registerDefaultState(stateDefinition.any().setValue(TreeBlock.APPEARANCE, 2)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TreeBlock.APPEARANCE); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return TreeBlock.shape(state.getValue(TreeBlock.APPEARANCE), true); }
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(TreeBlock.APPEARANCE) == 3 ? player.getDestroySpeed(state, pos) / 2F / 30F
                : CropBlock.destroyProgress(player.getDestroySpeed(state, pos), false, player.getMainHandItem().is(harvestfestival.tools.SickleItem.SICKLES));
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState other, RandomSource random) {
        return direction == Direction.DOWN && !other.is(OrchardContent.TREES.get(species).base().get()) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (TreeGrowth.available(level, pos.below()) && level.getBlockState(pos.below()).is(OrchardContent.TREES.get(species).base().get())) level.removeBlock(pos.below(), false);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.item.ItemStack tool, boolean harvest, net.minecraft.world.level.material.FluidState fluid) {
        return TreeBlock.permitted(level, pos, pos.below(), player, tool) && super.onDestroyedByPlayer(state, level, pos, player, tool, harvest, fluid);
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { TreeBlock.randomAdvance(level, pos, pos.below(), random); }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        var base = OrchardContent.TREES.get(species).base().get();
        return base.isValidBonemealTarget(level, pos.below(), base.defaultBlockState());
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { TreeBlock.advance(level, pos.below()); }
}

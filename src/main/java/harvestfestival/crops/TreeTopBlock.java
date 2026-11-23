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
        return TreeBlock.juvenile(state) && direction == Direction.DOWN && !other.is(OrchardContent.TREES.get(species).base().get()) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!TreeBlock.juvenile(state) || !TreeGrowth.available(level, pos.below())) return;
        var other = level.getBlockState(pos.below());
        if (other.is(OrchardContent.TREES.get(species).base().get()) && TreeBlock.juvenile(other)) level.removeBlock(pos.below(), false);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.item.ItemStack tool, boolean harvest, net.minecraft.world.level.material.FluidState fluid) {
        var lower = level.getBlockState(pos.below());
        var other = TreeBlock.juvenile(state) && lower.is(OrchardContent.TREES.get(species).base().get()) && TreeBlock.juvenile(lower) ? pos.below() : pos;
        return TreeBlock.permitted(level, pos, other, player, tool) && super.onDestroyedByPlayer(state, level, pos, player, tool, harvest, fluid);
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { TreeBlock.randomAdvance(level, pos, pos.below(), random); }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!TreeBlock.juvenile(state)) {
            if (level instanceof Level world && world.isClientSide()) return OrchardSync.client().bonemeal();
            if (level instanceof ServerLevel server) {
                var p = TreeProducers.get(server).at(pos);
                var definition = TreeDefinitions.INSTANCE.get(species);
                return TreeDefinitions.INSTANCE.settings().bonemeal() && p != null && definition != null && p.stage < definition.maximum();
            }
            return false;
        }
        var base = OrchardContent.TREES.get(species).base().get();
        return base.isValidBonemealTarget(level, pos.below(), base.defaultBlockState());
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        var data = TreeProducers.get(level);
        var p = data.at(pos);
        if (p != null) data.grow(level, p, CropDailyUpdates.seasonForDimension(level.dimension(),
                harvestfestival.calendar.WorldCalendar.get(level.getServer()).season()));
        else if (TreeBlock.juvenile(state)) TreeBlock.advance(level, pos.below());
    }
}

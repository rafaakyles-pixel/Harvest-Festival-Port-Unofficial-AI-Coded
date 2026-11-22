package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import harvestfestival.tools.SickleItem;

/** Visual upper half only. All crop counters and loot belong to the base. */
public final class CropTopBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 6, 11);
    public CropTopBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 6).setValue(CropBlock.WITHERED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, CropBlock.WITHERED);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, (state.getValue(STAGE) - 3) * 1.6, 16);
    }
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return CropBlock.destroyProgress(player.getDestroySpeed(state, pos), state.getValue(CropBlock.WITHERED),
                player.getMainHandItem().is(SickleItem.SICKLES));
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        return direction == Direction.DOWN && !level.getBlockState(pos.below()).is(CropContent.CROP.get())
                ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (level.getBlockState(pos.below()).is(CropContent.CROP.get())) level.removeBlock(pos.below(), false);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            ItemStack tool, boolean willHarvest, FluidState fluid) {
        if (level instanceof ServerLevel server) {
            if (!CropPair.permitted(server, pos, player, tool)) return false;
            var base = pos.below();
            var lower = level.getBlockState(base);
            if (lower.is(CropContent.CROP.get()))
                return lower.onDestroyedByPlayer(level, base, player, tool, willHarvest, level.getFluidState(base));
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        if (!state.getValue(CropBlock.WITHERED) && tool.is(SickleItem.SICKLES)) return;
        super.playerDestroy(level, player, pos, state, entity, tool);
    }
}

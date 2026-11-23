package harvestfestival.crops;

import com.mojang.serialization.MapCodec;
import harvestfestival.calendar.WorldCalendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.TriState;

public final class TreeBlock extends BaseEntityBlock implements BonemealableBlock {
    public static final IntegerProperty APPEARANCE = IntegerProperty.create("appearance", 0, 3);
    private final String species;
    public TreeBlock(String species, Properties properties) {
        super(properties); this.species = species;
        registerDefaultState(stateDefinition.any().setValue(APPEARANCE, 0));
    }
    public String species() { return species; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new TreeBlock(species, p)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(APPEARANCE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TreeBlockEntity(pos, state); }
    static VoxelShape shape(int appearance, boolean top) {
        return switch (appearance) {
            case 0 -> Block.box(0, 0, 0, 16, 8, 16);
            case 1 -> Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
            case 2 -> Block.box(1.6, top ? -16 : 0, 1.6, 14.4, top ? 12.8 : 28.8, 14.4);
            default -> Block.box(0, 0, 0, 16, 16, 16);
        };
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(APPEARANCE), false); }
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (state.getValue(APPEARANCE) == 3) return player.getDestroySpeed(state, pos) / 2F / 30F;
        return CropBlock.destroyProgress(player.getDestroySpeed(state, pos), false, player.getMainHandItem().is(harvestfestival.tools.SickleItem.SICKLES));
    }
    public static boolean supports(BlockState plant, LevelReader level, BlockPos pos) {
        var soil = level.getBlockState(pos.below());
        var hook = soil.canSustainPlant(level, pos.below(), Direction.UP, plant);
        return hook == TriState.TRUE || hook == TriState.DEFAULT && soil.is(OrchardContent.SOILS);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return supports(state, level, pos); }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbor, BlockState other, RandomSource random) {
        return direction == Direction.DOWN && level instanceof ServerLevel && !supports(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!juvenile(state) || !TreeGrowth.available(level, pos.above())) return;
        var other = level.getBlockState(pos.above());
        if (other.is(OrchardContent.TREES.get(species).top().get()) && juvenile(other)) level.removeBlock(pos.above(), false);
    }
    static boolean juvenile(BlockState state) { return state.getValue(APPEARANCE) < 3; }
    static boolean permitted(Level level, BlockPos pos, BlockPos other, Player player, net.minecraft.world.item.ItemStack tool) {
        if (!(level instanceof ServerLevel server)) return true;
        return CropPair.permitted(pos, other, p -> TreeGrowth.available(server, p) && server.mayInteract(player, p)
                && player.mayUseItemAt(p, Direction.UP, tool)
                && (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || !player.blockActionRestricted(server, p, sp.gameMode.getGameModeForPlayer())));
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.item.ItemStack tool, boolean harvest, net.minecraft.world.level.material.FluidState fluid) {
        var upper = level.getBlockState(pos.above());
        var other = juvenile(state) && upper.is(OrchardContent.TREES.get(species).top().get()) && juvenile(upper) ? pos.above() : pos;
        return permitted(level, pos, other, player, tool) && super.onDestroyedByPlayer(state, level, pos, player, tool, harvest, fluid);
    }
    static TreeBlockEntity entity(LevelReader level, BlockPos pos) { return level.getBlockEntity(pos) instanceof TreeBlockEntity tree ? tree : null; }
    static void advance(ServerLevel level, BlockPos pos) {
        var tree = entity(level, pos);
        if (tree != null) TreeGrowth.grow(level, tree, CropDailyUpdates.seasonForDimension(level.dimension(), WorldCalendar.get(level.getServer()).season()));
        else {
            var data = TreeProducers.get(level);
            var producer = data.at(pos.above());
            if (producer != null && producer.origin.equals(pos)) data.grow(level, producer,
                    CropDailyUpdates.seasonForDimension(level.dimension(), WorldCalendar.get(level.getServer()).season()));
        }
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        randomAdvance(level, pos, pos, random);
    }
    static void randomAdvance(ServerLevel level, BlockPos physical, BlockPos base, RandomSource random) {
        if (TreeDefinitions.INSTANCE.settings().daily()) return;
        var data = TreeProducers.get(level);
        var producer = data.at(physical);
        if (producer != null) data.random(level, producer, physical);
        else if (level.getMaxLocalRawBrightness(physical.above()) >= 9 && random.nextInt(20) == 0) advance(level, base);
    }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        var tree = entity(level, pos); var definition = TreeDefinitions.INSTANCE.get(species);
        if (level instanceof Level world && world.isClientSide()) {
            var info = OrchardSync.client().trees().get(species);
            return OrchardSync.client().bonemeal() && tree != null && tree.valid() && info != null && tree.stage() < info.maximum();
        }
        if (tree == null && level instanceof ServerLevel server) {
            var p = TreeProducers.get(server).at(pos.above());
            return TreeDefinitions.INSTANCE.settings().bonemeal() && p != null && p.origin.equals(pos)
                    && definition != null && p.stage < definition.maximum();
        }
        return TreeDefinitions.INSTANCE.settings().bonemeal() && tree != null && tree.valid() && definition != null && tree.stage() < definition.maximum();
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { advance(level, pos); }
}

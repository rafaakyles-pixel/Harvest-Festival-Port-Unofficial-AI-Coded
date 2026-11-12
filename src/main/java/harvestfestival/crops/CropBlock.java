package harvestfestival.crops;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CropBlock extends BaseEntityBlock {
    public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
    // Visual cache only. Logical stage lives in the block entity and has no blockstate bound.
    public static final IntegerProperty APPEARANCE = IntegerProperty.create("appearance", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    public CropBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(APPEARANCE, 0));
    }
    @Override protected MapCodec<CropBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(APPEARANCE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CropBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            var definition = CropDefinitions.INSTANCE.get(crop.crop().crop());
            if (definition != null) return level.getBlockState(pos.below()).is(definition.soilTag());
        }
        return level.getBlockState(pos.below()).is(CropContent.SOILS);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbor, BlockState neighborState, RandomSource random) {
        // Authority is server-side: definitions are not a client-side catalog.
        if (level instanceof ServerLevel && direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(CropContent.WATERING_CANS)) return InteractionResult.PASS;
        return harvest(state, level, pos, player, stack, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return harvest(state, level, pos, player, ItemStack.EMPTY, hit);
    }
    private InteractionResult harvest(BlockState state, Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), held)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof CropBlockEntity crop)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        boolean harvested = harvestOnce(crop.crop(), CropDefinitions.INSTANCE.get(crop.crop().crop()),
                () -> Block.getDrops(state, server, pos, crop),
                () -> server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL),
                stack -> Block.popResource(server, pos, stack));
        return harvested ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    public static boolean harvestOnce(CropState crop, CropDefinition definition, Supplier<List<ItemStack>> drops,
            BooleanSupplier remove, Consumer<ItemStack> output) {
        if (!CropLifecycle.canHarvest(crop, definition)) return false;
        var items = drops.get();
        if (items.isEmpty() || !remove.getAsBoolean()) return false;
        items.forEach(output);
        return true;
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CropBlockEntity crop)) return List.of();
        var definition = CropDefinitions.INSTANCE.get(crop.crop().crop());
        if (!CropLifecycle.canHarvest(crop.crop(), definition)) return List.of();
        var loot = params.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
        return params.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, definition.harvestLoot())).getRandomItems(loot);
    }
}

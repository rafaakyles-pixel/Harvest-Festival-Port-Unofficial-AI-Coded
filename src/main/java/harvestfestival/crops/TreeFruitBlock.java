package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class TreeFruitBlock extends Block {
    private final String species;
    public TreeFruitBlock(String species, Properties properties) { super(properties); this.species = species; }
    public String species() { return species; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        // Cross model: 0.8..15.2, rotated 45 degrees with rescale; bounds of opaque texture pixels.
        double min = species.equals("banana") ? .8 : 2.6;
        double top = species.equals("orange") || species.equals("peach") ? 14 : 15;
        return Block.box(min, 1, min, 16 - min, top, 16 - min);
    }
    static boolean collect(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide() || level.captureBlockSnapshots || level.restoringBlockSnapshots) return false;
        return collect(state, () -> level.getBlockState(pos), () -> level.removeBlock(pos, false),
                stack -> popResource(level, pos, stack));
    }
    static boolean collect(BlockState state, java.util.function.Supplier<BlockState> current,
            java.util.function.BooleanSupplier remove, java.util.function.Consumer<ItemStack> drop) {
        if (!current.get().equals(state) || !(state.getBlock() instanceof TreeFruitBlock fruit) || !remove.getAsBoolean()) return false;
        drop.accept(new ItemStack(OrchardContent.TREES.get(fruit.species).product().get()));
        return true;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return harvest(state, level, pos, player, stack, hit); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return harvest(state, level, pos, player, ItemStack.EMPTY, hit); }
    private InteractionResult harvest(BlockState state, Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) return InteractionResult.PASS;
        collect(state, level, pos);
        return InteractionResult.SUCCESS;
    }
}

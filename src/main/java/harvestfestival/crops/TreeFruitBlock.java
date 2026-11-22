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
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return harvest(state, level, pos, player, stack, hit); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return harvest(state, level, pos, player, ItemStack.EMPTY, hit); }
    private InteractionResult harvest(BlockState state, Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockState(pos).equals(state) && level.removeBlock(pos, false))
            popResource(level, pos, new ItemStack(OrchardContent.TREES.get(species).product().get()));
        return InteractionResult.SUCCESS;
    }
}

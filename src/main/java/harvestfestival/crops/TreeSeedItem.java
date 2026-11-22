package harvestfestival.crops;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class TreeSeedItem extends Item {
    private final String species;
    public TreeSeedItem(String species, Properties properties) { super(properties); this.species = species; }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        var info = OrchardSync.client().trees().get(species);
        if (info != null) {
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_water"));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_space", info.radius() * 2 + 1));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_maturity", info.maturity() - 1));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_season",
                    net.minecraft.network.chat.Component.translatable("season.harvestfestival." + info.season())));
        }
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (TreeDefinitions.INSTANCE.get(species) == null) return InteractionResult.PASS;
        var pos = context.getClickedPos().above();
        var stack = context.getItemInHand();
        for (var target : new net.minecraft.core.BlockPos[]{pos.below(), pos, pos.above()})
            if (!TreeGrowth.available(level, target) || !level.mayInteract(player, target) || !player.mayUseItemAt(target, Direction.UP, stack)) return InteractionResult.PASS;
        var block = OrchardContent.TREES.get(species).base().get();
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above()) || !TreeBlock.supports(block.defaultBlockState(), level, pos)) return InteractionResult.PASS;
        if (!level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TreeBlockEntity tree)) { level.removeBlock(pos, false); return InteractionResult.PASS; }
        tree.stage(1);
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

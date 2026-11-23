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
    public interface Plot {
        boolean editable(net.minecraft.core.BlockPos pos);
        boolean empty(net.minecraft.core.BlockPos pos);
        boolean support(net.minecraft.core.BlockPos pos);
        boolean place(net.minecraft.core.BlockPos pos);
    }
    public static boolean plant(Plot plot, net.minecraft.core.BlockPos pos, Direction face) {
        if (face != Direction.UP) return false;
        for (var target : new net.minecraft.core.BlockPos[]{pos.below(),pos,pos.above()}) if (!plot.editable(target)) return false;
        return plot.empty(pos) && plot.empty(pos.above()) && plot.support(pos) && plot.place(pos);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        var info = OrchardSync.client().trees().get(species);
        if (info != null) {
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_water"));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_space", info.radius() * 2 + 1, info.radius() * 2 + 1));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_maturity", info.maturity() - 1));
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.harvestfestival.tree_season",
                    net.minecraft.network.chat.Component.translatable("harvestfestival.season." + info.season())));
        }
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (TreeDefinitions.INSTANCE.get(species) == null) return InteractionResult.PASS;
        var pos = context.getClickedPos().above();
        var stack = context.getItemInHand();
        var block = OrchardContent.TREES.get(species).base().get();
        boolean planted = plant(new Plot() {
            public boolean editable(net.minecraft.core.BlockPos target) { return TreeGrowth.available(level, target)
                    && level.mayInteract(player, target) && player.mayUseItemAt(target, Direction.UP, stack); }
            public boolean empty(net.minecraft.core.BlockPos target) { return level.isEmptyBlock(target); }
            public boolean support(net.minecraft.core.BlockPos target) { return TreeBlock.supports(block.defaultBlockState(), level, target); }
            public boolean place(net.minecraft.core.BlockPos target) {
                if (!level.setBlock(target, block.defaultBlockState(), Block.UPDATE_ALL)) return false;
                if (!(level.getBlockEntity(target) instanceof TreeBlockEntity tree)) { level.removeBlock(target, false); return false; }
                tree.stage(1);
                if (TreeProducers.get(level).attach(level, tree) == null) { level.removeBlock(target, false); return false; }
                return true;
            }
        }, pos, context.getClickedFace());
        if (!planted) return InteractionResult.PASS;
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

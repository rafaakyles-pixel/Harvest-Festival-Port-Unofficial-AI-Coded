package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class CropSeedItem extends Item {
    public CropSeedItem(Properties properties) { super(properties); }

    public interface Plot {
        boolean editable(BlockPos pos);
        boolean empty(BlockPos pos);
        boolean soilMatches(BlockPos pos, TagKey<Block> tag);
        boolean plant(BlockPos pos, CropState state);
    }

    /** One bag attempts all valid squares, consuming once only after a successful placement. */
    public static int plant(Plot plot, BlockPos center, Direction face, ItemStack seeds, CropDefinition definition) {
        if (face != Direction.UP || seeds.isEmpty() || definition == null) return 0;
        // Explicit port rule: a valid neighboring plot cannot authorize a click on grass/dirt.
        if (!plot.editable(center.below()) || !plot.soilMatches(center.below(), definition.soilTag())) return 0;
        int planted = 0;
        int radius = definition.plantingRadius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                var pos = center.offset(x, 0, z);
                if (plot.editable(pos.below()) && plot.editable(pos) && plot.editable(pos.above())
                        && plot.empty(pos) && plot.empty(pos.above()) && plot.soilMatches(pos.below(), definition.soilTag())
                        && plot.plant(pos, CropState.planted(definition.id()))) planted++;
            }
        }
        if (planted > 0) seeds.shrink(1);
        return planted;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var stack = context.getItemInHand();
        var definition = CropDefinitions.INSTANCE.forSeed(stack);
        int planted = plant(new Plot() {
            public boolean editable(BlockPos pos) {
                return level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack);
            }
            public boolean empty(BlockPos pos) { return level.isEmptyBlock(pos); }
            public boolean soilMatches(BlockPos pos, TagKey<Block> tag) { return level.getBlockState(pos).is(tag); }
            public boolean plant(BlockPos pos, CropState state) {
                var previous = level.getBlockState(pos);
                if (!level.setBlock(pos, CropContent.CROP.get().defaultBlockState(), Block.UPDATE_ALL)) return false;
                if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
                    crop.setCrop(state);
                    return true;
                }
                level.setBlock(pos, previous, Block.UPDATE_ALL);
                return false;
            }
        }, context.getClickedPos().above(), context.getClickedFace(), stack, definition);
        return planted > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}

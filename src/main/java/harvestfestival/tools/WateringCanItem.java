package harvestfestival.tools;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class WateringCanItem extends Item {
    public WateringCanItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && fill(context.getLevel(), player, context.getHand())) return InteractionResult.SUCCESS;
        return ToolActions.useOn(ToolAction.WATER, context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return fill(level, player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private boolean fill(Level level, Player player, InteractionHand hand) {
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        var stack = player.getItemInHand(hand);
        if (hit.getType() != HitResult.Type.BLOCK
                || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)
                || !level.mayInteract(player, hit.getBlockPos())
                || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), stack)) return false;
        // Consume the water interaction even when full; do not water a block behind it.
        if (!level.isClientSide()) WateringCanState.fill(stack);
        return true;
    }
}

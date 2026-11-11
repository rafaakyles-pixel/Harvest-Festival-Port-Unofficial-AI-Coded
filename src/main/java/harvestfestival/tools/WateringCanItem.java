package harvestfestival.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class WateringCanItem extends Item {
    public WateringCanItem(Properties properties) { super(properties); }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int water = WateringCanState.get(stack).water();
        // Keep one visible pixel until the last unit is used; the tooltip gives the exact amount.
        return (water * 13 + WateringCanState.CAPACITY - 1) / WateringCanState.CAPACITY;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3D9FE6;
    }

    public static void registerTooltips(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, context, display, player, flag, builder) -> {
            if (!stack.is(ToolContent.WATERING_CAN.get()) || display.hideTooltip()
                    || !display.shows(ToolContent.WATER_STATE.get())) return;
            int water = WateringCanState.get(stack).water();
            builder.accept(Component.translatable("tooltip.harvestfestival.watering_can.water",
                    water, WateringCanState.CAPACITY).withStyle(water == 0 ? ChatFormatting.RED : ChatFormatting.AQUA));
        });
    }

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

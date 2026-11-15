package harvestfestival.tools;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import net.minecraft.world.item.ItemStack;

/** Only accepted block changes call this; the existing effort system still owns fainting. */
public final class FarmToolTasks {
    private FarmToolTasks() {}
    public static boolean canUse(ItemStack stack, ToolTier tier, ToolAction action) {
        return !stack.isEmpty() && FarmToolState.get(stack).validFor(tier)
                && (action != ToolAction.HOE || stack.getDamageValue() < tier.durability());
    }
    public static void complete(ItemStack stack, ToolTier tier, ToolAction action, boolean creative,
            ToolEffortSettings.Rule rule, IntUnaryOperator damage, Consumer<Float> exhaustion) {
        stack.set(ToolContent.FARM_TOOL_STATE.get(), FarmToolState.get(stack).advance(tier));
        if (creative || rule == null || !rule.consumeHunger()) return;
        exhaustion.accept(4F / rule.actionsPerHalfHaunch() * tier.effort());
        if (action == ToolAction.HOE)
            stack.setDamageValue(Math.min(tier.durability(), stack.getDamageValue() + Math.max(0, damage.applyAsInt(1))));
    }
}

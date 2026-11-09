package harvestfestival.tools;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class HoeItem extends Item {
    public HoeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return ToolActions.useOn(ToolAction.HOE, context);
    }
}

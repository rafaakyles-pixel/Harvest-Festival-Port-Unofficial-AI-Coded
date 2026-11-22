package harvestfestival.crops;

import java.util.List;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = "harvestfestival", value = Dist.CLIENT)
public final class OrchardClient {
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { OrchardSync.clearClient(); }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.BlockTintSources event) {
        OrchardContent.TREES.values().forEach(tree -> event.register(List.of(BlockTintSources.foliage()), tree.leaves().get()));
    }
}

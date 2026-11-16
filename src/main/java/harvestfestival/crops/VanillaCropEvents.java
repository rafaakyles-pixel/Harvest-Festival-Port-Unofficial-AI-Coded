package harvestfestival.crops;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class VanillaCropEvents {
    private VanillaCropEvents() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(VanillaCropRules::register);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::interact);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::grow);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::sync);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::respawn);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::dimension);
        NeoForge.EVENT_BUS.addListener(VanillaCropEvents::stopped);
    }
    static void interact(PlayerInteractEvent.RightClickBlock event) {
        var policy = event.getLevel().isClientSide() ? event.getEntity().getData(VanillaCropPolicy.CLIENT_POLICY)
                : VanillaCropRules.INSTANCE.active();
        if (policy.denyItemUse(event.getItemStack(), event.getLevel().getBlockState(event.getPos()))) event.setUseItem(TriState.FALSE);
    }
    static void grow(CropGrowEvent.Pre event) {
        if (VanillaCropRules.INSTANCE.active().denyGrowth(event.getState())) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }
    static void sync(OnDatapackSyncEvent event) { event.getRelevantPlayers().forEach(VanillaCropEvents::refresh); }
    static void refresh(IAttachmentHolder holder) { holder.setData(VanillaCropPolicy.CLIENT_POLICY, VanillaCropRules.INSTANCE.active()); }
    static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    static void stopped(ServerStoppedEvent event) { VanillaCropRules.INSTANCE.apply(VanillaCropPolicy.OPEN, null, null); }
}

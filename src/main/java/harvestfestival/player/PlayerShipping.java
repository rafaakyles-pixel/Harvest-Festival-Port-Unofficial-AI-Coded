package harvestfestival.player;

import harvestfestival.HarvestFestival;
import harvestfestival.calendar.NewDayEvent;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerShipping {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HarvestFestival.MOD_ID);
    public static final Supplier<AttachmentType<ShippingQueue>> QUEUE = ATTACHMENTS.register("shipping_queue",
            () -> AttachmentType.builder(() -> ShippingQueue.EMPTY)
                    .serialize(ShippingQueue.CODEC.fieldOf("queue")).copyOnDeath().build());

    private PlayerShipping() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    // Legacy per-stack SellValue, used when no datapack rule matches.
    public static long testPrice(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLongOr("SellValue", 0L);
    }

    public static boolean deposit(ServerPlayer player, ItemStack held) {
        return depositInto(player, held);
    }

    static boolean depositInto(IAttachmentHolder holder, ItemStack held) {
        return depositInto(holder, held, ShippingPrices.INSTANCE);
    }

    static boolean depositInto(IAttachmentHolder holder, ItemStack held, ShippingPrices prices) {
        long price = prices.price(held);
        if (price <= 0) return false;
        holder.setData(QUEUE, holder.getData(QUEUE).append(held, price));
        held.shrink(1);
        return true;
    }

    public static void newDay(NewDayEvent event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            pay(player);
        }
    }

    static void pay(IAttachmentHolder holder) {
        var queue = holder.getData(QUEUE);
        if (queue.entries().isEmpty()) return;
        // Both attachments are saved in the same player NBT. Run on the server thread,
        // without an intervening save; removing the queue makes repeated events harmless.
        holder.setData(QUEUE, ShippingQueue.EMPTY);
        PlayerGold.addTo(holder, queue.total());
    }
}

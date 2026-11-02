package harvestfestival.player;

import com.mojang.serialization.Codec;
import harvestfestival.HarvestFestival;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerGold {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HarvestFestival.MOD_ID);
    public static final Supplier<AttachmentType<Long>> GOLD = ATTACHMENTS.register("gold",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("gold"))
                    .copyOnDeath()
                    .build());

    private PlayerGold() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static long get(ServerPlayer player) {
        return player.getData(GOLD);
    }

    public static void set(ServerPlayer player, long amount) {
        player.setData(GOLD, amount);
    }

    public static void add(ServerPlayer player, long amount) {
        // Preserve the original signed-long arithmetic, including negative values and overflow.
        set(player, get(player) + amount);
    }
}

package harvestfestival.player;

import harvestfestival.HarvestFestival;
import harvestfestival.calendar.BirthdayDate;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerBirthday {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HarvestFestival.MOD_ID);
    public static final StreamCodec<ByteBuf, Optional<BirthdayDate>> STREAM_CODEC =
            ByteBufCodecs.optional(BirthdayDate.STREAM_CODEC);
    public static final Supplier<AttachmentType<Optional<BirthdayDate>>> BIRTHDAY = ATTACHMENTS.register("birthday",
            () -> AttachmentType.builder(() -> Optional.<BirthdayDate>empty())
                    .serialize(BirthdayDate.CODEC.optionalFieldOf("date"))
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, STREAM_CODEC)
                    .build());

    private PlayerBirthday() {}

    public static void register(IEventBus modBus) { ATTACHMENTS.register(modBus); }

    public static Optional<BirthdayDate> get(Player player) { return player.getData(BIRTHDAY); }

    public static void set(ServerPlayer player, BirthdayDate date) { setOn(player, date); }

    public static void clear(ServerPlayer player) { clearOn(player); }

    static void setOn(IAttachmentHolder holder, BirthdayDate date) {
        holder.setData(BIRTHDAY, Optional.of(date));
    }

    static void clearOn(IAttachmentHolder holder) {
        holder.setData(BIRTHDAY, Optional.empty());
    }
}

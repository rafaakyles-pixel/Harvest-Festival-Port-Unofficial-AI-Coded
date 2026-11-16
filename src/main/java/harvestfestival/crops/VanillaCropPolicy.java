package harvestfestival.crops;

import harvestfestival.HarvestFestival;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Independent switches; the synchronized player copy is transient, never saved as gameplay data. */
public record VanillaCropPolicy(boolean disablePlanting, boolean disableGrowth,
        boolean disableDrops, boolean disableGrassSeeds) {
    public static final VanillaCropPolicy OPEN = new VanillaCropPolicy(false, false, false, false);
    public static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, Identifier.parse("harvestfestival:vanilla_crop_seeds"));
    public static final TagKey<Block> CROPS = TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:vanilla_crops"));
    public static final TagKey<Block> GRASS = TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:vanilla_seed_drop_sources"));
    public static final StreamCodec<ByteBuf, VanillaCropPolicy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, VanillaCropPolicy::disablePlanting,
            ByteBufCodecs.BOOL, VanillaCropPolicy::disableGrowth,
            ByteBufCodecs.BOOL, VanillaCropPolicy::disableDrops,
            ByteBufCodecs.BOOL, VanillaCropPolicy::disableGrassSeeds, VanillaCropPolicy::new);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HarvestFestival.MOD_ID);
    public static final Supplier<AttachmentType<VanillaCropPolicy>> CLIENT_POLICY = ATTACHMENTS.register("vanilla_crop_policy",
            () -> AttachmentType.builder(() -> OPEN).sync((holder, recipient) -> holder == recipient, STREAM_CODEC).build());

    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }
    public boolean denyItemUse(ItemStack held, BlockState target) {
        return disablePlanting && held.is(SEEDS)
                || disableGrowth && target.is(CROPS) && !held.is(CropContent.WATERING_CANS);
    }
    public boolean denyGrowth(BlockState state) { return disableGrowth && state.is(CROPS); }
    public void filterDrops(BlockState state, List<ItemStack> drops) {
        if (disableDrops && state.is(CROPS)) drops.clear();
        else if (disableGrassSeeds && state.is(GRASS)) drops.removeIf(stack -> stack.is(SEEDS));
    }
}

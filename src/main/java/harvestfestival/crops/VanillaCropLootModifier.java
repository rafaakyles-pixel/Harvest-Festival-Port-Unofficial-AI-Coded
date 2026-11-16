package harvestfestival.crops;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import harvestfestival.HarvestFestival;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Filters only tagged block contexts, including explosions and explicit block loot queries. */
public final class VanillaCropLootModifier extends LootModifier {
    public static final MapCodec<VanillaCropLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, VanillaCropLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HarvestFestival.MOD_ID);
    static { TYPES.register("vanilla_crop_policy", () -> CODEC); }

    private VanillaCropLootModifier(LootItemCondition[] conditions, int priority) { super(conditions, priority); }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (state != null) VanillaCropRules.INSTANCE.active().filterDrops(state, loot);
        return loot;
    }
}

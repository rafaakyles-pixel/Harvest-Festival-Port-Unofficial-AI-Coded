package harvestfestival.crops;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import harvestfestival.HarvestFestival;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Preserves DropHandlerPotato's random condition on every loop evaluation. */
public final class PotatoHarvestLoot extends LootPoolSingletonContainer {
    public static final MapCodec<PotatoHarvestLoot> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Item.CODEC.fieldOf("normal").forGetter(e -> e.normal),
            Item.CODEC.fieldOf("poisonous").forGetter(e -> e.poisonous),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("extra_divisor").forGetter(e -> e.extraDivisor),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("poison_divisor").forGetter(e -> e.poisonDivisor)
    ).and(singletonFields(i)).apply(i, PotatoHarvestLoot::new));
    private static final DeferredRegister<MapCodec<? extends LootPoolEntryContainer>> TYPES =
            DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, HarvestFestival.MOD_ID);
    static { TYPES.register("potato_harvest", () -> MAP_CODEC); }

    private final Holder<Item> normal;
    private final Holder<Item> poisonous;
    private final int extraDivisor;
    private final int poisonDivisor;

    private PotatoHarvestLoot(Holder<Item> normal, Holder<Item> poisonous, int extraDivisor, int poisonDivisor,
            int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.normal = normal;
        this.poisonous = poisonous;
        this.extraDivisor = extraDivisor;
        this.poisonDivisor = poisonDivisor;
    }

    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public MapCodec<PotatoHarvestLoot> codec() { return MAP_CODEC; }
    @Override protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
        roll(context.getRandom()::nextInt, output);
    }

    void roll(IntUnaryOperator random, Consumer<ItemStack> output) {
        // The first and final condition rolls are intentional, even when they cannot change the count.
        for (int i = 0; i < (random.applyAsInt(extraDivisor) == 0 ? 2 : 1); i++) {
            output.accept(new ItemStack(random.applyAsInt(poisonDivisor) == 0 ? poisonous : normal));
        }
    }
}

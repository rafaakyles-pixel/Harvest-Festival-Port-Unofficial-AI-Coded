package harvestfestival.tools;

import harvestfestival.HarvestFestival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ToolEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HarvestFestival.MOD_ID);
    private static final class Effect extends MobEffect {
        Effect(int color) { super(MobEffectCategory.HARMFUL, color); }
    }
    public static final DeferredHolder<MobEffect, MobEffect> FATIGUE = EFFECTS.register("fatigue", () -> new Effect(0xD9D900)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.parse("harvestfestival:fatigue_speed"),
                    -0.10000000596046448D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> EXHAUSTION = EFFECTS.register("exhaustion", () -> new Effect(0xBBBBBB)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.parse("harvestfestival:exhaustion_speed"),
                    -0.50000000596046448D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, Identifier.parse("harvestfestival:exhaustion_attack"),
                    -0.50000000596046448D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    private ToolEffects() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}

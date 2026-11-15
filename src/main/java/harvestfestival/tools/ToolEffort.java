package harvestfestival.tools;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.storage.TagValueInput;

/** The original branch order is significant: creating exhaustion never faints on that same call. */
public final class ToolEffort {
    public enum Transition { NONE, FATIGUE, EXHAUSTION, FAINT }
    interface Target {
        int food();
        int exhaustionDuration();
        void exhaust(float amount);
        void fatigue();
        void exhaustion();
        void blindness();
        void faint(ToolEffortSettings.Rule rule);
        void clear();
        void restore();
    }
    private ToolEffort() {}
    public static Transition transition(int food, int exhaustionDuration, boolean early) {
        if (food > 2 && food <= 6) return Transition.FATIGUE;
        if (food <= 2 && exhaustionDuration < 0) return Transition.EXHAUSTION;
        if (exhaustionDuration >= 0 && ((food == 0 && exhaustionDuration <= 1990)
                || (early && exhaustionDuration <= 1500))) return Transition.FAINT;
        return Transition.NONE;
    }
    public static void basicAction(ServerPlayer player) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule != null && rule.consumeHunger() && !player.isCreative()) consume(player, rule.exhaustion());
    }
    public static void consume(ServerPlayer player, float amount) {
        consume(new Target() {
            public int food() { return player.getFoodData().getFoodLevel(); }
            public int exhaustionDuration() { var effect = player.getEffect(ToolEffects.EXHAUSTION); return effect == null ? -1 : effect.getDuration(); }
            public void exhaust(float cost) { player.getFoodData().addExhaustion(cost); }
            public void fatigue() { player.removeEffect(ToolEffects.EXHAUSTION); addLegacyEffect(player, ToolEffects.FATIGUE, 6000, 0, false, true); }
            public void exhaustion() { player.removeEffect(ToolEffects.FATIGUE); addLegacyEffect(player, ToolEffects.EXHAUSTION, 2000, 0, false, true); }
            public void blindness() { addLegacyEffect(player, MobEffects.BLINDNESS, 100, 7, false, true); }
            public void faint(ToolEffortSettings.Rule rule) { ToolSleep.faint(player, rule); }
            public void clear() { ToolEffort.clear(player); }
            public void restore() { ToolEffort.restore(player.getFoodData(), player.registryAccess()); }
        }, amount, ToolEffortSettings.INSTANCE.rule());
    }
    static void consume(Target target, float amount, ToolEffortSettings.Rule rule) {
        int food = target.food();
        if (amount > 0) target.exhaust(amount);
        if (rule == null) return; // Sickle's existing cost still applies if the new resource is invalid.
        switch (transition(food, target.exhaustionDuration(), rule.earlyFainting())) {
            case FATIGUE -> target.fatigue();
            case EXHAUSTION -> target.exhaustion();
            case FAINT -> {
                target.blindness();
                if (rule.fainting()) target.faint(rule);
                target.clear();
                if (rule.restoreOnFaint()) target.restore();
            }
            case NONE -> { }
        }
    }
    /** 1.12 combine replaces stronger effects without retaining a hidden lower-amplifier instance. */
    static void addLegacyEffect(ServerPlayer player, Holder<MobEffect> type, int ticks, int amplifier, boolean ambient, boolean particles) {
        var old = player.getEffect(type);
        var merged = merge(old, new MobEffectInstance(type, ticks, amplifier, ambient, particles));
        if (old != null) player.removeEffect(type);
        player.addEffect(merged);
    }
    static MobEffectInstance merge(MobEffectInstance old, MobEffectInstance incoming) {
        if (old == null) return incoming;
        int duration = old.getDuration(), amplifier = old.getAmplifier();
        boolean ambient = old.isAmbient();
        if (incoming.getAmplifier() > amplifier) {
            amplifier = incoming.getAmplifier(); duration = incoming.getDuration();
        } else if (incoming.getAmplifier() == amplifier && incoming.getDuration() > duration) {
            duration = incoming.getDuration();
        } else if (!incoming.isAmbient() && ambient) ambient = false;
        return new MobEffectInstance(incoming.getEffect(), duration, amplifier, ambient, incoming.isVisible());
    }
    public static void clear(ServerPlayer player) {
        player.removeEffect(ToolEffects.FATIGUE);
        player.removeEffect(ToolEffects.EXHAUSTION);
    }
    public static void restore(FoodData food, net.minecraft.core.HolderLookup.Provider registries) {
        // Read only FoodData's four-field payload, never the player entity's save data.
        CompoundTag tag = new CompoundTag();
        tag.putInt("foodLevel", 20);
        tag.putFloat("foodSaturationLevel", 5F);
        tag.putFloat("foodExhaustionLevel", 0F);
        tag.putInt("foodTickTimer", 0);
        food.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }
}

package harvestfestival.tools;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;
import static harvestfestival.tools.ToolEffort.Transition.*;
import static org.junit.jupiter.api.Assertions.*;

class ToolEffortTest {
    static final class Target implements ToolEffort.Target {
        int food, duration=-1; float cost;
        final java.util.List<String> calls=new java.util.ArrayList<>();
        public int food(){return food;}
        public int exhaustionDuration(){return duration;}
        public void exhaust(float amount){cost+=amount;calls.add("cost");}
        public void fatigue(){duration=-1;calls.add("fatigue");}
        public void exhaustion(){duration=2000;calls.add("exhaustion");}
        public void blindness(){calls.add("blindness");}
        public void faint(ToolEffortSettings.Rule rule){calls.add(rule.deathFainting()?"death":"teleport");}
        public void clear(){duration=-1;calls.add("clear");}
        public void restore(){food=20;calls.add("restore");}
    }
    @Test void faintingDisabledStillBlindsClearsAndRestoresWithoutTeleport() throws Exception {
        var json=com.google.gson.JsonParser.parseString(ToolEffortSettingsTest.bundled()).getAsJsonObject();
        json.addProperty("enable_fainting",false);
        var target=new Target();target.duration=1990;
        ToolEffort.consume(target,0,ToolEffortSettings.parse(json));
        assertEquals(java.util.List.of("blindness","clear","restore"),target.calls);
        assertEquals(20,target.food);assertEquals(-1,target.duration);assertEquals(0,target.cost);
    }
    @Test void firstEmptyFoodActionDoesNotFaintAndLaterActionTeleportsBeforeRecovery() throws Exception {
        var rule=ToolEffortSettings.parse(com.google.gson.JsonParser.parseString(ToolEffortSettingsTest.bundled()).getAsJsonObject());
        var target=new Target();ToolEffort.consume(target,rule.exhaustion(),rule);
        assertEquals(java.util.List.of("cost","exhaustion"),target.calls);
        assertEquals(2000,target.duration);
        target.calls.clear();target.duration=1990;
        ToolEffort.consume(target,rule.exhaustion(),rule);
        assertEquals(java.util.List.of("cost","blindness","teleport","clear","restore"),target.calls);
        assertEquals(rule.exhaustion()*2,target.cost);
    }
    @Test void recoveryDisabledAndExplicitDeathAreIndependentAndInvalidRulePreservesSickleCost() throws Exception {
        var json=com.google.gson.JsonParser.parseString(ToolEffortSettingsTest.bundled()).getAsJsonObject();
        json.addProperty("restore_hunger_on_fainting",false);json.addProperty("death_fainting",true);
        var target=new Target();target.duration=1990;
        ToolEffort.consume(target,0,ToolEffortSettings.parse(json));
        assertEquals(java.util.List.of("blindness","death","clear"),target.calls);assertEquals(0,target.food);
        target.calls.clear();ToolEffort.consume(target,0.5F,null);
        assertEquals(java.util.List.of("cost"),target.calls);assertEquals(0.5F,target.cost);
    }
    @Test void foodThresholdsAndOriginalBranchOrder() {
        assertEquals(NONE, ToolEffort.transition(7, -1, false));
        for (int food : new int[]{3, 4, 5, 6}) {
            assertEquals(FATIGUE, ToolEffort.transition(food, -1, false));
            assertEquals(FATIGUE, ToolEffort.transition(food, 1000, true));
        }
        for (int food : new int[]{0, 1, 2}) assertEquals(EXHAUSTION, ToolEffort.transition(food, -1, true));
        assertEquals(NONE, ToolEffort.transition(0, 2000, false));
        assertEquals(NONE, ToolEffort.transition(0, 1991, false));
        assertEquals(FAINT, ToolEffort.transition(0, 1990, false));
        assertEquals(FAINT, ToolEffort.transition(0, 1, false));
    }
    @Test void earlyFaintingNeedsExistingEffectAndDoesNotOverrideFatigueBranch() {
        for (int food : new int[]{1, 2, 7, 20}) {
            assertEquals(NONE, ToolEffort.transition(food, 1501, true));
            assertEquals(FAINT, ToolEffort.transition(food, 1500, true));
            assertEquals(NONE, ToolEffort.transition(food, 1500, false));
        }
        assertEquals(FATIGUE, ToolEffort.transition(6, 1499, true));
    }
    @Test void restorationResetsAllFourFoodFieldsAndRoundTrips() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var tag = new CompoundTag();
        tag.putInt("foodLevel", 0); tag.putFloat("foodSaturationLevel", 0);
        tag.putFloat("foodExhaustionLevel", 37); tag.putInt("foodTickTimer", 79);
        var food = new FoodData();
        food.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        ToolEffort.restore(food, registries);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        food.addAdditionalSaveData(output);
        var saved = output.buildResult();
        assertEquals(20, saved.getIntOr("foodLevel", -1));
        assertEquals(5F, saved.getFloatOr("foodSaturationLevel", -1));
        assertEquals(0F, saved.getFloatOr("foodExhaustionLevel", -1));
        assertEquals(0, saved.getIntOr("foodTickTimer", -1));
        var reopened = new FoodData();
        reopened.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
        assertEquals(20, reopened.getFoodLevel()); assertEquals(5F, reopened.getSaturationLevel());
    }
    @Test void legacyBlindnessMergeDoesNotRetainHiddenWeakerEffect() {
        var random = new MobEffectInstance(MobEffects.BLINDNESS, 300, 0, true, false);
        var faint = new MobEffectInstance(MobEffects.BLINDNESS, 100, 7, false, true);
        var result = ToolEffort.merge(random, faint);
        assertEquals(100, result.getDuration()); assertEquals(7, result.getAmplifier());
        assertTrue(result.isAmbient()); assertTrue(result.isVisible()); // Legacy combine retains old ambient in stronger branch.
        var weaker = ToolEffort.merge(result, random);
        assertEquals(100, weaker.getDuration()); assertEquals(7, weaker.getAmplifier());
        assertFalse(weaker.isVisible());
        var ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var encoded = MobEffectInstance.CODEC.encodeStart(ops, weaker).getOrThrow();
        assertFalse(encoded.toString().contains("hidden_effect"));
    }
    @Test void hfEffectsKeepDurationAmplifierAndTypeThroughCodec() {
        var ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        for (var effect : new MobEffectInstance[]{new MobEffectInstance(ToolEffects.FATIGUE, 5999),
                new MobEffectInstance(ToolEffects.EXHAUSTION, 1990)}) {
            var copy = MobEffectInstance.CODEC.parse(ops, MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow()).getOrThrow();
            assertEquals(effect.getEffect(), copy.getEffect()); assertEquals(effect.getDuration(), copy.getDuration());
            assertEquals(0, copy.getAmplifier());
        }
    }
    @Test void registeredAttributesMatchOriginalMultiplicativePenalties() {
        var fatigue = new java.util.ArrayList<net.minecraft.world.entity.ai.attributes.AttributeModifier>();
        var exhaustion = new java.util.ArrayList<net.minecraft.world.entity.ai.attributes.AttributeModifier>();
        ToolEffects.FATIGUE.get().createModifiers(0, (attribute, modifier) -> fatigue.add(modifier));
        ToolEffects.EXHAUSTION.get().createModifiers(0, (attribute, modifier) -> exhaustion.add(modifier));
        assertEquals(1, fatigue.size()); assertEquals(2, exhaustion.size());
        assertEquals(-0.10000000596046448D, fatigue.getFirst().amount());
        exhaustion.forEach(modifier -> { assertEquals(-0.50000000596046448D, modifier.amount());
            assertEquals(net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, modifier.operation()); });
    }
}

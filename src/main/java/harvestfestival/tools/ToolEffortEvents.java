package harvestfestival.tools;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ToolEffortEvents {
    private ToolEffortEvents() {}
    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ToolEffortSettings::register);
        bus.addListener(ToolEffortEvents::beforeTick);
        bus.addListener(ToolEffortEvents::afterTick);
        bus.addListener(ToolEffortEvents::finishedUsing);
        bus.addListener(ToolEffortEvents::attack);
        bus.addListener(ToolEffortEvents::drops);
        bus.addListener(ToolEffortEvents::damage);
        bus.addListener(ToolSleep::canSleep);
        bus.addListener(ToolSleep::continueSleeping);
        bus.addListener(ToolSleep::finished);
        bus.addListener(ToolSleep::woke);
    }
    static void randomBlindness(ServerPlayer player) {
        if (ToolEffortSettings.INSTANCE.rule() != null && player.hasEffect(ToolEffects.EXHAUSTION)
                && player.getRandom().nextInt(256) == 0)
            ToolEffort.addLegacyEffect(player, MobEffects.BLINDNESS, 300, 0, true, false);
    }
    public static void beforeTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getGameTime() % 20 == 0) randomBlindness(player);
    }
    public static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) randomBlindness(player);
    }
    public static void finishedUsing(LivingEntityUseItemEvent.Finish event) {
        if (ToolEffortSettings.INSTANCE.rule() == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        int food = player.getFoodData().getFoodLevel();
        if (food > 2) player.removeEffect(ToolEffects.EXHAUSTION);
        if (food > 6) player.removeEffect(ToolEffects.FATIGUE);
    }
    public static void attack(AttackEntityEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule != null && rule.attackFainting() && event.getEntity() instanceof ServerPlayer player) ToolEffort.consume(player, 0);
    }
    public static void drops(BlockDropsEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule != null && rule.blockFainting() && event.getBreaker() instanceof ServerPlayer player) ToolEffort.consume(player, 0);
    }
    public static void damage(LivingIncomingDamageEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule != null && rule.sleepHandlerEnabled() && event.getEntity() instanceof ServerPlayer player
                && player.isSleeping() && event.getSource().is(DamageTypes.STARVE)) event.setCanceled(true);
    }
}

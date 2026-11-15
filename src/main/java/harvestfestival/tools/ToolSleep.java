package harvestfestival.tools;

import harvestfestival.calendar.Season;
import harvestfestival.calendar.WorldCalendar;
import harvestfestival.tools.mixin.PlayerSleepAccessor;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

public final class ToolSleep {
    public static final TagKey<DimensionType> SURFACE = TagKey.create(Registries.DIMENSION_TYPE, Identifier.parse("harvestfestival:surface_sleep"));
    public static final TagKey<DimensionType> RESPAWN = TagKey.create(Registries.DIMENSION_TYPE, Identifier.parse("harvestfestival:fainting_respawn"));
    public static final ResourceKey<DamageType> EXHAUSTED = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("harvestfestival:exhausted"));
    private ToolSleep() {}
    public static boolean restrictedTime(long ticks, Season season) {
        long time = Math.floorMod(ticks + 6000L, 24000L);
        int sunset = switch (season) { case SPRING -> 20500; case SUMMER -> 21500; case AUTUMN -> 19000; case WINTER -> 16500; };
        return time >= 6000 && time <= sunset;
    }
    private static boolean restricted(ServerPlayer player, ToolEffortSettings.Rule rule) {
        var server = player.level().getServer();
        return rule.sleepOnlyAtNight() && restrictedTime(WorldCalendar.ticks(server), WorldCalendar.get(server).season());
    }
    static Player.BedSleepingProblem problem(ServerPlayer player, BlockPos pos) {
        if (player.isSleeping() || !player.isAlive()) return Player.BedSleepingProblem.OTHER_PROBLEM;
        if (!player.level().dimensionTypeRegistration().is(SURFACE)) return Player.BedSleepingProblem.OTHER_PROBLEM;
        if (Math.abs(player.getX() - pos.getX()) > 3 || Math.abs(player.getY() - pos.getY()) > 2
                || Math.abs(player.getZ() - pos.getZ()) > 3) return Player.BedSleepingProblem.TOO_FAR_AWAY;
        if (!player.level().getEntitiesOfClass(Monster.class, new AABB(pos.getX()-8, pos.getY()-5, pos.getZ()-8,
                pos.getX()+8, pos.getY()+5, pos.getZ()+8)).isEmpty()) return Player.BedSleepingProblem.NOT_SAFE;
        return null;
    }
    public static void canSleep(CanPlayerSleepEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule == null || !rule.sleepHandlerEnabled()) return;
        if (restricted(event.getEntity(), rule)) event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        else if (rule.sleepAnytime()) event.setProblem(problem(event.getEntity(), event.getPos()));
    }
    public static void continueSleeping(CanContinueSleepingEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule == null || !rule.sleepHandlerEnabled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        // The old isInBed check still applied before SleepingTimeCheck: never keep a missing bed alive.
        var pos = player.getSleepingPos();
        if (pos.isEmpty() || !player.level().getBlockState(pos.get()).isBed(player.level(), pos.get(), player)) {
            event.setContinueSleeping(false);
        } else if (rule.sleepAnytime() && !restricted(player, rule)) event.setContinueSleeping(true);
    }
    static void faint(ServerPlayer player, ToolEffortSettings.Rule rule) {
        if (rule.deathFainting()) {
            player.hurtServer(player.level(), new DamageSource(player.registryAccess().getOrThrow(EXHAUSTED)), 1000F);
            return;
        }
        ServerLevel destination = player.level().dimensionTypeRegistration().is(RESPAWN)
                ? player.level() : player.level().getServer().overworld();
        var spawn = player.getRespawnConfig();
        // Per-dimension memory explicitly deferred. Never use another dimension's stored coordinates here.
        BlockPos pos = spawn != null && spawn.respawnData().dimension().equals(destination.dimension())
                ? spawn.respawnData().pos() : randomSpawn(destination);
        if (!player.teleportTo(destination, pos.getX(), pos.getY(), pos.getZ(), Set.of(), player.getYRot(), player.getXRot(), false)) return;
        player.startSleepInBed(pos);
        if (rule.instantSleep()) ((PlayerSleepAccessor) player).harvestfestival$setSleepCounter(100);
    }
    static int spawnOffset(int fuzz, java.util.function.IntUnaryOperator random) {
        int size = Math.max(2, fuzz);
        return size / 2 - random.applyAsInt(size);
    }
    private static BlockPos randomSpawn(ServerLevel level) {
        BlockPos pos = level.getRespawnData().pos();
        int fuzz = Math.min(level.getGameRules().get(GameRules.RESPAWN_RADIUS),
                Mth.floor(level.getWorldBorder().getDistanceToBorder(pos.getX(), pos.getZ())));
        // Original uses the world's default game type, not the individual player's current mode.
        if (!level.dimensionType().hasCeiling() && level.getServer().getDefaultGameType() != net.minecraft.world.level.GameType.ADVENTURE && fuzz != 0) {
            pos = pos.offset(spawnOffset(fuzz, level.getRandom()::nextInt), 0, spawnOffset(fuzz, level.getRandom()::nextInt));
            pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            // Legacy getTopSolidOrLiquidBlock actually tests movement-blocking material, skipping leaves/foliage.
            // The modern heightmap includes fluids, so descend through them rather than treating water as ground.
            while (pos.getY() > level.getMinY()) {
                var below = level.getBlockState(pos.below());
                if (below.blocksMotion() && !below.is(net.minecraft.tags.BlockTags.LEAVES)) break;
                pos = pos.below();
            }
        }
        return pos;
    }
    public static void finished(SleepFinishedTimeEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule == null || !(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        long ticks = WorldCalendar.ticks(level.getServer());
        // Legacy sleep ends at the next 24000 boundary; use the clock API and the existing daily detector.
        event.setAdjustment(new ClockAdjustment.Absolute(Math.addExact(ticks - Math.floorMod(ticks, 24000), 24000)));
    }
    public static void woke(PlayerWakeUpEvent event) {
        var rule = ToolEffortSettings.INSTANCE.rule();
        if (rule != null && rule.restoreOnSleep() && event.getEntity() instanceof ServerPlayer player
                && WorldCalendar.ticks(player.level().getServer()) % 24000 == 0) {
            ToolEffort.clear(player);
            ToolEffort.restore(player.getFoodData(), player.registryAccess());
        }
    }
}

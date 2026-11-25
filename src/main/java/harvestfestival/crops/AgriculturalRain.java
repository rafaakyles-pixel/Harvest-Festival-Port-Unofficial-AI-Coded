package harvestfestival.crops;

import harvestfestival.calendar.DailyWeather;
import harvestfestival.calendar.Weather;
import harvestfestival.tools.FarmlandSoil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** The original daily predicate is independent of the world's instantaneous rain strength. */
public final class AgriculturalRain {
    public static final TagKey<Biome> EXCLUDED_BIOMES = TagKey.create(Registries.BIOME,
            Identifier.parse("harvestfestival:agricultural_rain_excluded"));
    private static final Map<ServerLevel, List<PendingTill>> PENDING = new IdentityHashMap<>();

    private AgriculturalRain() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AgriculturalRain::tilled);
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> tick(event));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> tick(event));
        NeoForge.EVENT_BUS.addListener(AgriculturalRain::stopped);
    }

    public static boolean at(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var biome = level.getBiome(pos);
        return at(DailyWeather.get(level.getServer()).today(), biome.value().hasPrecipitation(),
                biome.is(EXCLUDED_BIOMES), biome.value().getModifiedClimateSettings().downfall(),
                level.canSeeSkyFromBelowWater(pos),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY());
    }

    public static boolean at(Weather weather, boolean precipitation, boolean excludedBiome, float downfall,
            boolean skyVisible, boolean belowNoObstruction) {
        boolean rainyDay = weather == Weather.RAIN || weather == Weather.TYPHOON;
        boolean humidSnow = (weather == Weather.SNOW || weather == Weather.BLIZZARD) && downfall > 0.85F;
        return (rainyDay || humidSnow) && precipitation && !excludedBiome && skyVisible && belowNoObstruction;
    }

    /** World.isRaining in 1.12.2: no position, biome, weather-kind or dimension-type filter. */
    public static boolean onLoad(float rainStrength) {
        return (double) rainStrength > 0.2D;
    }

    public static BlockState hydrate(BlockState state) {
        return FarmlandSoil.managed(state) ? state.setValue(FarmlandBlock.MOISTURE, 7) : state;
    }

    public static BlockState soilDay(BlockState state, BlockState above, boolean rain) {
        return rain ? hydrate(state) : FarmlandSoil.dry(state, above);
    }

    private static void tilled(BlockEvent.BlockToolModificationEvent event) {
        if (event.isCanceled() || event.isSimulated() || event.getItemAbility() != ItemAbilities.HOE_TILL
                || !(event.getLevel() instanceof ServerLevel level)) return;
        // Sample at the hoe event, not at the delayed write. Both HF and vanilla hoes publish it.
        if (at(level, event.getPos().above(2))) {
            PENDING.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new PendingTill(event.getPos().immutable()));
        }
    }

    private static void tick(LevelTickEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pending = PENDING.get(level);
        if (pending == null) return;
        pending.removeIf(task -> {
            if (!task.ready()) return false;
            if (level.hasChunkAt(task.pos)) {
                var state = level.getBlockState(task.pos);
                var next = hydrate(state);
                if (!next.equals(state)) level.setBlock(task.pos, next, Block.UPDATE_ALL);
            }
            return true;
        });
        if (pending.isEmpty()) PENDING.remove(level);
    }

    private static void stopped(ServerStoppedEvent event) {
        PENDING.keySet().removeIf(level -> level.getServer() == event.getServer());
    }

    static final class PendingTill {
        final BlockPos pos;
        private int callbacks;
        PendingTill(BlockPos pos) { this.pos = pos; }
        // Original WorldTickEvent handler counts both START and END, writes on callback 31.
        boolean ready() { return callbacks++ >= 30; }
    }
}

package harvestfestival.calendar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Weather kind, instantaneous strength, and local precipitation are deliberately separate. */
public final class WinterWeather {
    public static final TagKey<Block> SNOW = tag("seasonal_snow");
    public static final TagKey<Block> ICE = tag("seasonal_ice");
    private WinterWeather() {}
    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.parse("harvestfestival:" + name));
    }
    public static boolean snowy(Weather weather) { return weather == Weather.SNOW || weather == Weather.BLIZZARD; }
    public static boolean rainy(Weather weather) { return weather == Weather.RAIN || weather == Weather.TYPHOON; }
    public static boolean humid(Biome biome) { return biome.getModifiedClimateSettings().downfall() > .85F; }
    public static boolean seasonal(Biome.Precipitation nativeType, boolean humid, Weather weather) {
        return nativeType == Biome.Precipitation.RAIN && !humid && snowy(weather);
    }
    public static Biome.Precipitation visual(Biome.Precipitation nativeType, boolean humid, Weather recent) {
        if (nativeType == Biome.Precipitation.NONE) return nativeType;
        return humid ? Biome.Precipitation.RAIN
                : snowy(recent) || nativeType == Biome.Precipitation.SNOW ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
    }
    public static boolean tracked(BlockState state) {
        return state.is(ICE) || state.is(SNOW) && state.hasProperty(SnowLayerBlock.LAYERS);
    }
    public static boolean snowSite(boolean insideHeight, int light, boolean existingSnow, boolean replaceable, boolean supported) {
        return insideHeight && light < 10 && !existingSnow && replaceable && supported;
    }
    public static boolean iceSite(boolean insideHeight, int light, boolean sourceWater, boolean surrounded) {
        return insideHeight && light < 10 && sourceWater && !surrounded;
    }
    public static boolean formSnow(Biome biome, ServerLevel level, BlockPos pos) {
        if (!DailyWeather.controls(level)) return biome.shouldSnow(level, pos);
        var state = level.getBlockState(pos);
        var type = biome.getPrecipitationAt(pos, level.getSeaLevel());
        boolean seasonal = seasonal(type, humid(biome), DailyWeather.get(level.getServer()).today());
        // Native cold formation places the first layer only, not the modern per-tick accumulation.
        boolean result = seasonal
                ? snowSite(level.isInsideBuildHeight(pos.getY()), level.getBrightness(LightLayer.BLOCK, pos),
                    state.is(SNOW), state.canBeReplaced(), Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
                : state.isAir() && biome.shouldSnow(level, pos);
        if (result && seasonal) WinterUpdates.track(level, pos);
        return result;
    }
    public static boolean formIce(Biome biome, ServerLevel level, BlockPos pos) {
        if (!DailyWeather.controls(level)) return biome.shouldFreeze(level, pos);
        boolean seasonal = seasonal(biome.getPrecipitationAt(pos, level.getSeaLevel()), humid(biome),
                DailyWeather.get(level.getServer()).today());
        if (!seasonal) return biome.shouldFreeze(level, pos);
        var state = level.getBlockState(pos);
        boolean result = iceSite(level.isInsideBuildHeight(pos.getY()), level.getBrightness(LightLayer.BLOCK, pos),
                state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource(),
                water(level, pos.west()) && water(level, pos.east()) && water(level, pos.north()) && water(level, pos.south()));
        if (result) WinterUpdates.track(level, pos);
        return result;
    }
    private static boolean water(ServerLevel level, BlockPos pos) { return level.getFluidState(pos).is(FluidTags.WATER); }
    public static boolean melts(Weather weather, Season season, int sample) {
        return !snowy(weather) && season != Season.WINTER && sample < 2;
    }
    public static int layers(Weather weather, int current) {
        return weather == Weather.BLIZZARD && current < 5 ? current + 1 : current;
    }
    public static int fogTarget(Weather weather, Biome.Precipitation type, boolean humid, boolean exposed, boolean belowSurface) {
        if (belowSurface) return 5000;
        if (!exposed) return 100;
        if (type != Biome.Precipitation.RAIN || humid) return 5000;
        return weather == Weather.BLIZZARD ? -20000 : weather == Weather.SNOW ? -1000 : 100;
    }
}

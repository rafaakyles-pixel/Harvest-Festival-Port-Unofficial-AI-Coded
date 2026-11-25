package harvestfestival.calendar;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer.ColumnInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = "harvestfestival", value = Dist.CLIENT)
public final class WinterClient {
    private static ClientLevel fogLevel;
    private static int fogStart, fogTarget;
    private WinterClient() {}
    public static WeatherSnapshot view(ClientLevel level) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() != level) return null;
        var view = player.getData(WeatherSnapshot.VIEW);
        return view.dimension().equals(level.dimension().identifier().toString()) ? view : null;
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        fogLevel = null;
        fogStart = fogTarget = 0;
    }
    @SubscribeEvent public static void precipitation(ExtractLevelRenderStateEvent event) {
        var level = event.getLevel();
        var view = view(level);
        if (view == null || !DailyWeather.controls(level)) return;
        var state = event.getRenderState().weatherRenderState;
        if (state.intensity <= 0) return;
        var columns = new ArrayList<ColumnInstance>(state.rainColumns);
        columns.addAll(state.snowColumns);
        state.rainColumns.clear();
        state.snowColumns.clear();
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int cameraY = event.getCamera().blockPosition().getY();
        for (var column : columns) {
            var pos = new BlockPos(column.x(), cameraY, column.z());
            var biome = level.getBiome(pos).value();
            var type = WinterWeather.visual(biome.getPrecipitationAt(pos, level.getSeaLevel()), WinterWeather.humid(biome), view.recent());
            var random = RandomSource.create(column.x() * column.x() * 3121 + column.x() * 45238971
                    ^ column.z() * column.z() * 418711 + column.z() * 13761);
            int light = LightCoordsUtil.getLightCoords(level, pos.atY(Math.max(cameraY,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.x(), column.z()))));
            long ticks = level.getGameTime();
            float u = 0, v;
            if (type == Biome.Precipitation.SNOW) {
                float time = (int) (ticks & 131071L) + partial;
                u = (float) (random.nextDouble() + time * .01F * (float) random.nextGaussian());
                v = (float) (random.nextDouble() + time * (float) random.nextGaussian() * .001F)
                        - ((ticks & 511L) + partial) / 512F;
                light = LightCoordsUtil.pack((LightCoordsUtil.block(light) * 3 + 15) / 4, (LightCoordsUtil.sky(light) * 3 + 15) / 4);
            } else {
                int offset = column.x() * column.x() * 3121 + column.x() * 45238971
                        + column.z() * column.z() * 418711 + column.z() * 13761 & 255;
                v = (-((int) (ticks & 131071L) + offset + partial) / 32F * (3F + random.nextFloat())) % 32F;
            }
            var next = new ColumnInstance(column.x(), column.z(), column.bottomY(), column.topY(), u, v, light);
            if (type == Biome.Precipitation.SNOW) state.snowColumns.add(next);
            else if (type == Biome.Precipitation.RAIN) state.rainColumns.add(next);
        }
    }
    private static WeatherSnapshot fogView() {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) return null;
        var view = view(level);
        return view != null && view.snowFog() ? view : null;
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        var view = fogView();
        if (view == null) return;
        var level = Minecraft.getInstance().level;
        if (fogLevel != level) { fogLevel = level; fogStart = fogTarget = 0; }
        if (event.getCamera().getFluidInCamera() == FogType.WATER || event.getCamera().getFluidInCamera() == FogType.LAVA) {
            fogStart = fogTarget = 100;
            return;
        }
        if (fogStart != fogTarget) fogStart += fogTarget > fogStart ? 5 : -5;
        var pos = event.getCamera().blockPosition();
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        int radius = Minecraft.getInstance().options.weatherRadius().get();
        var biome = level.getBiome(pos).value();
        boolean exposed = Math.max(pos.getY() - radius, height) != Math.max(pos.getY() + radius, height);
        var today = view.forecast().getFirst();
        fogTarget = WinterWeather.fogTarget(today, biome.getPrecipitationAt(pos, level.getSeaLevel()), WinterWeather.humid(biome), exposed, pos.getY() < height);
        if (WinterWeather.snowy(today)) {
            event.setFarPlaneDistance(Math.min(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16F, 150F) * .5F);
            event.setNearPlaneDistance(fogStart / 100F);
        }
    }
    @SubscribeEvent public static void fogColor(ViewportEvent.ComputeFogColor event) {
        var view = fogView();
        var fluid = event.getCamera().getFluidInCamera();
        if (view != null && WinterWeather.snowy(view.forecast().getFirst()) && fluid != FogType.WATER && fluid != FogType.LAVA) {
            event.setRed(1); event.setGreen(1); event.setBlue(1);
        }
    }
}

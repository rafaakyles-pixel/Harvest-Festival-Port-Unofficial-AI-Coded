package harvestfestival.tools.mixin;

import harvestfestival.calendar.WinterWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
abstract class WinterPrecipitationMixin {
    @Redirect(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean harvestfestival$winterSnow(Biome biome, LevelReader level, BlockPos pos) {
        return WinterWeather.formSnow(biome, (ServerLevel) level, pos);
    }
    @Redirect(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean harvestfestival$winterIce(Biome biome, LevelReader level, BlockPos pos) {
        return WinterWeather.formIce(biome, (ServerLevel) level, pos);
    }
}

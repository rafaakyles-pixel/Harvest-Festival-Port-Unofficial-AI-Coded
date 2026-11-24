package harvestfestival.tools.mixin;

import harvestfestival.calendar.DailyWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class DailyWeatherMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void harvestfestival$dailyWeather(CallbackInfo callback) {
        var level = (ServerLevel) (Object) this;
        if (DailyWeather.controls(level)) {
            DailyWeather.advanceVisual(level);
            callback.cancel();
        }
    }
}

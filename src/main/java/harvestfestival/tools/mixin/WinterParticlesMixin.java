package harvestfestival.tools.mixin;

import harvestfestival.calendar.WinterClient;
import harvestfestival.calendar.WinterWeather;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class WinterParticlesMixin {
    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void harvestfestival$winterParticles(CallbackInfo callback) {
        var view = WinterClient.view((ClientLevel) (Object) this);
        if (view != null && !WinterWeather.rainy(view.recent())) callback.cancel();
    }
}

package harvestfestival.tools.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Authorized, narrow access: vanilla has no setter/event for immediate deep sleep. */
@Mixin(Player.class)
public interface PlayerSleepAccessor {
    @Accessor("sleepCounter")
    void harvestfestival$setSleepCounter(int ticks);
}

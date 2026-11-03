package harvestfestival;

import com.mojang.logging.LogUtils;
import harvestfestival.calendar.DailyUpdates;
import harvestfestival.calendar.command.CalendarCommands;
import harvestfestival.player.PlayerGold;
import harvestfestival.player.command.GoldCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(HarvestFestival.MOD_ID)
public final class HarvestFestival {
    public static final String MOD_ID = "harvestfestival";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HarvestFestival(IEventBus modBus) {
        PlayerGold.register(modBus);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::beforeTick);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::afterTick);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::stopped);
        NeoForge.EVENT_BUS.addListener(GoldCommands::register);
        NeoForge.EVENT_BUS.addListener(CalendarCommands::register);
        LOGGER.info("Harvest Festival loaded with persistent player gold and the world calendar.");
    }
}

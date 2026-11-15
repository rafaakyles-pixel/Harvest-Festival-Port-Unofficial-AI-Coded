package harvestfestival;

import com.mojang.logging.LogUtils;
import harvestfestival.calendar.DailyUpdates;
import harvestfestival.calendar.NpcBirthdays;
import harvestfestival.calendar.command.NpcBirthdayCommands;
import harvestfestival.player.PlayerBirthday;
import harvestfestival.player.command.BirthdayCommands;
import harvestfestival.calendar.command.CalendarCommands;
import harvestfestival.player.PlayerGold;
import harvestfestival.player.PlayerShipping;
import harvestfestival.player.ShippingPrices;
import harvestfestival.player.command.GoldCommands;
import harvestfestival.tools.ToolContent;
import harvestfestival.tools.ToolActionRules;
import harvestfestival.crops.CropContent;
import harvestfestival.crops.CropDefinitions;
import harvestfestival.crops.CropDailyUpdates;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(HarvestFestival.MOD_ID)
public final class HarvestFestival {
    public static final String MOD_ID = "harvestfestival";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HarvestFestival(IEventBus modBus) {
        harvestfestival.tools.ToolEffects.register(modBus);
        harvestfestival.tools.ToolEffortEvents.register();
        CreativeTabs.register(modBus);
        NeoForge.EVENT_BUS.addListener(harvestfestival.tools.SickleRules::register);
        CropContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(CropDefinitions::register);
        NeoForge.EVENT_BUS.addListener(CropDailyUpdates::loaded);
        NeoForge.EVENT_BUS.addListener(CropDailyUpdates::unloaded);
        NeoForge.EVENT_BUS.addListener(CropDailyUpdates::stopped);
        NeoForge.EVENT_BUS.addListener(CropDailyUpdates::newDay);
        NeoForge.EVENT_BUS.addListener(CropDailyUpdates::migrateLoaded);
        ToolContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(ToolActionRules::register);
        PlayerGold.register(modBus);
        PlayerBirthday.register(modBus);
        NeoForge.EVENT_BUS.addListener(NpcBirthdays::register);
        NeoForge.EVENT_BUS.addListener(BirthdayCommands::register);
        NeoForge.EVENT_BUS.addListener(NpcBirthdayCommands::register);
        PlayerShipping.register(modBus);
        ShippingContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(ShippingPrices::register);
        NeoForge.EVENT_BUS.addListener(PlayerShipping::newDay);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::beforeTick);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::afterTick);
        NeoForge.EVENT_BUS.addListener(DailyUpdates::stopped);
        NeoForge.EVENT_BUS.addListener(GoldCommands::register);
        NeoForge.EVENT_BUS.addListener(CalendarCommands::register);
        LOGGER.info("Harvest Festival loaded with persistent player gold and the world calendar.");
    }
}

package harvestfestival;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(HarvestFestival.MOD_ID)
public final class HarvestFestival {
    public static final String MOD_ID = "harvestfestival";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HarvestFestival() {
        LOGGER.info("Harvest Festival skeleton loaded; no gameplay content registered.");
    }
}

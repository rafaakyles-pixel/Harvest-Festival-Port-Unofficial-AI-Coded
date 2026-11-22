package harvestfestival.crops;

import java.util.List;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = "harvestfestival", dist = Dist.CLIENT)
public final class CropColors {
    public CropColors(IEventBus bus) { bus.addListener(CropColors::register); }
    private static void register(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new BlockTintSource() {
            @Override public int color(BlockState state) {
                int slot = state.getValue(CropBlock.APPEARANCE);
                if (slot > 106) return -1;
                if (slot >= 96) return state.getValue(CropBlock.WITHERED) ? 0xFF7A5230 : -1;
                if (slot < 72) return -1;
                if (state.getValue(CropBlock.WITHERED)) return 0xFF753A00;
                int age = Math.min(7, (slot - 72) % 12);
                return 0xFF000000 | (age * 32 << 16) | ((255 - age * 8) << 8) | age * 4;
            }
            @Override public Set<Property<?>> relevantProperties() { return Set.of(CropBlock.APPEARANCE, CropBlock.WITHERED); }
        }), CropContent.CROP.get());
        event.register(List.of(new BlockTintSource() {
            @Override public int color(BlockState state) { return state.getValue(CropBlock.WITHERED) ? 0xFF7A5230 : -1; }
            @Override public Set<Property<?>> relevantProperties() { return Set.of(CropBlock.WITHERED); }
        }), CropContent.CROP_TOP.get());
    }
}

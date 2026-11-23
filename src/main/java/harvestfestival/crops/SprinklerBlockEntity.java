package harvestfestival.crops;

import harvestfestival.calendar.DailyCycleData;
import harvestfestival.tools.FarmlandSoil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class SprinklerBlockEntity extends BlockEntity {
    private long lastCycle;
    private int consumption;
    private int visualTick;
    private boolean loading;
    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, 1000) {
        @Override public boolean isValid(int index, FluidResource resource) { return resource.is(OrchardContent.WATER); }
        @Override protected void onContentsChanged(int index, FluidStack previous) { if (!loading) changed(); }
    };
    public SprinklerBlockEntity(BlockPos pos, BlockState state) { super(OrchardContent.SPRINKLER_ENTITY.get(), pos, state); consumption = TreeDefinitions.INSTANCE.settings().sprinklerConsumption(); }
    public FluidStacksResourceHandler tank() { return tank; }
    public int consumption() { return level != null && !level.isClientSide() ? TreeDefinitions.INSTANCE.settings().sprinklerConsumption() : consumption; }
    public static boolean fueled(int cost, long amount) { return cost <= 0 || amount > 1; }
    public static List<BlockPos> positions(BlockPos center, int range) {
        var result = new ArrayList<BlockPos>();
        for (int x = -range; x <= range; x++) for (int z = -range; z <= range; z++) for (int y = 0; y >= -1; y--)
            if (x != 0 || z != 0 || y != 0) result.add(center.offset(x, y, z));
        return List.copyOf(result);
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    /** Null means failure. The creative caller keeps the original hand and discards the temporary result. */
    public ItemStack fillFrom(ItemStack held, boolean creative) {
        if (consumption() <= 0 || held.isEmpty()) return null;
        var temporary = new ItemStacksResourceHandler(1);
        temporary.set(0, ItemResource.of(held), 1);
        var access = ItemAccess.forHandlerIndexStrict(temporary, 0);
        var source = access.getCapability(Capabilities.Fluid.ITEM);
        if (source == null) return null;
        try (var transaction = Transaction.openRoot()) {
            if (ResourceHandlerUtil.move(source, tank, r -> r.is(OrchardContent.WATER), 1000, transaction) <= 0) return null;
            transaction.commit();
        }
        return creative ? held : temporary.getResource(0).toStack((int)temporary.getAmountAsLong(0));
    }
    public void newDay(ServerLevel level, long cycle) {
        newDay(cycle, new SprinklerHydration.Soil() {
            public boolean available(BlockPos pos) { return TreeGrowth.available(level, pos); }
            public BlockState get(BlockPos pos) { return level.getBlockState(pos); }
            public boolean set(BlockPos pos, BlockState state) { return level.setBlock(pos, state, Block.UPDATE_ALL); }
        });
    }
    void newDay(long cycle, SprinklerHydration.Soil soil) {
        if (cycle <= 0 || lastCycle >= cycle) return;
        lastCycle = cycle; setChanged();
        int cost = consumption();
        if (!fueled(cost, tank.getAmountAsLong(0))) return;
        int range = ((SprinklerBlock)getBlockState().getBlock()).range();
        boolean changed = SprinklerHydration.water(worldPosition, range, soil);
        if (changed && cost > 0 && !tank.getResource(0).isEmpty()) try (var transaction = Transaction.openRoot()) {
            tank.extract(0, tank.getResource(0), cost, transaction); transaction.commit();
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity entity) {
        if (level.isClientSide()) {
            if (entity.visualTick++ % 15 == 0) SprinklerVisuals.particles(level, pos, (SprinklerBlock)state.getBlock(), entity.consumption, entity.tank.getAmountAsLong(0));
        } else {
            int current = TreeDefinitions.INSTANCE.settings().sprinklerConsumption();
            if (current != entity.consumption) { entity.consumption = current; entity.changed(); }
        }
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); output.putInt("schema", 1); output.putInt("consumption", consumption());
        output.store("last_cycle", DailyCycleData.MARKER_CODEC, lastCycle);
        tank.serialize(output.child("tank"));
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); loading = true;
        try {
            consumption = input.getIntOr("consumption", 0);
            lastCycle = input.getIntOr("schema", 0) == 1 ? input.read("last_cycle", DailyCycleData.MARKER_CODEC).orElse(Long.MAX_VALUE) : Long.MAX_VALUE;
            input.child("tank").ifPresent(tank::deserialize);
            if (tank.getAmountAsLong(0) > 1000 || (!tank.getResource(0).isEmpty() && !tank.isValid(0, tank.getResource(0)))) tank.set(0, FluidResource.EMPTY, 0);
        } finally { loading = false; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

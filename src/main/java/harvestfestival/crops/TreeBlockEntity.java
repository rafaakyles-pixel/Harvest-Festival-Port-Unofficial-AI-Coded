package harvestfestival.crops;

import harvestfestival.calendar.DailyCycleData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TreeBlockEntity extends BlockEntity {
    private int stage = 1;
    private long lastCycle;
    private boolean valid = true;
    public TreeBlockEntity(BlockPos pos, BlockState state) { super(OrchardContent.TREE_ENTITY.get(), pos, state); }
    public String species() { return ((TreeBlock) getBlockState().getBlock()).species(); }
    public int stage() { return stage; }
    public long lastCycle() { return lastCycle; }
    public boolean valid() { return valid; }
    public void mark(long cycle) { lastCycle = cycle; setChanged(); }
    public void stage(int value) {
        stage = value; setChanged();
        if (level != null && !level.isClientSide()) {
            var definition = TreeDefinitions.INSTANCE.get(species());
            if (definition == null) return;
            var next = getBlockState().setValue(TreeBlock.APPEARANCE, definition.appearance(stage));
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
            var top = OrchardContent.TREES.get(species()).top().get();
            if (level.getBlockState(worldPosition.above()).is(top))
                level.setBlock(worldPosition.above(), top.defaultBlockState().setValue(TreeBlock.APPEARANCE, definition.appearance(stage)), Block.UPDATE_CLIENTS);
            level.sendBlockUpdated(worldPosition, getBlockState(), next, Block.UPDATE_CLIENTS);
        }
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("schema", 1); output.putString("species", species()); output.putInt("stage", stage);
        output.store("last_cycle", DailyCycleData.MARKER_CODEC, lastCycle); output.putBoolean("valid", valid);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stage = input.getIntOr("stage", -1);
        lastCycle = input.read("last_cycle", DailyCycleData.MARKER_CODEC).orElse(Long.MAX_VALUE);
        valid = input.getIntOr("schema", 0) == 1 && input.getStringOr("species", "").equals(species())
                && stage >= 1 && stage <= 10000 && input.getBooleanOr("valid", false);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

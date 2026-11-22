package harvestfestival.crops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class CropBlockEntity extends BlockEntity {
    private CropState crop = CropState.INVALID;

    public CropBlockEntity(BlockPos pos, BlockState state) { super(CropContent.CROP_ENTITY.get(), pos, state); }
    public CropState crop() { return crop; }

    /** Compare the snapshot and commit before drops. A detached entity cannot supply a second loot pass. */
    public boolean commitHarvest(CropState expected, CropState next, java.util.function.BooleanSupplier remove) {
        if (!crop.equals(expected) || isRemoved()) return false;
        if (next == null) {
            if (!remove.getAsBoolean()) return false;
            crop = CropState.INVALID;
        } else {
            setCrop(next);
        }
        return true;
    }

    public void setCrop(CropState value) {
        if (isRemoved()) return;
        crop = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            var definition = CropDefinitions.INSTANCE.get(value.crop());
            int appearance = appearance(value, definition, getBlockState().getValue(CropBlock.APPEARANCE),
                    direction -> level.getBlockState(worldPosition.relative(direction)).is(definition.form().fruitTag()));
            var state = getBlockState().setValue(CropBlock.APPEARANCE, appearance).setValue(CropBlock.WITHERED, value.dead());
            if (!state.equals(getBlockState())) level.setBlock(worldPosition, state, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, getBlockState(), state, Block.UPDATE_CLIENTS);
        }
    }

    static int appearance(CropState value, CropDefinition definition, int fallback,
            java.util.function.Predicate<net.minecraft.core.Direction> fruitNeighbor) {
        int appearance = value.dead() && (definition == null || definition.form().equals(CropForm.SINGLE)) ? 3
                : definition == null ? fallback : definition.appearance(value.stage());
        if (definition != null && definition.form().transforms() && appearance == definition.appearances().getLast()) {
            // 1.12 horizontal iteration: north, east, south, west; pumpkin first, melon last.
            var directions = new net.minecraft.core.Direction[] {net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};
            for (int i = 0; i < directions.length; i++) if (fruitNeighbor.test(directions[i])) {
                appearance = definition.form().attachedAppearance() + i;
                if (!definition.form().lastNeighbor()) break;
            }
        }
        return appearance;
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        crop = input.read("crop_state", CropState.CODEC).orElse(CropState.INVALID);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("crop_state", CropState.CODEC, crop);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

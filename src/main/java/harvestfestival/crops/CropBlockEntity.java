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

    public void setCrop(CropState value) {
        crop = value;
        setChanged();
        if (level != null && !level.isClientSide()) {
            var definition = CropDefinitions.INSTANCE.get(value.crop());
            int appearance = value.dead() ? 3 : definition == null ? getBlockState().getValue(CropBlock.APPEARANCE)
                    : definition.appearance(value.stage());
            var state = getBlockState().setValue(CropBlock.APPEARANCE, appearance);
            if (!state.equals(getBlockState())) level.setBlock(worldPosition, state, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, getBlockState(), state, Block.UPDATE_CLIENTS);
        }
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

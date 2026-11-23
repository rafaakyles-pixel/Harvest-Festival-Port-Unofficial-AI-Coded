package harvestfestival.crops;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SprinklerBlock extends BaseEntityBlock {
    private final int range;
    private final double height;
    public SprinklerBlock(int range, double height, Properties properties) { super(properties); this.range = range; this.height = height; }
    public int range() { return range; }
    public double height() { return height; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new SprinklerBlock(range, height, p)); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(3.2, 0, 3.2, 12.8, height * 16, 12.8); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SprinklerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, OrchardContent.SPRINKLER_ENTITY.get(), SprinklerBlockEntity::tick);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler) || sprinkler.consumption() <= 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var result = sprinkler.fillFrom(stack, player.hasInfiniteMaterials());
        if (result == null) return InteractionResult.PASS;
        if (!player.hasInfiniteMaterials()) player.setItemInHand(hand, result);
        var water = sprinkler.tank().getResource(0);
        var sound = water.getFluidType().getSound(water.toStack(1000), net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
        if (sound != null) level.playSound(null, pos, sound, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 1F);
        return InteractionResult.SUCCESS;
    }
}

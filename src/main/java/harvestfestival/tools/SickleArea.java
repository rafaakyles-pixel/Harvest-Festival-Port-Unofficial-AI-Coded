package harvestfestival.tools;

import harvestfestival.crops.CropContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

/** Sickle callbacks are intentionally separate from the hoe/watering change-then-pay path. */
public final class SickleArea {
    private SickleArea() {}
    public interface Candidate {
        boolean canHarvest();
        boolean remove();
        void destroyed();
        void harvest();
    }
    public interface Target {
        boolean isCrop(BlockPos pos);
        /** Checks permissions and returns a snapshot, or null for an unavailable/denied target. */
        Candidate prepare(BlockPos pos, boolean center);
    }

    public static List<BlockPos> positions(BlockPos anchor, Direction facing, ToolTier charged) {
        int front = charged.sickleFront(), sides = charged.sickleSides();
        int minX = anchor.getX(), maxX = minX, minZ = anchor.getZ(), maxZ = minZ;
        switch (facing) {
            case SOUTH -> { minX -= sides; maxX += sides; maxZ += front; }
            case NORTH -> { minX -= sides; maxX += sides; minZ -= front; }
            case EAST -> { maxX += front; minZ -= sides; maxZ += sides; }
            case WEST -> { minX -= front; minZ -= sides; maxZ += sides; }
            default -> throw new IllegalArgumentException("Horizontal facing required");
        }
        var result = new ArrayList<BlockPos>();
        for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++) result.add(new BlockPos(x,anchor.getY(),z));
        return List.copyOf(result);
    }

    public static int perform(BlockPos anchor, Direction facing, ItemStack stack, boolean creative,
            Target target, BooleanSupplier task) {
        if (creative || !SickleItem.canUse(stack) || !target.isCrop(anchor)) return 0;
        var positions = positions(anchor,facing,ToolTier.charged(SickleState.get(stack).charge()));
        int count = 0;
        for (var pos : positions) {
            if (!SickleItem.canUse(stack)) break;
            if (!target.isCrop(pos)) continue;
            boolean center = pos.equals(anchor);
            var candidate = target.prepare(pos,center);
            if (candidate == null) continue;
            if (!task.getAsBoolean()) break;
            count++;
            // Paying the last durability point still permits this candidate's callbacks.
            // The outer game mode owns the center removal and its eventual drop pass.
            if (!center && candidate.canHarvest() && candidate.remove()) {
                candidate.destroyed();
                candidate.harvest();
            }
        }
        return count;
    }

    static Target world(ServerLevel level, ServerPlayer player, ItemStack stack) {
        return new Target() {
            @Override public boolean isCrop(BlockPos pos) {
                return level.hasChunkAt(pos) && level.getBlockState(pos).is(CropContent.CROPS);
            }
            @Override public Candidate prepare(BlockPos pos, boolean center) {
                if (!isCrop(pos) || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,stack)
                        || player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer())) return null;
                var state = level.getBlockState(pos);
                var entity = level.getBlockEntity(pos);
                // Center already passed the engine's break event. Never post it twice.
                if (!center && CommonHooks.fireBlockBreak(level,player.gameMode.getGameModeForPlayer(),player,pos,state).isCanceled()) return null;
                if (level.getBlockState(pos) != state || level.getBlockEntity(pos) != entity) return null;
                return new Candidate() {
                    private ItemStack tool;
                    private boolean unchanged() { return level.getBlockState(pos) == state && level.getBlockEntity(pos) == entity; }
                    @Override public boolean canHarvest() { return unchanged() && state.canHarvestBlock(level,pos,player); }
                    @Override public boolean remove() {
                        if (!unchanged()) return false;
                        tool = stack.copy();
                        return state.onDestroyedByPlayer(level,pos,player,tool,true,level.getFluidState(pos));
                    }
                    @Override public void destroyed() { state.getBlock().destroy(level,pos,state); }
                    @Override public void harvest() {
                        // Reuses the same entity: HF invalidates it during special harvest removal.
                        // Ordinary single-harvest crops retain their normal playerDestroy loot path.
                        state.getBlock().playerDestroy(level,player,pos,state,entity,tool);
                    }
                };
            }
        };
    }
}

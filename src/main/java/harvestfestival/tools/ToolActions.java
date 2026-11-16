package harvestfestival.tools;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import harvestfestival.crops.CropContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ToolActions {
    private ToolActions() {}

    public enum Result { CHANGED, NO_MATCH, BLOCKED, EMPTY, UNCHANGED, AMBIGUOUS, FAILED }

    /** A single authorized block target; setters must report whether the world accepted the change. */
    public interface Target {
        BlockState state();
        default boolean matches(TagKey<Block> tag) { return state().is(tag); }
        boolean airAbove();
        boolean canEdit();
        boolean set(BlockState state);
    }

    /** Server-only entry point for future consumers; never resolves a crop or another block implicitly. */
    public static Result apply(ToolAction action, Target target, ItemStack stack, boolean creative) {
        return apply(action, target, stack, creative, ToolActionRules.INSTANCE.rules(), ToolActionRules.WARN);
    }

    static Result apply(ToolAction action, Target target, ItemStack stack, boolean creative,
            List<ToolActionRules.Rule> rules, Consumer<String> warn) {
        if (stack.isEmpty() || !target.canEdit()) return Result.BLOCKED;
        var matches = rules.stream().filter(rule -> rule.matches(action, target)).toList();
        if (matches.isEmpty()) return Result.NO_MATCH;
        if (matches.size() > 1) {
            warn.accept("Ambiguous tool action " + action + ": " + matches.stream().map(ToolActionRules.Rule::id).toList());
            return Result.AMBIGUOUS;
        }
        var rule = matches.getFirst();
        if (rule.requiresAir() && !target.airAbove()) return Result.BLOCKED;
        if (rule.result().equals(target.state())) return Result.UNCHANGED;
        var water = WateringCanState.get(stack);
        if (action == ToolAction.WATER && water.water() == 0) return Result.EMPTY;
        if (!target.set(rule.result())) return Result.FAILED;
        if (action == ToolAction.WATER && !creative) stack.set(ToolContent.WATER_STATE.get(), water.consume());
        return Result.CHANGED;
    }

    static InteractionResult useArea(ToolAction action, ToolTier itemTier, ToolTier areaTier, Level level,
            Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        var clicked = hit.getBlockPos();
        if (hit.getType() != HitResult.Type.BLOCK || !level.hasChunkAt(clicked)
                || !FarmToolTasks.canUse(stack, itemTier, action)
                || !allowed(level, player, stack, clicked, hit.getDirection())) return InteractionResult.PASS;
        // The client predicts only acceptance; all state and costs are authoritative on the server.
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        var face = action == ToolAction.HOE ? Direction.UP : hit.getDirection();
        int changes = ToolArea.perform(ToolArea.positions(clicked, Direction.fromYRot(player.getYRot()), action, areaTier),
                () -> FarmToolTasks.canUse(stack, itemTier, action)
                        && (action != ToolAction.WATER || WateringCanState.get(stack).water() > 0),
                candidate -> {
                    if (!level.hasChunkAt(candidate)) return Result.BLOCKED;
                    var pos = resolveTarget(action, candidate, level::getBlockState);
                    return apply(action, new Target() {
                        public BlockState state() { return level.getBlockState(pos); }
                        public boolean airAbove() { return level.isEmptyBlock(pos.above()); }
                        public boolean canEdit() { return level.hasChunkAt(pos)
                                && allowed(level, player, stack, candidate, face) && allowed(level, player, stack, pos, face); }
                        public boolean set(BlockState result) {
                            if (action == ToolAction.HOE) {
                                // Publish the approved tag rule as the proposed result; don't inherit modern extra soil recipes.
                                var context = new UseOnContext(level, player, hand, stack, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                                var event = new BlockEvent.BlockToolModificationEvent(state(), context, ItemAbilities.HOE_TILL, false);
                                event.setFinalState(result);
                                NeoForge.EVENT_BUS.post(event);
                                if (event.isCanceled() || event.getFinalState() == null || event.getFinalState().equals(state())) return false;
                                result = event.getFinalState();
                            }
                            return level.setBlock(pos, result, Block.UPDATE_ALL);
                        }
                    }, stack, player.isCreative());
                }, pos -> FarmToolTasks.complete(stack, itemTier, action, player.isCreative(), ToolEffortSettings.INSTANCE.rule(),
                        damage -> EnchantmentHelper.processDurabilityChange(server, stack, damage), amount -> ToolEffort.consume(serverPlayer, amount)));
        return changes > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    private static boolean allowed(Level level, Player player, ItemStack stack, BlockPos pos, Direction face) {
        return face != Direction.DOWN && level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, stack);
    }

    public static BlockPos resolveTarget(ToolAction action, BlockPos clicked, Function<BlockPos, BlockState> states) {
        return action == ToolAction.WATER && states.apply(clicked).is(CropContent.CROPS) ? clicked.below() : clicked;
    }
}

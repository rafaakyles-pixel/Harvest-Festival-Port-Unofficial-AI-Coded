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

    static InteractionResult useOn(ToolAction action, UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var clicked = context.getClickedPos();
        var pos = resolveTarget(action, clicked, level::getBlockState);
        var stack = context.getItemInHand();
        if (player == null || context.getClickedFace() == Direction.DOWN
                || !level.mayInteract(player, pos)
                || !level.mayInteract(player, clicked)
                || !player.mayUseItemAt(clicked, context.getClickedFace(), stack)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;
        // Rules are server data. Let the server decide; never mutate a client world or stack.
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var result = apply(action, new Target() {
            public BlockState state() { return level.getBlockState(pos); }
            public boolean airAbove() { return level.isEmptyBlock(pos.above()); }
            public boolean canEdit() { return true; } // Checked against the real player above.
            public boolean set(BlockState state) { return level.setBlock(pos, state, Block.UPDATE_ALL); }
        }, stack, player.isCreative());
        if (result == Result.CHANGED && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            ToolEffort.basicAction(serverPlayer);
        return result == Result.CHANGED ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public static BlockPos resolveTarget(ToolAction action, BlockPos clicked, Function<BlockPos, BlockState> states) {
        return action == ToolAction.WATER && states.apply(clicked).is(CropContent.CROPS) ? clicked.below() : clicked;
    }
}

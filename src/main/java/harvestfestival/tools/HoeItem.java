package harvestfestival.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

public final class HoeItem extends Item {
    enum UseAction { DISCHARGE, CHARGE, APPLY }
    public static final int USE_DURATION = 32000;
    private final ToolTier tier;
    public HoeItem(Properties properties, ToolTier tier) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                        tier.chargeLimit() - 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
        this.tier = tier;
    }
    public ToolTier tier() { return tier; }
    @Override public boolean canPerformAction(net.minecraft.world.item.ItemInstance stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return ability == net.neoforged.neoforge.common.ItemAbilities.HOE_TILL;
    }
    static UseAction startAction(ToolTier tier, FarmToolState state, boolean sneaking) {
        if (tier != ToolTier.BASIC && sneaking) return UseAction.DISCHARGE;
        return state.charge() < tier.chargeLimit() ? UseAction.CHARGE : UseAction.APPLY;
    }
    @Override public Component getName(ItemStack stack) {
        return FarmToolTasks.canUse(stack, tier, ToolAction.HOE) ? super.getName(stack)
                : Component.translatable("item.harvestfestival.hoe.broken", super.getName(stack));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand());
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!FarmToolTasks.canUse(stack, tier, ToolAction.HOE)) return InteractionResult.PASS;
        var action = startAction(tier, FarmToolState.get(stack), player.isShiftKeyDown());
        if (action == UseAction.DISCHARGE) {
            if (!level.isClientSide()) {
                stack.set(ToolContent.FARM_TOOL_STATE.get(), FarmToolState.get(stack).discharge());
                player.sendOverlayMessage(Component.translatable("tooltip.harvestfestival.hoe.discharged").withStyle(ChatFormatting.RED));
            }
        } else if (action == UseAction.CHARGE) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        } else {
            act(level, player, hand, stack);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return USE_DURATION; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int elapsed = USE_DURATION - remaining;
        if (!level.isClientSide() && entity instanceof Player player && elapsed > 0 && elapsed % 20 == 0) {
            int charge = FarmToolState.get(stack).charge(tier, elapsed).charge();
            if (charge != FarmToolState.get(stack).charge())
                player.sendOverlayMessage(Component.translatable("tooltip.harvestfestival.hoe.charging", 1 + ToolTier.charged(charge).hoeFront())
                        .withStyle(ChatFormatting.GREEN));
        }
    }
    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player) || !FarmToolTasks.canUse(stack, tier, ToolAction.HOE)) return false;
        if (!level.isClientSide()) stack.set(ToolContent.FARM_TOOL_STATE.get(), FarmToolState.get(stack).charge(tier, USE_DURATION - remaining));
        act(level, player, player.getUsedItemHand(), stack);
        return true;
    }
    private void act(Level level, Player player, InteractionHand hand, ItemStack stack) {
        ToolActions.useArea(ToolAction.HOE, tier, ToolTier.charged(FarmToolState.get(stack).charge()), level, player, hand,
                getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE));
    }
    public static void registerTooltips(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, context, display, player, flag, builder) -> {
            if (!(stack.getItem() instanceof HoeItem hoe) || display.hideTooltip()
                    || !display.shows(ToolContent.FARM_TOOL_STATE.get())) return;
            var state = FarmToolState.get(stack);
            if (state.validFor(hoe.tier) && hoe.tier != ToolTier.BASIC) {
                builder.accept(Component.translatable("tooltip.harvestfestival.hoe.area", 1 + ToolTier.charged(state.charge()).hoeFront()).withStyle(ChatFormatting.GOLD));
                if (state.charge() < hoe.tier.chargeLimit()) builder.accept(Component.translatable("tooltip.harvestfestival.hoe.charge").withStyle(ChatFormatting.AQUA));
                if (state.charge() > 0) builder.accept(Component.translatable("tooltip.harvestfestival.hoe.discharge").withStyle(ChatFormatting.RED));
            }
        });
    }
}

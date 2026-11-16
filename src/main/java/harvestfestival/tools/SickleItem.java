package harvestfestival.tools;

import harvestfestival.crops.CropContent;
import java.util.function.IntUnaryOperator;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;

/** Agricultural sickle; charging stores an area, cutting remains the mining callback. */
public final class SickleItem extends Item {
    public static final int MAX_DAMAGE = 128;
    public static final int USE_DURATION = 32000;
    private final ToolTier tier;
    public static final TagKey<Item> SICKLES = TagKey.create(Registries.ITEM, Identifier.parse("harvestfestival:sickles"));
    public SickleItem(Properties properties) {
        this(properties, ToolTier.BASIC);
    }
    public SickleItem(Properties properties, ToolTier tier) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, tier.chargeLimit() - 3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
        this.tier = tier;
    }
    public ToolTier tier() { return tier; }
    public static boolean canUse(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SickleItem sickle && SickleState.get(stack).validFor(sickle.tier)
                && stack.getDamageValue() < sickle.tier.durability();
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return canUse(stack) ? 1F : 0.05F; }
    @Override public Component getName(ItemStack stack) {
        return canUse(stack) ? super.getName(stack) : Component.translatable("item.harvestfestival.sickle.broken", super.getName(stack));
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!canUse(stack)) return InteractionResult.PASS;
        if (tier != ToolTier.BASIC && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.set(ToolContent.SICKLE_STATE.get(), SickleState.get(stack).discharge());
                player.sendOverlayMessage(Component.translatable("tooltip.harvestfestival.sickle.discharged").withStyle(ChatFormatting.RED));
            }
        } else if (SickleState.get(stack).charge() < tier.chargeLimit()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return USE_DURATION; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int elapsed = USE_DURATION - remaining;
        if (!level.isClientSide() && entity instanceof Player player && elapsed > 0 && elapsed % 20 == 0 && canUse(stack)) {
            var next = SickleState.get(stack).charge(tier,elapsed);
            if (next.charge() != SickleState.get(stack).charge()) {
                var charged = ToolTier.charged(next.charge());
                player.sendOverlayMessage(Component.translatable("tooltip.harvestfestival.sickle.area",
                        1+2*charged.sickleSides(),1+charged.sickleFront()).withStyle(ChatFormatting.GREEN));
            }
        }
    }
    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!canUse(stack)) return false;
        if (!level.isClientSide()) stack.set(ToolContent.SICKLE_STATE.get(),SickleState.get(stack).charge(tier,USE_DURATION-remaining));
        return true;
    }
    public static boolean performTask(ItemStack stack, boolean creative, SickleRules.Rule rule,
            IntUnaryOperator durabilityChange, Consumer<Float> exhaustion) {
        if (!canUse(stack) || rule == null) return false;
        var tier = ((SickleItem)stack.getItem()).tier;
        stack.set(ToolContent.SICKLE_STATE.get(), SickleState.get(stack).advance(tier));
        if (creative || !rule.consumeHunger()) return true;
        exhaustion.accept(rule.exhaustion(tier));
        // Modern hurtAndBreak removes at >= max; original stops at max and retains the broken tool.
        stack.setDamageValue(Math.min(tier.durability(), stack.getDamageValue() + Math.max(0, durabilityChange.applyAsInt(1))));
        return true;
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (level instanceof ServerLevel server && owner instanceof net.minecraft.server.level.ServerPlayer player && state.is(CropContent.CROPS)) {
            SickleArea.perform(pos,Direction.fromYRot(player.getYRot()),stack,player.isCreative(),SickleArea.world(server,player,stack),
                    () -> performTask(stack, player.isCreative(), SickleRules.INSTANCE.rule(),
                    damage -> EnchantmentHelper.processDurabilityChange(server, stack, damage),
                    amount -> ToolEffort.consume(player, amount)));
        }
        return true;
    }
    public static void registerTooltips(RegisterTooltipAppendersEvent event) {
        event.registerAppender(TooltipLocation.POST_CUSTOM, (stack,context,display,player,flag,builder) -> {
            if (!(stack.getItem() instanceof SickleItem sickle) || display.hideTooltip()
                    || !display.shows(ToolContent.SICKLE_STATE.get())) return;
            var state = SickleState.get(stack);
            if (state.validFor(sickle.tier) && sickle.tier != ToolTier.BASIC) {
                var charged = ToolTier.charged(state.charge());
                builder.accept(Component.translatable("tooltip.harvestfestival.sickle.area",1+2*charged.sickleSides(),1+charged.sickleFront()).withStyle(ChatFormatting.GOLD));
                if (state.charge() < sickle.tier.chargeLimit()) builder.accept(Component.translatable("tooltip.harvestfestival.sickle.charge").withStyle(ChatFormatting.AQUA));
                if (state.charge() > 0) builder.accept(Component.translatable("tooltip.harvestfestival.sickle.discharge").withStyle(ChatFormatting.RED));
            }
        });
    }
}

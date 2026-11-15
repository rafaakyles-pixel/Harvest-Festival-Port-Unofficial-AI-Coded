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

/** Basic, single-target agricultural sickle. No charge tiers or HF fainting effects yet. */
public final class SickleItem extends Item {
    public static final int MAX_DAMAGE = 128;
    public static final TagKey<Item> SICKLES = TagKey.create(Registries.ITEM, Identifier.parse("harvestfestival:sickles"));
    public SickleItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
    }
    public static boolean canUse(ItemStack stack) {
        return !stack.isEmpty() && !SickleState.get(stack).invalid() && stack.getDamageValue() < MAX_DAMAGE;
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return canUse(stack) ? 1F : 0.05F; }
    @Override public Component getName(ItemStack stack) {
        return canUse(stack) ? super.getName(stack) : Component.translatable("item.harvestfestival.sickle.broken", super.getName(stack));
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return canUse(player.getItemInHand(hand)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    public static boolean performTask(ItemStack stack, boolean creative, SickleRules.Rule rule,
            IntUnaryOperator durabilityChange, Consumer<Float> exhaustion) {
        if (!canUse(stack) || rule == null) return false;
        stack.set(ToolContent.SICKLE_STATE.get(), SickleState.get(stack).advance());
        if (creative || !rule.consumeHunger()) return true;
        exhaustion.accept(rule.exhaustion());
        // Modern hurtAndBreak removes at >= max; original stops at max and retains the broken tool.
        stack.setDamageValue(Math.min(MAX_DAMAGE, stack.getDamageValue() + Math.max(0, durabilityChange.applyAsInt(1))));
        return true;
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (level instanceof ServerLevel server && owner instanceof net.minecraft.server.level.ServerPlayer player && state.is(CropContent.CROPS)) {
            performTask(stack, player.isCreative(), SickleRules.INSTANCE.rule(),
                    damage -> EnchantmentHelper.processDurabilityChange(server, stack, damage),
                    amount -> ToolEffort.consume(player, amount));
        }
        return true;
    }
}

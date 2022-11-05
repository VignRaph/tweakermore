package me.fallenbreath.tweakermore.mixins.tweaks.mod_tweaks.handRestoreRestriction;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.util.ModIds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Restriction(require = @Condition(ModIds.tweakeroo))
@Mixin(PlacementTweaks.class)
public abstract class PlacementTweaksMixin
{
	@ModifyExpressionValue(
			method = "onProcessRightClickPre",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;TWEAK_HAND_RESTOCK:Lfi/dy/masa/tweakeroo/config/FeatureToggle;",
							remap = false
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;getBooleanValue()Z",
					ordinal = 0,
					remap = false
			),
			remap = false
	)
	private static boolean applyHandRestoreRestriction(boolean booleanValue, /* parent method parameters -> */ PlayerEntity player, Hand hand)
	{
		Item currentItem = player.getStackInHand(hand).getItem();
		return booleanValue && TweakerMoreConfigs.HAND_RESTORE_RESTRICTION.isAllowed(currentItem);
	}

	@ModifyExpressionValue(
			method = "cacheStackInHand",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;TWEAK_HAND_RESTOCK:Lfi/dy/masa/tweakeroo/config/FeatureToggle;",
							remap = false
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;getBooleanValue()Z",
					ordinal = 0,
					remap = false
			),
			remap = false
	)
	private static boolean applyHandRestoreRestriction(boolean booleanValue, /* parent method parameters -> */ Hand hand)
	{
		boolean result = booleanValue;
		PlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null)
		{
			Item currentItem = player.getStackInHand(hand).getItem();
			result &= TweakerMoreConfigs.HAND_RESTORE_RESTRICTION.isAllowed(currentItem);
		}
		return result;
	}

	@ModifyExpressionValue(
			method = "tryRestockHand",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;TWEAK_HAND_RESTOCK:Lfi/dy/masa/tweakeroo/config/FeatureToggle;",
							remap = false
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lfi/dy/masa/tweakeroo/config/FeatureToggle;getBooleanValue()Z",
					ordinal = 0,
					remap = false
			),
			remap = false
	)
	private static boolean applyHandRestoreRestriction(boolean booleanValue, /* parent method parameters -> */ PlayerEntity player, Hand hand, ItemStack stackOriginal)
	{
		return booleanValue && TweakerMoreConfigs.HAND_RESTORE_RESTRICTION.isAllowed(stackOriginal.getItem());
	}
}

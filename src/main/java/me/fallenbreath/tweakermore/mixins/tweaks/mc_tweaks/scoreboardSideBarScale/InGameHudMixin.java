/*
 * This file is part of the TweakerMore project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023  Fallen_Breath and contributors
 *
 * TweakerMore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TweakerMore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TweakerMore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.fallenbreath.tweakermore.mixins.tweaks.mc_tweaks.scoreboardSideBarScale;

import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.util.render.RenderUtil;
import me.fallenbreath.tweakermore.util.render.context.RenderContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 12000
//$$ import net.minecraft.client.gui.DrawContext;
//#elseif MC >= 11600
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC >= 11600
//$$ import net.minecraft.scoreboard.ScoreboardObjective;
//#endif

@Mixin(InGameHud.class)
public abstract class InGameHudMixin
{
	@Shadow private int scaledWidth;

	@Nullable
	private RenderUtil.Scaler scaler = null;

	@ModifyVariable(
			method = "renderScoreboardSidebar",
			at = @At(
					value = "INVOKE",
					//#if MC >= 11600
					//$$ target = "Lnet/minecraft/client/option/GameOptions;getTextBackgroundColor(F)I",
					//#else
					target = "Lnet/minecraft/client/options/GameOptions;getTextBackgroundColor(F)I",
					//#endif
					ordinal = 0
			),
			ordinal = 3
	)
	private int tweakerMore_scoreboardSideBarScale_push(
			int centerY
			//#if MC >= 12000
			//$$ , DrawContext matrixStackOrDrawContext
			//#elseif MC >= 11600
			//$$ , MatrixStack matrixStackOrDrawContext
			//#endif
			//#if MC >= 11600
			//$$ , ScoreboardObjective objective
			//#endif
	)
	{
		this.scaler = null;
		if (TweakerMoreConfigs.SCOREBOARD_SIDE_BAR_SCALE.isModified())
		{
			this.scaler = RenderUtil.createScaler(this.scaledWidth, centerY, TweakerMoreConfigs.SCOREBOARD_SIDE_BAR_SCALE.getDoubleValue());
			this.scaler.apply(RenderContext.of(
					//#if MC >= 11600
					//$$ matrixStackOrDrawContext
					//#endif
			));
		}
		return centerY;
	}

	@Inject(method = "renderScoreboardSidebar", at = @At("RETURN"))
	private void tweakerMore_scoreboardSideBarScale_pop(CallbackInfo ci)
	{
		if (this.scaler != null)
		{
			this.scaler.restore();
		}
	}
}

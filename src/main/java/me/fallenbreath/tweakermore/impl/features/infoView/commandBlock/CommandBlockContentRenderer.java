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

package me.fallenbreath.tweakermore.impl.features.infoView.commandBlock;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.impl.features.infoView.AbstractBlockInfoViewer;
import me.fallenbreath.tweakermore.mixins.tweaks.features.infoView.commandBlock.CommandSuggestorAccessor;
import me.fallenbreath.tweakermore.util.render.context.RenderContext;
import me.fallenbreath.tweakermore.util.render.TextRenderer;
import me.fallenbreath.tweakermore.util.render.TextRenderingUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

//#if MC >= 11600
//$$ import me.fallenbreath.tweakermore.util.render.TextRenderingUtil;
//$$ import net.minecraft.text.OrderedText;
//#endif

public class CommandBlockContentRenderer extends AbstractBlockInfoViewer
{
	public CommandBlockContentRenderer()
	{
		super(
				TweakerMoreConfigs.INFO_VIEW_COMMAND_BLOCK,
				TweakerMoreConfigs.INFO_VIEW_COMMAND_BLOCK_RENDER_STRATEGY,
				TweakerMoreConfigs.INFO_VIEW_COMMAND_BLOCK_TARGET_STRATEGY
		);
	}

	@Override
	public boolean shouldRenderFor(World world, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity)
	{
		return blockState.getBlock() instanceof CommandBlock;
	}

	@Override
	public boolean requireBlockEntitySyncing()
	{
		return true;
	}

	@Override
	public void render(RenderContext context, World world, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity)
	{
		if (!(blockEntity instanceof CommandBlockBlockEntity))
		{
			return;
		}
		CommandBlockExecutor executor = ((CommandBlockBlockEntity) blockEntity).getCommandExecutor();
		String command = executor.getCommand();
		Text lastOutput = executor.getLastOutput();
		final int MAX_WIDTH = TweakerMoreConfigs.INFO_VIEW_COMMAND_BLOCK_MAX_WIDTH.getIntegerValue();

		// parse command for highlight logic
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null)
		{
			return;
		}
		StringReader stringReader = new StringReader(command);
		if (stringReader.canRead() && stringReader.peek() == '/')
		{
			stringReader.read();
		}
		ParseResults<CommandSource> parse = player.networkHandler.getCommandDispatcher().parse(
				stringReader, player.networkHandler.getCommandSource()
		);

		// trim command
		String trimmedCommand = TextRenderingUtil.trim(command, MAX_WIDTH);
		//#if MC >= 11600
		//$$ OrderedText
		//#else
		String
		//#endif
				displayText = CommandSuggestorAccessor.invokeHighlight(parse, trimmedCommand, 0);
		if (trimmedCommand.length() < command.length())
		{
			//#if MC >= 11600
			//$$ displayText = OrderedText.concat(displayText, TextRenderingUtil.string2orderedText(Formatting.DARK_GRAY + "..."));
			//#else
			displayText += Formatting.DARK_GRAY + "...";
			//#endif
		}

		// render
		TextRenderer textRenderer = TextRenderer.create().
				text(displayText).atCenter(blockPos).
				fontScale(0.025 * TweakerMoreConfigs.INFO_VIEW_COMMAND_BLOCK_TEXT_SCALE.getDoubleValue()).
				bgColor(0x3F000000).
				shadow().seeThrough();
		if (!lastOutput.getString().isEmpty())
		{
			//#if MC >= 11600
			//$$ OrderedText trimmedLastOutput = TextRenderingUtil.trim(
			//$$ 		lastOutput.asOrderedText(),
			//$$ 		MAX_WIDTH,
			//$$ 		trimmedText -> OrderedText.concat(trimmedText, TextRenderingUtil.string2orderedText(Formatting.DARK_GRAY + "..."))
			//$$ );
			//#else
			String trimmedLastOutput = TextRenderingUtil.trim(
					lastOutput.asFormattedString(),
					MAX_WIDTH,
					trimmedText -> trimmedText + Formatting.DARK_GRAY + "..."
			);
			//#endif
			textRenderer.addLine(trimmedLastOutput);
		}

		textRenderer.render();
	}
}

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

package me.fallenbreath.tweakermore.impl.features.infoView.villager;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.impl.features.infoView.AbstractEntityInfoViewer;
import me.fallenbreath.tweakermore.impl.mod_tweaks.serverDataSyncer.ServerDataSyncer;
import me.fallenbreath.tweakermore.util.Messenger;
import me.fallenbreath.tweakermore.util.render.InWorldPositionTransformer;
import me.fallenbreath.tweakermore.util.render.RenderUtil;
import me.fallenbreath.tweakermore.util.render.TextRenderer;
import me.fallenbreath.tweakermore.util.render.context.RenderContext;
import me.fallenbreath.tweakermore.util.render.context.RenderGlobals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.BaseText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//#if MC >= 12000
//$$ import net.minecraft.client.render.model.json.ModelTransformationMode;
//#endif

//#if MC >= 11700
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#endif

//#if MC >= 11600
//$$ import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC < 11500
//$$ import net.minecraft.client.texture.SpriteAtlasTexture;
//#endif

public class VillagerTradeRenderer extends AbstractEntityInfoViewer {
    private static final double FONT_SCALE = TextRenderer.DEFAULT_FONT_SCALE;
    private static final double MARGIN = 5;  // margin between icon and text
    private static final int ICON_SIZE = 18;  // the status effect icon texture is a 18x18 square
    private static final int ICON_RENDERED_SIZE = RenderUtil.TEXT_HEIGHT;

    public VillagerTradeRenderer () {
        super(
                TweakerMoreConfigs.INFO_VIEW_VILLAGER,
                TweakerMoreConfigs.INFO_VIEW_VILLAGER_RENDER_STRATEGY,
                TweakerMoreConfigs.INFO_VIEW_VILLAGER_TARGET_STRATEGY
        );
    }

    @Override
    public boolean shouldRenderFor (World world, Position position, Entity entity) {
        return entity instanceof AbstractTraderEntity;
    }

    @Override
    public boolean requireEntitySyncing () {
        return true;
    }

    @Override
    public void render (RenderContext context, World world, Position position, Entity entity) {
        if (!(entity instanceof AbstractTraderEntity)) {
            return;
        }

        if (TweakerMoreConfigs.INFO_VIEW_VILLAGER_UPDATE.getBooleanValue() || !entity.getScoreboardTags().contains("TweakerMoreServerSynced")) {
            ServerDataSyncer.getInstance().syncEntity(entity);
        }

        AbstractTraderEntity traderEntity = (AbstractTraderEntity) entity;
        TraderOfferList offers = traderEntity.getOffers();
        Vec3d pos = new Vec3d(position.getX(), position.getY() + 2.25, position.getZ());

        List<Pair<ItemStack, List<BaseText>>> tradesToShow = Lists.newArrayList();

        if (offers == null) {
            return;
        }

        //Filter trades to show only relevant ones
        int nbTradeToShow = 0;
        for (TradeOffer offer : offers) {
            ItemStack priceItem = offer.getOriginalFirstBuyItem();
            ItemStack adjustedPriceItem = offer.getAdjustedFirstBuyItem();
            ItemStack soldItem = offer.getSellItem();
            Item item = soldItem.getItem();
            List<BaseText> texts = Lists.newArrayList();

            if (TweakerMoreConfigs.INFO_VIEW_VILLAGER_BOOKS.getBooleanValue() &&
                    item instanceof EnchantedBookItem) {
                ListTag enchants = EnchantedBookItem.getEnchantmentTag(soldItem);

                for (int k = 0; k < enchants.size(); ++k) {
                    CompoundTag compoundTag = enchants.getCompound(k);
                    Optional<Enchantment> optionalEnchantment = Registry.ENCHANTMENT.getOrEmpty(Identifier.tryParse(compoundTag.getString("id")));
                    if (!optionalEnchantment.isPresent()) {
                        continue;
                    }
                    Enchantment enchant = optionalEnchantment.get();
                    if (compoundTag.getInt("lvl") < enchant.getMaximumLevel()) {
                        continue;
                    }
                    //Calculate technical minimum price for the trade
                    int minPrice = 2 + compoundTag.getInt("lvl") * 3;
                    if (enchant.isTreasure())
                        minPrice *= 2;

                    if (TweakerMoreConfigs.INFO_VIEW_VILLAGER_BOOKS_BEST.getBooleanValue()
                            && priceItem.getCount() > minPrice){
                        continue;
                    }
                    texts.addAll(Objects.requireNonNull(getEnchantmentText(soldItem)));
                }


            } else if (TweakerMoreConfigs.INFO_VIEW_VILLAGER_ENCHANTED_ITEMS.getBooleanValue() &&
                    TweakerMoreConfigs.INFO_VIEW_VILLAGER_ENCHANTED_ITEMS_RESTRICTION.isAllowed(item)) {
                ListTag enchants = soldItem.getEnchantments();

                for (int k = 0; k < enchants.size(); ++k) {
                    CompoundTag compoundTag = enchants.getCompound(k);
                    Registry.ENCHANTMENT.getOrEmpty(Identifier.tryParse(compoundTag.getString("id"))).ifPresent((enchant) -> {
                        if (((!TweakerMoreConfigs.INFO_VIEW_VILLAGER_ENCHANTED_ITEMS_MAX.getBooleanValue())
                                || compoundTag.getInt("lvl") >= enchant.getMaximumLevel()) && texts.isEmpty()) {
                            texts.addAll(Objects.requireNonNull(getEnchantmentText(soldItem)));
                        }
                    });
                }
            } else if (TweakerMoreConfigs.INFO_VIEW_VILLAGER_ITEMS.getBooleanValue() &&
                    TweakerMoreConfigs.INFO_VIEW_VILLAGER_ITEMS_RESTRICTION.isAllowed(item)) {
                BaseText baseText = (BaseText) item.getName(soldItem);
                Text text = baseText.formatted(Formatting.WHITE);
                texts.add((BaseText) text);
            }

            if (!texts.isEmpty()) {
                tradesToShow.add(Pair.of(soldItem, texts));
                nbTradeToShow++;
            }
        }

        if (nbTradeToShow > 0) {
            double DeltaY = 0;
            for (Pair<ItemStack, List<BaseText>> pair : tradesToShow) {
                double width = this.calculateRowWidth(pair.getSecond());
                double lines = pair.getSecond().size();
                double deltaX = -width / 2;  // unit: pixel (in scale=FONT_SCALE context)
                double kDeltaY = DeltaY - lines / 2.0 + 0.3;  // unit: ratio

                if (pair.getFirst().getItem() != Items.SHIELD) {
                    this.renderItemIcon(pos, pair.getFirst(), deltaX, kDeltaY);
                }
                for (BaseText text : pair.getSecond()) {
                    this.renderItemText(pos, text, deltaX, DeltaY);
                    DeltaY -= 1;
                }
                DeltaY -= 0.5;
            }
        }

    }

    private double calculateRowWidth (List<BaseText> texts) {
        double textWidth = texts.stream().mapToDouble(text -> RenderUtil.getRenderWidth(getItemText(text))).max().orElse(0);
        return ICON_RENDERED_SIZE + MARGIN + textWidth;
    }

    @SuppressWarnings ("AccessStaticViaInstance")
    private void renderItemIcon (Vec3d pos, ItemStack itemStack, double deltaX, double kDeltaY) {
        MinecraftClient mc = MinecraftClient.getInstance();

        Sprite sprite = mc.getItemRenderer().getModels().getModel(itemStack).getSprite();

        RenderContext renderContext = RenderContext.of(
                //#if MC >= 11600
                //$$ new MatrixStack()
                //#endif
        );

        InWorldPositionTransformer positionTransformer = new InWorldPositionTransformer(pos);
        positionTransformer.apply(renderContext);
        {
            RenderGlobals.disableDepthTest();
            RenderGlobals.enableBlend();  // maybe useful
            //#if MC < 11700
            RenderGlobals.disableLighting();
            //#endif

            // ref: net.minecraft.client.gui.hud.InGameHud.renderStatusEffectOverlay

            renderContext.scale(-FONT_SCALE, -FONT_SCALE, FONT_SCALE);
            renderContext.translate(deltaX, 0, 0);

            // scale 2: make the rendered texture height == expected height (line height)
            double k = 1.0 * ICON_RENDERED_SIZE / ICON_SIZE;
            renderContext.scale(k, k, k);
            renderContext.translate(0, ICON_SIZE * (-0.5 + kDeltaY), 0);

            //#if MC >= 11903
            //$$ RenderSystem.setShaderTexture(0, sprite.getAtlasId());
            //#elseif MC >= 11700
            //$$ RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
            //#elseif MC >= 11500
            mc.getTextureManager().bindTexture(sprite.getAtlas().getId());
            //#else
            //$$ mc.getTextureManager().bindTexture(SpriteAtlasTexture.STATUS_EFFECT_ATLAS_TEX);
            //#endif
            RenderGlobals.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            //#if MC >= 12000
            //$$ renderContext.getGuiDrawer().drawSprite(
            //#elseif MC >= 11600
            //$$ renderContext.getGuiDrawer().drawSprite(
            //$$ 		renderContext.getMatrixStack(),
            //#else
            renderContext.getGuiDrawer().blit(
                    //#endif
                    0, 0, 0, ICON_SIZE, ICON_SIZE, sprite
            );

            RenderGlobals.enableDepthTest();
        }
        positionTransformer.restore();
    }

    private void renderItemText (Vec3d pos, BaseText description, double deltaX, double kDeltaY) {
        TextRenderer textRenderer = TextRenderer.create().
                at(pos).
                text(description).fontScale(FONT_SCALE).
                align(TextRenderer.HorizontalAlignment.LEFT).
                seeThrough().shadow();
        textRenderer.shift(deltaX + ICON_RENDERED_SIZE + MARGIN, kDeltaY * textRenderer.getLineHeight());
        textRenderer.render();
    }

    private static String getItemText (BaseText text) {
        return text.getString();
    }

    private static List<BaseText> getEnchantmentText (ItemStack itemStack) {
        List<Text> enchantmentTexts = Lists.newArrayList();
        ListTag enchantmentTag = itemStack.getItem() instanceof EnchantedBookItem ? EnchantedBookItem.getEnchantmentTag(itemStack) : itemStack.getEnchantments();
        ItemStack.appendEnchantments(enchantmentTexts, enchantmentTag);

        int amount = enchantmentTexts.size();
        if (amount == 0) {
            return null;
        }
        List<BaseText> texts = Lists.newArrayList();

        int idx;
        for (idx = 0; idx < amount; idx++) {
            BaseText enchantText = Messenger.s(" ");
            Text enchant = enchantmentTexts.get(idx);
            if (!Objects.equals(Objects.requireNonNull(enchant.getStyle().getColor()).getName(), Formatting.RED.getName()))
                enchantText.append(Messenger.s(enchant.getString(), Formatting.WHITE));
            else
                enchantText.append(enchant);
            if (idx < amount - 1) {
                enchantText.append(Messenger.s(", ", Formatting.GRAY));
            }
            texts.add(enchantText);
        }

        return texts;
    }
}

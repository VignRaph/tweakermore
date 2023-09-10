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
import fi.dy.masa.malilib.util.StringUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import me.fallenbreath.tweakermore.impl.features.infoView.AbstractEntityInfoViewer;
import me.fallenbreath.tweakermore.mixins.tweaks.features.infoView.villager.VillagerEntityAccessor;
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
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    private static final Item[] ITEMS_WITH_ENCHANTS = new Item[]{
            Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET,
            Items.FISHING_ROD, Items.BOW, Items.CROSSBOW
    };

    private static final Item[] ITEMS_WO_ENCHANTS = new Item[]{
            // Mason
            Items.QUARTZ_BLOCK, Items.QUARTZ_PILLAR, Items.POLISHED_ANDESITE, Items.POLISHED_DIORITE, Items.POLISHED_GRANITE, Items.TERRACOTTA,
            Items.BLACK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA, Items.WHITE_TERRACOTTA, Items.BROWN_TERRACOTTA,
            Items.RED_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA, Items.BLUE_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.PINK_TERRACOTTA,
            // ToolSmith
            Items.DIAMOND_HOE,
            // Weapon smith
            // Armorer
            Items.SHIELD,
            // Librarian
            Items.LANTERN, Items.BOOKSHELF, Items.GLASS, Items.CLOCK, Items.COMPASS,
            // Leather worker
            // Shepherd
            Items.BLACK_WOOL, Items.GRAY_WOOL, Items.LIGHT_GRAY_WOOL, Items.WHITE_WOOL, Items.BROWN_WOOL,
            Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.GREEN_WOOL, Items.CYAN_WOOL, Items.LIGHT_BLUE_WOOL, Items.BLUE_WOOL, Items.MAGENTA_WOOL, Items.PURPLE_WOOL, Items.PINK_WOOL,

            Items.BLACK_CARPET, Items.GRAY_CARPET, Items.LIGHT_GRAY_CARPET, Items.WHITE_CARPET, Items.BROWN_CARPET,
            Items.RED_CARPET, Items.ORANGE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET, Items.GREEN_CARPET, Items.CYAN_CARPET, Items.LIGHT_BLUE_CARPET, Items.BLUE_CARPET, Items.MAGENTA_CARPET, Items.PURPLE_CARPET, Items.PINK_CARPET,

            Items.BLACK_BED, Items.GRAY_BED, Items.LIGHT_GRAY_BED, Items.WHITE_BED, Items.BROWN_BED,
            Items.RED_BED, Items.ORANGE_BED, Items.YELLOW_BED, Items.LIME_BED, Items.GREEN_BED, Items.CYAN_BED, Items.LIGHT_BLUE_BED, Items.BLUE_BED, Items.MAGENTA_BED, Items.PURPLE_BED, Items.PINK_BED,

            Items.BLACK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER, Items.WHITE_BANNER, Items.BROWN_BANNER,
            Items.RED_BANNER, Items.ORANGE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER, Items.GREEN_BANNER, Items.CYAN_BANNER, Items.LIGHT_BLUE_BANNER, Items.BLUE_BANNER, Items.MAGENTA_BANNER, Items.PURPLE_BANNER, Items.PINK_BANNER,

            // Farmer
            Items.APPLE, Items.PUMPKIN_PIE, Items.BREAD,
            // Cleric
            // Butcher
            Items.RABBIT_STEW, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN,
            // Fisherman
            Items.CAMPFIRE,
            // Cartographer
            Items.CREEPER_BANNER_PATTERN, Items.FLOWER_BANNER_PATTERN, Items.GLOBE_BANNER_PATTERN, Items.MOJANG_BANNER_PATTERN, Items.SKULL_BANNER_PATTERN,
            // Fletcher
            Items.TIPPED_ARROW, Items.ARROW
            // Wandering Trader
    };

    public VillagerTradeRenderer () {
        super(
                TweakerMoreConfigs.INFO_VIEW_BEACON,
                TweakerMoreConfigs.INFO_VIEW_BEACON_RENDER_STRATEGY,
                TweakerMoreConfigs.INFO_VIEW_BEACON_TARGET_STRATEGY
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
        VillagerEntityAccessor accessor = (VillagerEntityAccessor) entity;
        TraderOfferList offers = accessor.getOffers();
        Vec3d pos = new Vec3d(position.getX() + 1.5, position.getY() + 1.5, position.getZ() + 1.5);

        List<Pair<ItemStack, List<BaseText>>> tradesToShow = Lists.newArrayList();
        //Filter trades to show only relevant ones
        int nbTradeToShow = 0;
        for (int j = 0; j < offers.size(); j++) {
            TradeOffer offer = offers.get(j);
            ItemStack soldItem = offer.getSellItem();
            Item item = soldItem.getItem();
            List<BaseText> texts = Lists.newArrayList();

            if (item instanceof EnchantedBookItem) {
                ListTag enchants = EnchantedBookItem.getEnchantmentTag(soldItem);

                for (int k = 0; k < enchants.size(); ++k) {
                    CompoundTag compoundTag = enchants.getCompound(k);
                    Registry.ENCHANTMENT.getOrEmpty(Identifier.tryParse(compoundTag.getString("id"))).ifPresent((enchant) -> {
                        if (compoundTag.getInt("lvl") >= enchant.getMaximumLevel()) {
                            texts.add(getEnchantmentText(soldItem));
                        }
                    });
                }

            } else if (Arrays.stream(ITEMS_WITH_ENCHANTS).anyMatch((t) -> t == item)) {
                ListTag enchants = soldItem.getEnchantments();

                for (int k = 0; k < enchants.size(); ++k) {
                    CompoundTag compoundTag = enchants.getCompound(k);
                    Registry.ENCHANTMENT.getOrEmpty(Identifier.tryParse(compoundTag.getString("id"))).ifPresent((enchant) -> {
                        if (compoundTag.getInt("lvl") >= enchant.getMaximumLevel()) {
                            texts.add(getEnchantmentText(soldItem));
                        }
                    });
                }
            } else if (Arrays.stream(ITEMS_WO_ENCHANTS).anyMatch((t) -> t == item)) {
                texts.add((BaseText) item.getName());
            }

            if (!texts.isEmpty()) {
                tradesToShow.add(Pair.of(soldItem, texts));
                nbTradeToShow++;
            }
        }

        if (nbTradeToShow > 0) {
            double maxWidth = tradesToShow.stream().
                    mapToDouble(pair -> this.calculateRowWidth(pair.getSecond())).
                    max().orElse(0);
            int linesCount = tradesToShow.stream().mapToInt(pair -> pair.getSecond().size()).sum();
            double DeltaY = 0;
            for (int i = 0; i < tradesToShow.size(); i++) {
                Pair<ItemStack, List<BaseText>> pair = tradesToShow.get(i);

                double width = this.calculateRowWidth(pair.getSecond());
                double lines = pair.getSecond().size();
                double deltaX = -width / 2;  // unit: pixel (in scale=FONT_SCALE context)
                double kDeltaY = i - (linesCount - lines) / 2.0;  // unit: ratio

                this.renderItemIcon(pos, pair.getFirst(), deltaX, kDeltaY);
                for (BaseText text : pair.getSecond()){
                    this.renderItemText(pos, text, deltaX, DeltaY);
                    DeltaY += 1;
                }
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
        Sprite sprite = mc.getItemRenderer().getModels().getSprite(itemStack);

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

    private static String getItemText (BaseText statusEffect) {
        return statusEffect.getString();
    }

    private static BaseText getEnchantmentText (ItemStack itemStack) {
        List<Text> enchantmentTexts = Lists.newArrayList();
        ListTag enchantmentTag = itemStack.getItem() instanceof EnchantedBookItem ? EnchantedBookItem.getEnchantmentTag(itemStack) : itemStack.getEnchantments();
        ItemStack.appendEnchantments(enchantmentTexts, enchantmentTag);

        int amount = enchantmentTexts.size();
        if (amount == 0) {
            return null;
        }
        BaseText extraText = Messenger.s(" ", Formatting.WHITE);

        int maxLength = TweakerMoreConfigs.SHULKER_TOOLTIP_HINT_LENGTH_LIMIT.getIntegerValue();
        net.minecraft.client.font.TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int idx;
        for (idx = 0; idx < amount; idx++) {
            extraText.append(enchantmentTexts.get(idx));
            if (idx < amount - 1) {
                extraText.append(Messenger.s(", ", Formatting.GRAY));
            }
        }

        return extraText;
    }
}

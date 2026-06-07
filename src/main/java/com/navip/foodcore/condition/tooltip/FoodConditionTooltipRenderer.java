package com.navip.foodcore.condition.tooltip;

import com.navip.foodcore.condition.FoodConditionState;
import com.navip.foodcore.condition.effect.FoodConditionConsumptionEffects;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.navip.foodcore.data.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 食物状态 Tooltip 渲染器。
 *
 * 负责把内部的食物状态值转换为玩家可读的提示信息，
 * 包括状态文本、进度轴显示，以及与 Cold Sweat 联动时的额外温度提示。
 * 这里属于纯表现层，不直接参与食物状态计算与写回。
 */
public final class FoodConditionTooltipRenderer {
    private static final int AXIS_WIDTH = 21;
    private static final String BAR_SEGMENT = "|";
    private static final int FRESH_BLUE_DARK = 0x3F5FBF;
    private static final int FRESH_BLUE_BRIGHT = 0x8CCBFF;
    private static final int FRESH_RED_BRIGHT = 0xFF8A8A;
    private static final int FRESH_RED_DARK = 0xC44747;

    private FoodConditionTooltipRenderer() {
    }

    public static void appendTooltip(ItemTooltipEvent event) {
        if (!FoodCoreCommonConfig.isEnableFoodConditionTooltip()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        FoodConditionGroupConfig groupConfig = FoodConditionGroupConfigManager.getGroupForItem(stack.getItem());
        if (groupConfig == null) return;

        FoodConditionState state = stack.get(ModDataComponents.FOOD_STATE.get());
        if (state == null) return;

        float value = state.value();
        if (value < -0.5f || state.isFrozen()) {
            event.getToolTip().add(Component.translatable("tooltip.foodcore.freezing").withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(createFreezingAxis(value));
        } else if (state.isSpoiled()) {
            event.getToolTip().add(Component.translatable("tooltip.foodcore.spoiled").withStyle(ChatFormatting.DARK_RED));
            event.getToolTip().add(createSingleSideAxis(value, 0.5f, 1.5f, ChatFormatting.RED));
        } else {
            if (value > 0.5f) {
                event.getToolTip().add(Component.translatable("tooltip.foodcore.spoiling").withStyle(ChatFormatting.RED));
                event.getToolTip().add(createSingleSideAxis(value, 0.5f, 1.5f, ChatFormatting.RED));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.foodcore.fresh").withStyle(ChatFormatting.GREEN));
                event.getToolTip().add(createFreshAxis(value));
            }
        }

        FoodConditionConsumptionEffects.addColdSweatTooltip(event.getToolTip(), stack, groupConfig, value);
    }

    private static Component createFreshAxis(float value) {
        int sideSegments = AXIS_WIDTH / 2;
        int negativeFill = value < 0.0f
                ? getFilledSegments((-value) / 0.5f, sideSegments)
                : 0;
        int positiveFill = value > 0.0f
                ? getFilledSegments(value / 0.5f, sideSegments)
                : 0;
        MutableComponent axis = Component.empty();

        for (int i = 0; i < sideSegments; i++) {
            boolean filled = i >= sideSegments - negativeFill;
            int color = interpolateColor(FRESH_BLUE_DARK, FRESH_BLUE_BRIGHT, (float) i / Math.max(1, sideSegments - 1));
            axis.append(createSegment(filled ? color : 0x4A4A4A));
        }

        axis.append(createSegment(0x7A7A7A));

        for (int i = 0; i < sideSegments; i++) {
            boolean filled = i < positiveFill;
            int color = interpolateColor(FRESH_RED_BRIGHT, FRESH_RED_DARK, (float) i / Math.max(1, sideSegments - 1));
            axis.append(createSegment(filled ? color : 0x4A4A4A));
        }

        return axis;
    }

    private static Component createSingleSideAxis(float value, float min, float max, ChatFormatting color) {
        int filledSegments = getFilledSegments((value - min) / (max - min), AXIS_WIDTH);
        MutableComponent axis = Component.empty();

        for (int i = 0; i < AXIS_WIDTH; i++) {
            boolean filled = i < filledSegments;
            axis.append(createSegment(filled ? color : ChatFormatting.DARK_GRAY));
        }

        return axis;
    }

    private static Component createFreezingAxis(float value) {
        float progress = ((-0.5f) - value) / 1.0f;
        int filledSegments = getFilledSegments(progress, AXIS_WIDTH);
        MutableComponent axis = Component.empty();

        for (int i = 0; i < AXIS_WIDTH; i++) {
            boolean filled = i >= AXIS_WIDTH - filledSegments;
            axis.append(createSegment(filled ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
        }

        return axis;
    }

    private static Component createSegment(ChatFormatting color) {
        return Component.literal(BAR_SEGMENT).withStyle(color);
    }

    private static Component createSegment(int rgb) {
        return Component.literal(BAR_SEGMENT).withStyle(style -> style.withColor(rgb));
    }

    private static int interpolateColor(int startRgb, int endRgb, float progress) {
        float clamped = clamp(progress, 0.0f, 1.0f);
        int startR = (startRgb >> 16) & 0xFF;
        int startG = (startRgb >> 8) & 0xFF;
        int startB = startRgb & 0xFF;
        int endR = (endRgb >> 16) & 0xFF;
        int endG = (endRgb >> 8) & 0xFF;
        int endB = endRgb & 0xFF;

        int r = Math.round(startR + (endR - startR) * clamped);
        int g = Math.round(startG + (endG - startG) * clamped);
        int b = Math.round(startB + (endB - startB) * clamped);
        return (r << 16) | (g << 8) | b;
    }

    private static int getFilledSegments(float normalizedValue, int totalSegments) {
        float clamped = clamp(normalizedValue, 0.0f, 1.0f);
        int filled = (int) Math.floor(clamped * totalSegments + 1.0e-6f);
        if (clamped > 0.0f && filled == 0) {
            return 1;
        }
        return Math.min(totalSegments, filled);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

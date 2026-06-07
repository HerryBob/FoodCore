package com.navip.foodcore.condition.effect;

import com.momosoftworks.coldsweat.api.temperature.modifier.FoodTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Mode;
import com.momosoftworks.coldsweat.api.util.placement.Order;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import com.navip.foodcore.condition.FoodConditionState;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.util.TemperatureHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 食物食用效果结算器。
 *
 * 当玩家真正食用一个带状态的食物后，
 * 该类会根据其冻结程度和配置项决定是否向 Cold Sweat 写入体温影响，
 * 同时也负责为 Tooltip 提供对应的效果预览文本。
 */
public final class FoodConditionConsumptionEffects {
    private static final float EAT_EFFECT_START = 0.5f;
    private static final Style COLD_TOOLTIP_STYLE = Style.EMPTY.withColor(3767039);

    private FoodConditionConsumptionEffects() {
    }

    public static void applyFrozenConsumptionEffect(Player player, ItemStack consumedStack, FoodConditionGroupConfig groupConfig, float value) {
        double coreTempDelta = getFrozenCoreTempDelta(consumedStack, groupConfig, value);
        if (coreTempDelta == 0.0D) {
            return;
        }

        int durationTicks = groupConfig.getColdedFoodDurationTicks(consumedStack.getItem());
        if (durationTicks > 0) {
            FoodTempModifier foodModifier = new FoodTempModifier(coreTempDelta);
            foodModifier.getNBT().putInt("duration", durationTicks);
            foodModifier.expires(durationTicks).tickRate(durationTicks);
            Placement placement = Placement.LAST
                    .limitDuplicates(Matcher.EQUALS, 1)
                    .orElse(Placement.of(Mode.REPLACE, Order.FIRST, foodModifier::equals));
            Temperature.addModifier(player, foodModifier, Temperature.Trait.BASE, placement);
            return;
        }

        Temperature.add(player, Temperature.Trait.CORE, coreTempDelta);
    }

    public static void addColdSweatTooltip(List<Component> tooltip, ItemStack stack, FoodConditionGroupConfig groupConfig, float value) {
        double coreTempDelta = getFrozenCoreTempDelta(stack, groupConfig, value);
        if (coreTempDelta == 0.0D) {
            return;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.cold_sweat.section.consumed").withStyle(ChatFormatting.GRAY));

        String tempString = formatTemperature(coreTempDelta);
        MutableComponent effectLine = Component.translatable(
                "tooltip.cold_sweat.temperature_effect",
                tempString,
                Temperature.Trait.CORE.getFormattedName()
        ).withStyle(COLD_TOOLTIP_STYLE);

        int durationTicks = groupConfig.getColdedFoodDurationTicks(stack.getItem());
        if (durationTicks > 0) {
            effectLine.append(" (" + StringUtil.formatTickDuration(durationTicks, 20.0f) + ")");
        }

        tooltip.add(effectLine);
    }

    private static double getFrozenCoreTempDelta(ItemStack stack, FoodConditionGroupConfig groupConfig, float value) {
        if (value >= -EAT_EFFECT_START) {
            return 0.0D;
        }

        float configuredTemp = groupConfig.getColdedFoodTemperature(stack.getItem());
        if (configuredTemp <= 0.0f) {
            return 0.0D;
        }

        float progress = getFrozenEatProgress(value);
        if (progress <= 0.0f) {
            return 0.0D;
        }

        return -(configuredTemp * progress);
    }

    private static float getFrozenEatProgress(float value) {
        return clamp(((-value) - EAT_EFFECT_START) / ((-FoodConditionState.MIN_VALUE) - EAT_EFFECT_START), 0.0f, 1.0f);
    }

    private static String formatTemperature(double value) {
        double rounded = Math.round(value * 10.0D) / 10.0D;
        String text = Math.abs(rounded - Math.rint(rounded)) < 1.0e-6
                ? Integer.toString((int) Math.rint(rounded))
                : String.format(java.util.Locale.ROOT, "%.1f", rounded);
        return rounded > 0.0D ? "+" + text : text;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

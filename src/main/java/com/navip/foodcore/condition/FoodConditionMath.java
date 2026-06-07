package com.navip.foodcore.condition;

import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.data.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * 变质/冷冻的数学计算工具类
 * 方便后续添加新的数学逻辑
 */
public class FoodConditionMath {
    private static final float THAW_TEMP_RANGE = 20.0f;
    private static final float CONDITION_DISTANCE_FROM_FRESH = FoodConditionState.MAX_VALUE;

    /**
     * 计算变质系数（抛物线逻辑）
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 系数 [0.0, 1.0]
     */
    public static double getSpoilageCoefficient(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableSpoilage()) return 0.0;
        if (temperature <= config.getSpoilageMinTemp() || temperature >= config.getSpoilageMaxTemp()) return 0.0;
        
        double mid = (config.getSpoilageMinTemp() + config.getSpoilageMaxTemp()) / 2.0;
        double range = (config.getSpoilageMaxTemp() - config.getSpoilageMinTemp()) / 2.0;
        
        if (range <= 0) return 0.0;
        return 1.0 - Math.abs(temperature - mid) / range;
    }

    /**
     * 计算冷冻系数（线性逻辑）
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 系数 [0.0, 1.0]
     */
    public static double getFreezingCoefficient(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableFreezing()) return 0.0;
        if (temperature >= config.getFreezeStartTemp()) return 0.0;
        if (temperature <= config.getFreezeMinTemp()) return 1.0;
        
        return (config.getFreezeStartTemp() - temperature) / (config.getFreezeStartTemp() - config.getFreezeMinTemp());
    }

    /**
     * 计算每秒变质变化量
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 每秒的变化量
     */
    public static double getSpoilageDeltaPerSecond(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableSpoilage()) return 0.0;
        if (config.getSpoilageTimeSeconds() <= 0) return 0.0;
        double coeff = getSpoilageCoefficient(config, temperature);
        return coeff / config.getSpoilageTimeSeconds();
    }

    /**
     * 计算每秒冷冻变化量
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 每秒的变化量（负数表示冷冻）
     */
    public static double getFreezingDeltaPerSecond(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableFreezing()) return 0.0;
        if (config.getFreezeTimeSeconds() <= 0) return 0.0;
        double coeff = getFreezingCoefficient(config, temperature);
        return -coeff / config.getFreezeTimeSeconds();
    }

    /**
     * 计算解冻系数（线性逻辑）
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 系数 [0.0, 1.0]
     */
    public static double getThawingCoefficient(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableFreezing()) return 0.0;
        if (temperature <= config.getFreezeStartTemp()) return 0.0;

        double thawMaxTemp = config.getFreezeStartTemp() + THAW_TEMP_RANGE;
        if (temperature >= thawMaxTemp) return 1.0;
        return (temperature - config.getFreezeStartTemp()) / THAW_TEMP_RANGE;
    }

    /**
     * 计算每秒解冻变化量
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @return 每秒的变化量（正数表示解冻）
     */
    public static double getThawingDeltaPerSecond(FoodConditionGroupConfig config, double temperature) {
        if (!config.isEnableFreezing()) return 0.0;
        if (config.getFreezeTimeSeconds() <= 0) return 0.0;
        double coeff = getThawingCoefficient(config, temperature);
        return coeff / config.getFreezeTimeSeconds();
    }

    /**
     * 计算状态变化量
     *
     * @param config 食物组配置
     * @param temperature 环境温度
     * @param currentValue 当前状态值
     * @param deltaSeconds 经过的秒数
     * @return 变化量（正数表示变质，负数表示冷冻
     */
    public static float calculateDelta(FoodConditionGroupConfig config, float temperature, float currentValue, float deltaSeconds) {
        return calculateDelta(createUpdateProfileFromSeconds(config, temperature, deltaSeconds), currentValue);
    }

    public static GroupUpdateProfile createUpdateProfile(FoodConditionGroupConfig config, float temperature, float deltaTicks) {
        float deltaSeconds = deltaTicks / 20.0f;
        return createUpdateProfileFromSeconds(config, temperature, deltaSeconds);
    }

    private static GroupUpdateProfile createUpdateProfileFromSeconds(FoodConditionGroupConfig config, float temperature, float deltaSeconds) {
        return new GroupUpdateProfile(
                config,
                temperature,
                (float) getSpoilageDeltaPerSecond(config, temperature) * deltaSeconds * CONDITION_DISTANCE_FROM_FRESH,
                (float) getFreezingDeltaPerSecond(config, temperature) * deltaSeconds * CONDITION_DISTANCE_FROM_FRESH,
                (float) getThawingDeltaPerSecond(config, temperature) * deltaSeconds * CONDITION_DISTANCE_FROM_FRESH
        );
    }

    public static float calculateDelta(GroupUpdateProfile profile, float currentValue) {
        return calculateDelta(profile, getStateBand(currentValue));
    }

    public static float calculateDelta(GroupUpdateProfile profile, StateBand stateBand) {
        FoodConditionGroupConfig config = profile.config();
        float temperature = profile.temperature();
        float delta = 0.0f;

        if (stateBand == StateBand.FROZEN) {
            // 冻结区优先处理冷冻/解冻，先回到新鲜区再进入正常变质流程。
            if (config.isEnableFreezing() && temperature < config.getFreezeStartTemp()) {
                delta = profile.freezingDelta();
            } else if (config.isEnableFreezing() && temperature > config.getFreezeStartTemp()) {
                delta = profile.thawingDelta();
            }
        } else if (stateBand == StateBand.SPOILING) {
            // 超过 0.5 才视为真正进入腐败区；新鲜区内的轻微正值仍可被低温拉回。
            if (config.isEnableSpoilage() && temperature > config.getSpoilageMinTemp()) {
                delta = profile.spoilageDelta();
            }
        } else {
            // 还没变质，可以正常冷冻/变质
            if (config.isEnableSpoilage() && temperature > config.getSpoilageMinTemp()) {
                delta = profile.spoilageDelta();
            } else if (config.isEnableFreezing() && temperature < config.getFreezeStartTemp()) {
                delta = profile.freezingDelta();
            }
        }

        return delta;
    }

    public static StateBand getStateBand(float currentValue) {
        if (currentValue < -0.5f) {
            return StateBand.FROZEN;
        }
        if (currentValue > 0.5f) {
            return StateBand.SPOILING;
        }
        return StateBand.FRESH;
    }

    /**
     * 合并两个物品的状态（加权平均 + 偏向变质 + 冷冻降温）
     *
     * @param stack1 第一个物品
     * @param stack2 第二个物品
     * @return 合并后的状态值
     */
    public static float mergeFoodConditionStates(ItemStack stack1, ItemStack stack2) {
        return mergeFoodConditionStates(stack1, stack2, stack2.getCount());
    }

    /**
     * 按实际搬运数量合并两个物品的状态。
     *
     * @param targetStack 目标堆
     * @param sourceStack 来源堆
     * @param movedCount 实际搬运数量
     * @return 合并后的状态值
     */
    public static float mergeFoodConditionStates(ItemStack targetStack, ItemStack sourceStack, int movedCount) {
        if (movedCount <= 0) {
            return targetStack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial()).value();
        }

        FoodConditionState state1 = targetStack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial());
        FoodConditionState state2 = sourceStack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial());
        int count1 = targetStack.getCount();
        int count2 = Math.min(movedCount, sourceStack.getCount());

        return mergeFoodValues(state1.value(), count1, state2.value(), count2);
    }

    private static float mergeFoodValues(float value1, int count1, float value2, int count2) {
        int totalCount = count1 + count2;
        if (totalCount <= 0) {
            return 0.0f;
        }

        float positiveSum = 0.0f;
        int positiveCount = 0;
        float negativeSum = 0.0f;
        int negativeCount = 0;

        if (value1 > 0.0f) {
            positiveSum += value1 * count1;
            positiveCount += count1;
        } else if (value1 < 0.0f) {
            negativeSum += value1 * count1;
            negativeCount += count1;
        }

        if (value2 > 0.0f) {
            positiveSum += value2 * count2;
            positiveCount += count2;
        } else if (value2 < 0.0f) {
            negativeSum += value2 * count2;
            negativeCount += count2;
        }

        if (positiveCount > 0) {
            // 新鲜物品按 0 参与加权，避免少量异常状态主导整堆结果。
            float spoilageBase = positiveSum / totalCount;

            if (negativeCount > 0) {
                float reductionMax = FoodCoreCommonConfig.getFreezingReductionMax();
                float freezingRatio = (float) negativeCount / totalCount;
                float actualReduction = Math.min(reductionMax * freezingRatio, spoilageBase);
                return Math.max(0.0f, spoilageBase - actualReduction);
            }

            return spoilageBase;
        }

        if (negativeCount > 0) {
            return negativeSum / totalCount;
        }

        return 0.0f;
    }

    public record GroupUpdateProfile(
            FoodConditionGroupConfig config,
            float temperature,
            float spoilageDelta,
            float freezingDelta,
            float thawingDelta
    ) {
    }

    public enum StateBand {
        FROZEN,
        FRESH,
        SPOILING
    }
}


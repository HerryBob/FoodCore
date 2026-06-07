package com.navip.foodcore.condition;

import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.data.ModDataComponents;
import com.navip.foodcore.item.ModItems;
import com.navip.foodcore.util.TemperatureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 食物状态写回器。
 *
 * 负责把已经计算好的环境变化量应用到具体的 `ItemStack` 上，
 * 并在必要时生成新的物品堆副本、更新 `FOOD_STATE` 组件，
 * 或在食物彻底腐败时将其替换为腐败食物。
 */
public class FoodConditionUpdater {

    /**
     * 使用世界位置自动求温度后推进单个物品堆。
     */
    public static ItemStack updateItemStack(ItemStack stack, Level level, BlockPos pos, float deltaTicks) {
        return updateItemStack(stack, TemperatureHelper.getTemperature(level, pos), deltaTicks);
    }

    /**
     * 使用已知环境温度推进单个物品堆。
     *
     * 这是大多数调用场景的入口：
     * 外层先确定温度与时间片，再交给这里继续创建组级更新画像。
     */
    public static ItemStack updateItemStack(ItemStack stack, float envTemp, float deltaTicks) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        FoodConditionGroupConfig config = FoodConditionGroupConfigManager.getGroupForItem(stack.getItem());
        if (config == null) return stack;
        return updateItemStack(stack, FoodConditionMath.createUpdateProfile(config, envTemp, deltaTicks));
    }

    /**
     * 使用预先算好的组级更新画像推进物品堆。
     *
     * 适合在容器批处理里复用同一组食物的计算结果，
     * 从而减少重复的温度与时间换算成本。
     */
    public static ItemStack updateItemStack(ItemStack stack, FoodConditionMath.GroupUpdateProfile profile) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        FoodConditionState state = stack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial());
        return updateItemStack(stack, state, FoodConditionMath.calculateDelta(profile, state.value()));
    }

    /**
     * 直接应用已经算好的状态增量。
     */
    public static ItemStack updateItemStack(ItemStack stack, float delta) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        FoodConditionState state = stack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial());
        return updateItemStack(stack, state, delta);
    }

    private static ItemStack updateItemStack(ItemStack stack, FoodConditionState state, float delta) {
        if (state.isSpoiled()) {
            return createSpoiledStack(stack.getCount());
        }

        float newValue = state.value() + delta;
        newValue = Math.max(FoodConditionState.MIN_VALUE, Math.min(FoodConditionState.MAX_VALUE, newValue));
        if (Float.compare(newValue, state.value()) == 0) {
            return stack;
        }

        if (newValue <= FoodConditionState.MIN_VALUE) {
            ItemStack updatedStack = stack.copy();
            updatedStack.set(ModDataComponents.FOOD_STATE.get(), new FoodConditionState(FoodConditionState.MIN_VALUE));
            return updatedStack;
        } else if (newValue >= FoodConditionState.MAX_VALUE) {
            return createSpoiledStack(stack.getCount());
        } else {
            ItemStack updatedStack = stack.copy();
            updatedStack.set(ModDataComponents.FOOD_STATE.get(), new FoodConditionState(newValue));
            return updatedStack;
        }
    }

    /**
     * 以玩家当前位置为环境来源推进物品堆。
     */
    public static ItemStack updateItemStack(ItemStack stack, ServerPlayer player, float deltaTicks) {
        return updateItemStack(stack, TemperatureHelper.getTemperature(player.level(), player.blockPosition()), deltaTicks);
    }

    private static ItemStack createSpoiledStack(int count) {
        return new ItemStack(ModItems.SPOILED_FOOD.get(), Math.max(0, count));
    }
}








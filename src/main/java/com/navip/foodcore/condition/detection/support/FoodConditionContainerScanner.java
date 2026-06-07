package com.navip.foodcore.condition.detection.support;

import com.navip.foodcore.condition.FoodConditionMath;
import com.navip.foodcore.condition.FoodConditionState;
import com.navip.foodcore.condition.FoodConditionUpdater;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.data.ModDataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用容器扫描执行器。
 *
 * 负责把一个 `Container` 中的受控食物按“食物组 + 状态区间”进行批处理，
 * 先聚合同类槽位，再共享计算结果，最后统一写回物品堆，
 * 是玩家背包、静态容器和实体容器推进链共同复用的核心组件。
 */
public final class FoodConditionContainerScanner {
    private FoodConditionContainerScanner() {
    }

    /**
     * 以统一批处理方式推进一个容器中的食物状态。
     *
     * 处理流程分为三步：
     * 1. 先遍历容器，把槽位按食物组和状态区间分类；
     * 2. 为每个分组共享创建一次更新画像；
     * 3. 再逐槽位复核并写回更新后的物品堆。
     */
    public static void processContainerContext(Container container, float envTemp, float deltaTicks) {
        processSlots(container.getContainerSize(), container::getItem, container::setItem, envTemp, deltaTicks);
    }

    /**
     * 以统一批处理方式推进一个 `IItemHandlerModifiable` 中的食物状态。
     *
     * 这层主要服务于 NeoForge Capability 容器和后续 Create 动态结构库存，
     * 让它们复用与原版容器完全一致的分组、分段和写回逻辑。
     */
    public static void processItemHandlerContext(IItemHandlerModifiable itemHandler, float envTemp, float deltaTicks) {
        processSlots(itemHandler.getSlots(), itemHandler::getStackInSlot, itemHandler::setStackInSlot, envTemp, deltaTicks);
    }

    /**
     * 判断容器中是否存在任何受 FoodCore 管控的食物。
     *
     * 这个轻量方法常用于预过滤阶段，
     * 用来尽早跳过与食物状态系统无关的容器。
     */
    public static boolean containsTrackedFood(Container container) {
        return containsTrackedFood(container.getContainerSize(), container::getItem);
    }

    public static boolean containsTrackedFood(IItemHandler itemHandler) {
        return containsTrackedFood(itemHandler.getSlots(), itemHandler::getStackInSlot);
    }

    private static float getFoodConditionStateValue(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial()).value();
    }

    private static void processSlots(int slotCount, IntFunction<ItemStack> getter, BiConsumer<Integer, ItemStack> setter,
                                     float envTemp, float deltaTicks) {
        Map<FoodConditionGroupConfig, Map<FoodConditionMath.StateBand, List<Integer>>> groupedSlots = new LinkedHashMap<>();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = getter.apply(i);
            if (stack.isEmpty()) {
                continue;
            }

            FoodConditionGroupConfig groupConfig = FoodConditionGroupConfigManager.getGroupForItem(stack.getItem());
            if (groupConfig == null) {
                continue;
            }
            FoodConditionMath.StateBand stateBand = FoodConditionMath.getStateBand(getFoodConditionStateValue(stack));
            groupedSlots
                    .computeIfAbsent(groupConfig, unused -> new LinkedHashMap<>())
                    .computeIfAbsent(stateBand, unused -> new ArrayList<>())
                    .add(i);
        }

        for (Map.Entry<FoodConditionGroupConfig, Map<FoodConditionMath.StateBand, List<Integer>>> groupEntry : groupedSlots.entrySet()) {
            FoodConditionMath.GroupUpdateProfile updateProfile = FoodConditionMath.createUpdateProfile(groupEntry.getKey(), envTemp, deltaTicks);
            for (Map.Entry<FoodConditionMath.StateBand, List<Integer>> bandEntry : groupEntry.getValue().entrySet()) {
                float bandDelta = FoodConditionMath.calculateDelta(updateProfile, bandEntry.getKey());
                for (int slot : bandEntry.getValue()) {
                    ItemStack stack = getter.apply(slot);
                    if (stack.isEmpty()
                            || FoodConditionGroupConfigManager.getGroupForItem(stack.getItem()) != groupEntry.getKey()
                            || FoodConditionMath.getStateBand(getFoodConditionStateValue(stack)) != bandEntry.getKey()) {
                        continue;
                    }

                    ItemStack updatedStack = FoodConditionUpdater.updateItemStack(stack, bandDelta);
                    if (updatedStack != stack || updatedStack.isEmpty()) {
                        setter.accept(slot, updatedStack);
                    }
                }
            }
        }
    }

    private static boolean containsTrackedFood(int slotCount, IntFunction<ItemStack> getter) {
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = getter.apply(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (FoodConditionGroupConfigManager.getGroupForItem(stack.getItem()) != null) {
                return true;
            }
        }
        return false;
    }
}




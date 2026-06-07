package com.navip.foodcore.condition.merge;

import com.navip.foodcore.condition.FoodConditionMath;
import com.navip.foodcore.condition.FoodConditionState;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.data.ModDataComponents;
import com.navip.foodcore.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家背包自动合并服务。
 *
 * 负责在定时任务中查找可合并的同类食物堆，
 * 并在保留其他组件一致性的前提下，
 * 使用食物状态加权规则合并两堆物品的 `FOOD_STATE`。
 */
public final class FoodConditionInventoryMergeService {
    private FoodConditionInventoryMergeService() {
    }

    public static void mergeStacksInInventory(ServerPlayer player) {
        Map<Item, List<Integer>> itemsByType = new HashMap<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getCount() >= stack.getMaxStackSize()) continue;

            FoodConditionGroupConfig config = FoodConditionGroupConfigManager.getGroupForItem(stack.getItem());
            if (config == null) continue;

            Item item = stack.getItem();
            itemsByType.computeIfAbsent(item, k -> new ArrayList<>()).add(i);
        }

        for (List<Integer> slots : itemsByType.values()) {
            if (slots.size() <= 1) continue;
            mergeClosestStacks(player, slots);
        }
    }

    private static void mergeClosestStacks(ServerPlayer player, List<Integer> slots) {
        while (true) {
            MergeCandidate candidate = findBestMergeCandidate(player, slots);
            if (candidate == null) {
                return;
            }

            ItemStack targetStack = player.getInventory().getItem(candidate.targetSlot());
            ItemStack sourceStack = player.getInventory().getItem(candidate.sourceSlot());
            if (targetStack.isEmpty() || sourceStack.isEmpty()) continue;
            if (targetStack.getCount() >= targetStack.getMaxStackSize()) continue;
            if (!canMergeFoodStacks(targetStack, sourceStack)) continue;

            int toMove = Math.min(sourceStack.getCount(), targetStack.getMaxStackSize() - targetStack.getCount());
            if (toMove <= 0) continue;

            float newStateValue = FoodConditionMath.mergeFoodConditionStates(targetStack, sourceStack, toMove);
            ItemStack updatedTargetStack = targetStack.copy();
            ItemStack updatedSourceStack = sourceStack.copy();

            updatedTargetStack.grow(toMove);
            updatedSourceStack.shrink(toMove);
            if (newStateValue >= FoodConditionState.MAX_VALUE) {
                updatedTargetStack = new ItemStack(ModItems.SPOILED_FOOD.get(), updatedTargetStack.getCount());
            } else {
                updatedTargetStack.set(
                        ModDataComponents.FOOD_STATE.get(),
                        new FoodConditionState(Mth.clamp(newStateValue, FoodConditionState.MIN_VALUE, FoodConditionState.MAX_VALUE))
                );
            }
            player.getInventory().setItem(candidate.targetSlot(), updatedTargetStack);
            player.getInventory().setItem(candidate.sourceSlot(), updatedSourceStack.isEmpty() ? ItemStack.EMPTY : updatedSourceStack);
        }
    }

    private static MergeCandidate findBestMergeCandidate(ServerPlayer player, List<Integer> slots) {
        MergeCandidate bestCandidate = null;

        for (int i = 0; i < slots.size(); i++) {
            int firstSlot = slots.get(i);
            ItemStack firstStack = player.getInventory().getItem(firstSlot);
            if (firstStack.isEmpty()) continue;

            for (int j = i + 1; j < slots.size(); j++) {
                int secondSlot = slots.get(j);
                ItemStack secondStack = player.getInventory().getItem(secondSlot);
                if (secondStack.isEmpty()) continue;
                if (!canMergeFoodStacks(firstStack, secondStack)) continue;

                MergeCandidate candidate = createMergeCandidate(firstSlot, firstStack, secondSlot, secondStack);
                if (candidate == null) continue;

                if (bestCandidate == null
                        || candidate.priority() < bestCandidate.priority()
                        || (candidate.priority() == bestCandidate.priority() && candidate.distance() < bestCandidate.distance())
                        || (candidate.priority() == bestCandidate.priority()
                        && candidate.distance() == bestCandidate.distance()
                        && candidate.targetSlot() < bestCandidate.targetSlot())) {
                    bestCandidate = candidate;
                }
            }
        }

        return bestCandidate;
    }

    private static MergeCandidate createMergeCandidate(int firstSlot, ItemStack firstStack, int secondSlot, ItemStack secondStack) {
        int firstRemaining = firstStack.getMaxStackSize() - firstStack.getCount();
        int secondRemaining = secondStack.getMaxStackSize() - secondStack.getCount();

        if (firstRemaining <= 0 && secondRemaining <= 0) {
            return null;
        }

        int targetSlot;
        int sourceSlot;
        ItemStack targetStack;
        ItemStack sourceStack;

        if (firstRemaining <= 0) {
            targetSlot = secondSlot;
            sourceSlot = firstSlot;
            targetStack = secondStack;
            sourceStack = firstStack;
        } else if (secondRemaining <= 0) {
            targetSlot = firstSlot;
            sourceSlot = secondSlot;
            targetStack = firstStack;
            sourceStack = secondStack;
        } else if (firstStack.getCount() > secondStack.getCount()) {
            targetSlot = firstSlot;
            sourceSlot = secondSlot;
            targetStack = firstStack;
            sourceStack = secondStack;
        } else if (secondStack.getCount() > firstStack.getCount()) {
            targetSlot = secondSlot;
            sourceSlot = firstSlot;
            targetStack = secondStack;
            sourceStack = firstStack;
        } else if (firstSlot <= secondSlot) {
            targetSlot = firstSlot;
            sourceSlot = secondSlot;
            targetStack = firstStack;
            sourceStack = secondStack;
        } else {
            targetSlot = secondSlot;
            sourceSlot = firstSlot;
            targetStack = secondStack;
            sourceStack = firstStack;
        }

        if (targetStack.getCount() >= targetStack.getMaxStackSize()) {
            return null;
        }

        float firstValue = getFoodConditionStateValue(firstStack);
        float secondValue = getFoodConditionStateValue(secondStack);
        int priority = isCrossZeroPair(firstValue, secondValue) ? 1 : 0;
        float distance = Math.abs(firstValue - secondValue);
        return new MergeCandidate(targetSlot, sourceSlot, priority, distance);
    }

    private static float getFoodConditionStateValue(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial()).value();
    }

    private static boolean isCrossZeroPair(float firstValue, float secondValue) {
        return (firstValue < 0.0f && secondValue > 0.0f) || (firstValue > 0.0f && secondValue < 0.0f);
    }

    private static boolean canMergeFoodStacks(ItemStack stack1, ItemStack stack2) {
        ItemStack comparableStack1 = stack1.copy();
        ItemStack comparableStack2 = stack2.copy();
        comparableStack1.remove(ModDataComponents.FOOD_STATE.get());
        comparableStack2.remove(ModDataComponents.FOOD_STATE.get());
        return ItemStack.isSameItemSameComponents(comparableStack1, comparableStack2);
    }

    private record MergeCandidate(int targetSlot, int sourceSlot, int priority, float distance) {
    }
}

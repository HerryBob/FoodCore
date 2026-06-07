package com.navip.foodcore.condition.detection.support;

import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.IntFunction;

/**
 * 统一扫描容器中的食物与保鲜物品。
 * 第一版只做固定倍率修正，不做复杂温度偏移。
 */
public final class FoodConditionContainerPreservationHelper {
    private FoodConditionContainerPreservationHelper() {
    }

    /**
     * 扫描一个容器并返回食物存在情况与保鲜模式。
     *
     * 该方法会在一次遍历中同时完成两类判断：
     * 1. 容器内是否存在配置中的食物；
     * 2. 容器内是否存在保鲜、冷藏、静滞物品。
     */
    public static Inspection inspect(Container container) {
        return inspectSlots(container.getContainerSize(), container::getItem);
    }

    /**
     * 扫描一个 Capability 容器并返回食物存在情况与保鲜模式。
     */
    public static Inspection inspect(IItemHandler itemHandler) {
        return inspectSlots(itemHandler.getSlots(), itemHandler::getStackInSlot);
    }

    /**
     * 计算一个原版容器在当前扫描周期下真正应使用的有效 deltaTicks。
     *
     * 返回值语义：
     * - `> 0`：该容器应继续推进，返回已应用保鲜倍率后的刻数
     * - `<= 0`：该容器可直接跳过，可能原因包括没有受控食物或已被静滞核心完全免疫
     */
    public static float resolveEffectiveDeltaTicks(Container container, float deltaTicks) {
        return resolveEffectiveDeltaTicks(inspect(container), deltaTicks);
    }

    /**
     * 计算一个 capability / item handler 容器在当前扫描周期下真正应使用的有效 deltaTicks。
     */
    public static float resolveEffectiveDeltaTicks(IItemHandler itemHandler, float deltaTicks) {
        return resolveEffectiveDeltaTicks(inspect(itemHandler), deltaTicks);
    }

    /**
     * 复用已经完成的检测结果计算有效 deltaTicks。
     *
     * 适合上层在“先 inspect 再决定是否继续处理”的场景里避免重复扫描同一批槽位。
     */
    public static float resolveEffectiveDeltaTicks(Inspection inspection, float deltaTicks) {
        return resolveEffectiveDeltaTicksInternal(inspection, deltaTicks);
    }

    private static Inspection inspectSlots(int slotCount, IntFunction<ItemStack> getter) {
        boolean containsTrackedFood = false;
        PreservationMode mode = PreservationMode.NORMAL;

        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = getter.apply(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (FoodConditionGroupConfigManager.getGroupForItem(item) != null) {
                containsTrackedFood = true;
            }

            if (item == ModItems.STASIS_CORE.get()) {
                mode = PreservationMode.IMMUNE;
            } else if (mode != PreservationMode.IMMUNE && item == ModItems.REFRIGERATION_MODULE.get()) {
                mode = PreservationMode.REFRIGERATED;
            } else if (mode == PreservationMode.NORMAL && item == ModItems.PRESERVATION_LINING.get()) {
                mode = PreservationMode.PRESERVED;
            }

            if (containsTrackedFood && mode == PreservationMode.IMMUNE) {
                return new Inspection(true, PreservationMode.IMMUNE);
            }
        }

        return new Inspection(containsTrackedFood, mode);
    }

    private static float resolveEffectiveDeltaTicksInternal(Inspection inspection, float deltaTicks) {
        if (!inspection.containsTrackedFood()) {
            return 0.0f;
        }

        return inspection.mode().applyToDeltaTicks(deltaTicks);
    }

    public record Inspection(boolean containsTrackedFood, PreservationMode mode) {
    }

    public enum PreservationMode {
        NORMAL(1.0f),
        PRESERVED(0.5f),
        REFRIGERATED(0.25f),
        IMMUNE(0.0f);

        private final float changeMultiplier;

        PreservationMode(float changeMultiplier) {
            this.changeMultiplier = changeMultiplier;
        }

        public float applyToDeltaTicks(float deltaTicks) {
            return deltaTicks * this.changeMultiplier;
        }
    }
}


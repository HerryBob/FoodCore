package com.navip.foodcore.condition.detection.support;

import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * 通用 Create 包裹内容扫描器。
 *
 * 负责在外层容器或 ItemHandler 的槽位中识别 PackageItem，
 * 并递归推进包裹内部 9 格物品的食物状态，再把变更写回包裹组件。
 */
public final class FoodConditionPackageScanner {
    private FoodConditionPackageScanner() {
    }

    public static void processPackagesInContainer(Container container, float envTemp, float deltaTicks) {
        if (!FoodCoreCommonConfig.isEnablePackageProcessing()) {
            return;
        }
        processSlots(container.getContainerSize(), container::getItem, container::setItem, envTemp, deltaTicks);
    }

    public static void processPackagesInItemHandler(IItemHandlerModifiable itemHandler, float envTemp, float deltaTicks) {
        if (!FoodCoreCommonConfig.isEnablePackageProcessing()) {
            return;
        }
        processSlots(itemHandler.getSlots(), itemHandler::getStackInSlot, itemHandler::setStackInSlot, envTemp, deltaTicks);
    }

    private static void processSlots(int slotCount, IntFunction<ItemStack> getter, BiConsumer<Integer, ItemStack> setter,
                                     float envTemp, float deltaTicks) {
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = getter.apply(slot);
            if (stack.isEmpty() || !PackageItem.isPackage(stack)) {
                continue;
            }

            ItemStack updatedStack = processPackageStack(stack, envTemp, deltaTicks);
            if (updatedStack != stack) {
                setter.accept(slot, updatedStack);
            }
        }
    }

    private static ItemStack processPackageStack(ItemStack packageStack, float envTemp, float deltaTicks) {
        ItemStackHandler originalContents = PackageItem.getContents(packageStack);
        FoodConditionContainerPreservationHelper.Inspection inspection =
                FoodConditionContainerPreservationHelper.inspect(originalContents);
        float effectiveDeltaTicks = FoodConditionContainerPreservationHelper.resolveEffectiveDeltaTicks(inspection, deltaTicks);
        if (effectiveDeltaTicks <= 0.0f) {
            return packageStack;
        }

        ItemStackHandler updatedContents = copyContents(originalContents);
        FoodConditionContainerScanner.processItemHandlerContext(updatedContents, envTemp, effectiveDeltaTicks);
        if (!hasContentsChanged(originalContents, updatedContents)) {
            return packageStack;
        }

        ItemStack updatedPackage = packageStack.copy();
        ItemContainerContents contentsComponent = ItemHelper.containerContentsFromHandler(updatedContents);
        updatedPackage.set(AllDataComponents.PACKAGE_CONTENTS, contentsComponent);
        return updatedPackage;
    }

    private static ItemStackHandler copyContents(ItemStackHandler sourceContents) {
        ItemStackHandler copiedContents = new ItemStackHandler(sourceContents.getSlots());
        ItemHelper.copyContents(sourceContents, copiedContents);
        return copiedContents;
    }

    private static boolean hasContentsChanged(ItemStackHandler originalContents, ItemStackHandler updatedContents) {
        for (int slot = 0; slot < originalContents.getSlots(); slot++) {
            ItemStack originalStack = originalContents.getStackInSlot(slot);
            ItemStack updatedStack = updatedContents.getStackInSlot(slot);
            if (originalStack.getCount() != updatedStack.getCount()
                    || !ItemStack.isSameItemSameComponents(originalStack, updatedStack)) {
                return true;
            }
        }
        return false;
    }
}

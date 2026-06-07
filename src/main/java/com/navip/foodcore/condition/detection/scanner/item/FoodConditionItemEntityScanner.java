package com.navip.foodcore.condition.detection.scanner.item;

import com.navip.foodcore.condition.FoodConditionUpdater;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 掉落物扫描入口。
 *
 * 负责在服务器周期扫描中处理世界中的 `ItemEntity`，
 * 并根据其所处环境温度推进内部食物状态。
 * 当前实现仍偏直接，后续仍有继续重构为更细分检测机制的空间。
 */
public final class FoodConditionItemEntityScanner {
    private FoodConditionItemEntityScanner() {
    }

    public static void processLoadedItemEntities(MinecraftServer server, float deltaTicks) {
        server.getAllLevels().forEach(level -> {
            if (level.isClientSide()) {
                return;
            }

            for (Entity entity : level.getEntities().getAll()) {
                if (!(entity instanceof ItemEntity itemEntity)) {
                    continue;
                }

                ItemStack stack = itemEntity.getItem();
                ItemStack updatedStack = FoodConditionUpdater.updateItemStack(stack, level, itemEntity.blockPosition(), deltaTicks);
                if (updatedStack.isEmpty()) {
                    itemEntity.discard();
                } else if (updatedStack != stack) {
                    itemEntity.setItem(updatedStack);
                }
            }
        });
    }
}


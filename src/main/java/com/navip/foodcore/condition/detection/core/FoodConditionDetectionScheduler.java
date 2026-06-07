package com.navip.foodcore.condition.detection.core;

import com.navip.foodcore.condition.detection.scanner.container.FoodConditionEntityContainerScanner;
import com.navip.foodcore.condition.detection.scanner.container.create.FoodConditionCreateContraptionScanner;
import com.navip.foodcore.condition.detection.scanner.container.create.FoodConditionCreateStaticContainerScanner;
import com.navip.foodcore.condition.detection.scanner.container.FoodConditionStaticContainerScanner;
import com.navip.foodcore.condition.detection.scanner.item.FoodConditionItemEntityScanner;
import com.navip.foodcore.condition.detection.scanner.special.FoodConditionUnaffectedContainerScanner;
import com.navip.foodcore.condition.detection.support.FoodConditionPackageScanner;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerScanner;
import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.navip.foodcore.condition.merge.FoodConditionInventoryMergeService;
import com.navip.foodcore.util.TemperatureHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 检测与自动合并的总调度器。
 *
 * 该类按固定 Tick 周期统一驱动各类载体的食物状态推进，
 * 同时独立控制玩家背包自动合并的触发频率。
 * 它只决定“什么时候扫描谁”，具体如何扫描由各个 scanner 负责。
 */
public final class FoodConditionDetectionScheduler {
    private static int tickCounter = 0;
    private static int mergeTickCounter = 0;

    private FoodConditionDetectionScheduler() {
    }

    public static void reset() {
        tickCounter = 0;
        mergeTickCounter = 0;
    }

    /**
     * 调度一次全局检测。
     *
     * 它会分别判断“状态推进”和“自动合并”两条调度线是否到期，
     * 然后按载体类型把工作分发给不同的 scanner 或 merge service。
     */
    public static void tick(MinecraftServer server) {
        if (!FoodCoreCommonConfig.isEnableFoodConditionSystem()) {
            return;
        }

        int scanIntervalTicks = FoodCoreCommonConfig.getDetectionScanIntervalTicks();
        int mergeIntervalTicks = FoodCoreCommonConfig.getStackingMergeIntervalTicks();
        boolean shouldUpdateConditions = ++tickCounter >= scanIntervalTicks;
        boolean shouldMergeStacks = ++mergeTickCounter >= mergeIntervalTicks;

        if (!shouldUpdateConditions && !shouldMergeStacks) {
            return;
        }
        if (shouldUpdateConditions) {
            tickCounter = 0;
        }
        if (shouldMergeStacks) {
            mergeTickCounter = 0;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (shouldUpdateConditions && FoodCoreCommonConfig.isEnablePlayerInventoryDetection()) {
                float playerEnvTemp = TemperatureHelper.getTemperature(player.level(), player.blockPosition());
                FoodConditionPackageScanner.processPackagesInContainer(player.getInventory(), playerEnvTemp, scanIntervalTicks);
                FoodConditionContainerScanner.processContainerContext(player.getInventory(), playerEnvTemp, scanIntervalTicks);
                FoodConditionUnaffectedContainerScanner.processPlayerEnderChest(player, playerEnvTemp, scanIntervalTicks);
            }

            if (shouldMergeStacks && FoodCoreCommonConfig.isEnableStackingMerge()) {
                FoodConditionInventoryMergeService.mergeStacksInInventory(player);
            }
        }

        if (shouldUpdateConditions) {
            if (FoodCoreCommonConfig.isEnableItemEntityDetection()) {
                FoodConditionItemEntityScanner.processLoadedItemEntities(server, scanIntervalTicks);
            }
            if (FoodCoreCommonConfig.isEnableStaticContainerDetection()) {
                FoodConditionStaticContainerScanner.processLoadedStaticContainers(server, scanIntervalTicks);
            }
            if (FoodCoreCommonConfig.isEnableCreateStaticContainerDetection()) {
                FoodConditionCreateStaticContainerScanner.processLoadedStaticContainers(server, scanIntervalTicks);
            }
            if (FoodCoreCommonConfig.isEnableCreateContraptionDetection()) {
                FoodConditionCreateContraptionScanner.processLoadedContraptions(server, scanIntervalTicks);
            }
            if (FoodCoreCommonConfig.isEnableEntityContainerDetection()) {
                FoodConditionEntityContainerScanner.processLoadedEntityContainers(server, scanIntervalTicks);
            }
        }
    }
}

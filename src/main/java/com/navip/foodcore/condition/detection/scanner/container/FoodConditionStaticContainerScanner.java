package com.navip.foodcore.condition.detection.scanner.container;

import com.navip.foodcore.FoodCore;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerPreservationHelper;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerScanner;
import com.navip.foodcore.condition.detection.support.FoodConditionPackageScanner;
import com.navip.foodcore.util.TemperatureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 静态容器检测入口。
 * 当前按已加载区块维护方块容器集合，后续可继续扩到更多静态容器类型。
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public final class FoodConditionStaticContainerScanner {
    private static final Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> TRACKED_CONTAINERS = new HashMap<>();

    private FoodConditionStaticContainerScanner() {
    }

    /**
     * 处理当前所有已登记且仍处于加载状态的静态容器。
     *
     * 这一层会先做容器有效性、loot table 安全性和食物存在性的预过滤，
     * 只有真正需要推进的容器才会进入通用容器扫描主链。
     */
    public static void processLoadedStaticContainers(MinecraftServer server, float deltaTicks) {
        for (ServerLevel level : server.getAllLevels()) {
            LinkedHashSet<BlockPos> trackedPositions = TRACKED_CONTAINERS.get(level.dimension());
            if (trackedPositions == null || trackedPositions.isEmpty()) {
                continue;
            }

            Iterator<BlockPos> iterator = trackedPositions.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                if (!level.isLoaded(pos)) {
                    iterator.remove();
                    continue;
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof Container container) || !isSupportedStaticContainer(blockEntity)) {
                    iterator.remove();
                    continue;
                }
                if (hasPendingLootTable(blockEntity)) {
                    continue;
                }
                float envTemp = TemperatureHelper.getTemperature(level, pos);
                FoodConditionPackageScanner.processPackagesInContainer(container, envTemp, deltaTicks);
                float effectiveDeltaTicks = FoodConditionContainerPreservationHelper.resolveEffectiveDeltaTicks(container, deltaTicks);
                if (effectiveDeltaTicks <= 0.0f) {
                    continue;
                }

                FoodConditionContainerScanner.processContainerContext(container, envTemp, effectiveDeltaTicks);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            trackContainer(level, blockEntity);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        LinkedHashSet<BlockPos> trackedPositions = TRACKED_CONTAINERS.get(level.dimension());
        if (trackedPositions == null || trackedPositions.isEmpty()) {
            return;
        }

        for (BlockPos pos : chunk.getBlockEntities().keySet()) {
            trackedPositions.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (blockEntity != null) {
            trackContainer(level, blockEntity);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LinkedHashSet<BlockPos> trackedPositions = TRACKED_CONTAINERS.get(level.dimension());
        if (trackedPositions != null) {
            trackedPositions.remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TRACKED_CONTAINERS.remove(level.dimension());
        }
    }

    private static void trackContainer(ServerLevel level, BlockEntity blockEntity) {
        if (!isSupportedStaticContainer(blockEntity)) {
            return;
        }

        TRACKED_CONTAINERS
                .computeIfAbsent(level.dimension(), unused -> new LinkedHashSet<>())
                .add(blockEntity.getBlockPos().immutable());
    }

    private static boolean isSupportedStaticContainer(BlockEntity blockEntity) {
        return blockEntity instanceof Container;
    }

    private static boolean hasPendingLootTable(BlockEntity blockEntity) {
        return blockEntity instanceof RandomizableContainer randomizableContainer
                && randomizableContainer.getLootTable() != null;
    }
}


package com.navip.foodcore.condition.detection.scanner.container.create;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 扫描所有通过 NeoForge ItemHandler Capability 暴露库存的静态方块容器。
 *
 * 第一版主要面向 Create 等不直接实现原版 {@link Container}，
 * 但通过 Capability 提供物品槽位访问的方块实体。
 * 为避免和原版容器扫描重复处理，这里只处理“不是 Container、但有可写 ItemHandler”的方块实体。
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public final class FoodConditionCreateStaticContainerScanner {
    private static final Map<ResourceKey<Level>, LinkedHashSet<BlockPos>> TRACKED_CONTAINERS = new HashMap<>();

    private FoodConditionCreateStaticContainerScanner() {
    }

    /**
     * 处理所有已加载且提供 IItemHandler 能力的容器方块。
     *
     * @param server      MinecraftServer 实例
     * @param deltaTicks  距离上次扫描经过的游戏刻数
     */
    public static void processLoadedStaticContainers(MinecraftServer server, float deltaTicks) {
        Set<IItemHandlerModifiable> processedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerLevel level : server.getAllLevels()) {
            LinkedHashSet<BlockPos> positions = TRACKED_CONTAINERS.get(level.dimension());
            if (positions == null || positions.isEmpty()) {
                continue;
            }

            Iterator<BlockPos> iterator = positions.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                // 检查区块是否仍加载
                if (!level.isLoaded(pos)) {
                    iterator.remove();
                    continue;
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity == null || blockEntity instanceof Container) {
                    iterator.remove();
                    continue;
                }

                IItemHandlerModifiable itemHandler = getSupportedItemHandler(level, pos);
                if (itemHandler == null) {
                    iterator.remove();
                    continue;
                }
                if (!processedHandlers.add(itemHandler)) {
                    continue;
                }

                float envTemp = TemperatureHelper.getTemperature(level, pos);
                FoodConditionPackageScanner.processPackagesInItemHandler(itemHandler, envTemp, deltaTicks);
                float effectiveDeltaTicks = FoodConditionContainerPreservationHelper.resolveEffectiveDeltaTicks(itemHandler, deltaTicks);
                if (effectiveDeltaTicks <= 0.0f) {
                    continue;
                }

                FoodConditionContainerScanner.processItemHandlerContext(itemHandler, envTemp, effectiveDeltaTicks);
            }
        }
    }

    // ========== 事件监听：维护容器集合 ==========

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            trackIfCapabilityContainer(level, be);
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

        LinkedHashSet<BlockPos> positions = TRACKED_CONTAINERS.get(level.dimension());
        if (positions == null) {
            return;
        }
        for (BlockPos pos : chunk.getBlockEntities().keySet()) {
            positions.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be != null) {
            trackIfCapabilityContainer(level, be);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LinkedHashSet<BlockPos> positions = TRACKED_CONTAINERS.get(level.dimension());
        if (positions != null) {
            positions.remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TRACKED_CONTAINERS.remove(level.dimension());
        }
    }

    private static void trackIfCapabilityContainer(ServerLevel level, BlockEntity be) {
        if (be instanceof Container) {
            return;
        }

        if (getSupportedItemHandler(level, be.getBlockPos()) != null) {
            TRACKED_CONTAINERS
                    .computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>())
                    .add(be.getBlockPos().immutable());
        }
    }

    private static IItemHandlerModifiable getSupportedItemHandler(ServerLevel level, BlockPos pos) {
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (itemHandler == null || itemHandler.getSlots() <= 0) {
            return null;
        }

        return itemHandler instanceof IItemHandlerModifiable modifiable ? modifiable : null;
    }
}

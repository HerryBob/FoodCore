package com.navip.foodcore.condition.detection.scanner.container;

import com.navip.foodcore.FoodCore;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerPreservationHelper;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerScanner;
import com.navip.foodcore.condition.detection.support.FoodConditionPackageScanner;
import com.navip.foodcore.util.TemperatureHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 原版带库存实体检测入口。
 * 当前先兼容有箱子的驴/骡系、箱子矿车/漏斗矿车，以及箱船。
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public final class FoodConditionEntityContainerScanner {
    private static final Map<ResourceKey<Level>, LinkedHashSet<Integer>> TRACKED_ENTITY_CONTAINERS = new HashMap<>();

    private FoodConditionEntityContainerScanner() {
    }

    /**
     * 处理当前已登记的实体容器集合。
     *
     * 相比旧版“遍历所有实体再筛选”的做法，
     * 这里现在只扫描被事件登记过的目标实体，并在扫描前跳过仍带 loot table 的容器实体。
     */
    public static void processLoadedEntityContainers(MinecraftServer server, float deltaTicks) {
        for (ServerLevel level : server.getAllLevels()) {
            syncChestedHorses(level);

            LinkedHashSet<Integer> trackedEntityIds = TRACKED_ENTITY_CONTAINERS.get(level.dimension());
            if (trackedEntityIds == null || trackedEntityIds.isEmpty()) {
                continue;
            }

            Iterator<Integer> iterator = trackedEntityIds.iterator();
            while (iterator.hasNext()) {
                Entity entity = level.getEntity(iterator.next());
                if (entity == null || entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                Container container = getSupportedEntityContainer(entity);
                if (container == null) {
                    iterator.remove();
                    continue;
                }
                if (hasPendingLootTable(entity)) {
                    continue;
                }

                float envTemp = TemperatureHelper.getTemperature(level, entity.blockPosition());
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
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (!isSupportedEntityContainer(entity)) {
            return;
        }

        TRACKED_ENTITY_CONTAINERS
                .computeIfAbsent(level.dimension(), unused -> new LinkedHashSet<>())
                .add(entity.getId());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LinkedHashSet<Integer> trackedEntityIds = TRACKED_ENTITY_CONTAINERS.get(level.dimension());
        if (trackedEntityIds != null) {
            trackedEntityIds.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TRACKED_ENTITY_CONTAINERS.remove(level.dimension());
        }
    }

    private static boolean isSupportedEntityContainer(Entity entity) {
        return getSupportedEntityContainer(entity) != null;
    }

    private static Container getSupportedEntityContainer(Entity entity) {
        if (entity instanceof AbstractChestedHorse horse) {
            return horse.hasChest() ? horse.getInventory() : null;
        }
        if (entity instanceof AbstractMinecartContainer minecartContainer) {
            return minecartContainer;
        }
        if (entity instanceof ChestBoat chestBoat) {
            return chestBoat;
        }
        return null;
    }

    private static boolean hasPendingLootTable(Entity entity) {
        return entity instanceof ContainerEntity containerEntity && containerEntity.getLootTable() != null;
    }

    private static void syncChestedHorses(ServerLevel level) {
        for (Entity entity : level.getEntities().getAll()) {
            if (!(entity instanceof AbstractChestedHorse horse) || !horse.hasChest()) {
                continue;
            }

            TRACKED_ENTITY_CONTAINERS
                    .computeIfAbsent(level.dimension(), unused -> new LinkedHashSet<>())
                    .add(entity.getId());
        }
    }
}

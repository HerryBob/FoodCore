package com.navip.foodcore.condition.detection.scanner.container.create;

import com.navip.foodcore.FoodCore;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerPreservationHelper;
import com.navip.foodcore.condition.detection.support.FoodConditionContainerScanner;
import com.navip.foodcore.condition.detection.support.FoodConditionPackageScanner;
import com.navip.foodcore.util.TemperatureHelper;
import com.simibubi.create.AllTags.AllMountedItemStorageTypeTags;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Create 动态结构容器检测入口。
 *
 * 第一版只处理运行中的 contraption 上“外露的 mounted item storage”，
 * 并按每个 storage 单独进行食物检测、保鲜判定、温度计算和状态推进。
 * 这样可以复用当前基于 `IItemHandlerModifiable` 的批处理主链，
 * 同时避免把一个 storage 中的保鲜物品错误扩散到整个动态结构。
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public final class FoodConditionCreateContraptionScanner {
    private static final Map<ResourceKey<Level>, LinkedHashSet<Integer>> TRACKED_CONTRAPTIONS = new HashMap<>();

    private FoodConditionCreateContraptionScanner() {
    }

    /**
     * 处理当前已登记的 Create 动态结构实体。
     */
    public static void processLoadedContraptions(MinecraftServer server, float deltaTicks) {
        Set<MountedItemStorage> processedStorages = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerLevel level : server.getAllLevels()) {
            LinkedHashSet<Integer> trackedEntityIds = TRACKED_CONTRAPTIONS.get(level.dimension());
            if (trackedEntityIds == null || trackedEntityIds.isEmpty()) {
                continue;
            }

            Iterator<Integer> iterator = trackedEntityIds.iterator();
            while (iterator.hasNext()) {
                Entity entity = level.getEntity(iterator.next());
                if (!(entity instanceof AbstractContraptionEntity contraptionEntity) || entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                Contraption contraption = contraptionEntity.getContraption();
                if (contraption == null) {
                    continue;
                }

                MountedStorageManager storageManager = contraption.getStorage();
                if (storageManager == null) {
                    continue;
                }

                for (Map.Entry<BlockPos, MountedItemStorage> storageEntry : storageManager.getMountedItems().storages.entrySet()) {
                    MountedItemStorage storage = storageEntry.getValue();
                    if (storage == null || AllMountedItemStorageTypeTags.INTERNAL.matches(storage) || !processedStorages.add(storage)) {
                        continue;
                    }

                    BlockPos localPos = storageEntry.getKey();
                    Vec3 globalPosVec = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 0);
                    BlockPos globalPos = BlockPos.containing(globalPosVec);
                    float envTemp = TemperatureHelper.getTemperature(level, globalPos);
                    FoodConditionPackageScanner.processPackagesInItemHandler(storage, envTemp, deltaTicks);

                    float effectiveDeltaTicks = FoodConditionContainerPreservationHelper.resolveEffectiveDeltaTicks(storage, deltaTicks);
                    if (effectiveDeltaTicks <= 0.0f) {
                        continue;
                    }

                    FoodConditionContainerScanner.processItemHandlerContext(storage, envTemp, effectiveDeltaTicks);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (!isSupportedContraption(entity)) {
            return;
        }

        TRACKED_CONTRAPTIONS
                .computeIfAbsent(level.dimension(), unused -> new LinkedHashSet<>())
                .add(entity.getId());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LinkedHashSet<Integer> trackedEntityIds = TRACKED_CONTRAPTIONS.get(level.dimension());
        if (trackedEntityIds != null) {
            trackedEntityIds.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TRACKED_CONTRAPTIONS.remove(level.dimension());
        }
    }

    private static boolean isSupportedContraption(Entity entity) {
        return entity instanceof AbstractContraptionEntity;
    }
}

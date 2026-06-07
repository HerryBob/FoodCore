package com.navip.foodcore.config;

import com.navip.foodcore.FoodCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * FoodCore 全局配置
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public class FoodCoreCommonConfig {
    public static final ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue enableFoodConditionSystem;
    public static ModConfigSpec.IntValue detectionScanIntervalSeconds;
    public static ModConfigSpec.BooleanValue enablePlayerInventoryDetection;
    public static ModConfigSpec.BooleanValue enableStaticContainerDetection;
    public static ModConfigSpec.BooleanValue enableEntityContainerDetection;
    public static ModConfigSpec.BooleanValue enableCreateStaticContainerDetection;
    public static ModConfigSpec.BooleanValue enableCreateContraptionDetection;
    public static ModConfigSpec.BooleanValue enableItemEntityDetection;
    public static ModConfigSpec.BooleanValue enablePackageProcessing;
    public static ModConfigSpec.BooleanValue enableFoodConditionTooltip;
    public static ModConfigSpec.BooleanValue enableColdSweatConsumptionEffect;
    public static ModConfigSpec.BooleanValue enableStackingMerge;
    public static ModConfigSpec.IntValue stackingMergeIntervalSeconds;
    public static ModConfigSpec.DoubleValue freezingReductionMax;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        enableFoodConditionSystem = builder
                .comment("Master switch for the entire food condition system\n食物状态系统总开关")
                .define("enableFoodConditionSystem", true);

        detectionScanIntervalSeconds = builder
                .comment("Global scan interval in seconds for food condition detection\n食物状态检测的全局扫描间隔秒数")
                .defineInRange("detectionScanIntervalSeconds", 5, 1, Integer.MAX_VALUE);

        enablePlayerInventoryDetection = builder
                .comment("Enable player inventory food condition detection\n是否启用玩家背包食物状态检测")
                .define("enablePlayerInventoryDetection", true);

        enableStaticContainerDetection = builder
                .comment("Enable vanilla static container food condition detection\n是否启用原版静态容器食物状态检测")
                .define("enableStaticContainerDetection", true);

        enableEntityContainerDetection = builder
                .comment("Enable entity container food condition detection\n是否启用实体容器食物状态检测")
                .define("enableEntityContainerDetection", true);

        enableCreateStaticContainerDetection = builder
                .comment("Enable Create static capability container food condition detection\n是否启用 Create 静态容器食物状态检测")
                .define("enableCreateStaticContainerDetection", true);

        enableCreateContraptionDetection = builder
                .comment("Enable Create contraption storage food condition detection\n是否启用 Create 动态结构食物状态检测")
                .define("enableCreateContraptionDetection", true);

        enableItemEntityDetection = builder
                .comment("Enable dropped item food condition detection\n是否启用掉落物食物状态检测")
                .define("enableItemEntityDetection", true);

        enablePackageProcessing = builder
                .comment("Enable recursive food condition processing inside Create packages\n是否启用 Create 包裹内部食物状态递归处理")
                .define("enablePackageProcessing", true);

        enableFoodConditionTooltip = builder
                .comment("Enable food condition tooltip rendering\n是否启用食物状态 Tooltip 显示")
                .define("enableFoodConditionTooltip", true);

        enableColdSweatConsumptionEffect = builder
                .comment("Enable Cold Sweat consumption effect from frozen food\n是否启用冻结食物的 Cold Sweat 食用效果")
                .define("enableColdSweatConsumptionEffect", true);

        enableStackingMerge = builder
                .comment("Enable item stacking merge if true the food quality will be affected by stacking\n是否开启物品堆叠合并，开启后物品堆叠时会影响食物质量")
                .define("enableStackingMerge", true);

        stackingMergeIntervalSeconds = builder
                .comment("Auto merge interval in seconds for food stacks\n食物自动合并的间隔秒数")
                .defineInRange("stackingMergeIntervalSeconds", 30, 1, Integer.MAX_VALUE);
        
        freezingReductionMax = builder
                .comment("Max reduction from frozen items when merging (0.0 to 1.5)\n物品合并时的最大质量减少，范围0.0到1.5")
                .defineInRange("freezingReductionMax", 0.2, 0.0, 1.5);
        
        builder.pop();
        
        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(FoodCore.MODID)) {
            logConfigState("Loaded");
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(FoodCore.MODID)) {
            logConfigState("Reloaded");
        }
    }

    private static void logConfigState(String prefix) {
        FoodCore.LOGGER.info(
                "{} global config: system={}, scanIntervalSeconds={}, playerInventory={}, staticContainer={}, entityContainer={}, createStatic={}, createContraption={}, itemEntity={}, packageProcessing={}, tooltip={}, coldSweatConsumptionEffect={}, stackingMerge={}, stackingMergeIntervalSeconds={}, freezingReductionMax={}",
                prefix,
                enableFoodConditionSystem.get(),
                detectionScanIntervalSeconds.get(),
                enablePlayerInventoryDetection.get(),
                enableStaticContainerDetection.get(),
                enableEntityContainerDetection.get(),
                enableCreateStaticContainerDetection.get(),
                enableCreateContraptionDetection.get(),
                enableItemEntityDetection.get(),
                enablePackageProcessing.get(),
                enableFoodConditionTooltip.get(),
                enableColdSweatConsumptionEffect.get(),
                enableStackingMerge.get(),
                stackingMergeIntervalSeconds.get(),
                freezingReductionMax.get()
        );
    }

    public static boolean isEnableFoodConditionSystem() {
        return enableFoodConditionSystem.get();
    }

    public static int getDetectionScanIntervalTicks() {
        return Math.max(1, detectionScanIntervalSeconds.get()) * 20;
    }

    public static boolean isEnablePlayerInventoryDetection() {
        return enablePlayerInventoryDetection.get();
    }

    public static boolean isEnableStaticContainerDetection() {
        return enableStaticContainerDetection.get();
    }

    public static boolean isEnableEntityContainerDetection() {
        return enableEntityContainerDetection.get();
    }

    public static boolean isEnableCreateStaticContainerDetection() {
        return enableCreateStaticContainerDetection.get();
    }

    public static boolean isEnableCreateContraptionDetection() {
        return enableCreateContraptionDetection.get();
    }

    public static boolean isEnableItemEntityDetection() {
        return enableItemEntityDetection.get();
    }

    public static boolean isEnablePackageProcessing() {
        return enablePackageProcessing.get();
    }

    public static boolean isEnableFoodConditionTooltip() {
        return enableFoodConditionTooltip.get();
    }

    public static boolean isEnableColdSweatConsumptionEffect() {
        return enableColdSweatConsumptionEffect.get();
    }

    public static boolean isEnableStackingMerge() {
        return enableStackingMerge.get();
    }

    public static float getFreezingReductionMax() {
        return freezingReductionMax.get().floatValue();
    }

    public static int getStackingMergeIntervalTicks() {
        return Math.max(1, stackingMergeIntervalSeconds.get()) * 20;
    }
}

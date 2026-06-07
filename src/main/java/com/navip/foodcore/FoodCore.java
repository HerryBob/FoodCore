package com.navip.foodcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.data.ModDataComponents;
import com.navip.foodcore.item.ModCreativeTabs;
import com.navip.foodcore.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 模组主入口。
 *
 * 负责在 NeoForge 生命周期开始时完成 FoodCore 的基础注册工作，
 * 包括全局配置、Data Component、物品、创造标签页等基础内容。
 * 业务层的食物状态推进、检测调度与表现逻辑不会直接写在这里，
 * 而是由后续的 `condition`、`item`、`client` 等子模块继续完成。
 */
@Mod(FoodCore.MODID)
public class FoodCore {
    public static final String MODID = "foodcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造阶段完成注册链初始化。
     *
     * 这里处理的是“框架层注册”，不会直接做重型业务计算。
     * 真正依赖注册表完成后的配置加载被放到了 `commonSetup` 中执行。
     */
    public FoodCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("FoodCore bootstrap ready");
        modContainer.registerConfig(ModConfig.Type.COMMON, FoodCoreCommonConfig.SPEC, MODID + "/foodcore-common.toml");
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    /**
     * 在通用初始化阶段加载运行期配置。
     *
     * 使用 `enqueueWork` 可以把需要依赖注册表完成后的任务延后执行，
     * 从而安全建立“物品 -> 食物组配置”的运行时映射。
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FoodConditionGroupConfigManager.load();
            LOGGER.info("FoodCore configuration loaded");
        });
    }
}




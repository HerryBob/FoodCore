package com.navip.foodcore.condition;

import com.navip.foodcore.FoodCore;
import com.navip.foodcore.condition.detection.core.FoodConditionDetectionScheduler;
import com.navip.foodcore.condition.effect.FoodConditionConsumptionEffects;
import com.navip.foodcore.condition.tooltip.FoodConditionTooltipRenderer;
import com.navip.foodcore.config.FoodConditionGroupConfig;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.config.FoodCoreCommonConfig;
import com.navip.foodcore.data.ModDataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * FoodCore 的总事件入口。
 *
 * 这个类只负责把 NeoForge 事件分发到对应的业务模块，
 * 例如服务器 Tick 检测、物品 Tooltip 渲染、食物食用效果结算。
 * 这里刻意保持“薄入口”结构，避免把复杂业务直接堆在事件回调里。
 */
@EventBusSubscriber(modid = FoodCore.MODID)
public class FoodConditionEventHandler {
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        FoodConditionGroupConfigManager.load();
        FoodConditionDetectionScheduler.reset();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FoodConditionDetectionScheduler.reset();
    }

    @SubscribeEvent
    /**
     * 服务器 Tick 后置事件。
     *
     * 每个服务器 Tick 都会进入这里，
     * 然后交给调度器决定本轮是否需要推进食物状态或执行自动合并。
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        FoodConditionDetectionScheduler.tick(event.getServer());
    }

    @SubscribeEvent
    /**
     * 物品 Tooltip 事件入口。
     *
     * 这里只负责把事件转交给专用渲染器，
     * 避免在事件处理器中直接拼装大量表现逻辑。
     */
    public static void onTooltip(ItemTooltipEvent event) {
        FoodConditionTooltipRenderer.appendTooltip(event);
    }

    @SubscribeEvent
    /**
     * 食物真正被吃下或喝下后的结算入口。
     *
     * 这里会确认实体、物品类型和食物状态是否满足条件，
     * 然后把冻结食物的额外效果交给消费效果模块处理。
     */
    public static void onFoodConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!FoodCoreCommonConfig.isEnableColdSweatConsumptionEffect()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack consumedStack = event.getItem();
        if (consumedStack.isEmpty()) return;
        FoodConditionGroupConfig groupConfig = FoodConditionGroupConfigManager.getGroupForItem(consumedStack.getItem());
        if (groupConfig == null) return;

        UseAnim useAnim = consumedStack.getUseAnimation();
        if (useAnim != UseAnim.EAT && useAnim != UseAnim.DRINK) return;

        FoodConditionState state = consumedStack.get(ModDataComponents.FOOD_STATE.get());
        if (state == null) return;

        FoodConditionConsumptionEffects.applyFrozenConsumptionEffect(player, consumedStack, groupConfig, state.value());
    }
}


